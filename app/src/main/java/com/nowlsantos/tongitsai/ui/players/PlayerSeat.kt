package com.nowlsantos.tongitsai.ui.players

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.nowlsantos.tongitsai.R
import com.nowlsantos.tongitsai.domain.model.GameUiState
import com.nowlsantos.tongitsai.domain.model.PlayingCard
import com.nowlsantos.tongitsai.domain.model.UiPlayer
import com.nowlsantos.tongitsai.domain.model.enums.PlayerPosition
import com.nowlsantos.tongitsai.domain.model.enums.PlayerType
import com.nowlsantos.tongitsai.presentation.GameInput
import com.nowlsantos.tongitsai.presentation.GameViewModel
import com.nowlsantos.tongitsai.util.GameConstants.ADD_OFFSET

@Composable
fun PlayerSeat(
    modifier: Modifier = Modifier,
    uiPlayer: UiPlayer,
    uiState: GameUiState,
    viewModel: GameViewModel,
    onCardClick: ((PlayingCard, DpOffset) -> Unit)? = null,
    onPlayerCoordinates: (DpOffset) -> Unit,
    onCardOffsetsChanged: (Map<PlayingCard, DpOffset>) -> Unit,
    onMeldClickedForSapaw: (DpOffset, PlayerPosition, Int) -> Unit,
    cardBackRes: Int = R.drawable.card_back,
) {
    val density = LocalDensity.current

    Column(modifier = modifier) {
        if (uiPlayer.type == PlayerType.AI) {
            PlayerBalance(
                uiPlayer = uiPlayer,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )

            PlayerHand(
                gamePhase = uiState.gamePhase,
                uiPlayer = uiPlayer,
                onCardClick = null,
                onPlayerCoordinates = onPlayerCoordinates,
                onCardOffsetsChanged = onCardOffsetsChanged,
                cardBackRes = cardBackRes,
                modifier = Modifier
                    .weight(3f)
                    .fillMaxWidth()
                    .padding(20.dp)
            )

            Row(
                modifier = Modifier
                    .weight(3f)
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        val meldAreaOffset = with(density) {
                            DpOffset(
                                x = coordinates.positionInRoot().x.toDp() + coordinates.size.width.toDp() / 2 - ADD_OFFSET.x,
                                y = coordinates.positionInRoot().y.toDp() + ADD_OFFSET.y
                            )
                        }
                        viewModel.onEvent(GameInput.UpdateMeldAreaOffset(uiPlayer.position, meldAreaOffset))
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for ((index, meld) in uiPlayer.melds.withIndex()) {
                    val isSelected = uiState.selectedSapawTarget?.let { (_, playerPosition, meldIndex) ->
                        uiState.players.firstOrNull { p -> p.position == playerPosition }?.id == uiPlayer.id && meldIndex == index
                    } ?: false
                    PlayerMeld(
                        uiState = uiState,
                        meld = meld,
                        meldIndex = index,
                        playerPosition = uiPlayer.position,
                        onMeldClickedForSapaw = onMeldClickedForSapaw,
                        isSelected = isSelected,
                        hasSapaw = meld.hasSapaw
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .weight(3f)
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        val meldAreaOffset = with(density) {
                            DpOffset(
                                x = coordinates.positionInRoot().x.toDp() + coordinates.size.width.toDp() / 2 - ADD_OFFSET.x,
                                y = coordinates.positionInRoot().y.toDp() + ADD_OFFSET.y
                            )
                        }
                        viewModel.onEvent(GameInput.UpdateMeldAreaOffset(uiPlayer.position, meldAreaOffset))
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for ((index, meld) in uiPlayer.melds.withIndex()) {
                    val isSelected = uiState.selectedSapawTarget?.let { (_, playerPosition, meldIndex) ->
                        uiState.players.firstOrNull { p -> p.position == playerPosition }?.id == uiPlayer.id && meldIndex == index
                    } ?: false
                    PlayerMeld(
                        uiState = uiState,
                        meld = meld,
                        meldIndex = index,
                        playerPosition = uiPlayer.position,
                        onMeldClickedForSapaw = onMeldClickedForSapaw,
                        isSelected = isSelected,
                        hasSapaw = meld.hasSapaw
                    )
                }
            }

            PlayerHand(
                gamePhase = uiState.gamePhase,
                uiPlayer = uiPlayer,
                onCardClick = onCardClick,
                onPlayerCoordinates = onPlayerCoordinates,
                onCardOffsetsChanged = onCardOffsetsChanged,
                cardBackRes = cardBackRes,
                modifier = Modifier
                    .weight(3f)
                    .fillMaxWidth()
                    .padding(20.dp)
            )

            PlayerBalance(
                uiPlayer = uiPlayer,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
        }
    }
}
