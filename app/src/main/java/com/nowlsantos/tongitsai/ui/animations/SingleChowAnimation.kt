package com.nowlsantos.tongitsai.ui.animations

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.zIndex
import com.nowlsantos.tongitsai.domain.model.PlayingCard
import com.nowlsantos.tongitsai.ui.components.CardView
import com.nowlsantos.tongitsai.util.GameConstants.CARD_HEIGHT
import com.nowlsantos.tongitsai.util.GameConstants.CARD_WIDTH
import com.nowlsantos.tongitsai.util.Logger
import kotlinx.coroutines.launch

@Composable
fun SingleChowAnimation(
    chowCard: PlayingCard,
    discardPileOffset: DpOffset,
    targetOffset: DpOffset,
    onAnimationFinished: () -> Unit
) {
    val tag = "SingleChowAnimation"

    val animatedOffset = remember { Animatable(discardPileOffset, DpOffset.VectorConverter) }
    val animatedScale = remember { Animatable(1f) }

    LaunchedEffect(chowCard, discardPileOffset, targetOffset) {
        Logger.d(tag, "SingleChow animation started for $chowCard from $discardPileOffset to $targetOffset")

        animatedOffset.snapTo(discardPileOffset)

        // Smooth elevation/scale pop during flight
        launch {
            animatedScale.animateTo(1.1f, animationSpec = tween(200, easing = FastOutSlowInEasing))
            animatedScale.animateTo(1.0f, animationSpec = tween(200, easing = FastOutSlowInEasing))
        }

        animatedOffset.animateTo(
            targetValue = targetOffset,
            animationSpec = tween(
                durationMillis = 400,
                easing = FastOutSlowInEasing
            )
        )

        Logger.d(tag, "SingleChow animation completed for $chowCard")
        onAnimationFinished()
    }

    Box(
        modifier = Modifier
            .offset(x = animatedOffset.value.x, y = animatedOffset.value.y)
            .scale(animatedScale.value)
            .size(CARD_WIDTH, CARD_HEIGHT)
            .zIndex(100f)
    ) {
        CardView(
            card = chowCard,
            modifier = Modifier.matchParentSize(),
            isSelected = false,
            onClick = { _, _ -> }
        )
    }
}
