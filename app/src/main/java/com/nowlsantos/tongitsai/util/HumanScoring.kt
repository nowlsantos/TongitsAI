package com.nowlsantos.tongitsai.util

import com.nowlsantos.tongitsai.domain.model.Meld
import com.nowlsantos.tongitsai.domain.model.PlayingCard

data class RoundScoreResult(
    val bestMelds: List<Meld>,
    val leftoverCards: List<PlayingCard>,
    val score: Int
)

object HumanScoring {
    fun calculateRoundScore(hand: List<PlayingCard>): RoundScoreResult {
        val bestMelds = findBestMeldCombination(hand)
        val meldCards = bestMelds.flatMap { it.cards }.toSet()
        val leftover = hand.filter { it !in meldCards }
        val score = leftover.sumOf { it.rank.points }
        return RoundScoreResult(bestMelds, leftover, score)
    }

    fun findBestMeldCombination(hand: List<PlayingCard>): List<Meld> {
        val allMelds = generateAllPossibleMelds(hand)
        return bestCombination(allMelds, hand)
    }

    private fun generateAllPossibleMelds(hand: List<PlayingCard>): List<Meld> {
        val melds = mutableListOf<Meld>()

        // Runs (same suit, consecutive ranks)
        val groupedBySuit = hand.groupBy { it.suit }
        for ((_, cards) in groupedBySuit) {
            val sorted = cards.sortedBy { it.rank.points }
            var run = mutableListOf<PlayingCard>()
            for (card in sorted) {
                if (run.isEmpty() || card.rank.points == run.last().rank.points + 1) {
                    run.add(card)
                } else {
                    if (run.size >= 3) melds.add(Meld(run.toList()))
                    run = mutableListOf(card)
                }
            }
            if (run.size >= 3) melds.add(Meld(run.toList()))
        }

        // Sets (same rank, different suits)
        val groupedByRank = hand.groupBy { it.rank }
        for ((_, cards) in groupedByRank) {
            if (cards.size >= 3) melds.add(Meld(cards))
        }

        return melds
    }

    private fun bestCombination(melds: List<Meld>, hand: List<PlayingCard>): List<Meld> {
        var best: List<Meld> = emptyList()
        var bestScore = Int.MAX_VALUE

        fun dfs(index: Int, chosen: List<Meld>, used: Set<PlayingCard>) {
            if (index == melds.size) {
                val meldCards = chosen.flatMap { it.cards }.toSet()
                val leftover = hand.filter { it !in meldCards }
                val score = leftover.sumOf { it.rank.points }
                if (score < bestScore) {
                    bestScore = score
                    best = chosen
                }
                return
            }

            // Option 1: skip this meld
            dfs(index + 1, chosen, used)

            // Option 2: take this meld (if no overlap)
            val meld = melds[index]
            if (meld.cards.none { it in used }) {
                dfs(index + 1, chosen + meld, used + meld.cards)
            }
        }

        dfs(0, emptyList(), emptySet())
        return best
    }

    /*
    ✅ Example usage
    val result = calculateRoundScore(humanHand)

    println("Best melds: ${result.bestMelds}")
    println("Leftover: ${result.leftoverCards}")
    println("Score: ${result.score}")*/


    /*
    bestMelds → list of chosen melds.
    leftoverCards → cards not used in any meld.
    score → total penalty points.*/
}