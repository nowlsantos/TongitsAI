package com.nowlsantos.tongitsai.domain.ai

import androidx.compose.ui.unit.DpOffset
import com.nowlsantos.tongitsai.domain.fsm.player.PlayerPhase
import com.nowlsantos.tongitsai.domain.fsm.player.PlayerStateMachine
import com.nowlsantos.tongitsai.domain.model.GameState
import com.nowlsantos.tongitsai.domain.model.Meld
import com.nowlsantos.tongitsai.domain.model.Player
import com.nowlsantos.tongitsai.domain.model.PlayingCard
import com.nowlsantos.tongitsai.presentation.GameInput
import com.nowlsantos.tongitsai.util.Logger

class AiMinimax {
    private val tag = "AiMinimax"

    fun chooseMove(
        gameState: GameState,
        player: Player,
        playerStateMachine: PlayerStateMachine,
        depth: Int = 3,
        alpha: Int = Int.MIN_VALUE,
        beta: Int = Int.MAX_VALUE
    ): GameInput? {
        // Update secret melds based on current hand before making any decisions
        updateSecretMeldsFromHand(player)

        val aiHand = AiHandRepository.getAiHand(player)

        if (aiHand.secretMelds.isNotEmpty()) {
            val desc = aiHand.secretMelds.joinToString("; ") { meld ->
                meld.cards.joinToString(prefix = "[", postfix = "]") { c -> "${c.rank.display}${c.suit.symbol}" }
            }
            Logger.d(tag, "Player ${player.name} secret melds: $desc")
        }

        return when (val currentPhase = playerStateMachine.currentPhase.value) {
            PlayerPhase.CALL -> handleCallPhase(gameState, player)
            PlayerPhase.GET -> if (gameState.deck.isEmpty) null else GameInput.DrawCard
            PlayerPhase.CHOW -> handleChowPhase(gameState, player)
            PlayerPhase.MELD -> handleMeldPhase(gameState, player)
            PlayerPhase.SAPAW -> handleSapawPhase(gameState, player, aiHand)
            PlayerPhase.DISCARD -> handleDiscardPhase(gameState, player, aiHand)
            else -> {
                val moves = MoveGenerator.generateLegalMoves(gameState, player, currentPhase)
                if (moves.isEmpty()) return null
                findBestMove(gameState, player, playerStateMachine, moves, depth, alpha, beta)
            }
        }
    }

    private fun updateSecretMeldsFromHand(player: Player) {
        val aiHand = AiHandRepository.getAiHand(player)
        val currentSecretMelds = aiHand.secretMelds.toMutableList()

        // First, remove any secret melds that are no longer valid (cards have been used elsewhere)
        currentSecretMelds.forEach { secretMeld ->
            if (secretMeld.cards.any { card -> !player.cards.contains(card) }) {
                AiHandRepository.removeSecretMeld(player.id, secretMeld)
            }
        }

        // Find all possible melds in the current hand
        val allPossibleMelds = MoveGenerator.findAllValidMelds(player.cards)

        // Filter out melds that are already in secret melds or are subsets of existing melds
        val newMelds = allPossibleMelds.filter { newMeld ->
            // Check if this meld is already a secret meld
            val isAlreadySecret = currentSecretMelds.any { existingMeld ->
                existingMeld.cards.toSet() == newMeld.cards.toSet()
            }

            // Check if this meld is a subset of an existing secret meld
            val isSubset = currentSecretMelds.any { existingMeld ->
                newMeld.cards.all { it in existingMeld.cards } &&
                        newMeld.cards.size < existingMeld.cards.size
            }

            // Check if this meld is a superset of an existing secret meld
            val isSuperset = currentSecretMelds.any { existingMeld ->
                existingMeld.cards.all { it in newMeld.cards } &&
                        newMeld.cards.size > existingMeld.cards.size
            }

            // Only add if it's not already a secret, not a subset, and not a superset
            !isAlreadySecret && !isSubset && !isSuperset
        }

        // For superset cases, we need to replace the smaller meld with the larger one
        currentSecretMelds.forEach { existingMeld ->
            val supersetMeld = allPossibleMelds.find { newMeld ->
                existingMeld.cards.all { it in newMeld.cards } &&
                        newMeld.cards.size > existingMeld.cards.size
            }

            if (supersetMeld != null) {
                // Remove the smaller meld and add the larger one
                AiHandRepository.removeSecretMeld(player.id, existingMeld)
                AiHandRepository.addSecretMeld(player.id, supersetMeld)
            }
        }

        // Add new melds to secret melds
        newMelds.forEach { meld ->
            AiHandRepository.addSecretMeld(player.id, meld)
        }

        // Finally, ensure we don't have any overlapping melds
        removeOverlappingSecretMelds(player)
    }

