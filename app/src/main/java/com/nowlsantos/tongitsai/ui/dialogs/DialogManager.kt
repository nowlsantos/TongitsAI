package com.nowlsantos.tongitsai.ui.dialogs

import androidx.compose.runtime.Composable
import com.nowlsantos.tongitsai.data.GameData
import com.nowlsantos.tongitsai.presentation.GameInput.ChallengeDecision
import com.nowlsantos.tongitsai.presentation.GameInput.RoundEnd
import com.nowlsantos.tongitsai.presentation.GameViewModel

@Composable
fun DialogManager(
    viewModel: GameViewModel,
    gameRound: Int,
    gamePhase: com.nowlsantos.tongitsai.domain.fsm.game.GamePhase,
    callerId: String,
    callerName: String,
    challengerId: String,
    challengeDialog: Boolean,
    winnerDialog: Boolean,
    initialDialog: Boolean,
    isNext: Boolean,
    gameResultDialog: Boolean,
    message: String,
    gameData: GameData?,
    onDismissCallChallenge: () -> Unit
) {
    if (initialDialog && !challengeDialog) {
        InitialDialog(
            message = message,
            onDismiss = { }
        )
    }

    if (challengeDialog) {
        CallChallengeDialog(
            callerName = callerName,
            message = message,
            isInitial = initialDialog,
            isNext = isNext,
            onDismiss = {
                onDismissCallChallenge()
                viewModel.onEvent(
                    ChallengeDecision(
                        callerId = callerId,
                        challengerId = challengerId,
                        accepts = false
                    )
                )
            },
            onAccept = {
                onDismissCallChallenge()
                viewModel.onEvent(
                    ChallengeDecision(
                        callerId = callerId,
                        challengerId = challengerId,
                        accepts = true
                    )
                )
            },
            onFold = {
                onDismissCallChallenge()
                viewModel.onEvent(
                    ChallengeDecision(
                        callerId = callerId,
                        challengerId = challengerId,
                        accepts = false
                    )
                )
            }
        )
    }

    if (winnerDialog) {
        RoundWinnerDialog(
            gameRound = gameRound,
            message = message,
            onDismiss = {
                // Show the Score Tally table instead of auto-skipping
                viewModel.onShowGameResult()
            }
        )
    }

    if (gameResultDialog && gameData != null) {
        val isRoundEnding = gamePhase == com.nowlsantos.tongitsai.domain.fsm.game.GamePhase.ROUND_END
        GameResultsDialog(
            gameData = gameData,
            buttonText = if (isRoundEnding) "NEXT ROUND" else "Close",
            onDismissRequest = {
                viewModel.dismissResultsDialog()
                if (isRoundEnding) {
                    viewModel.saveGameResults()
                    viewModel.onEvent(RoundEnd)
                }
            }
        )
    }
}