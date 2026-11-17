package com.nowlsantos.tongitsai.domain.fsm.game

import com.nowlsantos.tongitsai.domain.model.Player
import com.nowlsantos.tongitsai.presentation.GameInput
import com.nowlsantos.tongitsai.presentation.UiEffect
import com.nowlsantos.tongitsai.util.Logger

class RoundEndState : GamePhaseState {
    private val tag = "RoundEndState"
    lateinit var winner: Player

    override fun onEnter(stateMachine: GameStateMachine) {
        Logger.d(tag, "Entering RoundEnd State")
        /*stateMachine.updateState { currentState ->
            currentState.copy(isReveal = true)
        }*/

        val gameState = stateMachine.gameState.value
        winner = gameState.players.firstOrNull { it.id == gameState.roundWinnerId }
            ?: gameState.players.firstOrNull { it.id == gameState.gameWinnerId }
            ?: gameState.currentPlayer

        Logger.d(tag, "Winner:: ${winner.name}")

        // Update hitCount directly in RoundEndState
        val wonTwoHits = winner.hitCount >= 1
        stateMachine.updateState { currentState ->
            val updatedPlayers = currentState.players.map { player ->
                if (player.id == winner.id) {
                    player.copy(hitCount = if (wonTwoHits) 0 else player.hitCount + 1)
                } else {
                    player.copy(hitCount = 0)
                }
            }
            currentState.copy(
                players = updatedPlayers,
                lastRoundWonTwoHits = wonTwoHits
            )
        }

        // Process financial chip payments
        when (gameState.revealType) {
            RevealType.ALL_FOLDS,
            RevealType.NORMAL -> {
                stateMachine.paymentProcessor.normalPayment(winner.id)
            }
            RevealType.CHALLENGE -> {
                stateMachine.paymentProcessor.challengePayment(winner.id)
            }
            RevealType.TONGITS -> {
                stateMachine.paymentProcessor.tongitsPayment(winner)
            }
            else -> { }
        }

        val updatedGameState = stateMachine.gameState.value
        val freshWinner = updatedGameState.players.firstOrNull { it.id == winner.id } ?: winner

        val message = when (updatedGameState.revealType) {
            RevealType.TONGITS -> {
                if (updatedGameState.lastRoundWonTwoHits) {
                    "${freshWinner.name} Wins By Tongits and 2 hits!"
                } else {
                    "${freshWinner.name} Wins By Tongits (Empty Hand)!"
                }
            }
            RevealType.CHALLENGE -> {
                if (updatedGameState.lastRoundWonTwoHits) {
                    "${freshWinner.name} Wins By Challenge Showdown and 2 hits!"
                } else {
                    "${freshWinner.name} Wins By Challenge Showdown!"
                }
            }
            RevealType.ALL_FOLDS -> {
                if (updatedGameState.lastRoundWonTwoHits) {
                  "${freshWinner.name} Wins By Call (No Challengers) and 2 hits!"
                } else {
                    "${freshWinner.name} Wins By Call (No Challengers)!"
                }
            }
            RevealType.NORMAL -> {
                if (updatedGameState.lastRoundWonTwoHits) {
                    "${freshWinner.name} Wins By Lowest Score and 2 hits!"
                } else {
                    "${freshWinner.name} Wins By Lowest Score!"
                }
            }
            else -> { "${freshWinner.name} Wins!" }
        }

        Logger.d(tag, "Message:: $message")
        stateMachine.emitUiEffect(UiEffect.ShowWinner(message))
    }

    override suspend fun handleInput(
        stateMachine: GameStateMachine,
        input: GameInput
    ) {
        when(input) {
            GameInput.RoundEnd -> {
                stateMachine.emitUiEffect(UiEffect.ResetDialog)
                if (stateMachine.gameState.value.isGameOver) {
                    Logger.d(tag, "Game is over (Bankruptcy). Transitioning to GameOverState.")
                    stateMachine.transitionTo(GamePhase.GAME_OVER)
                } else {
                    stateMachine.transitionTo(GamePhase.SETUP)
                }
            }
            else -> Logger.w(tag, "Ignored input $input")
        }
    }

    override fun onExit(stateMachine: GameStateMachine) {
        Logger.d(tag, "Exiting Round-End phase")
    }
}