    // Helper function to remove overlapping secret melds
    private fun removeOverlappingSecretMelds(player: Player) {
        val aiHand = AiHandRepository.getAiHand(player)
        val secretMelds = aiHand.secretMelds.toMutableList()
        val cardsInMultipleMelds = mutableSetOf<PlayingCard>()

        // Find cards that appear in multiple melds
        val cardCount = mutableMapOf<PlayingCard, Int>()
        secretMelds.flatMap { meld -> meld.cards }.forEach { card ->
            cardCount[card] = cardCount.getOrDefault(card, 0) + 1
            if (cardCount[card]!! > 1) {
                cardsInMultipleMelds.add(card)
            }
        }

        // If there are overlapping cards, remove the smaller melds
        if (cardsInMultipleMelds.isNotEmpty()) {
            // Sort melds by size (largest first) and remove overlapping ones
            val sortedMelds = secretMelds.sortedByDescending { it.cards.size }
            val nonOverlappingMelds = mutableListOf<Meld>()
            val usedCards = mutableSetOf<PlayingCard>()

            for (meld in sortedMelds) {
                if (meld.cards.none { it in usedCards }) {
                    nonOverlappingMelds.add(meld)
                    usedCards.addAll(meld.cards)
                }
            }

            // Replace all secret melds with the non-overlapping ones
            AiHandRepository.clearForPlayer(player.id)
            nonOverlappingMelds.forEach { meld ->
                AiHandRepository.addSecretMeld(player.id, meld)
            }
        }
    }

    private fun handleCallPhase(gameState: GameState, player: Player): GameInput? {
        val legalCallMoves = MoveGenerator.generateLegalMoves(gameState, player, PlayerPhase.CALL).filterIsInstance<GameInput.MakeCall>()
        if (legalCallMoves.isEmpty()) {
            Logger.d(tag, "CALL: No legal CALL moves available for ${player.name}.")
            return null
        }

        val aiHand = AiHandRepository.getAiHand(player)
        val opponents = gameState.players.filter { it.id != player.id }
        val gameStage = determineGameStage(gameState)

        // Mandatory Rule 3: Auto-call if AI has melds and opponents have none
        // But only in mid to late game, not in early game
        val opponentsHaveNoMelds = opponents.all { it.melds.isEmpty() }
        if (player.melds.isNotEmpty() && opponentsHaveNoMelds && gameStage != "early") {
            Logger.d(tag, "CALL: Rule 3 - Auto-calling as AI has melds and opponents have none.")
            return legalCallMoves.first()
        }

        // Only proceed with other rules if Rule 3 condition is not met
        val shouldCallByRule4 = shouldCallBasedOnHandAdvantage(aiHand, opponents, gameState, gameStage)
        if (shouldCallByRule4) {
            Logger.d(tag, "CALL: Rule 4 - Strategic call based on hand advantage.")
            return legalCallMoves.first()
        }

        val shouldBluffCall = shouldBluffCall(gameState, player, aiHand, gameStage)
        if (shouldBluffCall) {
            Logger.d(tag, "CALL: Rule 5 - Strategic bluff call.")
            return legalCallMoves.first()
        }

        Logger.d(tag, "CALL: ${player.name} decides NOT to CALL based on strategic evaluation.")
        return null
    }

    // Enhanced game stage detection
    private fun determineGameStage(gameState: GameState): String {
        //val totalCards = gameState.players.sumOf { it.cards.size } + gameState.discardPile.cards.size
        val deckSize = gameState.deck.size
        //val totalCardsInPlay = totalCards + deckSize

        return when {
            deckSize > 60 -> "early"   // More than half the deck remains
            deckSize > 30 -> "mid"     // About a quarter to half remains
            else -> "late"             // Less than a quarter remains
        }
    }

