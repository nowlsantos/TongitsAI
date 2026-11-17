package com.nowlsantos.tongitsai.domain.fsm.player

import com.nowlsantos.tongitsai.domain.fsm.game.GameStateMachine
import com.nowlsantos.tongitsai.domain.model.Meld
import com.nowlsantos.tongitsai.domain.model.Player
import com.nowlsantos.tongitsai.domain.model.PlayingCard

/**
 * Base interface for all states within the PlayerStateMachine.
 * Defines lifecycle methods for state transitions and action execution.
 */
interface PlayerPhaseState {

    // Now passes Player and GameStateMachine to onEnter
    fun onEnter(
        playerStateMachine: PlayerStateMachine,
        gameStateMachine: GameStateMachine,
        player: Player,
    )

    // This is the signature: it takes PlayerStateMachine, Player, and GameStateMachine
    suspend fun performAction(
        playerStateMachine: PlayerStateMachine,
        gameStateMachine: GameStateMachine,
        player: Player,
    )

    fun onExit(
        playerStateMachine: PlayerStateMachine,
        gameStateMachine: GameStateMachine,
        //player: Player,
    )

    fun processPreMeldTongits(
        gameStateMachine: GameStateMachine,
        playerStateMachine: PlayerStateMachine,
        player: Player,
        mels: Meld?
    )

    fun getPhase(): PlayerPhase
}