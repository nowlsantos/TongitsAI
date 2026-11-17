package com.nowlsantos.tongitsai.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.nowlsantos.tongitsai.domain.model.PlayingCard
import com.nowlsantos.tongitsai.util.CardImageMapper.getCardDrawableResId
import com.nowlsantos.tongitsai.util.GameConstants.CARD_HEIGHT
import com.nowlsantos.tongitsai.util.GameConstants.CARD_WIDTH

@Composable
fun CardView(
    card: PlayingCard,
    isSelected: Boolean = false,
    onClick: ((PlayingCard, DpOffset) -> Unit)?,
    modifier: Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val imageResId = context.getCardDrawableResId(card)

    // Animate the vertical offset for selected cards
    val offsetY by animateDpAsState(
        targetValue = if (isSelected) (-40).dp else 0.dp,
        animationSpec = tween(durationMillis = 100),
        label = "cardOffsetAnimation"
    )

    // Store the card's latest global position as DpOffset
    val currentCardGlobalOffset = remember { mutableStateOf(DpOffset.Zero) }

    val animatedOffsetModifier = Modifier.offset(y = offsetY)

    Image(
        painter = painterResource(id = imageResId),
        contentDescription = "${card.rank.name} of ${card.suit.symbol}",
        modifier = modifier
            .then(animatedOffsetModifier)
            .size(CARD_WIDTH, CARD_HEIGHT)
            .onGloballyPositioned { coordinates ->
                with(density) {
                    currentCardGlobalOffset.value = DpOffset(
                        x = coordinates.positionInRoot().x.toDp(),
                        y = coordinates.positionInRoot().y.toDp()
                    )
                }
            }
            .then(
                if (onClick != null) {
                    Modifier.clickable {
                        onClick(card, currentCardGlobalOffset.value)
                    }
                } else {
                    Modifier
                }
            )
    )
}