    // Enhanced hand evaluation with game stage consideration
    private fun shouldCallBasedOnHandAdvantage(
        aiHand: AiHand,
        opponents: List<Player>,
        gameState: GameState,
        gameStage: String
    ): Boolean {
        val remainingCardPoints = aiHand.remainingCards.sumOf { it.rank.points }
        val remainingSize = aiHand.remainingCards.size
        val opponentsHandSizes = opponents.map { it.cards.size }

        // Early game: Very conservative calling
        if (gameStage == "early") {
            // Only call in early game if we have a very strong advantage
            val allOpponentsMuchLarger = opponentsHandSizes.all { it > remainingSize + 3 }
            val veryLowPoints = remainingCardPoints <= 5

            return allOpponentsMuchLarger && veryLowPoints
        }

        // Mid to late game: Use the original logic with adjustments
        val allOpponentsLarger = opponentsHandSizes.all { it > remainingSize }
        val basicCondition = allOpponentsLarger && remainingCardPoints <= 10

        if (basicCondition) return true

        // Additional strategic considerations:

        // 1. Consider card distribution (high cards vs low cards)
        val highCardCount = aiHand.remainingCards.count { it.rank.sequentialValue >= 10 }
        val lowCardCount = remainingSize - highCardCount

        // Favorable if mostly low cards regardless of opponent hand size
        if (lowCardCount >= highCardCount * 2 && remainingCardPoints <= 15) {
            return true
        }

        // 2. Consider game phase (later game = more incentive to call)
        val totalCardsInPlay = gameState.players.sumOf { it.cards.size } + gameState.discardPile.cards.size
        val gameProgress = totalCardsInPlay / 108.0 // Assuming standard 108-card deck

        // More likely to call in later game with reasonable hand
        return gameProgress > 0.7 && remainingCardPoints <= 20
    }

    // More conservative bluff calling
    private fun shouldBluffCall(
        gameState: GameState,
        player: Player,
        aiHand: AiHand,
        gameStage: String
    ): Boolean {
        // Much lower base bluff chance, especially in early game
        val baseBluffChance = when (gameStage) {
            "early" -> 0.02  // Very low chance in early game
            "mid" -> 0.05    // Moderate chance in mid-game
            else -> 0.08     // Higher chance in late game
        }

        // Adjust bluff chance based on game situation
        var adjustedBluffChance = baseBluffChance

        // Increase bluff chance when significantly behind in points
        val aiPoints = player.melds.flatMap { it.cards }.sumOf { it.rank.points } +
                aiHand.remainingCards.sumOf { it.rank.points }
        val maxOpponentPoints = gameState.players
            .filter { it.id != player.id }
            .maxOf { opponent ->
                opponent.melds.flatMap { it.cards }.sumOf { it.rank.points } +
                        opponent.cards.sumOf { it.rank.points }
            }

        if (aiPoints < maxOpponentPoints * 0.7) {
            // Only increase bluff chance if significantly behind
            adjustedBluffChance += 0.05
        }

        // Decrease bluff chance when ahead
        if (aiPoints > maxOpponentPoints * 1.3) {
            adjustedBluffChance -= 0.03
        }

        // Additional factor: Number of opponents with melds
        val opponentsWithMelds = gameState.players.count { it.id != player.id && it.melds.isNotEmpty() }
        if (opponentsWithMelds > 0) {
            // Less likely to bluff if opponents already have melds
            adjustedBluffChance -= 0.02 * opponentsWithMelds
        }

        // Ensure bluff chance is within reasonable bounds
        adjustedBluffChance = adjustedBluffChance.coerceIn(0.0, 0.15)

        return kotlin.random.Random.nextDouble() < adjustedBluffChance
    }

