package com.nowlsantos.tongitsai.util

import com.nowlsantos.tongitsai.domain.model.Meld
import com.nowlsantos.tongitsai.domain.model.PlayingCard

object ScoreUtils {
    /**
     * Calculate round score for a player:
     * - Use MeldSolver to find best set of melds from the *physical hand* (cards visible for human player)
     * - Remaining cards (deadwood) are summed as penalty (points)
     *
     * Returns Pair(selectedMelds, deadwoodCards)
     */
    fun bestMeldsAndDeadwood(cards: List<PlayingCard>): Pair<List<Meld>, List<PlayingCard>> {
        return MeldSolver.selectBestMelds(cards)
    }

    /**
     * Calculate numeric round penalty (deadwood points) after extracting best melds.
     */
    fun calculateRoundPenalty(cards: List<PlayingCard>): Int {
        val (_, remaining) = bestMeldsAndDeadwood(cards)
        return remaining.sumOf { it.rank.points }
    }
}
