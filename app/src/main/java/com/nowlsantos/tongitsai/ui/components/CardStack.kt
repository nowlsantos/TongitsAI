package com.nowlsantos.tongitsai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowlsantos.tongitsai.domain.model.PlayingCard
import com.nowlsantos.tongitsai.ui.theme.CoolOrange
import com.nowlsantos.tongitsai.ui.theme.PureWhite
import com.nowlsantos.tongitsai.util.GameConstants.CARD_HEIGHT
import com.nowlsantos.tongitsai.util.GameConstants.CARD_WIDTH

@Composable
fun CardStack(
    cards: List<PlayingCard>,
    onDeckCoordinates: (DpOffset) -> Unit,
    modifier: Modifier,
    cardBackRes: Int = com.nowlsantos.tongitsai.R.drawable.card_back
) {
    val density = LocalDensity.current

    Row(
        modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Box(modifier = Modifier
            .size(60.dp)
            .clip(CircleShape)
            .background(CoolOrange),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${cards.size}",
                color = PureWhite,
                fontSize = 26.sp,
                fontWeight = FontWeight.Normal
            )
        }

        Box(modifier = Modifier
            .size(CARD_WIDTH, CARD_HEIGHT)
            .size(CARD_WIDTH, CARD_HEIGHT)
            .onGloballyPositioned { coordinates ->
                with(density) {
                    onDeckCoordinates(
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
                text = "Stack",
                color = PureWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.Normal
            )

            cards.lastOrNull()?.let {
                BackFaceView(
                    card = it,
                    cardBackRes = cardBackRes,
                    modifier = Modifier
                        .size(CARD_WIDTH, CARD_HEIGHT)
                        .onGloballyPositioned { coordinates ->
                            with(density) {
                                onDeckCoordinates(
                                    DpOffset(
                                        x = coordinates.positionInRoot().x.toDp(),
                                        y = coordinates.positionInRoot().y.toDp()
                                    )
                                )
                            }
                        }
                )
            }
        }
    }
}