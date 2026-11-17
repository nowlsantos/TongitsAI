package com.nowlsantos.tongitsai.domain.fsm.player

import androidx.compose.runtime.currentComposer
import com.nowlsantos.tongitsai.domain.fsm.game.GameStateMachine
import com.nowlsantos.tongitsai.domain.model.Meld
import com.nowlsantos.tongitsai.domain.model.Player
import com.nowlsantos.tongitsai.domain.model.PlayingCard
import com.nowlsantos.tongitsai.domain.model.enums.MoveType
import com.nowlsantos.tongitsai.domain.model.enums.PlayerPosition
import com.nowlsantos.tongitsai.util.Logger
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class IdleState : PlayerPhaseState {
    private val tag = "IdleState"

    override fun onEnter(
        playerStateMachine: PlayerStateMachine,
        gameStateMachine: GameStateMachine,
        player: Player,
    ) {
        Logger.d(tag, "AI ${player.name} entering IDLE phase.")
        gameStateMachine.updateState { currentState ->
            val updatedPlayers = currentState.players.map { p ->
                if (player.id == p.id && player.firstMove) {
                    p.copy(firstMove = false)
                } else {
                    p
                }
            }
            currentState.copy(players = updatedPlayers)
        }
        Logger.d(tag, "AI ${player.name}  | Hitter::  ${player.isHitter} | FirstMove:: ${player.firstMove}")
    }

    override suspend fun performAction(
        playerStateMachine: PlayerStateMachine,
        gameStateMachine: GameStateMachine,
        player: Player,
    ) {
        Logger.d(tag, "AI ${player.name} performing action in ${playerStateMachine.currentPhase.value} phase.")
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
        // Use the 'player' from the PlayerStateMachine's constructor directly.
        Logger.d(tag, "AI ${playerStateMachine.player.name} exiting IDLE phase.")
    }

    override fun getPhase(): PlayerPhase = PlayerPhase.IDLE
}