package com.nowlsantos.tongitsai.domain.fsm.game

import com.nowlsantos.tongitsai.presentation.GameInput

/**
 * Interface defining the contract for each game phase state.
 * Each state is responsible for handling inputs relevant to its phase
 * and potentially triggering transitions to other states.
 */
interface GamePhaseState {
    /**
     * Called when the state machine transitions into this phase.
     * Use this for initial setup or logging for the phase.
     */
    fun onEnter(stateMachine: GameStateMachine)

    /**
     * Handles a game input for the current phase.
     * May update the game state or trigger a transition to a new phase.
     * Marked as suspend because it might call suspend functions (e.g., emitUiEffect, delay).
     */
    suspend fun handleInput(stateMachine: GameStateMachine, input: GameInput)

    /**
     * Called when the state machine transitions out of this phase.
     * Use this for cleanup or final actions before leaving the phase.
     */
    fun onExit(stateMachine: GameStateMachine)
}