    private fun handleChowPhase(gameState: GameState, player: Player): GameInput? {
        val chowMoves = MoveGenerator.generateLegalMoves(gameState, player, PlayerPhase.CHOW).filterIsInstance<GameInput.ChowCard>()

        if (chowMoves.isEmpty()) {
            return null
        }

        // 1. Proactively check if any Chow leads to an immediate Tongits win
        for (move in chowMoves) {
            val potentialHand = player.cards.toMutableList().apply { removeAll(move.selectedCards) }
            val potentialWinningDiscard = findTongitsWinMove(player, potentialHand)
            if (potentialWinningDiscard != null) {
                Logger.d(tag, "CHOW: Player ${player.name} found a Tongits win. Performing Chow with card ${move.chowCard}.")
                return move
            }
        }

        // 2. Strategic evaluation: Compare hand penalty score of passing (drawing from deck) vs chowing (partial or full)
        val currentPenalty = com.nowlsantos.tongitsai.util.ScoreUtils.calculateRoundPenalty(player.cards)

        val scoredChows = chowMoves.map { move ->
            val potentialHand = player.cards.toMutableList().apply { removeAll(move.selectedCards) }
            val potentialPenalty = com.nowlsantos.tongitsai.util.ScoreUtils.calculateRoundPenalty(potentialHand)
            // Bias slightly toward larger melds / more cards used in chow
            val efficiencyBonus = move.selectedCards.size * 0.5
            val netScore = potentialPenalty - efficiencyBonus
            Pair(move, netScore)
        }

        val bestChow = scoredChows.minByOrNull { it.second }

        if (bestChow != null) {
            val baselinePassPenalty = currentPenalty.toDouble()
            if (bestChow.second <= baselinePassPenalty) {
                Logger.d(tag, "CHOW: Player ${player.name} chose strategic chow with ${bestChow.first.selectedCards.size} cards.")
                return bestChow.first
            } else {
                Logger.d(tag, "CHOW: Player ${player.name} chose to SKIP chow (passing is strategically better).")
                return null
            }
        }

        return chowMoves.firstOrNull()
    }


    private fun handleMeldPhase(gameState: GameState, player: Player): GameInput? {
        // Generate meld moves from all cards
        val meldMoves = generateMeldMovesFromHand(player.cards)
        if (meldMoves.isEmpty()) {
            Logger.d(tag, "MELD: ${player.name} has no possible melds")
            return null
        }

        // Check if opponents have melds
        //val opponents = gameState.players.filter { it.id != player.id }
        //val opponentsHaveMelds = opponents.any { it.melds.isNotEmpty() }

        // Proactively check if a Meld leads to a Tongits win
        for (move in meldMoves) {
            val potentialHand = player.cards.toMutableList().apply { removeAll(move.cards) }
            val potentialWinningDiscard = findTongitsWinMove(player, potentialHand)
            if (potentialWinningDiscard != null) {
                Logger.d(tag, "MELD: ${player.name} found a Tongits win. Performing Meld with cards ${move.cards}.")
                // Update secret melds after move selection
                updateSecretMeldsAfterMeldSelection(gameState, player, meldMoves, move)
                return move
            }
        }

        // Strategic meld selection
        val selectedMove = selectBestMeldMove(gameState, player, meldMoves)

        // Update secret melds after move selection
        updateSecretMeldsAfterMeldSelection(gameState, player, meldMoves, selectedMove)
        return selectedMove
    }

    // Helper function to generate meld moves from a specific hand
    private fun generateMeldMovesFromHand(hand: List<PlayingCard>): List<GameInput.MeldCards> {
        val possibleMelds = MoveGenerator.findAllValidMelds(hand)
        return possibleMelds.map { meld ->
            GameInput.MeldCards(
                cards = meld.cards,
                sourceCardOffsets = meld.cards.associateWith { DpOffset.Zero },
                targetMeldCenterOffset = DpOffset.Zero
            )
        }
    }

    private fun selectBestMeldMove(gameState: GameState, player: Player, meldMoves: List<GameInput.MeldCards>): GameInput.MeldCards? {
        val opponents = gameState.players.filter { it.id != player.id }
        val opponentsHaveMelds = opponents.any { it.melds.isNotEmpty() }
        val deckSize = gameState.deck.size

        // Get AI's secret melds for reference
        val aiHand = AiHandRepository.getAiHand(player)
        val secretMeldCards = aiHand.secretMelds.flatMap { it.cards }.toSet()

        // Enhanced logging
        Logger.d(tag, "MELD DECISION: ${player.name}")
        Logger.d(tag, " - Opponents have melds: $opponentsHaveMelds")
        opponents.forEach { opponent ->
            if (opponent.melds.isNotEmpty()) {
                Logger.d(tag, "   ${opponent.name} has ${opponent.melds.size} melds")
            }
        }
        Logger.d(tag, " - Deck size: $deckSize")
        Logger.d(tag, " - Available meld moves: ${meldMoves.size}")

        meldMoves.forEachIndexed { index, move ->
            val usesSecretCards = move.cards.any { it in secretMeldCards }
            Logger.d(tag, "   Move $index (uses secret: $usesSecretCards): ${move.cards.joinToString { "${it.rank.display}${it.suit.symbol}" }}")
        }

        // 1. If player already has melds, don't meld more (one meld per turn)
        if (player.melds.isNotEmpty()) {
            Logger.d(tag, "MELD: ${player.name} already has melds, skipping")
            return null
        }

        // 2. If opponents have melds, AI MUST meld to participate in challenges
        if (opponentsHaveMelds) {
            Logger.d(tag, "MELD: ${player.name} must meld (opponents have melds)")

            // Prioritize melds that don't break existing secret melds
            val nonSecretMelds = meldMoves.filter { move ->
                move.cards.none { it in secretMeldCards }
            }

            if (nonSecretMelds.isNotEmpty()) {
                Logger.d(tag, "MELD: Selecting from non-secret melds")
                return selectCheapestMeld(nonSecretMelds)
            }

            // If no non-secret melds available, allow breaking one secret meld
            Logger.d(tag, "MELD: No non-secret melds available, considering breaking one secret meld")
            return selectCheapestMeld(meldMoves)
        }

        // 3. If deck is almost empty (<= 4 cards), meld to position for potential win
        if (deckSize <= 4) {
            Logger.d(tag, "MELD: ${player.name} should meld (deck almost empty)")
            return selectCheapestMeld(meldMoves)
        }

        // 4. Default: Keep melds secret if opponents haven't melded and deck has more than 4 cards
        Logger.d(tag, "MELD: ${player.name} keeping melds secret (strategic)")
        return null
    }

