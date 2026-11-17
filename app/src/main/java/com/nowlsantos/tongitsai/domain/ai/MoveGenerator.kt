package com.nowlsantos.tongitsai.domain.ai

import androidx.compose.ui.unit.DpOffset
import com.nowlsantos.tongitsai.domain.fsm.player.PlayerPhase
import com.nowlsantos.tongitsai.domain.model.GameState
import com.nowlsantos.tongitsai.domain.model.Meld
import com.nowlsantos.tongitsai.domain.model.Player
import com.nowlsantos.tongitsai.domain.model.PlayingCard
import com.nowlsantos.tongitsai.domain.model.enums.Rank
import com.nowlsantos.tongitsai.domain.model.enums.Suit
import com.nowlsantos.tongitsai.presentation.GameInput
import com.nowlsantos.tongitsai.util.Logger

object MoveGenerator {
    private const val tag = "MoveGenerator"
    private var opponentsHaveAnyMelds: Boolean = false

    fun generateLegalMoves(
        gameState: GameState,
        player: Player,
        playerPhase: PlayerPhase
    ): List<GameInput> {
        opponentsHaveAnyMelds = gameState.players.any { it.id != player.id && it.melds.isNotEmpty() }

        return when (playerPhase) {
            PlayerPhase.CALL -> generateCallMove(gameState, player)
            PlayerPhase.GET -> generateDrawMove(gameState, player)
            PlayerPhase.CHOW -> generateChowMove(gameState, player)
            PlayerPhase.MELD -> generateMeldMove(gameState, player)
            PlayerPhase.SAPAW -> generateSapawMove(gameState, player)
            PlayerPhase.DISCARD -> {
                val aiHand = AiHandRepository.getAiHand(player)
                val visibleCards = aiHand.remainingCards
                generateDiscardMove(gameState, player, visibleCards)
            }
            else -> emptyList()
        }
    }

    private fun generateCallMove(gameState: GameState, player: Player): List<GameInput.MakeCall> {
        if (player.melds.isEmpty()) {
            Logger.d(tag, "CALL: ${player.name} has NO public meld -> CALL is forbidden.")
            return emptyList()
        }
        if (player.melds.any { it.hasSapaw }) {
            Logger.d(tag, "CALL: ${player.name} has a public meld with a sapaw -> CALL is forbidden.")
            return emptyList()
        }

        // Tactical Heuristic: Do not call if an opponent has very few cards (e.g. <= 3 cards) and we still hold deadwood.
        val ourCardCount = player.cards.size
        val opponents = gameState.players.filter { it.id != player.id }
        val opponentMinCardCount = opponents.minOfOrNull { it.cards.size } ?: 13

        val secretMelds = AiHandRepository.getSecretMelds(player)
        val secretCards = secretMelds.flatMap { it.cards }.toSet()
        val deadwoodCards = player.cards.filter { it !in secretCards }
        val hasDeadwood = deadwoodCards.isNotEmpty()

        if (opponentMinCardCount <= 3 && ourCardCount > opponentMinCardCount && hasDeadwood) {
            Logger.d(tag, "CALL: ${player.name} has $ourCardCount cards with deadwood, but opponent has only $opponentMinCardCount cards -> CALL is forbidden (too risky / obvious bluff).")
            return emptyList()
        }

        Logger.d(tag, "CALL: ${player.name} has a legal option to CALL.")
        return listOf(GameInput.MakeCall(caller = player))
    }

    private fun generateDrawMove(gameState: GameState, player: Player): List<GameInput> =
        if (gameState.deck.isEmpty) emptyList() else listOf(GameInput.DrawCard)

