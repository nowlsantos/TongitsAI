package com.nowlsantos.tongitsai.presentation

import com.nowlsantos.tongitsai.domain.fsm.game.GameStateMachine
import com.nowlsantos.tongitsai.domain.model.Player
import com.nowlsantos.tongitsai.domain.model.enums.ChallengerType
import com.nowlsantos.tongitsai.util.GameConstants
import com.nowlsantos.tongitsai.util.Logger

class PaymentProcessor(
    private val stateMachine: GameStateMachine
) {
    private val tag = "PaymentProcessor"

    /**
     * A generic function to process chip payments based on flexible deduction logic.
     * Purely handles financial transactions (balances and pot money).
     * @param winnerId The ID of the winning player.
     * @param getBaseDeduction Function to calculate base deduction per player.
     */
    private fun processPayment(
        winnerId: String,
        getBaseDeduction: (player: Player) -> Int
    ) {
        stateMachine.updateState { state ->
            var totalWinnings = 0
            var centerFeeWon = 0
            val wonTwoHits = state.lastRoundWonTwoHits
            val isCenterPotWon = wonTwoHits // Center pot is claimed ONLY when hitCount == 2 (wonTwoHits), regardless of win type

            val updatedPlayers = state.players.map { player ->
                if (player.id != winnerId) {
                    var deduction = getBaseDeduction(player)
                    if (player.melds.isEmpty()) {
                        deduction += GameConstants.NO_MELDS
                    }
                    val actualDeduction = minOf(player.balance, deduction)
                    totalWinnings += actualDeduction
                    Logger.d(tag, "DeductionAmount Loser: ${player.name}:: $actualDeduction")
                    player.copy(
                        balance = (player.balance - actualDeduction).coerceAtLeast(0)
                    )
                } else {
                    // Center pot is won ONLY on 2 consecutive hits (wonTwoHits), regardless of win type
                    if (isCenterPotWon) {
                        centerFeeWon = state.centerFee
                    }
                    player
                }
            }.map { player ->
                if (player.id == winnerId) {
                    val finalWinnings = totalWinnings + centerFeeWon
                    Logger.d(tag, "Winnings Winner: ${player.name}:: $finalWinnings | CenterPotWon:: $isCenterPotWon (TwoHits=$wonTwoHits)")
                    player.copy(
                        balance = player.balance + finalWinnings
                    )
                } else {
                    player
                }
            }

            // Center pot resets to 0 ONLY when claimed by 2 consecutive hits
            val newCenterFee = if (isCenterPotWon) 0 else state.centerFee

            // Check if any player is bankrupt (balance <= 0)
            val bankruptPlayer = updatedPlayers.firstOrNull { it.balance <= 0 }
            if (bankruptPlayer != null) {
                val overallWinner = updatedPlayers.maxByOrNull { it.balance } ?: updatedPlayers.first()
                Logger.d(tag, "BANKRUPTCY DETECTED! Player ${bankruptPlayer.name} is bankrupt. Game Over! Overall Winner: ${overallWinner.name}")
                state.copy(
                    players = updatedPlayers,
                    centerFee = newCenterFee,
                    gameWinnerId = overallWinner.id,
                    isGameOver = true
                )
            } else {
                state.copy(
                    players = updatedPlayers,
                    centerFee = newCenterFee
                )
            }
        }
        stateMachine.onRoundPaymentProcessed?.invoke(winnerId)
    }

    fun normalPayment(winnerId: String) {
        processPayment(winnerId) { GameConstants.NORMAL_DEDUCTION }
    }

    fun challengePayment(winnerId: String) {
        processPayment(winnerId) { player ->
            when (player.challengerType) {
                ChallengerType.CALLEE,
                ChallengerType.LAST_CALLEE -> GameConstants.CHALLENGE_DEDUCTION

                ChallengerType.CALLER -> GameConstants.CHALLENGER_FEE

                ChallengerType.FOLDEE,
                ChallengerType.LAST_FOLDEE -> GameConstants.FOLDEE_FEE

                else -> GameConstants.NORMAL_DEDUCTION
            }
        }
    }

    fun tongitsPayment(winner: Player) {
        processPayment(winner.id) { GameConstants.TONGITS_DEDUCTION }
    }
}
