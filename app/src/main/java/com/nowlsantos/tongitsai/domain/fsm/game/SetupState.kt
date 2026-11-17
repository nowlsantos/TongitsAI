package com.nowlsantos.tongitsai.domain.fsm.game

import com.nowlsantos.tongitsai.presentation.GameInput
import com.nowlsantos.tongitsai.util.Logger
import kotlinx.coroutines.launch

class SetupState : GamePhaseState {
    override fun onEnter(stateMachine: GameStateMachine) {
        Logger.d("SetupState", "Entering Setup phase")

        if (stateMachine.gameState.value.isGameOver) {
            Logger.d("SetupState", "Game is over (Bankruptcy). Transitioning to GameOverState.")
            stateMachine.transitionTo(GamePhase.GAME_OVER)
            return
        }

        stateMachine.setupNewGame()

        // Recall the GameInput.StartGame to transition to DealState
        if (stateMachine.gameState.value.roundNumber > 1 && !stateMachine.gameState.value.isGameOver) {
            stateMachine.externalScope.launch {
                stateMachine.handleInput(GameInput.StartGame)
            }
        }
    }

    override suspend fun handleInput(
        stateMachine: GameStateMachine,
        input: GameInput
    ) {
        when (input) {
            GameInput.StartGame -> {
                if (stateMachine.gameState.value.isGameOver) {
                    stateMachine.transitionTo(GamePhase.GAME_OVER)
                } else {
                    stateMachine.transitionTo(GamePhase.DEALING)
                }
            }
            else -> Logger.w("SetupState", "Ignored input $input")
        }
    }

    override fun onExit(stateMachine: GameStateMachine) {
        Logger.d("SetupState", "Exiting Setup phase")
    }
}