    private fun generateChowMove(gameState: GameState, player: Player): List<GameInput.ChowCard> {
        val discardTop = gameState.discardPile.topCard ?: return emptyList()

        // Protect secret meld cards in hand during Chow generation
        val secretMeldCards = AiHandRepository.getSecretMelds(player).flatMap { it.cards }.toSet()
        val nonSecretHand = player.cards.filterNot { it in secretMeldCards }
        val hand = nonSecretHand.ifEmpty { player.cards }

        val result = mutableListOf<GameInput.ChowCard>()
        val sameRankCards = hand.filter { it.rank == discardTop.rank }

        // If there are at least two cards in hand with the same rank as the discarded card,
        // a valid move can be made using all of them.
        if (sameRankCards.size >= 2) {
            val selectedCards = mutableListOf<PlayingCard>().apply {
                addAll(sameRankCards)
            }
            result.add(
                GameInput.ChowCard(
                    chowCard = discardTop,
                    disCardPileOffset = DpOffset.Zero,
                    selectedCards = selectedCards,
                    sourceCardOffsets = selectedCards.associateWith { DpOffset.Zero },
                )
            )
        }

        val suit = discardTop.suit
        val sameSuitCards = hand.filter { it.suit == suit }
        if (sameSuitCards.size >= 2) {
            val suitMap = sameSuitCards.associateBy { it.rank.sequentialValue }
            val availableRanks = suitMap.keys.toMutableSet()
            val discardRank = discardTop.rank.sequentialValue
            availableRanks.add(discardRank)
            val minRank = availableRanks.minOrNull() ?: discardRank
            val maxRank = availableRanks.maxOrNull() ?: discardRank

            for (start in minRank..discardRank) {
                for (end in discardRank..maxRank) {
                    val length = end - start + 1
                    if (length < 3) continue
                    var ok = true
                    for (r in start..end) {
                        if (r == discardRank) continue
                        if (!suitMap.containsKey(r)) {
                            ok = false; break
                        }
                    }
                    if (!ok) continue
                    val selectedRanks = (start..end).filter { it != discardRank }
                    val selectedCards = selectedRanks.map { suitMap[it]!! }
                    if (selectedCards.size >= 2) {
                        result.add(
                            GameInput.ChowCard(
                                chowCard = discardTop,
                                disCardPileOffset = DpOffset.Zero,
                                selectedCards = selectedCards,
                                sourceCardOffsets = selectedCards.associateWith { DpOffset.Zero },
                            )
                        )
                    }
                }
            }
        }
        val distinctMoves = result.distinctBy { it.chowCard to it.selectedCards.toSet() }

        // Filter out any Chow move whose selectedCards set is a strict subset of another Chow move's selectedCards set
        val maximalMoves = distinctMoves.filter { move ->
            val moveSet = move.selectedCards.toSet()
            distinctMoves.none { other ->
                other != move && moveSet.size < other.selectedCards.size && other.selectedCards.containsAll(moveSet)
            }
        }

        return maximalMoves.sortedByDescending { it.selectedCards.size }
    }

    fun generateMeldMove(gameState: GameState, player: Player): List<GameInput.MeldCards> {
        // Use all cards for meld generation, regardless of secret status
        val hand = player.cards

        val possibleMelds = findAllValidMelds(hand)

        // Enhanced logging for debugging
        Logger.d(tag, "MELD GENERATION: ${player.name}")
        Logger.d(tag, " - Full Hand: ${hand.joinToString { "${it.rank.display}${it.suit.symbol}" }}")
        Logger.d(tag, " - Found ${possibleMelds.size} possible melds")
        possibleMelds.forEachIndexed { index, meld ->
            Logger.d(tag, "   Meld $index: ${meld.cards.joinToString { "${it.rank.display}${it.suit.symbol}" }}")
        }

        return possibleMelds.map { meld ->
            GameInput.MeldCards(
                cards = meld.cards,
                sourceCardOffsets = meld.cards.associateWith { DpOffset.Zero },
                targetMeldCenterOffset = DpOffset.Zero
            )
        }
    }

