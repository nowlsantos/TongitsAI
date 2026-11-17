package com.nowlsantos.tongitsai.ui.players

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.nowlsantos.tongitsai.R
import com.nowlsantos.tongitsai.domain.fsm.game.GamePhase
import com.nowlsantos.tongitsai.domain.model.PlayingCard
import com.nowlsantos.tongitsai.domain.model.UiPlayer
import com.nowlsantos.tongitsai.domain.model.enums.PlayerPosition
import com.nowlsantos.tongitsai.domain.model.enums.PlayerType
import com.nowlsantos.tongitsai.presentation.PLAYER_HAND_LAYOUT_CONFIGS
import com.nowlsantos.tongitsai.ui.components.BackFaceView
import com.nowlsantos.tongitsai.ui.components.CardView
import com.nowlsantos.tongitsai.util.GameConstants.REGULAR_OVERLAP_FRACTION

@Composable
fun PlayerHand(
    modifier: Modifier = Modifier,
    gamePhase: GamePhase,
    uiPlayer: UiPlayer,
    onCardClick: ((PlayingCard, DpOffset) -> Unit)? = null,
    onPlayerCoordinates: (DpOffset) -> Unit,
    onCardOffsetsChanged: (Map<PlayingCard, DpOffset>) -> Unit,
    cardBackRes: Int = R.drawable.card_back,
) {
    var individualCardOffsets by remember { mutableStateOf(emptyMap<PlayingCard, DpOffset>()) }
    val density = LocalDensity.current

    SideEffect {
        if (individualCardOffsets.isNotEmpty()) {
            onCardOffsetsChanged(individualCardOffsets)
        }
    }

    val layoutConfig = PLAYER_HAND_LAYOUT_CONFIGS[uiPlayer.position]
        ?: throw IllegalStateException("Layout config for position ${uiPlayer.position} not found!")

    val alignment = when (uiPlayer.position) {
        PlayerPosition.BOTTOM -> Alignment.BottomStart
        else -> Alignment.TopStart
    }

    Box(
        modifier = modifier.onGloballyPositioned { coordinates ->
            with(density) {
                onPlayerCoordinates(
                    DpOffset(
                        x = coordinates.positionInRoot().x.toDp(),
                        y = coordinates.positionInRoot().y.toDp()
                    )
                )
            }
        }
    ) {
        uiPlayer.displayCards.forEachIndexed { index, card ->
            val cardDisplayModifier = Modifier
                .align(alignment)
                .offset(x = layoutConfig.cardOverlapDp * index)
                .zIndex(index.toFloat())
                .onGloballyPositioned { coordinates ->
                    with(density) {
                        val cardOffset = DpOffset(
                            x = coordinates.positionInRoot().x.toDp(),
                            y = coordinates.positionInRoot().y.toDp()
                        )
                        individualCardOffsets = individualCardOffsets.toMutableMap().apply {
                            this[card] = cardOffset
                        }
                    }
                }

            val isSelected = uiPlayer.selectedCards.contains(card)

            if (uiPlayer.isFaceUp) {
                CardView(
                    card = card,
                    isSelected = isSelected,
                    onClick = if (uiPlayer.type == PlayerType.HUMAN && gamePhase == GamePhase.PLAYING && onCardClick != null) {
                        { c, offset -> onCardClick(c, offset) }
                    } else null,
                    modifier = cardDisplayModifier
                )
            } else {
                BackFaceView(
                    card = card,
                    cardBackRes = cardBackRes,
                    modifier = cardDisplayModifier
                )
            }
        }

        val showScore = gamePhase == GamePhase.REVEAL || gamePhase == GamePhase.ROUND_END
        if (showScore && uiPlayer.score != 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center)
                    .zIndex(100f)
            ) {
                val offsetX = (REGULAR_OVERLAP_FRACTION * uiPlayer.displayCards.size) + 10.dp
                val offsetY = if (uiPlayer.position == PlayerPosition.BOTTOM) 40.dp else 25.dp
                PlayerScore(
                    id = uiPlayer.id,
                    score = uiPlayer.score,
                    offset = DpOffset(offsetX, offsetY)
                )
            }
        }
    }
}
