package com.nowlsantos.tongitsai.ui.players

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.nowlsantos.tongitsai.domain.model.GameUiState
import com.nowlsantos.tongitsai.domain.model.Meld
import com.nowlsantos.tongitsai.domain.model.enums.PlayerPosition
import com.nowlsantos.tongitsai.domain.model.getHandOffset
import com.nowlsantos.tongitsai.ui.components.CardView
import com.nowlsantos.tongitsai.ui.components.SapawMarker
import com.nowlsantos.tongitsai.ui.theme.BrightYellow
import com.nowlsantos.tongitsai.ui.theme.CoolOrange
import com.nowlsantos.tongitsai.util.GameConstants
import com.nowlsantos.tongitsai.util.GameConstants.CARD_HEIGHT
import com.nowlsantos.tongitsai.util.GameConstants.CARD_WIDTH
import com.nowlsantos.tongitsai.util.calculateSapawDropOffset

@Composable
fun PlayerMeld(
    uiState: GameUiState,
    meld: Meld,
    meldIndex: Int,
    playerPosition: PlayerPosition,
    onMeldClickedForSapaw: (meldSourceOffset: DpOffset, playerPosition: PlayerPosition, meldIndex: Int) -> Unit,
    isSelected: Boolean,
    hasSapaw: Boolean,
    modifier: Modifier = Modifier
) {
    //val tag = "PlayerMeld"
    val overlap = GameConstants.getDynamicOverlap(meld.cards.size)
    val cardWidth = CARD_WIDTH + (overlap * (meld.cards.size - 1))

    Box(modifier = modifier
        .size(cardWidth, CARD_HEIGHT)
        /*.border(
            border = BorderStroke(width = borderWidth, color = borderColor),
            shape = RoundedCornerShape(10.dp)
        )*/
        .clickable {
            val meldAreaOffset = uiState.meldAreaCoordinates[playerPosition] ?: DpOffset.Zero
            val baseHandOffset = uiState.localCoordinate.getHandOffset(playerPosition)
            val baseAreaOffset = DpOffset(baseHandOffset.x - CARD_WIDTH, (meldAreaOffset.y.minus(30.dp)))
            val player = uiState.players.firstOrNull { it.position == playerPosition }
            var targetMeldOffset = DpOffset.Zero
            player?.let {
                targetMeldOffset = calculateSapawDropOffset(
                    baseAreaOffset = baseAreaOffset,
                    allMeldsOfTargetPlayer = it.melds,
                    meldIndex = meldIndex,
                    targetMeld = meld
                )
            }
            onMeldClickedForSapaw(targetMeldOffset, playerPosition, meldIndex)
        },
    ) {
        meld.cards.forEachIndexed { index, card ->
            val xOffset = overlap * index
            val cardDisplayModifier = Modifier
                .offset(x = xOffset)
                .zIndex(index.toFloat())

            CardView(
                card = card,
                onClick = null,
                modifier = cardDisplayModifier,
            )
        }

        if (isSelected) {
            val borderColor = BrightYellow
            val borderWidth = 4.dp

            Box(modifier = modifier
                .size(cardWidth, CARD_HEIGHT)
                .scale(scaleX = 1.12f, scaleY = 1.18f)
                .border(
                    border = BorderStroke(width = borderWidth, color = borderColor),
                    shape = RoundedCornerShape(12.dp)
                )
            )
        }

        // Display the "SAPAW" marker if the meld was sapawed this turn
        if (hasSapaw) {
            SapawMarker(
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }
    }
}