package com.nowlsantos.tongitsai.domain.model

import androidx.compose.runtime.Immutable
import com.nowlsantos.tongitsai.domain.fsm.game.RevealType
import com.nowlsantos.tongitsai.domain.fsm.game.GamePhase

/**
 * Authoritative, immutable snapshot of one Tongits round.
 */
@Immutable
data class GameState(
    val gamePhase: GamePhase,
    val deck: Deck,
    val players: List<Player>,
    val currentPlayerIndex: Int,
    val discardPile: DiscardPile,
    val lastDiscardedCard: PlayingCard? = null,
    val roundWinnerId: String? = null,
    val gameWinnerId: String? = null,
    val roundNumber: Int = 0,
    val callerId: String? = null,
    val centerFee: Int = 0,
    val revealType: RevealType = RevealType.NONE,
    val isReveal: Boolean = false,
    val isGameOver: Boolean = false,
    val lastPlayerToDiscardId: String? = null,
    val lastRoundWonTwoHits: Boolean = false,
    val lastRoundWinnerId: String? = null,
) {
    init {
        require(players.size >= 2) { "Tongits requires exactly 3 players" }
        require(currentPlayerIndex in players.indices) { "Invalid player index" }
    }

    val currentPlayer: Player get() = players[currentPlayerIndex]

    companion object {
        fun initial(): GameState {
            // Initialize the players
            val players = Player.initialize()

            return GameState(
                gamePhase = GamePhase.SETUP,
                deck = Deck.standard(),
                players = players,
                currentPlayerIndex = 0,
                discardPile = DiscardPile(),
            )
        }
    }
}
