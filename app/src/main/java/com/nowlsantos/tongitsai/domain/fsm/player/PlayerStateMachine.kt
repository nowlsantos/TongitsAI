package com.nowlsantos.tongitsai.domain.fsm.player

import com.nowlsantos.tongitsai.domain.ai.AiHandRepository
import com.nowlsantos.tongitsai.domain.ai.AiStrategy
import com.nowlsantos.tongitsai.domain.fsm.game.GameStateMachine
import com.nowlsantos.tongitsai.domain.model.Player
import com.nowlsantos.tongitsai.domain.model.PlayingCard
import com.nowlsantos.tongitsai.domain.model.enums.MoveType
import com.nowlsantos.tongitsai.domain.model.enums.PlayerType
import com.nowlsantos.tongitsai.presentation.GameInput
import com.nowlsantos.tongitsai.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Manages the state and actions of a single AI player.
 * It's a state machine for an individual player's turn.
 */

class PlayerStateMachine(
    internal val player: Player, // Player associated with this state machine
    internal val externalScope: CoroutineScope,
    private val strategy: AiStrategy, // Correct: Now using your AiStrategy
    private val gameStateMachine: GameStateMachine,
    val aiHandRepository: AiHandRepository,
) {
    private val tag = "PlayerStateMachine-${player.name}" // Use player.name for the tag

    // New: Internal StateFlow to hold the LATEST version of THIS player's data
    internal val _latestPlayerState = MutableStateFlow<Player?>(null) // Make it internal for ReadyState
    val latestPlayerState: StateFlow<Player?> = _latestPlayerState.asStateFlow() // Expose as an immutable flow

    private val _currentPhase = MutableStateFlow(PlayerPhase.IDLE)
    val currentPhase: StateFlow<PlayerPhase> = _currentPhase.asStateFlow()

    private var _currentPlayerPhaseState: PlayerPhaseState

    // Initialize all PlayerPhaseState instances using by lazy, similar to GameStateMachine.
    private val idleState: IdleState by lazy { IdleState() }
    private val readyState: ReadyState by lazy { ReadyState(strategy) }
    private val callState: CallState by lazy { CallState(strategy) }
    private val getState: GetState by lazy { GetState(strategy) }
    private val chowState: ChowState by lazy { ChowState(strategy) }
    private val meldState: MeldState by lazy { MeldState(strategy) }
    private val sapawState: SapawState by lazy { SapawState(strategy) }
    private val discardState: DiscardState by lazy { DiscardState(strategy) }

    init {
        // Ensure _currentPlayerPhaseState is set to the correct initial state object
        _currentPlayerPhaseState = getPlayerPhaseState(_currentPhase.value) // Should be IdleState initially

        collectLatestGameState()
        collectPlayerMoveSignals()
    }

    private fun collectLatestGameState() {
        // Launch a coroutine to continuously observe the GameState for this player's updates
        externalScope.launch {
            gameStateMachine.gameState.collect { latestGameState ->
                val updatedPlayer = latestGameState.players.firstOrNull { it.id == player.id }
                if (updatedPlayer != null) {
                    _latestPlayerState.value = updatedPlayer
                }
            }
        }
    }

    /**
     * Listens for the specific command from GameStateMachine to launch the AI's turn.
     * This is the core logic that connects the Game's draw animation completion to the AI's decision.
     */
    private fun collectPlayerMoveSignals() {
        externalScope.launch {
            // Subscribe to the launchAiTurnDecision flow
            gameStateMachine.launchPlayerDecision.collect { playerDecisionMap ->
                // Unpack the map to get the player and the move type
                val currentPlayer = playerDecisionMap.player
                val moveType = playerDecisionMap.moveType
                val meld = playerDecisionMap.meld
                val isAiPlayerTurn = (player.id == currentPlayer.id) && (currentPlayer.type == PlayerType.AI)
                if (!isAiPlayerTurn) {
                    Logger.d(tag, "PSM :: Received launchPlayerTurnDecision Ignoring for player: ${player.name}. CurrentPlayer:: ${currentPlayer.name}")
                    return@collect
                }

                Logger.d(tag, "Received signal. Triggering turn for AI: ${currentPlayer.name} | HandSize: ${currentPlayer.cards.size}")
                Logger.d(tag, "CurrentPhase:: ${_currentPhase.value} | MoveType:: $moveType")
                if (moveType == MoveType.NEXT_TURN) {
                    transitionToPlayerPhase(PlayerPhase.IDLE)
                    transitionToPlayerPhase(PlayerPhase.READY)
                    return@collect
                }

                if ((_currentPhase.value == PlayerPhase.MELD ||
                     currentPhase.value == PlayerPhase.SAPAW ||
                     currentPhase.value == PlayerPhase.GET) &&
                     moveType == MoveType.PRE_TONGITS) {
                     _currentPlayerPhaseState.processPreMeldTongits(gameStateMachine, this@PlayerStateMachine, currentPlayer, meld)
                     return@collect
                }

                when(_currentPhase.value) {
                    PlayerPhase.GET -> {
                        if (moveType == MoveType.GET_CARD) {
                            transitionToPlayerPhase(PlayerPhase.MELD)
                        }
                    }
                    PlayerPhase.CHOW -> {
                        if (moveType == MoveType.GET_CARD || moveType == MoveType.MELD_CARD) {
                            transitionToPlayerPhase(PlayerPhase.SAPAW)
                        }
                    }
                    PlayerPhase.MELD -> {
                        if (moveType == MoveType.MELD_CARD) {
                            transitionToPlayerPhase(PlayerPhase.SAPAW)
                        }
                    }
                    PlayerPhase.SAPAW -> {
                        if (moveType == MoveType.MELD_CARD || moveType == MoveType.SAPAW_CARD) {
                            transitionToPlayerPhase(PlayerPhase.DISCARD)
                        }
                    }
                    PlayerPhase.DISCARD -> {
                        if (moveType == MoveType.DISCARD_CARD) {
                            transitionToPlayerPhase(PlayerPhase.IDLE)
                        }
                    }
                    else -> { }
                }
            }
        }
    }

    private fun getPlayerPhaseState(phase: PlayerPhase): PlayerPhaseState {
        return when (phase) {
            PlayerPhase.IDLE -> idleState
            PlayerPhase.READY -> readyState
            PlayerPhase.CALL -> callState
            PlayerPhase.GET -> getState
            PlayerPhase.CHOW -> chowState
            PlayerPhase.MELD -> meldState
            PlayerPhase.SAPAW -> sapawState
            PlayerPhase.DISCARD -> discardState
        }
    }

    fun transitionToPlayerPhase(newPlayerPhase: PlayerPhase) {
        val freshPlayer = gameStateMachine.gameState.value.players.firstOrNull { it.id == player.id } ?: player
        _latestPlayerState.value = freshPlayer

        if (_currentPhase.value != newPlayerPhase) {
            _currentPlayerPhaseState.onExit(this, gameStateMachine)

            _currentPhase.value = newPlayerPhase

            val newPhaseStateObject = getPlayerPhaseState(newPlayerPhase)

            _currentPlayerPhaseState = newPhaseStateObject

            _currentPlayerPhaseState.onEnter(this, gameStateMachine, freshPlayer)
        } else {
            Logger.w(tag, "AI ${player.name} attempted to transition to the same player phase: $newPlayerPhase.")
        }
    }

    /**
     * Sorts the AI player's hand and updates the game state.
     * This method is now called from ReadyState.
     */
    internal fun sortHand() {
        val currentPlayerData = _latestPlayerState.value
        if (currentPlayerData == null) {
            Logger.e(tag, "Cannot sort hand for AI ${player.name}: player data is null.")
            return
        }

        val secretMelds = AiHandRepository.getSecretMelds(currentPlayerData)

        val allCards = currentPlayerData.cards.toMutableList()
        val sortedHand = mutableListOf<PlayingCard>()

        secretMelds.forEach { meld ->
            meld.cards.forEach { card ->
                val found = allCards.firstOrNull { it.rank == card.rank && it.suit == card.suit }
                if (found != null) {
                    sortedHand.add(found)
                    allCards.remove(found)
                }
            }
        }
        sortedHand.addAll(allCards.sortedWith(compareBy({ it.suit.ordinal }, { it.rank.sequentialValue })))

        val updatedPlayerWithSortedHand = currentPlayerData.copy(cards = sortedHand)

        gameStateMachine.updateState { currentState ->
            currentState.copy(
                players = currentState.players.map { p ->
                    if (p.id == updatedPlayerWithSortedHand.id) updatedPlayerWithSortedHand else p
                }
            )
        }
        Logger.d(tag, "AI ${player.name}'s hand sorted with secret melds grouped. New hand size: ${updatedPlayerWithSortedHand.cards.size}")
    }

    fun handleInput(input: GameInput) {
        gameStateMachine.handleInput(input)
    }
}
