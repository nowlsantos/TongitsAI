package com.nowlsantos.tongitsai.presentation

import androidx.compose.ui.unit.DpOffset
import com.nowlsantos.tongitsai.domain.model.GameUiState
import com.nowlsantos.tongitsai.domain.model.PlayingCard
import com.nowlsantos.tongitsai.domain.model.enums.PlayerType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

object CardSelectionHandler {

    fun toggleCardSelection(
        gameUiState: MutableStateFlow<GameUiState>,
        card: PlayingCard,
        cardOffset: DpOffset
    ) {
        gameUiState.update { currentUiState ->
            val currentHumanPlayer = currentUiState.players.firstOrNull { it.type == PlayerType.HUMAN }
                ?: return@update currentUiState

            val updatedPlayers = currentUiState.players.map { player ->
                if (player.id == currentHumanPlayer.id) {
                    val isAlreadySelected = player.selectedCards.contains(card)
                    val newSelection = if (isAlreadySelected) {
                        player.selectedCards - card
                    } else {
                        player.selectedCards + card
                    }
                    player.copy(selectedCards = newSelection.toMutableSet())
                } else {
                    player
                }
            }

            val updatedUiPlayers = updatedPlayers.map { player ->
                GameUiStateMapper.mapToUiPlayer(
                    player = player,
                    roundNumber = currentUiState.roundNumber,
                    isReveal = currentUiState.isReveal
                )
            }

            currentUiState.copy(
                players = updatedPlayers,
                uiPlayers = updatedUiPlayers,
                localCoordinate = currentUiState.localCoordinate.copy(
                    cardOffset = cardOffset
                )
            )
        }
    }
}