    private fun generateSapawMove(gameState: GameState, player: Player): List<GameInput.SapawCard> {
        val aiHand = AiHandRepository.getAiHand(player)
        val sapawMoves = mutableListOf<GameInput.SapawCard>()

        // Generate moves for all valid cards and all possible targets
        for (card in aiHand.remainingCards) { // Use remainingCards, not player.cards
            if (card in aiHand.secretMelds.flatMap { it.cards }) continue

            for ((_, _, _, _, position, _, melds) in gameState.players) {
                for ((meldIndex, meld) in melds.withIndex()) {
                    if (meld.canAdd(card)) {
                        sapawMoves.add(
                            GameInput.SapawCard(
                                sapawCard = card,
                                startOffset = DpOffset.Zero,
                                sapawTarget = Triple(DpOffset.Zero, position, meldIndex)
                            )
                        )
                    }
                }
            }
        }

        return sapawMoves
    }

    fun generateDiscardMove(
        gameState: GameState,
        player: Player,
        visibleCards: List<PlayingCard>
    ): List<GameInput.DiscardCard> {
        val baseCards = if (visibleCards.isNotEmpty()) visibleCards else player.cards
        if (baseCards.isEmpty()) return emptyList()

        // Get AI's secret meld cards to exclude from discard considerations
        val secretMeldCards = AiHandRepository.getAiHand(player).secretMelds.flatMap { it.cards }.toSet()
        // Filter out secret cards from the visible hand, falling back if all cards are in secret melds
        val hand = baseCards.filterNot { it in secretMeldCards }
            .ifEmpty { baseCards }
            .toMutableList()

        if (hand.isEmpty()) return emptyList()

        // Filter out cards that can be Sapawed onto the player's OWN melds on the table
        val ownMelds = player.melds
        val nonOwnSapawHand = hand.filterNot { card ->
            ownMelds.any { meld -> meld.canAdd(card) }
        }.ifEmpty { hand }

        // Collect all opponent melds to check for sapaw risks
        val opponentMelds = gameState.players.filter { it.id != player.id }.flatMap { it.melds }

        // Separate sapaw risk cards from safe cards
        val (sapawRiskCards, safeCards) = nonOwnSapawHand.partition { card ->
            opponentMelds.any { meld -> meld.canAdd(card) }
        }

        // Use safe cards if available, otherwise use sapaw risk cards as last resort
        val cardsToConsider = safeCards.ifEmpty { sapawRiskCards }

        // Find partial melds
        val partialMelds = findPartialMelds(cardsToConsider)

        // Collect all visible cards to check for dead partials
        val allVisibleCards = gameState.players.flatMap { it.melds.flatMap { meld -> meld.cards } } +
                gameState.discardPile.cards +
                visibleCards.filterNot { it in secretMeldCards }

        // Enhanced: Track which cards are no longer available (in discard pile or melded by others)
        val unavailableCards = gameState.discardPile.cards.toSet() +
                gameState.players.filter { it.id != player.id }
                    .flatMap { it.melds.flatMap { meld -> meld.cards } }

        // Categorize partial melds based on completion possibility and value
        val (deadPartialMelds, livePartialMelds) = categorizePartialMelds(
            partialMelds, allVisibleCards, unavailableCards, cardsToConsider
        )

        // Identify orphan cards (not part of any partial meld)
        val orphanCards = cardsToConsider.filter { card ->
            !partialMelds.any { it.contains(card) }
        }.toSet()

        // Categorize orphans by value
        val highOrphans = orphanCards.filter { it.rank.sequentialValue >= 7 }
        val mediumOrphans = orphanCards.filter { it.rank.sequentialValue in 4..6 }
        val lowOrphans = orphanCards.filter { it.rank.sequentialValue <= 3 }

        // UPDATED PRIORITY ORDER AS SPECIFIED:
        // 1. Dead high-value partial melds (cards needed are unavailable)
        // 2. High-value orphans
        // 3. Medium-value orphans
        // 4. Live high-value partial melds (can still be completed)
        // 5. Dead low-value partial melds (only if no other options)
        // 6. Live low-value partial melds (only if no other options)
        // 7. Low-value orphans (absolute last resort)

        val discardCandidates = when {
            // Priority 1: Dead high-value partial melds
            deadPartialMelds.highValue.isNotEmpty() -> {
                deadPartialMelds.highValue.sortedByDescending { it.rank.sequentialValue }
            }

            // Priority 2: High-value orphans
            highOrphans.isNotEmpty() -> {
                highOrphans.sortedByDescending { it.rank.sequentialValue }
            }

            // Priority 3: Medium-value orphans
            mediumOrphans.isNotEmpty() -> {
                mediumOrphans.sortedByDescending { it.rank.sequentialValue }
            }

            // Priority 4: Live high-value partial melds
            livePartialMelds.highValue.isNotEmpty() -> {
                livePartialMelds.highValue.sortedByDescending { it.rank.sequentialValue }
            }

            // Priority 5: Dead low-value partial melds
            deadPartialMelds.lowValue.isNotEmpty() -> {
                deadPartialMelds.lowValue.sortedByDescending { it.rank.sequentialValue }
            }

            // Priority 6: Live low-value partial melds
            livePartialMelds.lowValue.isNotEmpty() -> {
                livePartialMelds.lowValue.sortedByDescending { it.rank.sequentialValue }
            }

            // Last resort: Low-value orphans
            else -> {
                lowOrphans.sortedByDescending { it.rank.sequentialValue }
            }
        }

        // Return all candidates in priority order
        return discardCandidates.map { card ->
            GameInput.DiscardCard(card, DpOffset.Zero, DpOffset.Zero)
        }
    }

