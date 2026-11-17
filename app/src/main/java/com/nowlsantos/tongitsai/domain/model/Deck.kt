package com.nowlsantos.tongitsai.domain.model

import androidx.compose.runtime.Immutable
import com.nowlsantos.tongitsai.domain.model.enums.Rank
import com.nowlsantos.tongitsai.domain.model.enums.Suit
import kotlin.random.Random


@Immutable
data class Deck constructor(
    val cards: List<PlayingCard>
) {
    val size: Int get() = cards.size
    val isEmpty: Boolean get() = cards.isEmpty()
    val topCard: PlayingCard? get() = cards.lastOrNull()

    /**
     * Remove and return the top card along with the new deck instance.
     */
    fun draw(): Pair<PlayingCard, Deck> {
        require(cards.isNotEmpty()) { "Deck is empty" }
        return cards.last() to Deck(cards.dropLast(1))
    }

    /**
     * Return a new shuffled deck.
     */
    fun shuffled(random: Random = Random.Default): Deck =
        Deck(cards.shuffled(random))

    companion object {
        /**
         * Factory that produces a fresh 52-card deck in ascending order.
         */
        fun standard(): Deck {
            val list = Suit.entries.flatMap { suit ->
                Rank.valuesAscending().map { rank -> PlayingCard(rank, suit) }
            }
            return Deck(list)
        }
    }

    fun resetAndShuffle(): Deck = standard()
        .shuffled()
        .shuffled()
        .shuffled()
}