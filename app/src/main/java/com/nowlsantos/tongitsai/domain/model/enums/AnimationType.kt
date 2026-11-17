package com.nowlsantos.tongitsai.domain.model.enums

enum class AnimationType {
    DEAL_CARD,       // Card dealt from Deck to a Player's hand
    DISCARD,    // Card discarded from a Player's hand to Discard Pile
    CHOW_CARD,       // Card picked from Discard Pile to a Player's hand
    SINGLE_CHOW,
    SAPAW_CARD,      // Card sapawed from a Player's hand to a specific Meld
    MELD_CARDS,       // Meld picked up from player's exposed melds back to hand (future use)
    GET_CARD         // Card drawn from Deck (or picked from Discard) to a Player's hand
}