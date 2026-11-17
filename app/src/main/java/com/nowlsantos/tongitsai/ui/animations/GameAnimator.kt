package com.nowlsantos.tongitsai.ui.animations

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.zIndex
import com.nowlsantos.tongitsai.domain.model.GameUiState
import com.nowlsantos.tongitsai.domain.model.LocalCoordinate
import com.nowlsantos.tongitsai.domain.model.PlayingCard
import com.nowlsantos.tongitsai.domain.model.enums.AnimationType
import com.nowlsantos.tongitsai.domain.model.enums.PlayerPosition
import com.nowlsantos.tongitsai.presentation.GameInput
import com.nowlsantos.tongitsai.presentation.GameViewModel
import com.nowlsantos.tongitsai.presentation.UiEffect
import kotlinx.coroutines.flow.collectLatest

@Composable
fun GameAnimator(
    localCoordinate: LocalCoordinate,
    uiState: GameUiState,
    viewModel: GameViewModel,
    cardBackRes: Int = com.nowlsantos.tongitsai.R.drawable.card_back
) {
    // Animation specific states (consolidated)
    var currentAnimatingCard by remember { mutableStateOf<PlayingCard?>(null) }
    var currentAnimationType by remember { mutableStateOf<AnimationType?>(null) }
    var animationStartOffset by remember { mutableStateOf(DpOffset.Zero) }
    var animationTargetOffset by remember { mutableStateOf(DpOffset.Zero) }
    var animationTargetPlayerPosition by remember { mutableStateOf<PlayerPosition?>(null) }
    var animationCardIndexInHand by remember { mutableIntStateOf(0) }

    // Source offsets
    var currentMeldingCards by remember { mutableStateOf<List<PlayingCard>>(emptyList()) }
    var meldingSourceOffsets by remember { mutableStateOf<Map<PlayingCard, DpOffset>>(emptyMap()) }
    var meldingTargetOffset by remember { mutableStateOf(DpOffset.Zero) }

    // reset functions (now owned by this composable)
    val resetSingleCardAnimationStates: () -> Unit = {
        currentAnimatingCard = null
        currentAnimationType = null
        animationStartOffset = DpOffset.Zero
        animationTargetOffset = DpOffset.Zero
        animationTargetPlayerPosition = null
        animationCardIndexInHand = -1
    }
    val resetMeldAnimationStates: () -> Unit = {
        currentMeldingCards = emptyList()
        meldingSourceOffsets = emptyMap()
        meldingTargetOffset = DpOffset.Zero
    }

    // LaunchedEffect to observe the UiEffect stream and trigger state changes
    LaunchedEffect(Unit) {
        viewModel.uiEffect.collectLatest { effect ->
            when (effect) {
                is UiEffect.StartCardAnimation -> {
                    resetMeldAnimationStates()

                    currentAnimatingCard = effect.card
                    currentAnimationType = effect.type
                    animationStartOffset = effect.startOffset
                    animationTargetOffset = effect.targetOffset
                    animationTargetPlayerPosition = uiState.players.firstOrNull { it.position == effect.playerPosition }?.position
                    animationCardIndexInHand = effect.cardIndexInHand ?: -1
                }
                is UiEffect.StartMeldAnimation -> {
                    resetSingleCardAnimationStates()

                    currentMeldingCards = effect.meldedCards
                    meldingSourceOffsets = effect.sourceCardOffsets
                    meldingTargetOffset = effect.targetMeldCenterOffset
                    currentAnimationType = AnimationType.MELD_CARDS
                }
                is UiEffect.StartDiscardAnimation -> {
                    currentAnimatingCard = effect.card
                    animationStartOffset = effect.startOffset
                    animationTargetOffset = effect.targetOffset
                    currentAnimationType = AnimationType.DISCARD
                }
                is UiEffect.StartChowCardAnimation -> {
                    resetSingleCardAnimationStates()
                    currentAnimatingCard = effect.discardCard
                    animationStartOffset = effect.discardPileOffset
                    currentAnimationType = AnimationType.CHOW_CARD
                    animationCardIndexInHand = effect.cardIndexInHand ?: -1
                    animationTargetPlayerPosition = effect.playerPosition
                }
                is UiEffect.StartSingleChowAnimation -> {
                    resetSingleCardAnimationStates()
                    currentAnimatingCard = effect.chowCard
                    animationStartOffset = effect.discardPileOffset
                    animationTargetOffset = effect.targetOffset
                    currentAnimationType = AnimationType.SINGLE_CHOW
                }
                is UiEffect.StartSapawCardAnimation -> {
                    currentAnimatingCard = effect.card
                    animationStartOffset = effect.startOffset
                    animationTargetOffset = effect.targetMeldOffset
                    currentAnimationType = AnimationType.SAPAW_CARD
                }
                // Other UiEffect types should be handled elsewhere
                else -> { /* No animation */ }
            }
        }
    }

    // Your existing animation code, now within a single composable
    currentAnimatingCard?.let { card ->
        Box(modifier = Modifier
            .fillMaxSize()
            //.align(Alignment.TopStart)
            .zIndex(1f)
        ) {
            when (currentAnimationType) {
                AnimationType.DEAL_CARD,
                AnimationType.GET_CARD -> {
                    DealCardAnimation(
                        localCoordinate = localCoordinate,
                        card = card,
                        playerPosition = animationTargetPlayerPosition,
                        cardIndexInHand = animationCardIndexInHand,
                        cardBackRes = cardBackRes,
                        onAnimationFinished = {
                            viewModel.onEvent(GameInput.CardAnimationFinished)
                            resetSingleCardAnimationStates()
                        }
                    )
                }
                AnimationType.SINGLE_CHOW -> {
                    SingleChowAnimation(
                        chowCard = card,
                        discardPileOffset = animationStartOffset,
                        targetOffset = animationTargetOffset,
                        onAnimationFinished = {
                            viewModel.onEvent(GameInput.CardAnimationFinished)
                            resetSingleCardAnimationStates()
                        }
                    )
                }
                AnimationType.DISCARD -> {
                    DiscardAnimation(
                        card = card,
                        startOffset = animationStartOffset,
                        targetOffset = animationTargetOffset,
                        onAnimationFinished = {
                            viewModel.onEvent(GameInput.CardAnimationFinished)
                            resetSingleCardAnimationStates()
                        }
                    )
                }
                AnimationType.CHOW_CARD -> {
                    TopCardAnimation(
                        chowCard = card,
                        cardIndexInHand = animationCardIndexInHand,
                        playerPosition = animationTargetPlayerPosition,
                        localCoordinate = localCoordinate,
                        onAnimationFinished = {
                            viewModel.onEvent(GameInput.CardAnimationFinished)
                            resetSingleCardAnimationStates()
                        }
                    )
                }
                AnimationType.SAPAW_CARD -> {
                    SapawCardAnimation(
                        card = card,
                        startOffset = animationStartOffset,
                        targetOffset = animationTargetOffset,
                        onAnimationFinished = {
                            viewModel.onEvent(GameInput.CardAnimationFinished)
                            resetSingleCardAnimationStates()
                        }
                    )
                }
                else -> { /* No animation for other types or null */ }
            }
        }
    }

    if (currentMeldingCards.isNotEmpty()) {
        MeldCardsAnimation(
            meldedCards = currentMeldingCards ,
            sourceCardOffsets = meldingSourceOffsets,
            targetMeldAreaOffset = meldingTargetOffset,
            onAnimationFinished = {
                resetMeldAnimationStates()
                viewModel.onEvent(GameInput.CardAnimationFinished)
            },
            modifier = Modifier
                .fillMaxSize()
                .zIndex(2f)
        )
    }
}