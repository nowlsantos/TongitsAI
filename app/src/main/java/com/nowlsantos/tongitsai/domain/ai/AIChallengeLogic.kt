package com.nowlsantos.tongitsai.domain.ai

import com.nowlsantos.tongitsai.domain.model.GameState
import com.nowlsantos.tongitsai.domain.model.Player
import com.nowlsantos.tongitsai.util.Logger
import com.nowlsantos.tongitsai.util.ScoreUtils
import kotlin.random.Random

/**
 * A class containing the AI's logic for making a decision during a call challenge.
 * This engine determines whether the AI player should accept or fold the challenge.
 */
object AIChallengeLogic {
    private val tag = "AIChallengeLogic"

    /**
     * Determines the AI's decision to accept or fold a call based on a probabilistic model.
     * The logic is based on a score that represents the AI's perceived probability of winning.
     *
     * @param player The AI player making the decision.
     * @param gameState The current state of the game.
     * @return true if the AI accepts the challenge, false if it folds.
     */
    fun decideCallChallenge(player: Player, gameState: GameState): Boolean {
        // Find the player who made the call.
        val caller = gameState.players.firstOrNull { it.id == gameState.callerId } ?: return false
        if (player.melds.isEmpty()) return false

        // ----- BEGIN PROBABILITY-BASED SCORING -----
        var perceivedProbabilityOfWinning = 0.0

        // Factor 1: The AI's own hand strength (most important).
        // Use ScoreUtils to get the best possible score from the AI's current hand.
        val aiHandScore = ScoreUtils.calculateRoundPenalty(player.cards)
        val aiMeldCount = ScoreUtils.bestMeldsAndDeadwood(player.cards).first.size

        // The lower the AI's score, the higher its perceived probability of winning.
        /*perceivedProbabilityOfWinning += when {
            aiHandScore <= 5 -> 0.6  // Very strong hand, 60% confidence
            aiHandScore <= 15 -> 0.4 // Good hand, 40% confidence
            aiHandScore <= 30 -> 0.2 // Medium hand, 20% confidence
            else -> 0.0              // Weak hand, very low confidence
        }*/
        perceivedProbabilityOfWinning += when {
            aiHandScore <= 5 -> 0.8  // Increased confidence from 0.6 to 0.8
            aiHandScore <= 15 -> 0.6 // Increased confidence from 0.4 to 0.6
            aiHandScore <= 30 -> 0.4 // Increased confidence from 0.2 to 0.4
            else -> 0.1              // Small confidence boost even for weak hands
        }

        // Factor 2: The number of melds the AI has.
        // Each meld significantly increases confidence.
        perceivedProbabilityOfWinning += aiMeldCount * 0.15

        // Factor 3: The caller's hand size (inference of their strength).
        val callerCardCount = caller.cards.size

        // A smaller hand size for the caller implies a stronger hand. This lowers the AI's confidence.
        /*perceivedProbabilityOfWinning -= when {
            callerCardCount <= 3 -> 0.4 // The caller is very strong, lowers probability significantly.
            callerCardCount <= 5 -> 0.2 // The caller is likely strong, lowers probability.
            else -> 0.0                 // Caller still has a large hand. No negative impact.
        }*/
        perceivedProbabilityOfWinning -= when {
            callerCardCount <= 3 -> 0.2 // Reduced negative impact from 0.4 to 0.2
            callerCardCount <= 5 -> 0.1 // Reduced negative impact from 0.2 to 0.1
            else -> 0.0                 // No negative impact
        }

        // Factor 4: Randomness to make the AI less predictable.
        // Adds a random element to the final decision.
        perceivedProbabilityOfWinning += Random.nextDouble(-0.1, 0.1)

        // ----- END PROBABILITY-BASED SCORING -----

        // Clamp the final probability score between 0 and 1.
        perceivedProbabilityOfWinning = perceivedProbabilityOfWinning.coerceIn(0.0, 1.0)

        // The AI decides to challenge if the final score is above a certain threshold.
        // A threshold of 0.5 means the AI will challenge if it feels it has a better-than-even chance of winning.
        val decisionThreshold = 0.5
        val shouldChallenge = perceivedProbabilityOfWinning >= decisionThreshold

        Logger.d(tag, "${player.name} is deciding on the call:")
        Logger.d(tag, " - My Hand Score: $aiHandScore | Melds: $aiMeldCount")
        Logger.d(tag, " - Caller's Cards: $callerCardCount")
        Logger.d(tag, " - Final Probability of Winning: %.2f".format(perceivedProbabilityOfWinning))
        Logger.d(tag, " - Decision: ${if (shouldChallenge) "ACCEPT" else "FOLD"}")

        return shouldChallenge
    }
}