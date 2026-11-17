package com.nowlsantos.tongitsai.ui.animations

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import kotlinx.coroutines.launch

@Composable
fun SapawCardAnimation(
    card: PlayingCard,
    startOffset: DpOffset,
    targetOffset: DpOffset,
    onAnimationFinished: () -> Unit,
) {
    val animatedOffset = remember { Animatable(startOffset, DpOffset.VectorConverter) }
    val animatedScale = remember { Animatable(1f) }

    LaunchedEffect(card, startOffset, targetOffset) {
        animatedOffset.snapTo(startOffset)

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
        onAnimationFinished()
    }

    Box(
        modifier = Modifier
            .size(CARD_WIDTH, CARD_HEIGHT)
            .offset(
                x = animatedOffset.value.x,
                y = animatedOffset.value.y
            )
            .scale(animatedScale.value)
            .zIndex(100f)
    ) {
        CardView(
            card = card,
            modifier = Modifier.matchParentSize(),
            isSelected = false,
            onClick = { _, _ -> }
        )
    }
}
