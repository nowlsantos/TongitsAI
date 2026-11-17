package com.nowlsantos.tongitsai.domain.fsm.player

import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.nowlsantos.tongitsai.domain.ai.AiStrategy
import com.nowlsantos.tongitsai.domain.fsm.game.GameStateMachine
import com.nowlsantos.tongitsai.domain.model.Meld
import com.nowlsantos.tongitsai.domain.model.Player
import com.nowlsantos.tongitsai.domain.model.enums.PlayerPosition
import com.nowlsantos.tongitsai.domain.model.getHandOffset
import com.nowlsantos.tongitsai.presentation.GameInput
import com.nowlsantos.tongitsai.util.GameConstants.REGULAR_OVERLAP_FRACTION
import com.nowlsantos.tongitsai.util.Logger
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * The AI player's state for attaching cards to existing melds on the table.
 * After performing its action, it will transition to the DISCARD phase.
 */
class SapawState(private val strategy: AiStrategy) : PlayerPhaseState {
    private val tag = "SapawState"
    private var actionJob: kotlinx.coroutines.Job? = null

    // The queue to hold all sapaw moves for the turn
    private val _sapawQueue: MutableList<GameInput.SapawCard> = mutableListOf()

    override fun onEnter(
        playerStateMachine: PlayerStateMachine,
        gameStateMachine: GameStateMachine,
        player: Player,
    ) {
        Logger.d(tag, "AI ${player.name} entering SAPAW phase.")
        actionJob?.cancel()
        actionJob = playerStateMachine.externalScope.launch {
            performAction(playerStateMachine, gameStateMachine, player)
        }

        /*playerStateMachine.externalScope.launch {
            // The queue is only populated on the very first entry of the SAPAW phase.
            if (_sapawQueue.isEmpty()) {
                val allSapawMoves = strategy.chooseMove(gameStateMachine.gameState.value, player, playerStateMachine)
                    ?.filterIsInstance<GameInput.SapawCard>() ?: emptyList()
                _sapawQueue.addAll(allSapawMoves)
            }

            // After populating, or if returning from an animation, check if there are moves.
            if (_sapawQueue.isNotEmpty()) {
                performAction(playerStateMachine, gameStateMachine, player)
            } else {
                // The queue is empty, so the sapaw phase is complete.
                Logger.d(tag, "AI ${player.name} has no cards left to sapaw, proceeding to check for win/turn end.")
                gameStateMachine.checkPostWinOrBroadCastDecision(player.id, MoveType.MELD_CARD)
            }
        }*/
    }

    override suspend fun performAction(
        playerStateMachine: PlayerStateMachine,
        gameStateMachine: GameStateMachine,
        player: Player,
    ) {
        Logger.d(tag, "AI ${player.name} performing action in ${playerStateMachine.currentPhase.value} phase.")
        delay(600.milliseconds)

        val gameState = gameStateMachine.gameState.value
        val latestPlayer = gameState.players.firstOrNull { it.id == player.id } ?: player
        val uiState = gameStateMachine.uiState.value
        val sapawInput = strategy.chooseMove(gameState, latestPlayer, playerStateMachine)
        if (sapawInput is GameInput.SapawCard) {
            val sapawCard = sapawInput.sapawCard
            Logger.d(tag, "AI ${latestPlayer.name} is sapaw-ing $sapawCard")
            val targetPlayer = gameState.players.firstOrNull{ it.position == sapawInput.sapawTarget.second}
            targetPlayer?.let {
                Logger.d(tag, "AI ${latestPlayer.name} is sapawing to ${targetPlayer.name}")
                val baseHandOffset = uiState.localCoordinate.getHandOffset(player.position)
                val targetPosition = sapawInput.sapawTarget.second
                val targetMeldIndex = sapawInput.sapawTarget.third
                val targetMeldOffset = when(targetPosition) {
                    PlayerPosition.BOTTOM -> { uiState.meldAreaCoordinates[PlayerPosition.BOTTOM] ?: DpOffset.Zero}
                    PlayerPosition.LEFT -> { uiState.meldAreaCoordinates[PlayerPosition.LEFT] ?: DpOffset.Zero}
                    PlayerPosition.RIGHT -> { uiState.meldAreaCoordinates[PlayerPosition.RIGHT] ?: DpOffset.Zero}
                }
                /*Logger.d(tag, "targetMeldOffset:: $targetMeldOffset")
                Logger.d(tag, "targetPosition:: $targetPosition")
                Logger.d(tag, "targetMeldIndex:: $targetMeldIndex")*/

                val cardIndexInHand = player.cards.indexOf(sapawCard).coerceAtLeast(0)
                val measuredOffset = uiState.localCoordinate.allPlayersCardOffsets[player.position]?.get(sapawCard)
                val startOffset = measuredOffset ?: run {
                    val layoutConfig = com.nowlsantos.tongitsai.presentation.PLAYER_HAND_LAYOUT_CONFIGS[player.position]
                    val cardOverlap = layoutConfig?.cardOverlapDp ?: 48.dp
                    baseHandOffset.copy(x = baseHandOffset.x + (cardOverlap * cardIndexInHand))
                }

                playerStateMachine.handleInput(
                    GameInput.SapawCard(
                        sapawCard = sapawCard,
                        startOffset = startOffset,
                        sapawTarget = Triple(
                            targetMeldOffset,
                            targetPosition,
                            targetMeldIndex
                        )
                    )
                )
            }
        } else {
            Logger.d(tag, "AI ${player.name} has no cards to sapaw skipping to Discard")
            playerStateMachine.transitionToPlayerPhase(PlayerPhase.DISCARD)
        }
    }

    override fun processPreMeldTongits(
        gameStateMachine: GameStateMachine,
        playerStateMachine: PlayerStateMachine,
        player: Player,
        mels: Meld?
    ) {
        Logger.d(tag, "AI ${playerStateMachine.player.name} processPreMeldTongits phase.")
    }

    override fun onExit(playerStateMachine: PlayerStateMachine, gameStateMachine: GameStateMachine) {
        Logger.d(tag, "AI ${playerStateMachine.player.name} exiting SAPAW phase.")
        actionJob?.cancel()
        actionJob = null
    }

    override fun getPhase(): PlayerPhase = PlayerPhase.SAPAW
}

/*sapawCard = sapawCard,
               startOffset = startOffset,
               playerPosition = sapawTarget.second,
               targetMeldIndex = sapawTarget.third,
               targetMeldDropOffset = sapawTarget.first,*/

/*val meldAreaOffset = uiState.meldAreaCoordinates[playerPosition] ?: DpOffset.Zero
val baseHandOffset = uiState.localCoordinate.getHandOffset(playerPosition)
val baseAreaOffset = DpOffset(baseHandOffset.x - CARD_WIDTH, (meldAreaOffset.y.minus(30.dp)))
val player = uiState.players.firstOrNull { it.position == playerPosition }
var targetMeldOffset = DpOffset.Zero
player?.let {
    targetMeldOffset = calculateSapawDropOffset(
        baseAreaOffset = baseAreaOffset,
        allMeldsOfTargetPlayer = it.melds,
        meldIndex = meldIndex,
        targetMeld = meld
    )
}*/