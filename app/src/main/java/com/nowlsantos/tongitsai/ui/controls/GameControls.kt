package com.nowlsantos.tongitsai.ui.controls

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.nowlsantos.tongitsai.domain.model.GameUiState
import com.nowlsantos.tongitsai.domain.model.PlayingCard
import com.nowlsantos.tongitsai.domain.model.enums.PlayerPosition
import com.nowlsantos.tongitsai.presentation.GameViewModel

@Composable
fun GameControls(
    uiState: GameUiState,
    viewModel: GameViewModel,
    message: String,
    meldingTargetOffset: DpOffset,
    allPlayersCardOffsets: Map<PlayerPosition, Map<PlayingCard, DpOffset>>,
    onDeckCoordinates: (offset: DpOffset) -> Unit,
    onDiscardCoordinates: (offset: DpOffset) -> Unit,
    modifier: Modifier = Modifier,
    cardBackRes: Int = com.nowlsantos.tongitsai.R.drawable.card_back
) {
    val buttonState by viewModel.buttonState.collectAsState()

    Column(modifier = modifier.fillMaxWidth()) {
        InformationControl(
            uiState = uiState,
            viewModel = viewModel,
            onDeckCoordinates = onDeckCoordinates,
            onDiscardCoordinates = onDiscardCoordinates,
            modifier = Modifier.weight(1f),
            cardBackRes = cardBackRes
        )

        HorizontalDivider(
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f),
            thickness = 1.dp
        )

        Column(
            modifier = Modifier
                .weight(1.35f)
                .fillMaxWidth()
        ) {
            MessageControl(
                gamePhase = uiState.gamePhase,
                viewModel = viewModel,
                message = message,
                modifier = Modifier
                    .weight(0.75f)
                    .fillMaxWidth()
            )

            UIControls(
                uiState = uiState,
                viewModel = viewModel,
                discardPile = uiState.discardPile,
                targetMeldCenterOffset = meldingTargetOffset,
                allPlayersCardOffsets = allPlayersCardOffsets,
                buttonState = buttonState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
        }
    }
}
