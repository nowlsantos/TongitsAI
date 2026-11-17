package com.nowlsantos.tongitsai.util

import com.nowlsantos.tongitsai.domain.model.PlayingCard
import com.nowlsantos.tongitsai.domain.model.enums.Rank
import com.nowlsantos.tongitsai.domain.model.enums.Suit
import android.content.Context
import androidx.annotation.DrawableRes

object CardImageMapper {
    /**
     * Maps a PlayCard object to its corresponding drawable resource ID.
     * Assumes drawable names follow the convention: `card_[rank_short][suit_short].png`
     *
     * Example: "card_3d" for Three of Diamonds, "card_jc" for Jack of Clubs.
     * Note: 't' is used for Ten to maintain a single-character abbreviation consistency
     * with other face cards (j, q, k, a) and single-digit ranks (3, 4, etc.).
     *
     * @param context The Android Context, needed to access resources.
     * @param card The PlayCard object.
     * @return The resource ID of the card image, or 0 if not found.
     */
    @DrawableRes
    fun getDrawableResId(context: Context, card: PlayingCard): Int {
        val rankShortName = when (card.rank) {
            Rank.TWO -> "2"
            Rank.THREE -> "3"
            Rank.FOUR -> "4"
            Rank.FIVE -> "5"
            Rank.SIX -> "6"
            Rank.SEVEN -> "7"
            Rank.EIGHT -> "8"
            Rank.NINE -> "9"
            Rank.TEN -> "t" // 't' for Ten
            Rank.JACK -> "j"
            Rank.QUEEN -> "q"
            Rank.KING -> "k"
            Rank.ACE -> "a"
        }

        val suitShortName = when (card.suit) {
            Suit.DIAMONDS -> "d"
            Suit.CLUBS -> "c"
            Suit.HEARTS -> "h"
            Suit.SPADES -> "s"
        }

        val drawableName = "card_${rankShortName}${suitShortName}" // Added "card_" prefix

        return context.resources.getIdentifier(
            drawableName,
            "drawable",
            context.packageName
        )
    }

    /**
     * Extension property for PlayCard to directly get its short name for drawable.
     * This makes it cleaner to use `card.drawableShortName`
     */
    val PlayingCard.drawableShortName: String
        get() {
            val rankShortName = when (this.rank) {
                Rank.TWO -> "2"
                Rank.THREE -> "3"
                Rank.FOUR -> "4"
                Rank.FIVE -> "5"
                Rank.SIX -> "6"
                Rank.SEVEN -> "7"
                Rank.EIGHT -> "8"
                Rank.NINE -> "9"
                Rank.TEN -> "t" // 't' for Ten
                Rank.JACK -> "j"
                Rank.QUEEN -> "q"
                Rank.KING -> "k"
                Rank.ACE -> "a"
            }

            val suitShortName = when (this.suit) {
                Suit.DIAMONDS -> "d"
                Suit.CLUBS -> "c"
                Suit.HEARTS -> "h"
                Suit.SPADES -> "s"
            }
            return "card_${rankShortName}${suitShortName}" // Added "card_" prefix
        }

    /**
     * Extension function on Context to easily get a drawable ID for a PlayCard.
     * Usage: `context.getCardDrawableResId(myPlayCard)`
     */
    @DrawableRes
    fun Context.getCardDrawableResId(card: PlayingCard): Int {
        return getDrawableResId(this, card)
    }
}