    // Helper function to categorize partial melds based on completion possibility and value
    private fun categorizePartialMelds(
        partialMelds: List<Set<PlayingCard>>,
        allVisibleCards: List<PlayingCard>,
        unavailableCards: Set<PlayingCard>,
        cardsToConsider: List<PlayingCard>
    ): Pair<PartialMeldCategories, PartialMeldCategories> {
        val deadHighValue = mutableSetOf<PlayingCard>()
        val deadLowValue = mutableSetOf<PlayingCard>()
        val liveHighValue = mutableSetOf<PlayingCard>()
        val liveLowValue = mutableSetOf<PlayingCard>()

        for (meld in partialMelds) {
            val waitingCards = findWaitingCards(meld)
            val isDead = waitingCards.all { waitingCard ->
                waitingCard in allVisibleCards || waitingCard in unavailableCards
            }

            // Categorize by value
            val maxCardValue = meld.maxOf { it.rank.sequentialValue }
            val isHighValue = maxCardValue >= 7

            if (isDead) {
                if (isHighValue) {
                    deadHighValue.addAll(meld)
                } else {
                    deadLowValue.addAll(meld)
                }
            } else {
                if (isHighValue) {
                    liveHighValue.addAll(meld)
                } else {
                    liveLowValue.addAll(meld)
                }
            }
        }

        return Pair(
            PartialMeldCategories(deadHighValue, deadLowValue),
            PartialMeldCategories(liveHighValue, liveLowValue)
        )
    }

    // Data class to hold categorized partial melds
    private data class PartialMeldCategories(
        val highValue: Set<PlayingCard>,
        val lowValue: Set<PlayingCard>
    )

    private fun isPartialMeldDead(meld: Set<PlayingCard>, gameState: GameState, player: Player): Boolean {
        val waitingCards = findWaitingCards(meld)
        if (waitingCards.isEmpty()) return false

        val unavailableCards = gameState.players
            .filter { it.id != player.id }
            .flatMap { it.melds.flatMap { meld -> meld.cards } }
            .plus(gameState.discardPile.cards)
            .toSet()

        // Consider deck size - in late game, assume cards are unavailable
        val isLateGame = gameState.deck.size < 20
        if (isLateGame) {
            return waitingCards.isNotEmpty()
        }

        return waitingCards.isNotEmpty() && waitingCards.all { it in unavailableCards }
    }

