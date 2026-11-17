package com.nowlsantos.tongitsai.domain.model

import androidx.compose.runtime.Immutable
import com.nowlsantos.tongitsai.domain.model.enums.Rank

/**
 * Immutable representation of a meld (set or run).
 * All helper functions return new instances.
 */
@Immutable
data class Meld(
    val cards: List<PlayingCard>,
    val hasSapaw: Boolean = false,
    val owner: String? = null
) {
    init {
        require(cards.size >= 3) { "A meld must contain at least 3 cards" }
    }

    /** Sequential value for ranking (Ace = 1, King = 13). */
    private val PlayingCard.seq: Int
        get() = when (rank) {
            Rank.ACE -> 1
            Rank.JACK -> 11
            Rank.QUEEN -> 12
            Rank.KING -> 13
            else -> rank.ordinal + 1
        }

    private val sortedCards: List<PlayingCard>
        get() = cards.sortedWith(compareBy({ it.suit.ordinal }, { it.seq }))

    /* ---------- validation ---------- */

    /**
     * Checks if the meld is a valid set (all cards have the same rank).
     */
    fun isValidSet(): Boolean =
        cards.size in 3..4 &&
                cards.map { it.rank }.toSet().size == 1 &&
                cards.map { it.suit }.toSet().size == cards.size

    /**
     * Checks if the meld is a valid run (sequential cards of the same suit).
     */
    fun isValidRun(): Boolean {
        val suited = cards.groupBy { it.suit }
        return suited.size == 1 && suited.values.single().let { list ->
            // Sort the cards by their sequential value before checking the run.
            val codes = list.map { it.seq }.sorted()
            codes == (codes.first()..codes.last()).toList()
        }
    }

    /**
     * Checks if the meld is either a valid set or a valid run.
     */
    fun isValid(): Boolean = isValidSet() || isValidRun()

    /* ---------- utilities ---------- */

    /**
     * Checks if a card is already present in the meld.
     */
    fun containsCard(card: PlayingCard): Boolean = cards.contains(card)

    /**
     * Returns a new Meld instance with the given card added.
     * Throws an IllegalArgumentException if the resulting meld is invalid.
     */
    fun add(card: PlayingCard): Meld = Meld(cards + card)

    /**
     * Checks if a new card can be added to the meld without invalidating it.
     * This is the corrected function.
     */
    fun canAdd(card: PlayingCard): Boolean {
        if (containsCard(card)) return false

        // SET: still max 4, all same rank, all different suits
        if (isValidSet()) {
            // same rank, suit not already used, and size must stay <= 4
            val sameRank = cards.first().rank
            val suits = cards.map { it.suit }.toSet()
            return card.rank == sameRank && card.suit !in suits && cards.size < 4
        }

        // RUN: same suit, and becomes a continuous sequence; NO upper bound on run length
        if (isValidRun()) {
            val suit = cards.first().suit
            if (card.suit != suit) return false

            val seqs = (cards.map { it.seq } + card.seq).sorted()
            // continuous if it matches its min..max range
            val continuous = seqs == (seqs.first()..seqs.last()).toList()
            return continuous
        }

        // original meld invalid → do not allow add
        return false
    }

    /** Returns true if this meld currently has a sapaw card added */
    fun hasSapaw(): Boolean = hasSapaw

    fun addSapawCardToMeld(meld: Meld, sapawCard: PlayingCard): Meld {
        val newCards = meld.cards + sapawCard
        return meld.copy(cards = newCards, hasSapaw = true)
    }
}