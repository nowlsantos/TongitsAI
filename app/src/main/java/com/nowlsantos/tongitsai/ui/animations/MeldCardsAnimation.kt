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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.nowlsantos.tongitsai.domain.model.PlayingCard
import com.nowlsantos.tongitsai.ui.components.CardView
import com.nowlsantos.tongitsai.util.GameConstants.CARD_HEIGHT
import com.nowlsantos.tongitsai.util.GameConstants.CARD_WIDTH
import com.nowlsantos.tongitsai.util.GameConstants.OVERLAP_FRACTION
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MeldCardsAnimation(
    meldedCards: List<PlayingCard>,
    sourceCardOffsets: Map<PlayingCard, DpOffset>,
    targetMeldAreaOffset: DpOffset,
    onAnimationFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedOffsets = meldedCards.associateWith { card ->
        remember(card) { Animatable(sourceCardOffsets[card] ?: DpOffset.Zero, DpOffset.VectorConverter) }
    }
    val animatedScales = meldedCards.associateWith { card ->
        remember(card) { Animatable(1f) }
    }

    LaunchedEffect(meldedCards, sourceCardOffsets, targetMeldAreaOffset) {
        if (meldedCards.isNotEmpty()) {
            val animationDuration = 400
            val staggerDelay = 60

            val finalPositions = meldedCards.mapIndexed { index, card ->
                val xOffsetDp = OVERLAP_FRACTION * index.toFloat()
                targetMeldAreaOffset + DpOffset(xOffsetDp, 0.dp)
            }

            meldedCards.forEachIndexed { index, card ->
                val targetOffset = finalPositions[index]
                launch {
                    delay(index * staggerDelay.toLong())

                    launch {
                        animatedScales[card]?.animateTo(1.1f, animationSpec = tween(200, easing = FastOutSlowInEasing))
                        animatedScales[card]?.animateTo(1.0f, animationSpec = tween(200, easing = FastOutSlowInEasing))
                    }

                    animatedOffsets[card]?.animateTo(
                        targetValue = targetOffset,
                        animationSpec = tween(
                            durationMillis = animationDuration,
                            easing = FastOutSlowInEasing
                        )
                    )
                }
            }
            val totalAnimationTime = (animationDuration + (meldedCards.size - 1) * staggerDelay).toLong()
            delay(totalAnimationTime + 60)

            onAnimationFinished()
        }
    }

    Box(modifier = modifier) {
        meldedCards.forEach { card ->
            val currentOffset = animatedOffsets[card]?.value ?: DpOffset.Zero
            val currentScale = animatedScales[card]?.value ?: 1f
            Box(
                modifier = Modifier
                    .offset(
                        x = currentOffset.x,
                        y = currentOffset.y
                    )
                    .scale(currentScale)
                    .size(CARD_WIDTH, CARD_HEIGHT)
                    .zIndex(card.rank.sequentialValue.toFloat() + 100f)
            ) {
                CardView(
                    card = card,
                    modifier = Modifier.matchParentSize(),
                    isSelected = false,
                    onClick = { _, _ -> }
                )
            }
        }
    }
}
