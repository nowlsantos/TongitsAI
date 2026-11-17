package com.nowlsantos.tongitsai.util

import com.nowlsantos.tongitsai.domain.ai.AiHand
import com.nowlsantos.tongitsai.domain.model.Meld
import com.nowlsantos.tongitsai.domain.model.Player
import com.nowlsantos.tongitsai.domain.model.PlayingCard

object ScoreEvaluator {

    fun calculateRoundScore(
        remainingCards: List<PlayingCard>,
        secretMelds: List<Meld> // Make sure Meld class has 'cards' property
    ): Int {
        val secretMeldPoints = secretMelds.sumOf { meld -> meld.cards.sumOf { card -> card.rank.points } }
        val totalPoints = remainingCards.sumOf { it.rank.points }
        return totalPoints - secretMeldPoints
    }

    fun buildAiHandFromPlayer(player: Player): AiHand {
        // Cards not part of any meld are considered remaining cards
        val meldCards = player.melds.flatMap { it.cards }.toSet()
        val remainingCards = player.cards.filter { it !in meldCards }

        // For secret melds: if your Player class differentiates secret vs visible melds,
        // filter accordingly. If not, assume all melds are visible (or adjust as needed).
        // Here we assume all melds are secret melds for AI purposes.
        val secretMelds = player.melds

        return AiHand(
            player = player,
            remainingCards = remainingCards,
            secretMelds = secretMelds
        )
    }
}