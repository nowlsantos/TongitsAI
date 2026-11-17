package com.nowlsantos.tongitsai.presentation

import androidx.compose.ui.unit.DpOffset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nowlsantos.tongitsai.data.GameData
import com.nowlsantos.tongitsai.domain.fsm.game.GamePhase
import com.nowlsantos.tongitsai.domain.fsm.game.GameStateMachine
import com.nowlsantos.tongitsai.domain.fsm.human.ButtonState
import com.nowlsantos.tongitsai.domain.fsm.human.ButtonType
import com.nowlsantos.tongitsai.domain.model.FamousTrio
import com.nowlsantos.tongitsai.domain.model.GameState
import com.nowlsantos.tongitsai.domain.model.GameUiState
import com.nowlsantos.tongitsai.domain.model.Meld
import com.nowlsantos.tongitsai.domain.model.Player
import com.nowlsantos.tongitsai.domain.model.PlayingCard
import com.nowlsantos.tongitsai.domain.model.enums.PlayerPosition
import com.nowlsantos.tongitsai.domain.model.enums.PlayerType
import com.nowlsantos.tongitsai.domain.usecase.GameHistoryUseCase
import com.nowlsantos.tongitsai.util.Logger
import com.nowlsantos.tongitsai.validator.GameValidator
import com.nowlsantos.tongitsai.validator.ValidationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GameViewModel(
    val gameStateMachine: GameStateMachine,
    private val _gameUiState: MutableStateFlow<GameUiState>,
    private val gameHistoryUseCase: GameHistoryUseCase,
) : ViewModel() {

    private val tag = "GameViewModel"
    val gameUiState: StateFlow<GameUiState> = _gameUiState.asStateFlow()

    /* ---------- EFFECTS ---------- */
    val uiEffect: SharedFlow<UiEffect> = gameStateMachine.uiEffect

    private val _hasDrawnThisTurn = MutableStateFlow(false)
    private val _isActionProcessing = MutableStateFlow(false)

    // Derived reactive StateFlow for human button states
    val buttonState: StateFlow<ButtonState> = combine(
        gameStateMachine.gameState,
        _gameUiState,
        _hasDrawnThisTurn,
        _isActionProcessing
    ) { gs, uiState, hasDrawn, isProcessing ->
        if (isProcessing) {
            return@combine ButtonState.ALL_DISABLED
        }

        val isHumanTurn = gs.gamePhase == GamePhase.PLAYING && gs.currentPlayer.type == PlayerType.HUMAN
        val humanGsPlayer = gs.players.firstOrNull { it.type == PlayerType.HUMAN }
        val humanUiPlayer = uiState.players.firstOrNull { it.type == PlayerType.HUMAN }
        val selectedCards = humanUiPlayer?.selectedCards ?: emptySet()
        val isDealerOpening = isHumanTurn && ((humanGsPlayer?.firstMove == true || humanGsPlayer?.isHitter == true || (humanGsPlayer?.cards?.size ?: 0) >= 13) && humanGsPlayer?.firstMove == true)

        val isActionPhase = isHumanTurn && (hasDrawn || isDealerOpening)

        val drawEnabled = isHumanTurn && !isActionPhase && gs.deck.cards.isNotEmpty()
        val chowEnabled = isHumanTurn && !isActionPhase && gs.lastDiscardedCard != null
        val callEnabled = isHumanTurn && !isActionPhase && (humanGsPlayer?.melds?.isNotEmpty() == true)

        val discardEnabled = isActionPhase && selectedCards.size == 1
        val meldEnabled = isActionPhase && selectedCards.size >= 3
        val sapawEnabled = isActionPhase && selectedCards.isNotEmpty() && uiState.selectedSapawTarget != null

        ButtonState(
            buttons = mapOf(
                ButtonType.DRAW to drawEnabled,
                ButtonType.CHOW to chowEnabled,
                ButtonType.CALL to callEnabled,
                ButtonType.DISCARD to discardEnabled,
                ButtonType.MELD to meldEnabled,
                ButtonType.SAPAW to sapawEnabled,
                ButtonType.SORT_RANK to isHumanTurn,
                ButtonType.SORT_SUIT to isHumanTurn,
                ButtonType.SWAP to isHumanTurn
            ),
            isActionPhase = isActionPhase
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ButtonState.ALL_DISABLED)

    val gameValidator = GameValidator()

    // Expose game data from GameHistoryUseCase
    val gameData: StateFlow<GameData?> = gameHistoryUseCase.gameData

    init {
        observeDomainState()
        observeEffects()
    }

    /* ---------- DOMAIN → UI ---------- */
    private fun observeDomainState() = viewModelScope.launch {
        gameStateMachine.gameState.collect { gs ->
            if (gs.currentPlayer.type != PlayerType.HUMAN) {
                _hasDrawnThisTurn.value = false
            }
            _gameUiState.update { ui ->
                GameUiStateMapper.mapToGameUiState(gs, ui)
            }
        }
    }

    private fun observeEffects() = viewModelScope.launch {
        gameStateMachine.uiEffect.collect { effect ->
            _gameUiState.update { it.copy(uiEffect = effect) }
            _gameUiState.update { it.copy(uiEffect = null) }
        }
    }

    /* ---------- Game Result ---------- */
    fun onShowGameResult() {
        gameHistoryUseCase.onShowGameResult()
    }

    fun dismissResultsDialog() {
        gameStateMachine.emitUiEffect(UiEffect.ResetDialog)
    }

    fun saveGameResults() {
        gameHistoryUseCase.saveGameResults()
    }

    /* ---------- UI → DOMAIN ---------- */
    fun onEvent(input: GameInput) {
        if (_isActionProcessing.value && isActionCommand(input)) {
            Logger.w(tag, "Action ignored: another action is currently processing (anti-race condition lock).")
            return
        }

        viewModelScope.launch {
            when (input) {
                is GameInput.DrawCard -> {
                    _isActionProcessing.value = true
                    _hasDrawnThisTurn.value = true
                    gameStateMachine.handleInput(input)
                }
                is GameInput.MakeCall -> {
                    _isActionProcessing.value = true
                    handlePlayerCall(input.caller)
                }
                is GameInput.ChowCard -> {
                    _isActionProcessing.value = true
                    handleChowCard(
                        input.chowCard,
                        input.disCardPileOffset,
                        input.selectedCards,
                        input.sourceCardOffsets
                    )
                }
                is GameInput.MeldCards -> {
                    _isActionProcessing.value = true
                    handleMeldCards(
                        input.cards,
                        input.sourceCardOffsets,
                        input.targetMeldCenterOffset
                    )
                }
                is GameInput.CardSelection -> { handleSelectCard(input.card, input.offset) }
                is GameInput.SapawCard -> {
                    _isActionProcessing.value = true
                    handleSapawCard(input.sapawCard, input.startOffset, input.sapawTarget)
                }
                is GameInput.ShowValidationError -> {
                    _isActionProcessing.value = false
                    handleValidationError(input.error)
                }
                is GameInput.SelectSapawTarget -> { handleSapawTarget(input.meldSourceOffset, input.playerPosition, input.targetMeldIndex) }
                is GameInput.UpdateLocalCoordinates -> { updatePlayerHandCoordinates(input.position, input.offset) }
                is GameInput.UpdateDeckOffset -> { handleDeckOffset(input.offset) }
                is GameInput.UpdateDiscardPileOffset -> { handleDiscardPileOffset(input.offset) }
                is GameInput.UpdateMeldAreaOffset -> { handleMeldAreaOffsets(input.playerPosition, input.offset)}
                is GameInput.UpdateAllPlayersCardOffsets -> { updateAllPlayersCardOffsets(input.position, input.cardOffsets) }
                is GameInput.SwapCards -> {
                    _isActionProcessing.value = true
                    handleSwapCards(input.cards)
                }
                is GameInput.DiscardCard -> {
                    _isActionProcessing.value = true
                    handleDiscardCard(input.card, input.startOffset, input.targetOffset)
                }
                is GameInput.CardAnimationFinished -> {
                    _isActionProcessing.value = false
                    gameStateMachine.handleInput(input)
                }

                // Other inputs are handled by the GameStateMachine directly
                else -> gameStateMachine.handleInput(input)
            }
        }
    }

    private fun isActionCommand(input: GameInput): Boolean {
        return input is GameInput.DrawCard ||
                input is GameInput.MakeCall ||
                input is GameInput.ChowCard ||
                input is GameInput.MeldCards ||
                input is GameInput.SapawCard ||
                input is GameInput.SwapCards ||
                input is GameInput.DiscardCard
    }

    private fun handleSelectCard(card: PlayingCard, cardOffset: DpOffset) {
        CardSelectionHandler.toggleCardSelection(_gameUiState, card, cardOffset)
    }

    private fun updatePlayerHandCoordinates(position: PlayerPosition, offset: DpOffset) {
        _gameUiState.update { currentUiState ->
            currentUiState.copy(
                localCoordinate = when (position) {
                    PlayerPosition.BOTTOM -> currentUiState.localCoordinate.copy(bottomHandOffset = offset)
                    PlayerPosition.RIGHT -> currentUiState.localCoordinate.copy(rightHandOffset = offset)
                    PlayerPosition.LEFT -> currentUiState.localCoordinate.copy(leftHandOffset = offset)
                }
            )
        }
    }

    private fun handleDeckOffset(offset: DpOffset) {
        _gameUiState.update {
            it.copy(
                localCoordinate = it.localCoordinate.copy(
                    deckOffset = offset
                )
            )
        }
    }

    private fun handleDiscardPileOffset(offset: DpOffset) {
        _gameUiState.update { it.copy(
            localCoordinate = it.localCoordinate.copy(
                discardPileOffset = offset
            ))
        }
    }

    private fun handleMeldAreaOffsets(playerPosition: PlayerPosition, offset: DpOffset) {
        _gameUiState.update { currentState ->
            currentState.copy(
                meldAreaCoordinates = currentState.meldAreaCoordinates.toMutableMap().apply {
                    put(playerPosition, offset)
                }
            )
        }
        //Logger.d(tag, "Position:: $playerPosition | MeldAreaOffset:: $offset")
    }

    private fun updateAllPlayersCardOffsets(position: PlayerPosition, cardOffsets: Map<PlayingCard, DpOffset>) {
        _gameUiState.update { currentState ->
            currentState.copy(
                localCoordinate = currentState.localCoordinate.copy(
                    allPlayersCardOffsets = currentState.localCoordinate.allPlayersCardOffsets.toMutableMap().apply {
                        put(position, cardOffsets)
                    }
                )
            )
        }
    }

    private fun handlePlayerCall(caller: Player) {
        when (val validationResult = gameValidator.validateCall(caller)) {
            is ValidationResult.Error -> {
                handleValidationError(validationResult.message)
            }
            is ValidationResult.Success -> {
                gameStateMachine.handleInput(
                    GameInput.MakeCall(caller)
                )
            }
            else -> {
                // Handle other cases if needed
                handleValidationError("Unexpected validation result")
            }
        }
    }

    private fun handleChowCard(
        chowCard: PlayingCard,
        discardPileOffset: DpOffset,
        selectedCards: List<PlayingCard>,
        sourceCardOffsets: Map<PlayingCard, DpOffset>) {

        when (val validationResult = gameValidator.validateChow(_gameUiState.value, selectedCards, chowCard)) {
            is ValidationResult.Error -> {
                handleValidationError(validationResult.message)
            }
            is ValidationResult.Success -> {
                _hasDrawnThisTurn.value = true
                gameStateMachine.handleInput(
                    GameInput.ChowCard(
                        chowCard,
                        discardPileOffset,
                        selectedCards,
                        sourceCardOffsets,
                    )
                )
            }
            else -> {
                // Handle other cases if needed
                handleValidationError("Unexpected validation result")
            }
        }
    }

    private fun handleMeldCards(
        selectedCards: List<PlayingCard>,
        sourceCardOffsets: Map<PlayingCard, DpOffset>,
        targetMeldCenterOffset: DpOffset) {

        var potentialMeld: Meld
        try {
            val humanPlayer = _gameUiState.value.players.firstOrNull { it.type == PlayerType.HUMAN }
            potentialMeld = Meld(cards = selectedCards, owner = humanPlayer?.id)
            Logger.d(tag, "PotentialMeldOwner:: ${potentialMeld.owner}")
        } catch (e: IllegalArgumentException) {
            Logger.e(tag, "Error creating potential meld: ${e.message}")
            handleValidationError("${e.message}")
            return
        }

        when (val validationResult = gameValidator.validateMeld(_gameUiState.value, potentialMeld)) {
            is ValidationResult.Error -> {
                handleValidationError(validationResult.message)
            }
            is ValidationResult.Success -> {
                gameStateMachine.handleInput(
                    GameInput.MeldCards(
                        selectedCards,
                        sourceCardOffsets,
                        targetMeldCenterOffset
                    )
                )
            }
            else -> {
                // Handle other cases if needed
                handleValidationError("Unexpected validation result")
            }
        }
    }

    private fun handleSapawTarget(meldSourceOffset: DpOffset, playerPosition: PlayerPosition, targetMeldIndex: Int) {
        _gameUiState.update { currentState ->
            val meldTarget = if (currentState.selectedSapawTarget?.first == meldSourceOffset &&
                currentState.selectedSapawTarget.second == playerPosition &&
                currentState.selectedSapawTarget.third == targetMeldIndex) {
                //Logger.d(tag, "SelectSapawTarget: Deselecting meld: $meldSourceOffset,  $playerPosition, MeldIndex: $targetMeldIndex")
                null
            } else {
                //Logger.d(tag, "SelectSapawTarget: Selecting meld: $meldSourceOffset, $playerPosition, MeldIndex: $targetMeldIndex")
                Triple(meldSourceOffset, playerPosition, targetMeldIndex)
            }
            currentState.copy(
                selectedSapawTarget = meldTarget
            )
        }
        //Logger.d(tag, "SelectSapawTarget: ${_gameUiState.value.selectedSapawTarget}")
    }

    private fun handleSapawCard(
        sapawCard: PlayingCard,
        startOffset: DpOffset,
        sapawTarget: Triple<DpOffset, PlayerPosition, Int>
    ) {
        val targetPlayer = _gameUiState.value.players.firstOrNull { it.position == sapawTarget.second }
        targetPlayer?.let {
            val targetMeld = it.melds[sapawTarget.third]
            when (val validationResult = gameValidator.validateSapaw(_gameUiState.value, sapawCard, targetMeld)) {
                is ValidationResult.Error -> {
                    handleValidationError(validationResult.message)
                }
                is ValidationResult.Success -> {
                    gameStateMachine.handleInput(
                        GameInput.SapawCard(sapawCard, startOffset, sapawTarget)
                    )
                }
                else -> {
                    // Handle other cases if needed
                    handleValidationError("Unexpected validation result")
                }
            }
        }
    }

    private fun handleValidationError(error: String) {
        _isActionProcessing.value = false
        gameStateMachine.emitUiEffect(UiEffect.ShowError(error))
    }

    private fun handleDiscardCard(discard: PlayingCard, cardOffset: DpOffset, pileOffset: DpOffset) {
        val gameState = gameStateMachine.gameState.value
        val currentPlayer = _gameUiState.value.players.firstOrNull { it.type == PlayerType.HUMAN }
        currentPlayer?.let {
            when (val validationResult = gameValidator.validateDiscardCard(gameState, discard, it.selectedCards.toList())) {
                is ValidationResult.Error -> {
                    handleValidationError(validationResult.message)
                }
                is ValidationResult.Success -> {
                    gameStateMachine.handleInput(
                        GameInput.DiscardCard(discard, cardOffset, pileOffset)
                    )
                }
                else -> {
                    // Handle other cases if needed
                    handleValidationError("Unexpected validation result")
                }
            }
        }
    }

    // This function now receives a Pair, simplifying the logic.
    private fun handleSwapCards(cardsToSwap: Pair<PlayingCard, PlayingCard>) {
        val currentPlayer = _gameUiState.value.players.firstOrNull { it.type == PlayerType.HUMAN }
        currentPlayer?.let {
            when (val validationResult = gameValidator.validateSwap(it)) {
                is ValidationResult.Error -> {
                    handleValidationError(validationResult.message)
                }
                is ValidationResult.Success -> {
                    gameStateMachine.handleInput(
                        GameInput.SwapCards(cards = cardsToSwap)
                    )
                }
                else -> {
                    // Handle other cases if needed
                    handleValidationError("Unexpected validation result")
                }
            }

        }
    }

    fun setTrio(trio: FamousTrio) {
        gameStateMachine.setTrio(trio)
    }

    fun restartGame() {
        gameStateMachine.updateState { GameState.initial() }
        gameStateMachine.transitionTo(GamePhase.SETUP)
    }
}