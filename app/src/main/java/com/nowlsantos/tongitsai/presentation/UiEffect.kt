package com.nowlsantos.tongitsai.presentation

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.DpOffset
import com.nowlsantos.tongitsai.data.GameData
import com.nowlsantos.tongitsai.domain.model.PlayingCard
import com.nowlsantos.tongitsai.domain.model.enums.AnimationType
import com.nowlsantos.tongitsai.domain.model.enums.PlayerPosition

/**
 * One-shot UI events that the ViewModel sends to the Composable layer.
 * They are consumed once via `LaunchedEffect`.
 */
@Immutable
sealed interface UiEffect {
    data class ShowWinner(val message: String) : UiEffect
    data class ShowMessage(val message: String) : UiEffect
    data class InitialMessage(val message: String) : UiEffect
    data class ShowError(val message: String) : UiEffect
    data class ShowGameOver(val message: String) : UiEffect
    data object ClearMessage : UiEffect
    data object ResetDialog: UiEffect
    data class GameResultDialog(val gameData: GameData): UiEffect

    /* ---------- Animation effects ---------- */
    data class StartCardAnimation(
        val card: PlayingCard,
        val type: AnimationType,
        val cardIndexInHand: Int? = null,
        val playerPosition: PlayerPosition,
        val startOffset: DpOffset,
        val targetOffset: DpOffset
    ) : UiEffect

    data class StartMeldAnimation(
        val meldedCards: List<PlayingCard>,
        val sourceCardOffsets: Map<PlayingCard, DpOffset>,
        val targetMeldCenterOffset: DpOffset
    ) : UiEffect

    data class StartDiscardAnimation(
        val card: PlayingCard,
        val startOffset: DpOffset,
        val targetOffset: DpOffset
    ) : UiEffect

    data class StartSingleChowAnimation(
        val chowCard: PlayingCard,
        val discardPileOffset: DpOffset,
        val targetOffset: DpOffset,
    ) : UiEffect

    data class StartChowCardAnimation(
        val discardCard: PlayingCard,
        val discardPileOffset: DpOffset,
        val targetMeldCenterOffset: DpOffset,
        val cardIndexInHand: Int? = null,
        val playerPosition: PlayerPosition,
    ) : UiEffect

    data class StartSapawCardAnimation(
        val card: PlayingCard,
        val startOffset: DpOffset,
        val playerPosition: PlayerPosition,
        val targetMeldIndex: Int,
        val targetMeldOffset: DpOffset
    ) : UiEffect

    data class PlayerCall(
        val callerId: String,
        val message: String,
        val isNext: Boolean,
    ) : UiEffect

    data class AskCallChallenge(
        val callerId: String,
        val callerName: String,
        val challengerId: String,
        val message: String
    ) : UiEffect
}