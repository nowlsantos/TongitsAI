package com.nowlsantos.tongitsai.domain.fsm.player

import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.nowlsantos.tongitsai.domain.ai.AiStrategy
import com.nowlsantos.tongitsai.domain.fsm.game.GameStateMachine
import com.nowlsantos.tongitsai.domain.model.Meld
import com.nowlsantos.tongitsai.domain.model.Player
import com.nowlsantos.tongitsai.domain.model.PlayingCard
import com.nowlsantos.tongitsai.domain.model.getHandOffset
import com.nowlsantos.tongitsai.presentation.GameInput
import com.nowlsantos.tongitsai.util.GameConstants.CARD_WIDTH
import com.nowlsantos.tongitsai.util.GameConstants.REGULAR_OVERLAP_FRACTION
import com.nowlsantos.tongitsai.util.Logger
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class ChowState(val strategy: AiStrategy): PlayerPhaseState {
    private val tag = "ChowState"
    private var actionJob: kotlinx.coroutines.Job? = null

    override fun onEnter(
        playerStateMachine: PlayerStateMachine,
        gameStateMachine: GameStateMachine,
        player: Player,
    ) {
        Logger.d(tag, "AI ${player.name} entering CHOW phase.")
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
        /*val latestPlayer = playerStateMachine.latestPlayerState.value
        if (latestPlayer == null) {
            Logger.e(tag, "latestPlayer is null for ${player.name}. Cannot determine next phase.")
            return
        }*/

        val gameState = gameStateMachine.gameState.value
        val latestPlayer = gameState.players.firstOrNull { it.id == player.id } ?: player
        val uiState = gameStateMachine.uiState.value
        val chowInput = strategy.chooseMove(gameState, latestPlayer, playerStateMachine)

        if (chowInput is GameInput.ChowCard) {
            val chowCard = chowInput.chowCard
            Logger.d(tag, "AI ${latestPlayer.name} : $chowCard.")
            val baseHandOffset = uiState.localCoordinate.getHandOffset(latestPlayer.position)
            val discardPileOffset = uiState.localCoordinate.discardPileOffset
            val targetMeldCenterOffset = uiState.meldAreaCoordinates[latestPlayer.position] ?: DpOffset.Zero

            val chowMeld = Meld(cards = chowInput.selectedCards + chowCard, owner = latestPlayer.id)
            Logger.d(tag, "ChowMeld:: $chowMeld")
            val allCardOffsets = uiState.localCoordinate.allPlayersCardOffsets[latestPlayer.position] ?: emptyMap()
            val layoutConfig = com.nowlsantos.tongitsai.presentation.PLAYER_HAND_LAYOUT_CONFIGS[latestPlayer.position]
            val cardOverlap = layoutConfig?.cardOverlapDp ?: 48.dp

            val sourceCardOffsets = mutableMapOf<PlayingCard, DpOffset>()
            chowMeld.cards.forEach { card ->
                val cardIndexInHand = latestPlayer.cards.indexOf(card).coerceAtLeast(0)
                val cardOffset = allCardOffsets[card] ?: baseHandOffset.copy(x = baseHandOffset.x + (cardOverlap * cardIndexInHand))
                sourceCardOffsets[card] = cardOffset
            }

            val lastCardIndex = latestPlayer.cards.size.coerceAtLeast(0)
            val targetOffset = baseHandOffset.copy(x = baseHandOffset.x + (cardOverlap * lastCardIndex))

            playerStateMachine.handleInput(
                GameInput.SingleChowCard(
                    chowCard = chowCard,
                    disCardPileOffset = discardPileOffset,
                    targetOffset = targetOffset,
                    selectedCards = chowMeld.cards,
                    sourceCardOffsets = sourceCardOffsets,
                    targetMeldCenterOffset = targetMeldCenterOffset
                )
            )
        } else {
            Logger.d(tag, "AI ${player.name} has no Chow to lay. Skipping to Get.")
            playerStateMachine.transitionToPlayerPhase(PlayerPhase.GET)
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
        Logger.d(tag, "AI ${playerStateMachine.player.name} exiting CHOW phase.")
        actionJob?.cancel()
        actionJob = null
    }

    override fun getPhase(): PlayerPhase = PlayerPhase.CHOW
}