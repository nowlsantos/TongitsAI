package com.nowlsantos.tongitsai.domain.fsm.player

import com.nowlsantos.tongitsai.domain.ai.AiStrategy
import com.nowlsantos.tongitsai.domain.fsm.game.GamePhase
import com.nowlsantos.tongitsai.domain.fsm.game.GameStateMachine
import com.nowlsantos.tongitsai.domain.model.Meld
import com.nowlsantos.tongitsai.domain.model.Player
import com.nowlsantos.tongitsai.domain.model.PlayingCard
import com.nowlsantos.tongitsai.presentation.GameInput
import com.nowlsantos.tongitsai.util.Logger
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class CallState(private val strategy: AiStrategy) : PlayerPhaseState { // Re-add private val here
    private val tag = "CallState"
    private var actionJob: kotlinx.coroutines.Job? = null

    override fun onEnter(
        playerStateMachine: PlayerStateMachine,
        gameStateMachine: GameStateMachine,
        player: Player,
    ) {
        Logger.d(tag, "AI ${player.name} entering CALL phase. Current Hand size: ${player.cards.size}")
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

        // Get the AI's chosen move for the current phase
        val gameState = gameStateMachine.gameState.value
        val callInput = strategy.chooseMove(gameState, player, playerStateMachine)
        if (callInput is GameInput.MakeCall) {
            Logger.d(tag, "AI ${player.name} is making a call.")
            playerStateMachine.handleInput(GameInput.MakeCall(caller = player))
        } else {
            Logger.d(tag, "AI ${player.name} decided NOT to CALL. Proceeding to Chow phase.")
            playerStateMachine.transitionToPlayerPhase(PlayerPhase.CHOW)
        }
    }

    override fun onExit(playerStateMachine: PlayerStateMachine, gameStateMachine: GameStateMachine) {
        Logger.d(tag, "AI ${playerStateMachine.player.name} exiting CALL phase.")
        actionJob?.cancel()
        actionJob = null
    }

    override fun processPreMeldTongits(
        gameStateMachine: GameStateMachine,
        playerStateMachine: PlayerStateMachine,
        player: Player,
        mels: Meld?
    ) {
        Logger.d(tag, "AI ${playerStateMachine.player.name} processPreMeldTongits phase.")
    }

    override fun getPhase(): PlayerPhase = PlayerPhase.CALL
}