    private fun findWaitingCards(meld: Set<PlayingCard>): List<PlayingCard> {
        if (meld.size < 2) return emptyList()

        val sortedMeld = meld.sortedBy { it.rank.sequentialValue }
        val firstCard = sortedMeld.first()

        // Logic for SETS (e.g., two Kings)
        if (sortedMeld.all { it.rank == firstCard.rank }) {
            val existingSuits = sortedMeld.map { it.suit }.toSet()
            val neededSuits = Suit.entries.toSet() - existingSuits
            return neededSuits.map { PlayingCard(firstCard.rank, it) }
        }

        // Logic for RUNS (e.g., King-Queen of Spades)
        val ranks = sortedMeld.map { it.rank.sequentialValue }
        if (sortedMeld.all { it.suit == firstCard.suit } && (ranks.last() - ranks.first()) == (ranks.size - 1)) {
            val waitingCards = mutableListOf<PlayingCard>()
            val minRank = ranks.first()
            val maxRank = ranks.last()

            if (minRank > 1) waitingCards.add(PlayingCard(Rank.fromSequentialValue(minRank - 1), firstCard.suit))
            if (maxRank < 13) waitingCards.add(PlayingCard(Rank.fromSequentialValue(maxRank + 1), firstCard.suit))
            if (maxRank == 13 && minRank == 12) waitingCards.add(PlayingCard(Rank.ACE, firstCard.suit)) // Q-K -> A
            if (minRank == 1 && maxRank == 2) waitingCards.add(PlayingCard(Rank.THREE, firstCard.suit)) // A-2 -> 3

            return waitingCards.distinct()
        }

        return emptyList()
    }

    fun findAllValidMelds(hand: List<PlayingCard>): List<Meld> {
        val melds = mutableListOf<Meld>()

        // 1. Sets (all 3 and 4 combinations per rank)
        val rankGroups = hand.groupBy { it.rank }
        for ((_, group) in rankGroups) {
            if (group.size >= 3) {
                val maxSize = minOf(4, group.size)
                for (k in 3..maxSize) {
                    combinations(group, k).forEach { combo ->
                        val m = Meld(combo)
                        if (m.isValidSet()) melds.add(m)
                    }
                }
            }
        }

        // 2. Runs (all sub-windows of length >= 3 per suit)
        val suitGroups = hand.groupBy { it.suit }
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
                            if (m.isValidRun()) melds.add(m)
                        }
                    }
                }
                start = end + 1
            }
        }

        return melds.distinctBy { it.cards.toSet() }
    }

    fun findPartialMelds(cards: List<PlayingCard>): List<Set<PlayingCard>> {
        val partials = mutableListOf<Set<PlayingCard>>()

        // Find partial sets (two cards of the same rank, different suits)
        val groupedByRank = cards.groupBy { it.rank }
        groupedByRank.values.forEach { group ->
            if (group.size >= 2) {
                // Check all combinations of 2 cards
                for (i in 0 until group.size - 1) {
                    for (j in i + 1 until group.size) {
                        partials.add(setOf(group[i], group[j]))
                    }
                }
            }
        }

        // Find partial runs (two cards of the same suit, sequential ranks)
        val groupedBySuit = cards.groupBy { it.suit }
        groupedBySuit.values.forEach { group ->
            // Check all combinations of 2 cards within the same suit
            for (i in 0 until group.size - 1) {
                for (j in i + 1 until group.size) {
                    val c1 = group[i]
                    val c2 = group[j]
                    val diff = kotlin.math.abs(c1.rank.sequentialValue - c2.rank.sequentialValue)
                    if (diff == 1) {
                        partials.add(setOf(c1, c2))
                    }
                }
            }
        }

        return partials.distinct()
    }

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