    private fun selectCheapestMeld(meldMoves: List<GameInput.MeldCards>): GameInput.MeldCards? {
        if (meldMoves.isEmpty()) return null

        // Prioritize melds that don't use high-value cards
        val safeMelds = meldMoves.filter { move ->
            move.cards.none { it.rank.sequentialValue >= 10 } // Avoid using Jack, Queen, King, Ace
        }

        return if (safeMelds.isNotEmpty()) {
            safeMelds.minByOrNull { it.cards.sumOf { c -> c.rank.points } }
        } else {
            meldMoves.minByOrNull { it.cards.sumOf { c -> c.rank.points } }
        }
    }

    private fun updateSecretMeldsAfterMeldSelection(
        gameState: GameState,
        player: Player,
        allMeldMoves: List<GameInput.MeldCards>,
        selectedMove: GameInput.MeldCards?
    ) {
        if (selectedMove == null) {
            // Add all possible melds to secret melds
            allMeldMoves.forEach { move ->
                AiHandRepository.addSecretMeld(player.id, Meld(move.cards))
            }
        } else {
            // Remove any secret melds that were used in the selected move
            val aiHand = AiHandRepository.getAiHand(player)
            aiHand.secretMelds.forEach { secretMeld ->
                if (secretMeld.cards.any { it in selectedMove.cards }) {
                    AiHandRepository.removeSecretMeld(player.id, secretMeld)
                }
            }

            // Add all non-selected melds that don't conflict with the selected move
            allMeldMoves.filter { it != selectedMove }.forEach { move ->
                // Check if this move conflicts with the selected move
                val conflicts = move.cards.any { card -> selectedMove.cards.contains(card) }
                if (!conflicts) {
                    AiHandRepository.addSecretMeld(player.id, Meld(move.cards))
                }
            }

            // Also find and add any new potential melds in the remaining hand
            val remainingHand = player.cards.toMutableList().apply { removeAll(selectedMove.cards) }
            val newPotentialMelds = MoveGenerator.findAllValidMelds(remainingHand)
            newPotentialMelds.forEach { meld ->
                AiHandRepository.addSecretMeld(player.id, meld)
            }
        }
    }

