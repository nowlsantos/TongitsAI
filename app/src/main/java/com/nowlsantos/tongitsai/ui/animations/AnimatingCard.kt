package com.nowlsantos.tongitsai.ui.animations

import androidx.compose.animation.core.*
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
import com.nowlsantos.tongitsai.ui.components.BackFaceView
import com.nowlsantos.tongitsai.util.GameConstants.CARD_HEIGHT
import com.nowlsantos.tongitsai.util.GameConstants.CARD_WIDTH
import kotlinx.coroutines.launch

@Composable
fun AnimatingCard(
    card: PlayingCard,
    initialOffset: DpOffset,
    targetOffset: DpOffset,
    modifier: Modifier = Modifier,
    faceUp: Boolean,
    cardBackRes: Int = com.nowlsantos.tongitsai.R.drawable.card_back,
    animationSpec: AnimationSpec<DpOffset> = tween(durationMillis = 400, easing = FastOutSlowInEasing),
    onAnimationEnd: (() -> Unit)? = null,
) {
    val animatedOffset = remember {
        Animatable(initialOffset, DpOffset.VectorConverter)
    }
    val animatedScale = remember { Animatable(1f) }

    LaunchedEffect(initialOffset, targetOffset) {
        animatedOffset.snapTo(initialOffset)

        launch {
            animatedScale.animateTo(1.1f, animationSpec = tween(200, easing = FastOutSlowInEasing))
            animatedScale.animateTo(1.0f, animationSpec = tween(200, easing = FastOutSlowInEasing))
        }

        animatedOffset.animateTo(
            targetValue = targetOffset,
            animationSpec = animationSpec
        )

        onAnimationEnd?.invoke()
    }

    Box(
        modifier = modifier
            .size(CARD_WIDTH, CARD_HEIGHT)
            .offset(
                x = animatedOffset.value.x,
                y = animatedOffset.value.y
            )
            .scale(animatedScale.value)
            .zIndex(100f)
    ) {
        BackFaceView(card = card, cardBackRes = cardBackRes)
    }
}
