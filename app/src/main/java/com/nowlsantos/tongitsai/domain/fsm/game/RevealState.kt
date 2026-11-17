package com.nowlsantos.tongitsai.domain.fsm.game

import com.nowlsantos.tongitsai.domain.model.GameState
import com.nowlsantos.tongitsai.domain.model.enums.ChallengerType
import com.nowlsantos.tongitsai.presentation.GameInput
import com.nowlsantos.tongitsai.presentation.UiEffect
import com.nowlsantos.tongitsai.util.Logger
import com.nowlsantos.tongitsai.util.ScoreUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class RevealState : GamePhaseState {
    private val tag = "RevealState"
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    override fun onEnter(stateMachine: GameStateMachine) {
        Logger.d(tag, "Entering RevealState phase")
        stateMachine.updateState { currentState ->
            currentState.copy(isReveal = true)
        }
        val gameState = stateMachine.gameState.value
        Logger.d(tag, "RevealType:: ${gameState.revealType}")

        when(gameState.revealType) {
            RevealType.NORMAL -> {
                stateMachine.emitUiEffect(
                    UiEffect.InitialMessage("Deck is empty. \nRevealing cards and tallying scores...")
                )
            }
            RevealType.CHALLENGE -> {
                stateMachine.emitUiEffect(
                    UiEffect.InitialMessage("Reveal Showdown. \nTallying scores...")
                )
            }
            else -> RevealType.NONE
        }

        coroutineScope.launch {
            Logger.d(tag, "Delaying for 2 seconds")
            delay(1500.milliseconds)
            stateMachine.externalScope.launch {
                val gameState = stateMachine.gameState.value
                val activePlayerIds = if (gameState.revealType == RevealType.CHALLENGE) {
                    gameState.players
                        .filter {
                            it.challengerType != ChallengerType.FOLDEE &&
                            it.challengerType != ChallengerType.LAST_FOLDEE &&
                            it.challengerType != ChallengerType.NONE
                        }
                        .map { it.id }
                } else {
                    gameState.players.map { it.id }
                }

                stateMachine.handleInput(GameInput.RoundReveal(
                    playerIds = activePlayerIds
                ))
            }
        }
    }

    override suspend fun handleInput(
        stateMachine: GameStateMachine,
        input: GameInput
    ) {
        when(input) {
            is GameInput.RoundReveal -> {
                stateMachine.emitUiEffect(UiEffect.ResetDialog)
                Logger.d(tag, "Handling GameInput.RoundReveal. | ${input.playerIds}")
                val gameState = stateMachine.gameState.value

                // Filter out players who folded before processing
                val activePlayers = gameState.players.filter {
                    it.id in input.playerIds &&
                    it.challengerType != ChallengerType.FOLDEE &&
                    it.challengerType != ChallengerType.LAST_FOLDEE
                }

                // Reveal order: Caller reveals 1st, followed by challengers in response order (Callee, Last Callee)
                val callerPlayer = activePlayers.firstOrNull {
                    it.challengerType == ChallengerType.CALLER || it.id == gameState.callerId
                }
                val challengerOrder = listOf(
                    ChallengerType.CALLEE,
                    ChallengerType.LAST_CALLEE
                )
                val challengerPlayers = activePlayers
                    .filter { it.id != callerPlayer?.id }
                    .sortedBy { challengerOrder.indexOf(it.challengerType).takeIf { idx -> idx >= 0 } ?: 99 }

                val sortedPlayersToReveal = listOfNotNull(callerPlayer) + challengerPlayers

                // Calculate scores map
                val scoresMap = sortedPlayersToReveal.associate { player ->
                    player.id to ScoreUtils.calculateRoundPenalty(player.cards)
                }

                // Staggered individual player score revelation (Caller first, then challengers)
                for ((id, name) in sortedPlayersToReveal) {
                    val score = scoresMap[id] ?: 0
                    Logger.d(tag, "$name revealed score: $score")
                    stateMachine.emitUiEffect(UiEffect.ShowMessage("$name's hand score: $score pts"))

                    stateMachine.updateState { currentState ->
                        val updatedPlayers = currentState.players.map { p ->
                            if (p.id == id && p.challengerType != ChallengerType.FOLDEE) {
                                p.copy(score = score)
                            } else {
                                p
                            }
                        }
                        currentState.copy(players = updatedPlayers)
                    }
                    delay(800.milliseconds) // Suspenseful fraction-of-a-second pause per individual reveal
                }

                // Re-fetch the updated game state
                val updatedGameState = stateMachine.gameState.value

                // Evaluate scores strictly among active sortedPlayersToReveal (excluding anyone who folded)
                val activePlayersWithScores = sortedPlayersToReveal.map { player ->
                    updatedGameState.players.firstOrNull { it.id == player.id } ?: player
                }
                val scores = activePlayersWithScores.map { it.score }
                val minimumScore = scores.minOfOrNull { it } ?: 0

                // Find all active players with the lowest score.
                val tiedPlayers = activePlayersWithScores.filter { it.score == minimumScore }

                // Tongits Tie-Breaker Rule: In a 2-way or 3-way tie for lowest score,
                // whoever was the LAST player in reveal order is declared the WINNER!
                val winner = sortedPlayersToReveal
                    .filter { p -> tiedPlayers.any { t -> t.id == p.id } }
                    .lastOrNull() ?: tiedPlayers.firstOrNull()

                winner?.let {
                    val winnerName = it.name
                    val winnerId = it.id
                    Logger.d(tag, "RoundWinner:: $winnerName (Tie-breaker applied)")

                    stateMachine.updateState { currentState ->
                        currentState.copy(
                            roundWinnerId = winnerId,
                            lastRoundWinnerId = winnerId
                        )
                    }
                }

                // Final pause before transition to RoundEnd
                stateMachine.emitUiEffect(UiEffect.ShowMessage("Winner determined! Calculating payouts..."))
                delay(1200.milliseconds)
                stateMachine.transitionTo(GamePhase.ROUND_END)
            }
            else -> Logger.w(tag, "Ignored input $input")
        }
    }

    override fun onExit(stateMachine: GameStateMachine) {
        Logger.d(tag, "Exiting RevealState phase")
    }
}
