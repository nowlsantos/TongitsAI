package com.nowlsantos.tongitsai.domain.fsm.player

import com.nowlsantos.tongitsai.domain.ai.AiStrategy
import com.nowlsantos.tongitsai.domain.fsm.game.GameStateMachine
import com.nowlsantos.tongitsai.domain.model.Meld
import com.nowlsantos.tongitsai.domain.model.Player
import com.nowlsantos.tongitsai.domain.model.PlayingCard
import com.nowlsantos.tongitsai.presentation.GameInput
import com.nowlsantos.tongitsai.util.Logger
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class GetState(val strategy: AiStrategy): PlayerPhaseState {
    private val tag = "GetState"
    private var actionJob: kotlinx.coroutines.Job? = null

    override fun onEnter(
        playerStateMachine: PlayerStateMachine,
        gameStateMachine: GameStateMachine,
        player: Player,
    ) {
        Logger.d(tag, "AI ${player.name} entering GET phase.")
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
        delay(350.milliseconds)

        // directly delegate to the PlayState handleDrawCard
        playerStateMachine.handleInput(GameInput.DrawCard)
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
        Logger.d(tag, "AI ${playerStateMachine.player.name} exiting GET phase.")
        actionJob?.cancel()
        actionJob = null
    }

    override fun getPhase(): PlayerPhase = PlayerPhase.GET
}