    private fun handleSapawPhase(gameState: GameState, player: Player, aiHand: AiHand): GameInput? {
        val sapawMoves = MoveGenerator.generateLegalMoves(gameState, player, PlayerPhase.SAPAW).filterIsInstance<GameInput.SapawCard>()
        if (sapawMoves.isEmpty()) return null

        // Priority 0: ALWAYS perform Sapaws on the AI's OWN melds on the table!
        val ownMeldSapaw = sapawMoves.firstOrNull { move ->
            val targetPlayer = gameState.players.find { p -> p.position == move.sapawTarget.second }
            targetPlayer?.id == player.id
        }
        if (ownMeldSapaw != null) {
            Logger.d(tag, "SAPAW: Player ${player.name} performing mandatory Sapaw on OWN meld with card ${ownMeldSapaw.sapawCard}.")
            return ownMeldSapaw
        }

        // Filter out moves that use secret cards
        val validMoves = sapawMoves.filter { move ->
            move.sapawCard in aiHand.remainingCards &&
                    move.sapawCard !in aiHand.secretMelds.flatMap { it.cards }
        }

        if (validMoves.isEmpty()) {
            Logger.d(tag, "Player ${player.name} skips SAPAW: all options consume SECRET/non-remaining cards.")
            return null
        }

        // Check for immediate Tongits win opportunities first (highest priority)
        val winningMove = findWinningSapawMove(gameState, player, validMoves, aiHand.remainingCards)
        if (winningMove != null) {
            Logger.d(tag, "SAPAW: Player ${player.name} found a Tongits win. Performing Sapaw with card ${winningMove.sapawCard}.")
            return winningMove
        }

        // Prioritize moves based on strategic value
        val prioritizedMoves = validMoves.sortedByDescending { move ->
            calculateSapawMoveScore(gameState, player, move)
        }

        val chosenMove = prioritizedMoves.first()
        Logger.d(tag, "Player ${player.name} chosen SAPAW: ${chosenMove.sapawCard} -> ${chosenMove.sapawTarget}")
        return chosenMove
    }

    private fun findWinningSapawMove(
        gameState: GameState,
        player: Player,
        sapawMoves: List<GameInput.SapawCard>,
        nonSecretCards: List<PlayingCard>
    ): GameInput.SapawCard? {
        for (move in sapawMoves) {
            val simulatedHand = nonSecretCards.toMutableList().apply { remove(move.sapawCard) }
            if (wouldLeadToTongitsWin(gameState, player, simulatedHand)) {
                return move
            }
        }
        return null
    }

    fun canPartitionIntoMelds(cards: List<PlayingCard>): Boolean {
        if (cards.isEmpty()) return true
        
        val validMelds = MoveGenerator.findAllValidMelds(cards)
        if (validMelds.isEmpty()) return false

        fun solve(remainingCards: List<PlayingCard>): Boolean {
            if (remainingCards.isEmpty()) return true
            for ((cards1) in validMelds) {
                if (cards1.all { it in remainingCards }) {
                    val newRemaining = remainingCards.toMutableList()
                    cards1.forEach { newRemaining.remove(it) }
                    if (solve(newRemaining)) return true
                }
            }
            return false
        }

        return solve(cards)
    }

    fun findTongitsWinMove(player: Player, hand: List<PlayingCard>): GameInput.DiscardCard? {
        val aiHand = AiHandRepository.getAiHand(player)
        val nonSecretCards = hand.filter { it !in aiHand.secretMelds.flatMap { it -> it.cards } }

        // Test each card in nonSecretCards as the candidate winning discard
        for (candidateCard in nonSecretCards) {
            val remainingAfterDiscard = nonSecretCards.toMutableList().apply { remove(candidateCard) }
            if (canPartitionIntoMelds(remainingAfterDiscard)) {
                Logger.d(tag, "TONGITS WIN DETECTED! Discarding $candidateCard leaves a completely melded hand.")
                return GameInput.DiscardCard(
                    card = candidateCard,
                    startOffset = DpOffset.Zero,
                    targetOffset = DpOffset.Zero
                )
            }
        }
        return null
    }

    // Helper function to calculate the strategic score of a sapaw move
    private fun calculateSapawMoveScore(gameState: GameState, player: Player, move: GameInput.SapawCard): Int {
        val targetPlayer = gameState.players.find { it.position == move.sapawTarget.second }
        val addsToOwnMeld = targetPlayer?.id == player.id
        val isTargetHitter = targetPlayer?.isHitter == true || (targetPlayer?.hitCount ?: 0) >= 1
        val deckSize = gameState.deck.size

        var score = 0

        // Priority 0: Emergency Hitter Suppression (Prevent 2-Hit Pot Claim)
        if (isTargetHitter && !addsToOwnMeld) {
            score += 2500 // Extremely high priority to suppress Hitter!
        }

        // Priority 1: Adding to own meld is generally better
        if (addsToOwnMeld) {
            score += 1000

            // Bonus for completing a meld
            val targetMeld = targetPlayer?.melds?.getOrNull(move.sapawTarget.third)
            if (targetMeld != null && targetMeld.cards.size == 2 && targetMeld.canAdd(move.sapawCard)) {
                score += 500 // Bonus for completing a partial meld
            }
        } else {
            // Target is an opponent
            val targetMeld = targetPlayer?.melds?.getOrNull(move.sapawTarget.third)
            val targetAlreadyHasSapaw = (targetMeld?.cards?.size ?: 0) > 3

            if (deckSize > 15 && targetAlreadyHasSapaw && !isTargetHitter) {
                // Early/Mid game redundant Sapaw on non-hitter: Preserve card for next turn!
                score -= 600
            } else {
                // Late game or non-redundant Sapaw
                score += 200
            }
        }

        // Priority 2: In late game (deck <= 10), shed high-value deadwood points
        if (deckSize <= 10) {
            score += move.sapawCard.rank.points * 10
        } else {
            score += move.sapawCard.rank.points
        }

        // Priority 3: Prefer cards that are not part of potential future melds
        if (isCardPartOfPotentialMeld(move.sapawCard, player.cards)) {
            score -= 50
        }

        return score
    }

