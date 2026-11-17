package com.nowlsantos.tongitsai.ui.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import com.nowlsantos.tongitsai.domain.model.GameUiState
import com.nowlsantos.tongitsai.presentation.GameViewModel
import com.nowlsantos.tongitsai.ui.components.CardStack
import com.nowlsantos.tongitsai.ui.components.CenterPot
import com.nowlsantos.tongitsai.ui.components.DiscardedPile
import com.nowlsantos.tongitsai.ui.theme.PureWhite

@Composable
fun InformationControl(
    uiState: GameUiState,
    viewModel: GameViewModel,
    onDeckCoordinates: (DpOffset) -> Unit,
    onDiscardCoordinates: (DpOffset) -> Unit,
    modifier: Modifier,
    cardBackRes: Int = com.nowlsantos.tongitsai.R.drawable.card_back
) {
    Row(modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.weight(1f).background(PureWhite.copy(alpha = 0.15f)),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CardStack(
                cards = uiState.deckCards,
                onDeckCoordinates = onDeckCoordinates,
                modifier = Modifier.weight(1f),
                cardBackRes = cardBackRes
            )

            DiscardedPile(
                cards = uiState.discardPile.cards,
                onDiscardCoordinates = onDiscardCoordinates,
                modifier = Modifier.weight(1f)
            )
        }

        CenterPot(
            centerFee = uiState.centerFee,
            roundNumber = uiState.roundNumber,
            gamePhase = uiState.gamePhase,
            viewModel = viewModel,
            modifier = Modifier.weight(1f)
        )
    }
}
