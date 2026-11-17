package com.nowlsantos.tongitsai.domain.fsm.game

import com.nowlsantos.tongitsai.domain.ai.AiHandRepository
import com.nowlsantos.tongitsai.domain.fsm.animation.GameAnimationState
import com.nowlsantos.tongitsai.domain.model.DiscardPile
import com.nowlsantos.tongitsai.domain.model.FamousTrio
import com.nowlsantos.tongitsai.domain.model.GameState
import com.nowlsantos.tongitsai.domain.model.GameUiState
import com.nowlsantos.tongitsai.domain.model.Meld
import com.nowlsantos.tongitsai.domain.model.Player
import com.nowlsantos.tongitsai.domain.model.PlayingCard
import com.nowlsantos.tongitsai.domain.model.enums.ChallengerType
import com.nowlsantos.tongitsai.domain.model.enums.MoveType
import com.nowlsantos.tongitsai.domain.model.enums.PlayerPosition
import com.nowlsantos.tongitsai.domain.model.enums.PlayerType
import com.nowlsantos.tongitsai.presentation.GameInput
import com.nowlsantos.tongitsai.presentation.PaymentProcessor
import com.nowlsantos.tongitsai.presentation.UiEffect
import com.nowlsantos.tongitsai.util.GameConstants
import com.nowlsantos.tongitsai.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

data class PlayerDecision(
    val player: Player,
    val moveType: MoveType,
    val meld: Meld? = null
)

// Data class to encapsulate Tongits state
data class TongitsState(
    var isTongits: Boolean = false,
    val preSecretMelds: MutableList<Meld> = mutableListOf()
) {

    fun preMeldTongits(): Meld? {
        return if (preSecretMelds.isNotEmpty()) preSecretMelds.removeAt(0) else null
    }

    fun reset() {
        isTongits = false
        preSecretMelds.clear()
    }
}

/**
 * Manages the overall game state and transitions between different game phases.
 * Acts as a finite state machine (FSM) by delegating input handling
 * to the currently active GamePhaseState.
 */
