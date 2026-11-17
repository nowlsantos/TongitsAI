package com.nowlsantos.tongitsai.domain.fsm.player

/**
 * The AI player's state for laying down melds from its hand.
 * This state is now responsible for deciding if melds are possible
 * and transitioning to the next phase accordingly.
 */

import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.nowlsantos.tongitsai.domain.ai.AiStrategy
import com.nowlsantos.tongitsai.domain.fsm.game.GameStateMachine
import com.nowlsantos.tongitsai.domain.model.Meld
import com.nowlsantos.tongitsai.domain.model.Player
import com.nowlsantos.tongitsai.domain.model.PlayingCard
import com.nowlsantos.tongitsai.domain.model.getHandOffset
import com.nowlsantos.tongitsai.presentation.GameInput
import com.nowlsantos.tongitsai.util.GameConstants.REGULAR_OVERLAP_FRACTION
import com.nowlsantos.tongitsai.util.Logger
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds


class MeldState(val strategy: AiStrategy) : PlayerPhaseState {
    private val tag = "MeldState"
    private var actionJob: kotlinx.coroutines.Job? = null

    override fun onEnter(
        playerStateMachine: PlayerStateMachine,
        gameStateMachine: GameStateMachine,
        player: Player,
    ) {
        Logger.d(tag, "AI ${player.name} entering MELD phase.")
        actionJob?.cancel()
        actionJob = playerStateMachine.externalScope.launch {
            performAction(playerStateMachine, gameStateMachine, player)
        }
    }

    override suspend fun performAction(
        playerStateMachine: PlayerStateMachine,
        gameStateMachine: GameStateMachine,
        player: Player,
    ) {
        Logger.d(tag, "AI ${player.name} performing action in ${playerStateMachine.currentPhase.value} phase.")
        delay(600.milliseconds)

        // Call the new sortHand() method on PlayerStateMachine
        playerStateMachine.sortHand()

        val gameState = gameStateMachine.gameState.value
        val latestPlayer = gameState.players.firstOrNull { it.id == player.id } ?: player
        val meldInput = strategy.chooseMove(gameState, latestPlayer, playerStateMachine)
        if (meldInput is GameInput.MeldCards) {
            val opponents = gameState.players.filter { it.id != latestPlayer.id }
            val opponentsHaveMelds = opponents.any { it.melds.isNotEmpty() }
            if (!opponentsHaveMelds && gameState.deck.size > 3) {
                // No visible meld move yet
                playerStateMachine.transitionToPlayerPhase(PlayerPhase.SAPAW)
            } else {
                val meldCards = meldInput.cards
                Logger.d(tag, "AI ${latestPlayer.name} meld decision: $meldCards")
                val currentHand = latestPlayer.cards.toMutableList()
                val remainingCards = currentHand.filter { it !in meldCards }

                // Update state to remove melded cards
                gameStateMachine.updateState { currentState ->
                    currentState.copy(
                        players = currentState.players.map { currentPlayer ->
                            if (currentPlayer.id == player.id) {
                                currentPlayer.copy(cards = remainingCards)
                            } else currentPlayer
                        }
                    )
                }

                // Animation handling
                val baseHandOffset = gameStateMachine.uiState.value.localCoordinate.getHandOffset(player.position)
                val sourceCardOffsets = mutableMapOf<PlayingCard, DpOffset>()
                val targetMeldCenterOffset = gameStateMachine.uiState.value.meldAreaCoordinates[player.position] ?: DpOffset.Zero
                val allCardOffsets = gameStateMachine.uiState.value.localCoordinate.allPlayersCardOffsets[player.position] ?: emptyMap()
                val layoutConfig = com.nowlsantos.tongitsai.presentation.PLAYER_HAND_LAYOUT_CONFIGS[player.position]
                val cardOverlap = layoutConfig?.cardOverlapDp ?: 48.dp

                meldCards.forEachIndexed { _, card ->
                    val cardIndexInHand = player.cards.indexOf(card).coerceAtLeast(0)
                    val cardOffset = allCardOffsets[card] ?: baseHandOffset.copy(x = baseHandOffset.x + (cardOverlap * cardIndexInHand))
                    sourceCardOffsets[card] = cardOffset
                }

                playerStateMachine.handleInput(
                    GameInput.MeldCards(
                        cards = meldCards,
                        sourceCardOffsets = sourceCardOffsets,
                        targetMeldCenterOffset = targetMeldCenterOffset
                    )
                )
            }
        } else {
            Logger.d(tag, "AI ${player.name} has no melds to lay. Moving to Sapaw.")
            playerStateMachine.transitionToPlayerPhase(PlayerPhase.SAPAW)
        }
    }

    override fun processPreMeldTongits(
        gameStateMachine: GameStateMachine,
        playerStateMachine: PlayerStateMachine,
        player: Player,
        meld: Meld?) {
        // Animation handling
        val baseHandOffset = gameStateMachine.uiState.value.localCoordinate.getHandOffset(player.position)
        val sourceCardOffsets = mutableMapOf<PlayingCard, DpOffset>()
        val targetMeldCenterOffset = gameStateMachine.uiState.value.meldAreaCoordinates[player.position] ?: DpOffset.Zero

        meld?.let {
            it.cards.forEachIndexed { index, card ->
                val cardXOffset = baseHandOffset.x + (REGULAR_OVERLAP_FRACTION * index)
                sourceCardOffsets[card] = baseHandOffset.copy(x = cardXOffset)
            }

            playerStateMachine.handleInput(
                GameInput.MeldCards(
                    cards = it.cards,
                    sourceCardOffsets = sourceCardOffsets,
                    targetMeldCenterOffset = targetMeldCenterOffset
                )
            )
        }
    }

    override fun onExit(playerStateMachine: PlayerStateMachine, gameStateMachine: GameStateMachine) {
        Logger.d(tag, "AI ${playerStateMachine.player.name} exiting MELD phase.")
        actionJob?.cancel()
        actionJob = null
    }

    override fun getPhase(): PlayerPhase = PlayerPhase.MELD
}