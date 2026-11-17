package com.nowlsantos.tongitsai.domain.fsm.player

import androidx.compose.ui.unit.dp
import com.nowlsantos.tongitsai.domain.ai.AiStrategy
import com.nowlsantos.tongitsai.domain.fsm.game.GameStateMachine
import com.nowlsantos.tongitsai.domain.model.Meld
import com.nowlsantos.tongitsai.domain.model.Player
import com.nowlsantos.tongitsai.domain.model.enums.MoveType
import com.nowlsantos.tongitsai.domain.model.enums.PlayerPosition
import com.nowlsantos.tongitsai.domain.model.enums.PlayerType
import com.nowlsantos.tongitsai.domain.model.getHandOffset
import com.nowlsantos.tongitsai.presentation.GameInput
import com.nowlsantos.tongitsai.util.GameConstants.REGULAR_OVERLAP_FRACTION
import com.nowlsantos.tongitsai.util.Logger
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class DiscardState(private val aiStrategy: AiStrategy): PlayerPhaseState {
    private val tag = "DiscardState"
    private var actionJob: kotlinx.coroutines.Job? = null

    override fun onEnter(
        playerStateMachine: PlayerStateMachine,
        gameStateMachine: GameStateMachine,
        player: Player,
    ) {
        Logger.d(tag, "AI ${player.name} entering DISCARD phase.")
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
        delay(500.milliseconds)
        val gameState = gameStateMachine.gameState.value
        val latestPlayer = gameState.players.firstOrNull { it.id == player.id } ?: player
        val currentUiState = gameStateMachine.currentUiState.value
        val discardPileOffset = currentUiState.localCoordinate.discardPileOffset
        val discardInput = aiStrategy.chooseMove(gameState, latestPlayer, playerStateMachine)

        if (discardInput is GameInput.DiscardCard) {
            val cardToDiscard = discardInput.card
            Logger.d(tag, "AI ${latestPlayer.name} : $cardToDiscard.")

            val cardIndex = latestPlayer.cards.indexOf(cardToDiscard).coerceAtLeast(0)
            val coordinate = currentUiState.localCoordinate
            val measuredOffset = coordinate.allPlayersCardOffsets[latestPlayer.position]?.get(cardToDiscard)

            val cardOffset = measuredOffset ?: run {
                val layoutConfig = com.nowlsantos.tongitsai.presentation.PLAYER_HAND_LAYOUT_CONFIGS[latestPlayer.position]
                val cardOverlap = layoutConfig?.cardOverlapDp ?: 48.dp
                val baseOffset = coordinate.getHandOffset(latestPlayer.position)
                baseOffset.copy(x = baseOffset.x + (cardOverlap * cardIndex))
            }

            playerStateMachine.handleInput(
                GameInput.DiscardCard(
                    card = cardToDiscard,
                    startOffset = cardOffset,
                    targetOffset = discardPileOffset
                )
            )
            Logger.d(tag, "AI ${player.name} decided to DISCARD: $cardToDiscard | DiscardPileOffset:: ${coordinate.discardPileOffset}")
        } else if (player.cards.isNotEmpty()) {
            val fallbackCard = player.cards.last()
            Logger.w(tag, "AI ${player.name} fallback discard: $fallbackCard")
            val cardIndex = player.cards.indexOf(fallbackCard).coerceAtLeast(0)
            val coordinate = currentUiState.localCoordinate
            val measuredOffset = coordinate.allPlayersCardOffsets[player.position]?.get(fallbackCard)

            val cardOffset = measuredOffset ?: run {
                val layoutConfig = com.nowlsantos.tongitsai.presentation.PLAYER_HAND_LAYOUT_CONFIGS[player.position]
                val cardOverlap = layoutConfig?.cardOverlapDp ?: 48.dp
                val baseOffset = coordinate.getHandOffset(player.position)
                baseOffset.copy(x = baseOffset.x + (cardOverlap * cardIndex))
            }

            playerStateMachine.handleInput(
                GameInput.DiscardCard(
                    card = fallbackCard,
                    startOffset = cardOffset,
                    targetOffset = discardPileOffset
                )
            )
        } else {
            Logger.d(tag, "AI ${player.name} has no cards left. Triggering RoundEnd.")
            gameStateMachine.handleInput(GameInput.RoundEnd)
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
        Logger.d(tag, "AI ${playerStateMachine.player.name} exiting DISCARD phase.")
        actionJob?.cancel()
        actionJob = null
    }

    override fun getPhase(): PlayerPhase = PlayerPhase.DISCARD
}