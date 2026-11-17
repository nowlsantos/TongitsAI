package com.nowlsantos.tongitsai.ui.animations

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.zIndex
import com.nowlsantos.tongitsai.domain.model.LocalCoordinate
import com.nowlsantos.tongitsai.domain.model.PlayingCard
import com.nowlsantos.tongitsai.domain.model.enums.PlayerPosition
import com.nowlsantos.tongitsai.ui.components.CardView
import com.nowlsantos.tongitsai.util.GameConstants.CARD_HEIGHT
import com.nowlsantos.tongitsai.util.GameConstants.CARD_WIDTH
import com.nowlsantos.tongitsai.util.calculateTargetOffset
import kotlinx.coroutines.launch

@Composable
fun TopCardAnimation(
    chowCard: PlayingCard,
    cardIndexInHand: Int,
    playerPosition: PlayerPosition?,
    localCoordinate: LocalCoordinate,
    onAnimationFinished: () -> Unit
) {
    val discardPileOffset = localCoordinate.discardPileOffset
    val animatedOffset = remember { Animatable(discardPileOffset, DpOffset.VectorConverter) }
    val animatedScale = remember { Animatable(1f) }

    LaunchedEffect(chowCard, discardPileOffset) {
        playerPosition?.let {
            val targetOffset = calculateTargetOffset(
                playerPosition = it,
                cardIndexInHand = cardIndexInHand,
                localCoordinate = localCoordinate
            )

            val durationMillis = 350
            val halfDuration = durationMillis / 2

            launch {
                animatedScale.animateTo(1.08f, animationSpec = tween(halfDuration, easing = FastOutSlowInEasing))
                animatedScale.animateTo(1.0f, animationSpec = tween(halfDuration, easing = FastOutSlowInEasing))
            }

            animatedOffset.animateTo(
                targetOffset,
                animationSpec = tween(
                    durationMillis = durationMillis,
                    easing = FastOutSlowInEasing
                )
            )

            onAnimationFinished()
        }
    }

    CardView(
        card = chowCard,
        isSelected = false,
        onClick = { _, _ -> /* No interaction */ },
        modifier = Modifier
            .offset(x = animatedOffset.value.x, y = animatedOffset.value.y)
            .scale(animatedScale.value)
            .size(CARD_WIDTH, CARD_HEIGHT)
            .zIndex(100f)
    )
}
