package com.nowlsantos.tongitsai.domain.ai

import com.nowlsantos.tongitsai.domain.model.GameState
import com.nowlsantos.tongitsai.domain.model.Meld
import com.nowlsantos.tongitsai.presentation.GameInput

object GameRules {
    fun applyMove(gameState: GameState, move: GameInput): GameState {
        val currentPlayerId = gameState.currentPlayer.id
        val players = gameState.players.toMutableList()
        val deck = gameState.deck
        val discardPile = gameState.discardPile

        val currentPlayerIndex = players.indexOfFirst { it.id == currentPlayerId }
        if (currentPlayerIndex == -1) return gameState
        val currentPlayer = players[currentPlayerIndex]

        return when (move) {
            is GameInput.DrawCard -> {
                if (deck.isEmpty) return gameState
                val (drawnCard, newDeck) = deck.draw()
                val updatedPlayer = currentPlayer.copy(cards = (currentPlayer.cards + drawnCard).distinctBy { c -> c.rank to c.suit })
                players[currentPlayerIndex] = updatedPlayer

                gameState.copy(
                    deck = newDeck,
                    players = players
                )
            }

            is GameInput.DiscardCard -> {
                val updatedPlayerCards = currentPlayer.cards.filterNot { it == move.card }.distinctBy { c -> c.rank to c.suit }
                val updatedPlayer = currentPlayer.copy(cards = updatedPlayerCards)
                players[currentPlayerIndex] = updatedPlayer

                gameState.copy(
                    discardPile = discardPile.add(move.card),
                    players = players
                )
            }

            is GameInput.MeldCards -> {
                val updatedPlayerCards = currentPlayer.cards.filterNot { it in move.cards }.distinctBy { c -> c.rank to c.suit }
                val updatedPlayerMelds = currentPlayer.melds + Meld(move.cards)
                val updatedPlayer = currentPlayer.copy(
                    cards = updatedPlayerCards,
                    melds = updatedPlayerMelds
                )
                players[currentPlayerIndex] = updatedPlayer

                // FIX: Update AiHandRepository after the move is public
                AiHandRepository.updateSecretsAfterPublicMove(currentPlayerId, move.cards.toSet())

                gameState.copy(players = players)
            }

            is GameInput.SapawCard -> {
                val sapawCard = move.sapawCard
                val targetPlayerIndex = players.indexOfFirst { it.position == move.sapawTarget.second }
                if (targetPlayerIndex == -1) return gameState

                val updatedTargetMelds = players[targetPlayerIndex].melds.toMutableList()
                val meldIndex = move.sapawTarget.third
                val meldToUpdate = updatedTargetMelds.getOrNull(meldIndex) ?: return gameState
                updatedTargetMelds[meldIndex] = Meld(meldToUpdate.cards + sapawCard)

                players[targetPlayerIndex] = players[targetPlayerIndex].copy(melds = updatedTargetMelds)
                players[currentPlayerIndex] = currentPlayer.copy(cards = currentPlayer.cards.filterNot { it == sapawCard }.distinctBy { c -> c.rank to c.suit })

                // FIX: Update AiHandRepository after the move is public
                AiHandRepository.updateSecretsAfterPublicMove(currentPlayerId, setOf(sapawCard))

                gameState.copy(players = players)
            }

            is GameInput.ChowCard -> {
                val discardTop = discardPile.topCard ?: return gameState
                val updatedPlayerCards = currentPlayer.cards.filterNot { it in move.selectedCards }.distinctBy { c -> c.rank to c.suit }
                val newMeld = Meld(move.selectedCards + discardTop)

                val updatedMelds = currentPlayer.melds.map { if (it.hasSapaw) it.copy(hasSapaw = false) else it } + newMeld

                val updatedPlayer = currentPlayer.copy(
                    cards = updatedPlayerCards,
                    melds = updatedMelds
                )
                players[currentPlayerIndex] = updatedPlayer

                // FIX: Update AiHandRepository after the move is public
                val cardsUsedForChow = (move.selectedCards + discardTop).toSet()
                AiHandRepository.updateSecretsAfterPublicMove(currentPlayerId, cardsUsedForChow)

                gameState.copy(
                    players = players,
                    discardPile = discardPile.removeTop()
                )
            }
            is GameInput.MakeCall -> {
                // Calls don't change cards directly
                gameState
            }
            else -> gameState
        }
    }

    private fun isLateGame(gameState: GameState): Boolean {
        return gameState.deck.size < 20 // Adjust threshold as needed
    }
}