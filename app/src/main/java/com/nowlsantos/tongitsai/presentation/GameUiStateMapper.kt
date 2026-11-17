package com.nowlsantos.tongitsai.presentation

import com.nowlsantos.tongitsai.domain.model.GameState
import com.nowlsantos.tongitsai.domain.model.GameUiState
import com.nowlsantos.tongitsai.domain.model.Player
import com.nowlsantos.tongitsai.domain.model.UiPlayer
import com.nowlsantos.tongitsai.domain.model.enums.ChallengerType
import com.nowlsantos.tongitsai.domain.model.enums.PlayerType
import com.nowlsantos.tongitsai.util.ScoreUtils

object GameUiStateMapper {

    fun mapToUiPlayer(
        player: Player,
        roundNumber: Int,
        isReveal: Boolean
    ): UiPlayer {
        val isFoldee = player.challengerType == ChallengerType.FOLDEE || player.challengerType == ChallengerType.LAST_FOLDEE
        val isFaceUp = player.type == PlayerType.HUMAN || (isReveal && !isFoldee)
        val isHitter = player.isHitter

        val displayCards = if (isReveal && player.type == PlayerType.AI) {
            val (bestMelds, deadwood) = ScoreUtils.bestMeldsAndDeadwood(player.cards)
            bestMelds.flatMap { it.cards } + deadwood
        } else {
            player.cards
        }

        return UiPlayer(
            id = player.id,
            name = player.name,
            type = player.type,
            position = player.position,
            balance = player.balance,
            score = player.score,
            isHitter = isHitter,
            isFaceUp = isFaceUp,
            displayCards = displayCards,
            selectedCards = player.selectedCards,
            melds = player.melds,
            challengerType = player.challengerType
        )
    }

    fun mapToGameUiState(
        gs: GameState,
        currentUiState: GameUiState
    ): GameUiState {
        val uiPlayers = gs.players.map { player ->
            mapToUiPlayer(
                player = player,
                roundNumber = gs.roundNumber,
                isReveal = gs.isReveal
            )
        }

        return currentUiState.copy(
            gamePhase = gs.gamePhase,
            players = gs.players,
            uiPlayers = uiPlayers,
            deckCards = gs.deck.cards,
            discardPile = gs.discardPile,
            lastDiscardedCard = gs.lastDiscardedCard,
            roundWinnerName = gs.roundWinnerId?.let { id -> gs.players.firstOrNull { it.id == id }?.name },
            gameWinnerName = gs.gameWinnerId?.let { id -> gs.players.firstOrNull { it.id == id }?.name },
            roundNumber = gs.roundNumber,
            centerFee = gs.centerFee,
            callerId = gs.callerId?.let { id -> gs.players.firstOrNull { it.id == id }?.name },
            isReveal = gs.isReveal
        )
    }
}