    // Helper function to check if a card is part of a potential meld
    private fun isCardPartOfPotentialMeld(card: PlayingCard, hand: List<PlayingCard>): Boolean {
        // Implementation would check if the card can form a meld with other cards in hand
        // This is a simplified placeholder
        val sameRank = hand.count { it.rank == card.rank }
        val sameSuitSequence = hand.any {
            it.suit == card.suit &&
                    (it.rank.sequentialValue == card.rank.sequentialValue - 1 ||
                            it.rank.sequentialValue == card.rank.sequentialValue + 1)
        }

        return sameRank >= 2 || sameSuitSequence
    }

    // Helper function to check if a move would lead to a Tongits win
    private fun wouldLeadToTongitsWin(gameState: GameState, player: Player, simulatedHand: List<PlayingCard>): Boolean {
        // Implementation would check if the player can win immediately after this sapaw
        // This is a simplified placeholder
        return simulatedHand.isEmpty() ||
                (simulatedHand.size == 1 && canMeldAllCards(gameState, simulatedHand))
    }

    // Helper function to check if all cards can be melded
    private fun canMeldAllCards(gameState: GameState, cards: List<PlayingCard>): Boolean {
        // Implementation would check if all cards can be added to existing melds
        // This is a simplified placeholder
        val allMelds = gameState.players.flatMap { it.melds }
        return cards.all { card -> allMelds.any { it.canAdd(card) } }
    }

    private fun handleDiscardPhase(
        gameState: GameState,
        player: Player,
        aiHand: AiHand
    ): GameInput? {
        //Logger.d(tag, "DISCARD: Player ${player.name} hand before discard: ${aiHand.remainingCards.sortedBy { it.rank.ordinal }}")
        Logger.d(tag, "DISCARD: Player ${player.name} hand: ${aiHand.remainingCards.sortedBy { it.rank.ordinal }}")
        Logger.d(tag, "DISCARD: Secret melds: ${aiHand.secretMelds}")
        Logger.d(tag, "DISCARD: Deck size: ${gameState.deck.size}")
        val visibleCards = aiHand.remainingCards
        if (visibleCards.isEmpty()) return null

        // This check is the final step of a Tongits win, after all melds are made.
        val tongitsMove = findTongitsWinMove(player, visibleCards)
        if (tongitsMove != null) {
            Logger.d(tag, "Tongits! Player ${player.name} has a winning hand. Discarding the last card: ${tongitsMove.card}")
            return tongitsMove
        }

        val discardMovesAll = MoveGenerator.generateDiscardMove(gameState, player, visibleCards)
        if (discardMovesAll.isEmpty()) {
            Logger.e(tag, "No legal discard moves available.")
            return null
        }

        val potentialMelds = MoveGenerator.findPartialMelds(visibleCards)

        val prioritizedMoves = discardMovesAll.sortedByDescending { discardMove ->
            scoreDiscard(discardMove.card, gameState, player, potentialMelds)
        }

        return prioritizedMoves.firstOrNull()
    }

    private fun scoreDiscard(card: PlayingCard, gameState: GameState, player: Player, potentialMelds: List<Set<PlayingCard>>): Double {
        // Never discard a card that can be Sapawed onto the player's OWN melds!
        val ownMelds = player.melds
        if (ownMelds.any { it.canAdd(card) }) {
            return -10000.0
        }

        // Avoid discarding cards that can be Sapawed onto any table melds (anti-feeding)
        val allTableMelds = gameState.players.flatMap { it.melds }
        if (allTableMelds.any { it.canAdd(card) }) {
            return -5000.0
        }

        val isPartOfPotentialMeld = potentialMelds.any { it.contains(card) }

        if (isPartOfPotentialMeld) {
            return -1000.0
        }

        return card.rank.sequentialValue.toDouble()
    }