class GameStateMachine(
    internal val externalScope: CoroutineScope,
    val uiState: StateFlow<GameUiState>,
) {
    private val tag = "GameStateMachine"

    // MutableStateFlow to hold the current game state.
    private val _gameState = MutableStateFlow(GameState.initial())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    // MutableSharedFlow for one-time UI effects (e.g., showing messages, animations).
    private val _uiEffect = MutableSharedFlow<UiEffect>()
    val uiEffect: SharedFlow<UiEffect> = _uiEffect.asSharedFlow()

    // Initialize it with the current value of the injected uiState
    private val _currentUiState: MutableStateFlow<GameUiState> = MutableStateFlow(uiState.value)

    // You can expose this as a StateFlow for other classes to collect/observe if needed
    val currentUiState: StateFlow<GameUiState> = _currentUiState.asStateFlow()

    private val _launchPlayerDecision = MutableSharedFlow<PlayerDecision>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val launchPlayerDecision: SharedFlow<PlayerDecision> = _launchPlayerDecision.asSharedFlow()

    internal val gameAnimationState: GameAnimationState = GameAnimationState(
        externalScope = externalScope,
        gameState = _gameState,
        uiState = _currentUiState,
        emitUiEffect = { effect ->
            externalScope.launch { _uiEffect.emit(effect) }
        },
        handleGameInput = { input -> handleInput(input) },
        onDiscardCompleteCallback = this::onDiscardComplete,
        onDrawCompleteCallback = this::onDrawCardComplete,
        onMeldCompleteCallback = this::onMeldComplete,
        onSapawCompleteCallback = this::onSapawComplete,
    )

    // Internal map to hold instances of each GamePhaseState for easy access and reusability.
    private val setupState: SetupState by lazy { SetupState() }
    private val dealState: DealState by lazy { DealState() }
    private val playState: PlayState by lazy { PlayState(gameAnimationState) }
    private val callChallengeState: CallChallengeState by lazy { CallChallengeState() }
    private val revealState: RevealState by lazy { RevealState() }
    private val roundEndState: RoundEndState by lazy { RoundEndState() }
    private val gameOverState: GameOverState by lazy { GameOverState() }

    // Private property to hold the currently active GamePhaseState object.
    private var _currentPhaseState: GamePhaseState = setupState

    // Initialize the round and payment processor
    val paymentProcessor = PaymentProcessor(this@GameStateMachine)
    var onRoundPaymentProcessed: ((String) -> Unit)? = null

    // Assuming you have a TongitsState instance for each player
    private val tongitsStateMap = mutableMapOf<String, TongitsState>()

    init {
        Logger.d(tag, "GameStateMachine initialized. Starting in SetupPhase.")
        _currentPhaseState.onEnter(this@GameStateMachine)

        externalScope.launch {
            uiState.collectLatest { newState ->
                _currentUiState.value = newState // Update our internal state
            }
        }
    }

    fun broadcastDecision(targetPlayer: Player, moveType: MoveType, meld: Meld? = null) {
        externalScope.launch {
            val freshPlayer = _gameState.value.players.find { it.id == targetPlayer.id } ?: targetPlayer
            val decision = PlayerDecision(freshPlayer, moveType, meld)
            _launchPlayerDecision.emit(decision)
        }
    }

    private fun onDrawCardComplete(drawnCard: PlayingCard, targetPlayer: Player) {
        _gameState.update { currentGameState ->
            Logger.d(tag, "Draw animation finished. Adding $drawnCard to player : ${targetPlayer.name}'s hand.")
            val updatedPlayers = currentGameState.players.map { player ->
                if (player.id == targetPlayer.id) {
                    // --- This block is to signal the sapaw display
                    val updatedMelds = player.melds.map { meld ->
                        if (meld.hasSapaw) {
                            meld.copy(hasSapaw = false) // Return a new Meld object with hasSapaw set to false
                        } else {
                            meld // Otherwise, return the original meld object
                        }
                    }
                    //---------------------------------------------
                    player.cards.sortedBy { it.rank.ordinal }
                    // Return a new Player object with the newly created list of melds
                    player.copy(
                        cards = (player.cards + drawnCard).distinctBy { c -> c.rank to c.suit },
                        melds = updatedMelds
                    )
                } else {
                    player
                }
            }
            currentGameState.copy(players = updatedPlayers)
        }

        val isTongits = checkPlayerForTongits(targetPlayer.id)
        if (isTongits) {
            handleTongitsWin(targetPlayer)
            return
        }
        broadcastDecision(targetPlayer, MoveType.GET_CARD)
    }

    private fun onMeldComplete(meld: Meld, targetPlayer: Player) {
        Logger.d(tag, "Meld animation for ${targetPlayer.name} completed.")

        // Find the player in the current state to get their latest data.
        val currentPlayerState = _gameState.value.players.find { it.id == targetPlayer.id }
        if (currentPlayerState == null) {
            Logger.e(tag, "onMeldAnimationComplete: Player not found in game state.")
            return
        }

        // Get the cards from the meld that need to be removed from the player's hand.
        val cardsToRemove = meld.cards.toSet()

        _gameState.update { currentState ->
            currentState.copy(
                players = currentState.players.map { p ->
                    if (p.id == targetPlayer.id) {
                        val updatedMelds = p.melds.map { existingMeld ->
                            if (existingMeld.hasSapaw) existingMeld.copy(hasSapaw = false) else existingMeld
                        }
                        val newMeld = Meld(cards = meld.cards, owner = targetPlayer.id)
                        val finalMelds = (updatedMelds + newMeld).distinctBy { m -> m.cards.map { c -> c.rank to c.suit }.toSet() }
                        p.copy(
                            cards = (p.cards - cardsToRemove).distinctBy { c -> c.rank to c.suit },
                            melds = finalMelds
                        )
                    } else {
                        p
                    }
                },
            )
        }

        val isTongitsMeld = checkPlayerForTongits(targetPlayer.id)
        if (isTongitsMeld) {
            handleTongitsWin(targetPlayer)
            return
        }

        if (targetPlayer.type == PlayerType.AI) {
            broadcastDecision(targetPlayer, MoveType.MELD_CARD)
        } else {
            if (targetPlayer.cards.isNotEmpty()) broadcastDecision(targetPlayer, MoveType.GET_CARD)
            else checkPostWinOrBroadCastDecision(targetPlayer.id, MoveType.MELD_CARD)
        }
    }

    private fun onSapawComplete(sapawCard: PlayingCard, meldIndex: Int, position: PlayerPosition, targetPlayer: Player) {
        _gameState.update { currentGameState ->
            val updatedPlayers = currentGameState.players.map { player ->
                if (player.position == position) {
                    // This is the player whose meld is receiving the sapaw card
                    val updatedMelds = player.melds.toMutableList()
                    if (meldIndex >= 0 && meldIndex <= updatedMelds.size) {
                        val targetMeld = updatedMelds[meldIndex]

                        // Create a new list of cards for the updated meld, adding the sapaw card
                        val newMeldCards = targetMeld.cards + sapawCard

                        // Create a new Meld object with the updated cards (ensuring it remains valid) and sort
                        val updateMeld = targetMeld.copy(
                            cards = newMeldCards.sortedWith(
                                compareBy({ it.suit.ordinal }, { it.rank.sequentialValue }),
                            ),
                            hasSapaw = true
                        )

                        // Replace the old meld with the updated meld in the list
                        updatedMelds[meldIndex] = updateMeld
                    } else {
                        Logger.e(tag, "SapawCardAnimated: Invalid targetMeldIndex $meldIndex for player ${targetPlayer.name}.")
                    }
                    Logger.d(tag, "Game state updated: Sapawcard added ${player.name}'s meld.")
                    // Return a new Player object with the updated melds
                    player.copy(melds = updatedMelds)
                } else {
                    player // Return other players as they are, without modification
                }
            }
            // Return a new GameState object with the updated players list
            currentGameState.copy(players = updatedPlayers)
        }

        val isTongitsSapaw = checkPlayerForTongits(targetPlayer.id)
        if (isTongitsSapaw) {
            handleTongitsWin(targetPlayer)
            return
        }

        if (targetPlayer.type == PlayerType.AI) {
            broadcastDecision(targetPlayer, MoveType.MELD_CARD)
        } else {
            if (targetPlayer.cards.isNotEmpty()) broadcastDecision(targetPlayer, MoveType.MELD_CARD)
            else checkPostWinOrBroadCastDecision(targetPlayer.id, MoveType.SAPAW_CARD)
        }
    }

    fun onDiscardComplete(discardedCard: PlayingCard, targetPlayer: Player) {
        // Perform a single, atomic state update to add the card to the discard pile.
        _gameState.update { currentState ->
            val discardPileCards = currentState.discardPile.cards.toMutableList().apply { add(discardedCard) }
            val nextPlayerIndex = (currentState.currentPlayerIndex + 1) % currentState.players.size

            currentState.copy(
                lastDiscardedCard = discardedCard,
                discardPile = currentState.discardPile.copy(cards = discardPileCards),
                currentPlayerIndex = nextPlayerIndex,
            )
        }

        val nextPlayer = _gameState.value.currentPlayer

        val isTongitsDiscard = checkPlayerForTongits(targetPlayer.id)
        if (isTongitsDiscard) {
            Logger.d(tag, "${targetPlayer.name} achieved Tongits on discard! Ending round.")
            handleTongitsWin(targetPlayer)
            return
        }

        val isHandEmpty = targetPlayer.cards.isEmpty()
        val isDeckEmpty = _gameState.value.deck.cards.isEmpty()

        when {
            isHandEmpty -> {
                Logger.d(tag, "Player ${targetPlayer.name}'s hand is empty. Transitioning to ROUND_END.")
                _gameState.update { state ->
                    state.copy(
                        roundWinnerId = targetPlayer.id,
                        gameWinnerId = targetPlayer.id,
                        revealType = RevealType.TONGITS,
                        isReveal = true
                    )
                }
                handleInput(GameInput.RoundEnd)
            }
            isDeckEmpty -> {
                Logger.d(tag, "Deck is empty. Transitioning to REVEAL phase.")
                updateState { currentState ->
                    currentState.copy(
                        revealType = RevealType.NORMAL,
                        isReveal = true,
                        lastPlayerToDiscardId = targetPlayer.id
                    )
                }
                val allPlayerIds = _gameState.value.players
                    .filter { p -> p.melds.isNotEmpty() }
                    .map { it.id }
                handleInput(GameInput.RoundReveal(playerIds = allPlayerIds))
            }
            else -> {
                Logger.d(tag, "Broadcasting next turn to player: ${nextPlayer.name}")
                broadcastDecision(nextPlayer, MoveType.NEXT_TURN)
            }
        }
    }

    // --- Public API for GameViewModel to interact with ---
    fun setTrio(trio: FamousTrio) {
        _gameState.update { currentState ->
            val updatedPlayers = currentState.players.map { player ->
                when (player.id) {
                    GameConstants.ID_HUMAN -> player.copy(name = trio.humanName)
                    GameConstants.ID_AI_1 -> player.copy(name = trio.ai1Name)
                    GameConstants.ID_AI_2 -> player.copy(name = trio.ai2Name)
                    else -> player
                }
            }
            currentState.copy(players = updatedPlayers)
        }
    }

    fun handleInput(input: GameInput) {
        externalScope.launch {
            // Delegate input handling to the current phase state object
            _currentPhaseState.handleInput(this@GameStateMachine, input)
        }
    }

    fun emitUiEffect(effect: UiEffect) {
        externalScope.launch {
            _uiEffect.emit(effect)
        }
    }

    /**
     * Allows GamePhaseState implementations to request a state update.
     * @param updateBlock A lambda function that takes the current GameState and returns the new GameState.
     */
    fun updateState(updateBlock: (GameState) -> GameState) {
        _gameState.update(updateBlock)
    }

    // --- State Transition Logic ---
    fun transitionTo(newPhase: GamePhase) {
        if (_gameState.value.gamePhase != newPhase) {
            Logger.d(tag, "Game phase transition: ${_gameState.value.gamePhase} -> $newPhase")

            // Call onExit for the old phase state object
            _currentPhaseState.onExit(this)

            // This ensures that any observers (like gameState.collect) see the new phase immediately.
            _gameState.update { it.copy(gamePhase = newPhase) }

            // --- ADD THIS NEW LOG IMMEDIATELY AFTER THE UPDATE ---
            Logger.d(tag, "GameStateMachine: _gameState updated to phase: ${_gameState.value.gamePhase}")

            // Get the new phase state object
            val newPhaseState = getGamePhaseState(newPhase)

            // Update the _currentPhaseState to the new state object
            _currentPhaseState = newPhaseState

            // Call onEnter for the new phase state object
            // At this point, _gameState.value.gamePhase should already be the newPhase
            _currentPhaseState.onEnter(this)

        } else {
            // Log a warning if attempting to transition to the same phase
            Logger.w(tag, "Attempted to transition to the same phase: $newPhase. Current phase is already ${_gameState.value.gamePhase}.")
        }
    }

    private fun getGamePhaseState(phase: GamePhase): GamePhaseState {
        return when (phase) {
            GamePhase.SETUP -> setupState
            GamePhase.DEALING -> dealState
            GamePhase.PLAYING -> playState
            GamePhase.CALL_CHALLENGE -> callChallengeState
            GamePhase.REVEAL -> revealState
            GamePhase.ROUND_END -> roundEndState
            GamePhase.GAME_OVER -> gameOverState
        }
    }

    fun setupNewGame() {
        val allPlayers = _gameState.value.players
        Logger.d(tag, "Setting up new game (deck, round number, and players)...")

        tongitsStateMap.clear()
        AiHandRepository.resetAll(allPlayers)

        val currentRoundNumber = _gameState.value.roundNumber
        val wonTwoHits = _gameState.value.lastRoundWonTwoHits
        val lastRoundWinnerId = _gameState.value.roundWinnerId ?: _gameState.value.lastRoundWinnerId
        val nextRoundNumber = currentRoundNumber + 1

        // Winner of previous round (or Luke on Round 1) is both the opening player (13 cards) and hitter
        val hitterPlayer: Player = if (nextRoundNumber == 1) {
            allPlayers[0]
        } else {
            allPlayers.firstOrNull { it.id == lastRoundWinnerId } ?: allPlayers[0]
        }

        // Logger.d(tag, "Player: ${allPlayers[currentRoundNumber].id} | HitterId: ${hitterPlayer.id}")

        val totalFee = GameConstants.CENTER_FEE * allPlayers.size
        val newCenterFee = _gameState.value.centerFee + totalFee

        val updatedPlayers = allPlayers.map { player ->
            val startingBalance = if (player.balance < GameConstants.CENTER_FEE) GameConstants.BALANCE else player.balance
            val isThisHitterPlayer = player.id == hitterPlayer.id
            val isThisHitterIcon = isThisHitterPlayer && (nextRoundNumber >= 2) && !wonTwoHits

            player.copy(
                cards = emptyList(),
                selectedCards = emptySet(),
                melds = emptyList(),
                balance = (startingBalance - GameConstants.CENTER_FEE).coerceAtLeast(0),
                score = 0,
                challengerType = ChallengerType.NONE,
                isHitter = isThisHitterIcon,
                firstMove = isThisHitterPlayer
            )
        }

        _gameState.update { currentState ->
            currentState.copy(
                deck = currentState.deck.resetAndShuffle(),
                discardPile = DiscardPile(),
                roundNumber = nextRoundNumber,
                centerFee = newCenterFee,
                players = updatedPlayers,
                currentPlayerIndex = updatedPlayers.indexOfFirst { it.id == hitterPlayer.id },
                lastDiscardedCard = null,
                lastRoundWinnerId = currentState.roundWinnerId ?: currentState.lastRoundWinnerId,
                roundWinnerId = null,
                gameWinnerId = null,
                callerId = null,
                revealType = RevealType.NONE,
                isReveal = false,
                isGameOver = false,
                lastPlayerToDiscardId = null,
                lastRoundWonTwoHits = false
            )
        }
    }

    private fun handleTongitsWin(targetPlayer: Player) {
        Logger.d(tag, "${targetPlayer.name} achieved Tongits! Revealing cards and transitioning to ROUND_END.")
        _gameState.update { state ->
            state.copy(
                roundWinnerId = targetPlayer.id,
                gameWinnerId = targetPlayer.id,
                revealType = RevealType.TONGITS,
                isReveal = true
            )
        }
        emitUiEffect(UiEffect.InitialMessage("${targetPlayer.name} achieves Tongits!\nRevealing cards..."))
        externalScope.launch {
            delay(1500.milliseconds)
            emitUiEffect(UiEffect.ResetDialog)
            transitionTo(GamePhase.ROUND_END)
        }
    }

    private fun checkPlayerForTongits(playerId: String): Boolean {
        val currentState = _gameState.value
        val player = currentState.players.find { it.id == playerId } ?: return false

        val secretMelds = AiHandRepository.getSecretMelds(player)
        val secretCards = secretMelds.flatMap { it.cards }.toSet()

        val isHandEmpty = player.cards.isEmpty()
        val allCardsAreSecret = player.cards.isNotEmpty() && player.cards.all { it in secretCards }
        val oneCardToDiscardAndRestSecret = player.cards.size == 1 && (player.cards.toSet() - secretCards).isEmpty()

        val isTongits = isHandEmpty || allCardsAreSecret || oneCardToDiscardAndRestSecret

        if (isTongits) {
            Logger.d(tag, "${player.name} has achieved Tongits (Empty Hand or Secret Melds)! Secret melds: $secretMelds, Hand: ${player.cards}")
        }

        return isTongits
    }

    fun checkPostWinOrBroadCastDecision(playerId: String, moveType: MoveType) {
        val player = _gameState.value.players.find { it.id == playerId }

        player?.let {
            val isHandEmpty = player.cards.isEmpty()
            val isDeckEmpty = _gameState.value.deck.cards.isEmpty()

            when {
                isHandEmpty -> {
                    Logger.d(tag, "Player ${it.name}'s hand is empty. Transitioning to ROUND_END.")
                    _gameState.update { state ->
                        state.copy(
                            roundWinnerId = it.id,
                            gameWinnerId = it.id,
                            revealType = RevealType.TONGITS,
                            isReveal = true
                        )
                    }
                    val currentState = _gameState.value
                    Logger.d(tag, "Winner:: ${currentState.roundWinnerId} | RevealType: ${currentState.revealType} | IsReveal: ${currentState.isReveal}")
                    handleInput(GameInput.RoundEnd)
                }

                isDeckEmpty -> {
                    Logger.d(tag, "Deck is empty. Transitioning to REVEAL phase to score hands.")
                    updateState { currentState ->
                        currentState.copy(
                            revealType = RevealType.NORMAL,
                            isReveal = true,
                            lastPlayerToDiscardId = player.id
                        )
                    }

                    // Get all players and pass their IDs who has a meld only to the RevealState
                    val allPlayerIds = _gameState.value.players
                        .filter { p -> p.melds.isNotEmpty() }
                        .map { player -> player.id }

                    // Delegate to PlayState the transition from PlayState -> RevealState
                    handleInput(GameInput.RoundReveal(playerIds = allPlayerIds))
                }
                else -> {
                    // No phase change is needed, but you can add a broadcast or other logic here if necessary.
                    broadcastDecision(it, moveType)
                }
            }
        }
    }
}