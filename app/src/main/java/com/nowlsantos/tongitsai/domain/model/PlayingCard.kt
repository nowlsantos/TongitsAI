package com.nowlsantos.tongitsai.domain.model

import androidx.compose.runtime.Immutable
import com.nowlsantos.tongitsai.domain.model.enums.Rank
import com.nowlsantos.tongitsai.domain.model.enums.Suit

/**
 * Immutable playing card.
 * Implements Comparable so a deck can be sorted deterministically.
 */
@Immutable
data class PlayingCard(
    val rank: Rank,
    val suit: Suit
) : Comparable<PlayingCard> {

    override fun compareTo(other: PlayingCard): Int =
        compareValuesBy(this, other, { it.suit.ordinal }, { it.rank.ordinal })

    //override fun toString(): String = "${rank.display}${suit.symbol}"
    override fun toString(): String {
        return "$rank of $suit"
    }
}