package com.nowlsantos.tongitsai.util

import com.nowlsantos.tongitsai.domain.model.Meld
import com.nowlsantos.tongitsai.domain.model.PlayingCard

/**
 * MeldSolver: choose a set of non-overlapping melds that maximizes total points covered by melds
 * (equivalently minimize deadwood). Uses DFS/backtracking and is exact for typical hand sizes.
 */
object MeldSolver {

    /**
     * Returns Pair(selectedMelds, remainingCards).
     * selectedMelds: chosen melds (non-overlapping), maximizing points covered.
     * remainingCards: cards not covered by the chosen melds.
     */
    fun selectBestMelds(cards: List<PlayingCard>): Pair<List<Meld>, List<PlayingCard>> {
        if (cards.isEmpty()) return Pair(emptyList(), emptyList())
        val candidates = generateCandidateMelds(cards)
        val best = backtrackChoose(candidates)
        val chosen = best.selectedMelds
        val covered = chosen.flatMap { it.cards }.toSet()
        val remaining = cards.filter { it !in covered }
        return Pair(chosen, remaining)
    }

    // Data class for backtrack result
    private data class BacktrackResult(val score: Int, val selectedMelds: List<Meld>)

    // Backtracking: choose subset of disjoint candidates maximizing sum of card.points
    private fun backtrackChoose(candidates: List<Meld>): BacktrackResult {
        var best = BacktrackResult(0, emptyList())

        // Precompute candidate card-points (sum of ranks points)
        val candPoints = candidates.map { it.cards.sumOf { c -> c.rank.points } }

        // Simple upper-bound pruning: prefix sums of remaining potentials (descending)
        val remainingMaxFrom = IntArray(candidates.size + 1)
        var suffix = 0
        for (i in candidates.size - 1 downTo 0) {
            suffix += candPoints[i]
            remainingMaxFrom[i] = suffix
        }
        remainingMaxFrom[candidates.size] = 0

        fun dfs(index: Int, chosen: MutableList<Meld>, used: MutableSet<PlayingCard>, scoreSoFar: Int) {
            if (index >= candidates.size) {
                if (scoreSoFar > best.score) best = BacktrackResult(scoreSoFar, chosen.toList())
                return
            }
            // prune by optimistic upper bound
            val optimistic = scoreSoFar + remainingMaxFrom[index]
            if (optimistic <= best.score) return

            // option: skip
            dfs(index + 1, chosen, used, scoreSoFar)

            // option: take if disjoint
            val cand = candidates[index]
            if (cand.cards.none { it in used }) {
                chosen.add(cand)
                used.addAll(cand.cards)
                val addScore = cand.cards.sumOf { it.rank.points }
                dfs(index + 1, chosen, used, scoreSoFar + addScore)
                chosen.removeAt(chosen.size - 1)
                used.removeAll(cand.cards)
            }
        }

        dfs(0, mutableListOf(), mutableSetOf(), 0)
        return best
    }

    /**
     * Generate candidate melds:
     *  - sets: all 3..4 combinations per rank,
     *  - runs: maximal runs (length >= 3) per suit (we prefer maximal windows to reduce redundant subsets).
     */
    private fun generateCandidateMelds(cards: List<PlayingCard>): List<Meld> {
        val out = mutableListOf<Meld>()

        // Sets
        val rankGroups = cards.groupBy { it.rank }
        for ((_, group) in rankGroups) {
            if (group.size >= 3) {
                val maxSize = minOf(4, group.size)
                for (k in 3..maxSize) {
                    combinations(group, k).forEach { combo ->
                        val m = Meld(combo)
                        if (m.isValidSet()) out.add(m)
                    }
                }
            }
        }

        // Runs: find all consecutive runs of length >= 3 per suit
        val suitGroups = cards.groupBy { it.suit }
        for ((_, group) in suitGroups) {
            val sorted = group.sortedBy { it.rank.ordinal }
            if (sorted.size < 3) continue
            var start = 0
            while (start < sorted.size) {
                var end = start
                while (end + 1 < sorted.size && sorted[end + 1].rank.ordinal == sorted[end].rank.ordinal + 1) {
                    end++
                }
                val length = end - start + 1
                if (length >= 3) {
                    val run = sorted.subList(start, end + 1)
                    for (k in 3..length) {
                        for (windowStart in 0..(length - k)) {
                            val subRun = run.subList(windowStart, windowStart + k)
                            val m = Meld(subRun)
                            if (m.isValidRun()) out.add(m)
                        }
                    }
                }
                start = end + 1
            }
        }

        return out.distinctBy { it.cards.toSet() }
    }

    // combinations utility
    private fun <T> combinations(list: List<T>, k: Int): List<List<T>> {
        val out = mutableListOf<List<T>>()
        if (k <= 0 || k > list.size) return out
        val indices = IntArray(k) { it }
        while (true) {
            out.add(indices.map { list[it] })
            var i = k - 1
            while (i >= 0) {
                if (indices[i] < list.size - k + i) {
                    indices[i]++
                    for (j in i + 1 until k) indices[j] = indices[j - 1] + 1
                    break
                } else i--
            }
            if (i < 0) break
        }
        return out
    }
}