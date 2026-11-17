package com.nowlsantos.tongitsai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowlsantos.tongitsai.domain.model.PlayingCard
import com.nowlsantos.tongitsai.ui.theme.PureWhite
import com.nowlsantos.tongitsai.util.GameConstants.CARD_HEIGHT
import com.nowlsantos.tongitsai.util.GameConstants.CARD_WIDTH

@Composable
fun DiscardedPile(
    cards: List<PlayingCard?>,
    onDiscardCoordinates: (DpOffset) -> Unit,
    modifier: Modifier
) {
    val density = LocalDensity.current

    Box(
        modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier
            .size(CARD_WIDTH, CARD_HEIGHT)
            .onGloballyPositioned { coordinates ->
                with(density) {
                    onDiscardCoordinates(
                        DpOffset(
                            x = coordinates.positionInRoot().x.toDp(),
                            y = coordinates.positionInRoot().y.toDp()
                        )
                    )
                }
            }
            .background(PureWhite.copy(alpha = 0.25f), shape = RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Discard",
                color = PureWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.Normal
            )
        }

        cards.lastOrNull()?.let {
            CardView(
                card = it,
                isSelected = false,
                onClick = {_, _ ->},
                modifier = Modifier
                    .size(CARD_WIDTH, CARD_HEIGHT)
                    .fillMaxSize()
            )
        }
    }
}