package com.nowlsantos.tongitsai.domain.usecase

import com.nowlsantos.tongitsai.data.GameData
import com.nowlsantos.tongitsai.data.GameRepository
import com.nowlsantos.tongitsai.data.PlayerSummary
import com.nowlsantos.tongitsai.domain.fsm.game.GameStateMachine
import com.nowlsantos.tongitsai.presentation.UiEffect
import com.nowlsantos.tongitsai.util.GameConstants
import com.nowlsantos.tongitsai.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GameHistoryUseCase(
    private val repository: GameRepository,
    private val gameStateMachine: GameStateMachine,
    private val scope: CoroutineScope
) {
    private val tag = "GameHistoryUseCase"
    private val _gameData = MutableStateFlow<GameData?>(null)
    val gameData: StateFlow<GameData?> = _gameData.asStateFlow()

    private val roundHistory = mutableListOf<Map<String, String>>()

    init {
        gameStateMachine.onRoundPaymentProcessed = { winnerId ->
            recordRoundResultAfterPayment(winnerId)
        }
    }

    fun recordRoundResultAfterPayment(winnerId: String) {
        val currentState = gameStateMachine.gameState.value
        val winner = currentState.players.firstOrNull { it.id == winnerId } ?: return

        val roundOutcome = mutableMapOf<String, String>()
        currentState.players.forEach { p ->
            roundOutcome[p.name] = if (p.id == winner.id) "WIN" else "LOST"
        }

        val currentRoundStr = currentState.roundNumber.toString()
        if (roundHistory.none { it["Round"] == currentRoundStr }) {
            roundOutcome["Round"] = currentRoundStr
            roundHistory.add(roundOutcome)
            saveGameResults()
        }
    }

    fun saveGameResults() {
        val currentState = gameStateMachine.gameState.value

        scope.launch {
            val playerWinnings = mutableMapOf<String, Int>()

            currentState.players.forEach { player ->
                playerWinnings[player.name] = 0
            }

            roundHistory.forEach { round ->
                round.forEach { (playerName, result) ->
                    if (result == "WIN" || result == "W") {
                        playerWinnings[playerName] = (playerWinnings[playerName] ?: 0) + 1
                    }
                }
            }

            val playerSummaries = currentState.players.associate { player ->
                player.name to PlayerSummary(
                    balance = player.balance,
                    score = player.score,
                    winnings = player.balance - GameConstants.BALANCE
                )
            }

            val realGameData = GameData(
                playerSummaries = playerSummaries,
                roundResults = roundHistory
            )

            repository.saveGameData(realGameData)
            _gameData.value = realGameData
        }
    }

    fun onShowGameResult() {
        scope.launch {
            var loadedData = repository.loadGameData()
            val currentState = gameStateMachine.gameState.value
            val playerSummaries = currentState.players.associate { player ->
                player.name to PlayerSummary(
                    balance = player.balance,
                    score = player.score,
                    winnings = player.balance - GameConstants.BALANCE
                )
            }
            if (loadedData == null || loadedData.roundResults.isEmpty()) {
                loadedData = GameData(
                    playerSummaries = playerSummaries,
                    roundResults = roundHistory.ifEmpty {
                        listOf(mapOf("Status" to "Round ${currentState.roundNumber} in progress"))
                    }
                )
            } else {
                loadedData = loadedData.copy(playerSummaries = playerSummaries)
            }
            _gameData.value = loadedData
            gameStateMachine.emitUiEffect(UiEffect.GameResultDialog(loadedData))
            Logger.d(tag, "Game results displayed successfully: $loadedData")
        }
    }
}
