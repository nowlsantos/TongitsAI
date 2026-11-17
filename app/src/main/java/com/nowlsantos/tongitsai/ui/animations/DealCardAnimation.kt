package com.nowlsantos.tongitsai.ui.animations

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.zIndex
import com.nowlsantos.tongitsai.domain.model.LocalCoordinate
import com.nowlsantos.tongitsai.domain.model.PlayingCard
import com.nowlsantos.tongitsai.domain.model.enums.PlayerPosition
import com.nowlsantos.tongitsai.util.calculateTargetOffset

// Internal data class to hold animating card's state
private data class AnimatingCardInstance(
    val card: PlayingCard,
    val initialOffset: DpOffset, // Where the card starts (in Dp)
    val targetOffset: DpOffset, // Where the card ends (in Dp)
    val faceUp: Boolean, // Whether the card is face up or down
    val zIndex: Float = 1f // Optional: z-index for layering
)

@Composable
fun DealCardAnimation(
    localCoordinate: LocalCoordinate,
    card: PlayingCard,
    playerPosition: PlayerPosition?,
    cardIndexInHand: Int,
    cardBackRes: Int = com.nowlsantos.tongitsai.R.drawable.card_back,
    onAnimationFinished: () -> Unit,
) {
    val animatingCards = remember { mutableStateListOf<AnimatingCardInstance>() }
    val deckOffset = localCoordinate.deckOffset

    // Trigger animation when the 'card' or deck position changes
    LaunchedEffect(card) {
        animatingCards.clear()

        playerPosition?.let {
            // Determine the target offset using your calculateTargetOffset function,
            // which should also return a DpOffset
            val targetOffset = calculateTargetOffset(
                playerPosition = it,
                cardIndexInHand = cardIndexInHand,
                localCoordinate = localCoordinate
            )
            // Add the single card to the list for animation
            animatingCards.add(
                AnimatingCardInstance(
                    card = card,
                    initialOffset = deckOffset,
                    targetOffset = targetOffset,
                    faceUp = true
                )
            )
        }
    }

    // Monitor when the single card animation finishes
    LaunchedEffect(animatingCards.isEmpty()) {
        if (animatingCards.isEmpty()) {
            onAnimationFinished.invoke()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        animatingCards.forEach { animCard ->
            AnimatingCard(
                card = animCard.card,
                initialOffset = animCard.initialOffset,
                targetOffset = animCard.targetOffset,
                faceUp = animCard.faceUp,
                cardBackRes = cardBackRes,
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                modifier = Modifier.zIndex(animCard.zIndex),
                onAnimationEnd = {
                    animatingCards.remove(animCard)
                }
            )
        }
    }
}
