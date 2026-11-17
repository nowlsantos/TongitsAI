package com.nowlsantos.tongitsai.domain.model

import androidx.compose.runtime.Immutable

/**
 * Immutable snapshot of the discard pile.
 * All “mutating” operations return a new instance.
 */
@Immutable
data class DiscardPile(
    val cards: List<PlayingCard> = emptyList()
) {
    /** Top card or null if empty. */
    val topCard: PlayingCard? get() = cards.lastOrNull()

    /** True if pile is empty. */
    val isEmpty: Boolean get() = cards.isEmpty()

    /** Number of cards in the pile. */
    val size: Int get() = cards.size

    /** Add a card on top and return a new pile. */
    fun add(card: PlayingCard): DiscardPile =
        DiscardPile(cards + card)

    /** Remove the top card and return a new pile. */
    fun removeTop(): DiscardPile =
        DiscardPile(cards.dropLast(1))

    /** Return an empty pile. */
    fun clear(): DiscardPile =
        DiscardPile()
}