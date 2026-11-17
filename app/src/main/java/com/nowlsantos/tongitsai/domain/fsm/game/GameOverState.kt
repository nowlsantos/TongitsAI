package com.nowlsantos.tongitsai.domain.fsm.game

import com.nowlsantos.tongitsai.presentation.GameInput
import com.nowlsantos.tongitsai.presentation.UiEffect
import com.nowlsantos.tongitsai.util.Logger

class GameOverState : GamePhaseState {
    override fun onEnter(stateMachine: GameStateMachine) {
        Logger.d("GameOverState", "Entering Game-Over phase")
        val gameState = stateMachine.gameState.value
        stateMachine.updateState { it.copy(isGameOver = true) }

        val bankruptPlayer = gameState.players.firstOrNull { it.balance <= 0 }
        val overallWinner = gameState.players.firstOrNull { it.id == gameState.gameWinnerId }
            ?: gameState.players.maxByOrNull { it.balance }

        val winnerName = overallWinner?.name ?: "Champion!"
        val winnerBalance = overallWinner?.balance ?: 0
        val bankruptName = bankruptPlayer?.name ?: "A player"

        val message = "GAME OVER!\n$bankruptName is bankrupt!\n🏆 $winnerName is the Overall Game Champion with $winnerBalance chips!"

        stateMachine.emitUiEffect(UiEffect.ShowWinner(message))
    }

    override suspend fun handleInput(
        stateMachine: GameStateMachine,
        input: GameInput
    ) {
        when (input) {
            GameInput.GameOver -> {}
            GameInput.StartGame -> stateMachine.transitionTo(GamePhase.SETUP)
            else -> Logger.w("GameOverState", "Ignored input $input")
        }
    }

    override fun onExit(stateMachine: GameStateMachine) {
        Logger.d("GameOverState", "Exiting Game-Over phase")
    }
}
