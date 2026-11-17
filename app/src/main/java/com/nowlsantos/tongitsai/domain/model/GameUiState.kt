package com.nowlsantos.tongitsai.domain.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.DpOffset
import com.nowlsantos.tongitsai.domain.fsm.game.GamePhase
import com.nowlsantos.tongitsai.domain.model.enums.PlayerPosition
import com.nowlsantos.tongitsai.presentation.UiEffect

/**
 * UI-specific projection of GameState plus transient UI bits.
 * All collections are immutable.
 */
@Immutable
data class GameUiState(
    val gamePhase: GamePhase,
    val players: List<Player>,
    val uiPlayers: List<UiPlayer> = emptyList(),
    val deckCards: List<PlayingCard>,
    val discardPile: DiscardPile,
    val lastDiscardedCard: PlayingCard?,
    val roundWinnerName: String?,
    val gameWinnerName: String?,
    val roundNumber: Int,
    val centerFee: Int,
    val callerId: String?,
    val localCoordinate: LocalCoordinate,
    val meldAreaCoordinates: Map<PlayerPosition, DpOffset> = emptyMap(),
    val uiEffect: UiEffect? = null,
    val isReveal: Boolean = false,
    val selectedSapawTarget: Triple<DpOffset, PlayerPosition, Int>? = null,
) {
    companion object {
        fun fromGameState(
            gs: GameState,
            localCoordinate: LocalCoordinate,
            meldAreaCoordinates: Map<PlayerPosition, DpOffset> = emptyMap(),
        ): GameUiState =
            GameUiState(
                gamePhase = gs.gamePhase,
                players = gs.players,
                uiPlayers = emptyList(),
                deckCards = gs.deck.cards,
                discardPile = gs.discardPile,
                lastDiscardedCard = gs.lastDiscardedCard,
                roundWinnerName = gs.roundWinnerId?.let { id ->
                    gs.players.firstOrNull { it.id == id }?.name
                },
                gameWinnerName = gs.gameWinnerId?.let { id ->
                    gs.players.firstOrNull { it.id == id }?.name
                },
                roundNumber = gs.roundNumber,
                centerFee = gs.centerFee,
                callerId = gs.callerId ?.let { id ->
                    gs.players.firstOrNull { it.id == id }?.name
                },
                localCoordinate = localCoordinate,
                meldAreaCoordinates = meldAreaCoordinates,
                uiEffect = null,
                selectedSapawTarget = null,
                isReveal = gs.isReveal
            )
    }
}
