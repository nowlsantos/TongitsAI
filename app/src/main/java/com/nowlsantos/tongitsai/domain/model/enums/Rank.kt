package com.nowlsantos.tongitsai.domain.model.enums

/**
 * Ace is low, King is high.
 * J-Q-K count 10 points for scoring.
 */
enum class Rank(val display: String, val points: Int, val sequentialValue: Int) {
    ACE("A", 1, 1),
    TWO("2", 2, 2),
    THREE("3", 3, 3),
    FOUR("4", 4, 4),
    FIVE("5", 5, 5),
    SIX("6", 6, 6),
    SEVEN("7", 7, 7),
    EIGHT("8", 8, 8),
    NINE("9", 9, 9),
    TEN("10", 10, 10),
    JACK("J", 10, 11),
    QUEEN("Q", 10, 12),
    KING("K", 10, 13);

    companion object {
        fun valuesAscending(): List<Rank> = entries

        // Add this function to resolve the "Unresolved reference" error
        fun fromSequentialValue(value: Int): Rank {
            return entries.first { it.sequentialValue == value }
        }
    }
}