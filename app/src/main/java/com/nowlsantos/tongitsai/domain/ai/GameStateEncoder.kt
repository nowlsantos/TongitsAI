package com.nowlsantos.tongitsai.domain.ai

import com.nowlsantos.tongitsai.domain.model.GameState
import com.nowlsantos.tongitsai.domain.model.Player
import com.nowlsantos.tongitsai.domain.model.PlayingCard
import com.nowlsantos.tongitsai.domain.model.enums.Rank
import com.nowlsantos.tongitsai.domain.model.enums.Suit

class GameStateEncoder {
    companion object {
        const val FEATURE_SIZE = 52 + 52 + 52 + 10 // Hand (52) + Discard (52) + LastDiscard (52) + Scalars (10) = 166
    }

    fun encode(gameState: GameState, player: Player): FloatArray {
        val features = FloatArray(FEATURE_SIZE)
        var idx = 0

        val handCards = player.cards.toSet()
        for (rank in Rank.entries) {
            for (suit in Suit.entries) {
                val card = PlayingCard(rank, suit)
                features[idx++] = if (card in handCards) 1.0f else 0.0f
            }
        }

        val discardCards = gameState.discardPile.cards.toSet()
        for (rank in Rank.entries) {
            for (suit in Suit.entries) {
                val card = PlayingCard(rank, suit)
                features[idx++] = if (card in discardCards) 1.0f else 0.0f
            }
        }

        val lastCard = gameState.lastDiscardedCard
        for (rank in Rank.entries) {
            for (suit in Suit.entries) {
                val card = PlayingCard(rank, suit)
                features[idx++] = if (lastCard != null && lastCard == card) 1.0f else 0.0f
            }
        }

        // Scalars
        features[idx++] = (gameState.centerFee / 100f).coerceIn(0f, 1f)
        features[idx++] = (gameState.roundNumber / 10f).coerceIn(0f, 1f)
        features[idx++] = if (gameState.currentPlayerIndex == gameState.players.indexOf(player)) 1.0f else 0.0f

        return features
    }
}
