package com.nowlsantos.tongitsai.domain.fsm.player

import com.nowlsantos.tongitsai.domain.ai.AiStrategy
import com.nowlsantos.tongitsai.domain.fsm.game.GameStateMachine
import com.nowlsantos.tongitsai.domain.model.GameState
import com.nowlsantos.tongitsai.domain.model.Meld
import com.nowlsantos.tongitsai.domain.model.Player
import com.nowlsantos.tongitsai.domain.model.PlayingCard
import com.nowlsantos.tongitsai.util.Logger
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class ReadyState(private val strategy: AiStrategy) : PlayerPhaseState {
    private val tag = "ReadyState"
    private var actionJob: kotlinx.coroutines.Job? = null

    override fun onEnter(
        playerStateMachine: PlayerStateMachine,
        gameStateMachine: GameStateMachine,
        player: Player,
    ) {
        val freshPlayer = gameStateMachine.gameState.value.players.firstOrNull { it.id == player.id } ?: player
        Logger.d(tag, "AI ${freshPlayer.name} entering READY phase. HandSize: ${freshPlayer.cards.size} | Hitter: ${freshPlayer.isHitter} | FirstMove: ${freshPlayer.firstMove}")
        
        // Reset AI hand for this player (clears secret melds)
        playerStateMachine.aiHandRepository.clearForPlayer(freshPlayer.id)
        playerStateMachine.aiHandRepository.resetAll(gameStateMachine.gameState.value.players)

        val has13Cards = freshPlayer.cards.size >= 13
        if (has13Cards) {
            Logger.d(tag, "AI ${freshPlayer.name} has 13 cards. Skipping Call/Chow/Get and transitioning directly to MELD.")
            playerStateMachine.transitionToPlayerPhase(PlayerPhase.MELD)
        } else {
            actionJob?.cancel()
            actionJob = playerStateMachine.externalScope.launch {
                performAction(playerStateMachine, gameStateMachine, freshPlayer)
            }
        }
    }

    override suspend fun performAction(
        playerStateMachine: PlayerStateMachine,
        gameStateMachine: GameStateMachine,
        player: Player,
    ) {
        Logger.d(tag, "AI ${player.name} performing action in ${playerStateMachine.currentPhase.value} phase.")

        // Call the new sortHand() method on PlayerStateMachine
        playerStateMachine.sortHand()

        // Immediately make a call if possible
        playerStateMachine.transitionToPlayerPhase(PlayerPhase.CALL)
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
        val aiPlayer = playerStateMachine.player
        Logger.d(tag, "AI ${aiPlayer.name} exiting READY phase.")
        actionJob?.cancel()
        actionJob = null
    }

    override fun getPhase(): PlayerPhase = PlayerPhase.READY
}