package com.nowlsantos.tongitsai.domain.fsm.animation

import androidx.compose.ui.unit.DpOffset
import com.nowlsantos.tongitsai.domain.model.Meld
import com.nowlsantos.tongitsai.domain.model.Player
import com.nowlsantos.tongitsai.domain.model.PlayingCard
import com.nowlsantos.tongitsai.domain.model.enums.AnimationType
import com.nowlsantos.tongitsai.domain.model.enums.PlayerPosition

sealed class PendingAction {
    data class CardDealt(
        val card: PlayingCard,
        val startOffset: DpOffset,
        val targetOffset: DpOffset,
        val playerPosition: PlayerPosition,
        val cardIndexInHand: Int,
        val type: AnimationType = AnimationType.DEAL_CARD
    ) : PendingAction()

    data class CardDrawn(
        val card: PlayingCard,
        val playerPosition: PlayerPosition,
        val cardIndexInHand: Int,
        val startOffset: DpOffset,
        val targetOffset: DpOffset,
        val type: AnimationType = AnimationType.GET_CARD,
        val targetPlayer: Player
    ) : PendingAction()

    data class CardDiscarded(
        val card: PlayingCard,
        val startOffset: DpOffset,
        val targetOffset: DpOffset,
        val targetPlayer: Player
    ) : PendingAction()

    data class MeldAnimated(
        val meld: Meld,
        val sourceCardOffsets: Map<PlayingCard, DpOffset>,
        val targetMeldCenterOffset: DpOffset,
        val playerPosition: PlayerPosition,
        val targetPlayer: Player,
    ) : PendingAction()

    data class SingleChowAnimated(
        val chowCard: PlayingCard,
        val discardPileOffset: DpOffset,
        val targetOffset: DpOffset,
        val playerPosition: PlayerPosition,
        val targetPlayer: Player,
        val selectedCards: List<PlayingCard>,
        val sourceCardOffsets: Map<PlayingCard, DpOffset>,
        val targetMeldCenterOffset: DpOffset,
    ) : PendingAction()

    data class ChowCardAnimated(
        val discardCard: PlayingCard,
        val discardPileOffset: DpOffset,
        val playerPosition: PlayerPosition,
        val selectedCards: List<PlayingCard>,
        val sourceCardOffsets: Map<PlayingCard, DpOffset>,
        val targetMeldCenterOffset: DpOffset,
        val targetPlayer: Player
    ) : PendingAction()

    data class SapawCardAnimated(
        val sapawCard: PlayingCard,
        val startOffset: DpOffset,
        val playerPosition: PlayerPosition,
        val targetMeldIndex: Int,
        val targetMeldDropOffset: DpOffset,
        val targetPlayer: Player
    ) : PendingAction()

    data object DealComplete : PendingAction()
}