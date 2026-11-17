package com.nowlsantos.tongitsai.domain.model.enums

/**
 * The four Filipino-standard suits.
 * Order is arbitrary but fixed (♣ < ♦ < ♥ < ♠).
 */
enum class Suit {
    CLUBS,
    DIAMONDS,
    HEARTS,
    SPADES;

    val symbol: String
        get() = when (this) {
            CLUBS    -> "♣"
            DIAMONDS -> "♦"
            HEARTS   -> "♥"
            SPADES   -> "♠"
        }
}