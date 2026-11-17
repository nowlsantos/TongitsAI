package com.nowlsantos.tongitsai.domain.ai

import com.nowlsantos.tongitsai.domain.model.Meld
import com.nowlsantos.tongitsai.domain.model.Player
import com.nowlsantos.tongitsai.domain.model.PlayingCard

data class AiHand(
    val player: Player,
    val secretMelds: List<Meld> = emptyList(),
    val remainingCards: List<PlayingCard> = player.cards
) {
    companion object {
        fun fromPlayer(player: Player) = AiHand(player)
    }

    val totalCardsCount: Int
        get() = secretMelds.sumOf { it.cards.size } + remainingCards.size

    fun reset(): AiHand = fromPlayer(player)
}