    private fun findBestMove(
        gameState: GameState,
        player: Player,
        playerStateMachine: PlayerStateMachine,
        legalMoves: List<GameInput>,
        depth: Int = 3,
        alpha: Int = Int.MIN_VALUE,
        beta: Int = Int.MAX_VALUE
    ): GameInput? {
        var bestScore = Int.MIN_VALUE
        var bestMove: GameInput? = null
        var currentAlpha = alpha

        // Sort moves for better alpha-beta pruning
        val sortedMoves = legalMoves.sortedByDescending { move ->
            when (move) {
                is GameInput.MeldCards -> move.cards.sumOf { it.rank.points }
                is GameInput.ChowCard -> move.selectedCards.sumOf { it.rank.points }
                else -> 0
            }
        }

        for (move in sortedMoves) {
            val newState = GameRules.applyMove(gameState, move)
            val score = minimax(newState, player, depth - 1, currentAlpha, beta, false, playerStateMachine)

            if (score > bestScore) {
                bestScore = score
                bestMove = move
            }

            currentAlpha = maxOf(currentAlpha, bestScore)
            if (beta <= currentAlpha) break // Alpha-beta pruning
        }

        return bestMove
    }

    private fun minimax(
        gameState: GameState,
        player: Player,
        depth: Int,
        alpha: Int,
        beta: Int,
        maximizingPlayer: Boolean,
        playerStateMachine: PlayerStateMachine
    ): Int {
        if (depth == 0 || gameState.gameWinnerId != null) {
            return evaluateGameState(gameState, player)
        }

        val currentPlayer = gameState.currentPlayer
        val moves = legalMovesForSimState(gameState, currentPlayer)

        if (moves.isEmpty()) {
            return evaluateGameState(gameState, player)
        }

        return if (maximizingPlayer) {
            var maxEval = Int.MIN_VALUE
            var currentAlpha = alpha

            for (move in moves) {
                val newState = GameRules.applyMove(gameState, move)
                val eval = minimax(newState, player, depth - 1, currentAlpha, beta, false, playerStateMachine)
                maxEval = maxOf(maxEval, eval)
                currentAlpha = maxOf(currentAlpha, eval)
                if (beta <= currentAlpha) break
            }

            maxEval
        } else {
            var minEval = Int.MAX_VALUE
            var currentBeta = beta

            for (move in moves) {
                val newState = GameRules.applyMove(gameState, move)
                val eval = minimax(newState, player, depth - 1, alpha, currentBeta, true, playerStateMachine)
                minEval = minOf(minEval, eval)
                currentBeta = minOf(currentBeta, eval)
                if (currentBeta <= alpha) break
            }

            minEval
        }
    }

    private fun evaluateGameState(gameState: GameState, player: Player): Int {
        val aiPlayer = gameState.players.first { it.id == player.id }
        val aiHand = AiHandRepository.getAiHand(player)

        // Comprehensive evaluation considering:
        // 1. Points in melds
        // 2. Points in hand
        // 3. Number of cards
        // 4. Potential melds
        // 5. Opponent status

        val meldPoints = aiPlayer.melds.flatMap { it.cards }.sumOf { it.rank.points }
        val handPoints = aiHand.remainingCards.sumOf { it.rank.points }
        val secretMeldPoints = aiHand.secretMelds.flatMap { it.cards }.sumOf { it.rank.points }

        // Negative points are bad (more points left in hand)
        return meldPoints + secretMeldPoints - handPoints
    }

    private fun legalMovesForSimState(state: GameState, currentPlayer: Player): List<GameInput> {
        val tryOrder = listOf(
            PlayerPhase.CHOW,
            PlayerPhase.MELD,
            PlayerPhase.SAPAW,
            PlayerPhase.DISCARD,
            PlayerPhase.CALL,
            PlayerPhase.GET
        )
        for (phase in tryOrder) {
            val moves = MoveGenerator.generateLegalMoves(state, currentPlayer, phase)
            if (moves.isNotEmpty()) return moves
        }
        return emptyList()
    }
}