package com.nowlsantos.tongitsai.presentation

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.DpOffset
import com.nowlsantos.tongitsai.domain.model.Player
import com.nowlsantos.tongitsai.domain.model.PlayingCard
import com.nowlsantos.tongitsai.domain.model.enums.PlayerPosition

/**
 * All possible inputs (events) that the Game State Machine will process.
 */
@Immutable
sealed interface GameInput {

    /* ---------- Setup ---------- */
    data object StartGame : GameInput
    data object DealingCardsComplete : GameInput
    data object CardAnimationFinished : GameInput

    /* ---------- Interaction ---------- */
    data class CardSelection(
        val card: PlayingCard,
        val offset: DpOffset
    ) : GameInput

    data class MeldCards(
        val cards: List<PlayingCard>,
        val sourceCardOffsets: Map<PlayingCard, DpOffset>,
        val targetMeldCenterOffset: DpOffset
    ) : GameInput

    data class DiscardCard(
        val card: PlayingCard,
        val startOffset: DpOffset,
        val targetOffset: DpOffset,
    ) : GameInput

    data class MakeCall(val caller: Player) : GameInput

    data object DrawCard : GameInput
    data object SortBySuit : GameInput
    data object SortByRank : GameInput
    data object RoundEnd : GameInput
    data object GameOver : GameInput

    data class ChowCard(
        val chowCard: PlayingCard,
        val disCardPileOffset: DpOffset,
        val selectedCards: List<PlayingCard>,
        val sourceCardOffsets: Map<PlayingCard, DpOffset>,
    ) : GameInput

    data class SingleChowCard(
        val chowCard: PlayingCard,
        val disCardPileOffset: DpOffset,
        val targetOffset: DpOffset,
        val selectedCards: List<PlayingCard>,
        val sourceCardOffsets: Map<PlayingCard, DpOffset>,
        val targetMeldCenterOffset: DpOffset
    ) : GameInput

    data class SapawCard(
        val sapawCard: PlayingCard,
        val startOffset: DpOffset,
        val sapawTarget: Triple<DpOffset, PlayerPosition, Int>
    ) : GameInput

    data class SelectSapawTarget(
        val meldSourceOffset: DpOffset,
        val playerPosition: PlayerPosition,
        val targetMeldIndex: Int
    ) : GameInput

    //data class ShowSapawError(val error: String) : GameInput
    data class ShowValidationError(val error: String): GameInput

    data class SwapCards(
        val cards: Pair<PlayingCard, PlayingCard>
    ): GameInput

    data class ChallengeDecision(
        val callerId: String,
        val challengerId: String,
        val accepts: Boolean
    ) : GameInput

    data class RoundReveal(val playerIds: List<String>) : GameInput

    data class UpdateLocalCoordinates(val position: PlayerPosition, val offset: DpOffset) : GameInput
    data class UpdateDiscardPileOffset(val offset: DpOffset): GameInput
    data class UpdateDeckOffset(val offset: DpOffset): GameInput
    data class UpdateMeldAreaOffset(val playerPosition: PlayerPosition, val offset: DpOffset): GameInput
    data class UpdateAllPlayersCardOffsets(val position: PlayerPosition, val cardOffsets: Map<PlayingCard, DpOffset>): GameInput
}
