package com.nowlsantos.tongitsai.ui.controls

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import com.nowlsantos.tongitsai.domain.fsm.human.ButtonState
import com.nowlsantos.tongitsai.domain.model.DiscardPile
import com.nowlsantos.tongitsai.domain.model.GameUiState
import com.nowlsantos.tongitsai.domain.model.PlayingCard
import com.nowlsantos.tongitsai.domain.model.enums.PlayerPosition
import com.nowlsantos.tongitsai.domain.model.enums.PlayerType
import com.nowlsantos.tongitsai.presentation.GameInput
import com.nowlsantos.tongitsai.presentation.GameViewModel
import com.nowlsantos.tongitsai.presentation.UserControlEvent
import com.nowlsantos.tongitsai.util.Logger

@Composable
fun UIControls(
    uiState: GameUiState,
    viewModel: GameViewModel,
    discardPile: DiscardPile,
    targetMeldCenterOffset: DpOffset,
    allPlayersCardOffsets: Map<PlayerPosition, Map<PlayingCard, DpOffset>>,
    buttonState: ButtonState,
    modifier: Modifier
) {
    val tag = "UIControls"
    val human = uiState.players.firstOrNull { it.type == PlayerType.HUMAN }
    val selectedCards = human?.selectedCards
    val disCardPileOffset = uiState.localCoordinate.discardPileOffset

    UserControls(
        buttonState = buttonState,
        onClick = { event ->
            when (event) {
                UserControlEvent.Call -> {
                    Logger.d(tag, "Clicked: CALL")
                    val caller = uiState.players.firstOrNull { it.type == PlayerType.HUMAN }
                    caller?.let {
                        viewModel.onEvent(GameInput.MakeCall(caller = it))
                    }
                }

                UserControlEvent.Chow -> {
                    val discardCard = discardPile.topCard
                    selectedCards?.let {
                        if (discardCard != null && it.size > 1) {
                            val humanCardOffsets = allPlayersCardOffsets[PlayerPosition.BOTTOM] ?: emptyMap()
                            val handCardsSourceOffsets = it.associateWith { card ->
                                humanCardOffsets[card] ?: DpOffset.Zero
                            }
                            viewModel.onEvent(
                                GameInput.ChowCard(
                                    chowCard = discardCard,
                                    disCardPileOffset = disCardPileOffset,
                                    selectedCards = selectedCards.toList(),
                                    sourceCardOffsets = handCardsSourceOffsets,
                                )
                            )
                        }
                    }.run {
                        if (discardCard == null) {
                            viewModel.onEvent(GameInput.ShowValidationError("Cannot perform Chow: Discard pile is empty"))
                        } else if (selectedCards!!.isEmpty()) {
                            viewModel.onEvent(GameInput.ShowValidationError("No cards selected from hand."))
                        } else if (selectedCards.size < 2) {
                            viewModel.onEvent(GameInput.ShowValidationError("You must select at least 2 cards to perform a chow."))
                        }
                    }
                }

                UserControlEvent.Get -> {
                    viewModel.onEvent(GameInput.DrawCard)
                }

                UserControlEvent.Meld -> {
                    selectedCards?.let { cards ->
                        viewModel.onEvent(
                            GameInput.MeldCards(
                                cards = cards.toList(),
                                sourceCardOffsets = cards.associateWith { card ->
                                    allPlayersCardOffsets[PlayerPosition.BOTTOM]?.get(card) ?: DpOffset.Zero
                                },
                                targetMeldCenterOffset = targetMeldCenterOffset
                            )
                        )
                    }.run {
                        Logger.d(tag, "Select at least 3 cards from your hand.")
                    }
                }

                UserControlEvent.Sapaw -> {
                    val sapawCard = selectedCards?.firstOrNull()
                    val sapawTarget = uiState.selectedSapawTarget
                    val cardOffset = uiState.localCoordinate.cardOffset

                    if (sapawCard != null && sapawTarget != null) {
                        Logger.d(tag, "SapawTarget:: $sapawTarget | SapawCard:: $sapawCard")
                        viewModel.onEvent(
                            GameInput.SapawCard(sapawCard, cardOffset, sapawTarget)
                        )
                    } else {
                        // Handle the case where either sapawCard or sapawTarget is null
                        Logger.w(tag, "No sapaw card selected")
                        Logger.w(tag, "No sapaw target selected")
                        when {
                            sapawCard == null -> viewModel.onEvent(GameInput.ShowValidationError("No Sapaw card selected."))
                            sapawTarget == null -> viewModel.onEvent(GameInput.ShowValidationError("No sapaw target selected"))
                        }
                    }
                }

                UserControlEvent.Discard -> {
                    selectedCards?.firstOrNull()?.let { discardCard ->
                        Logger.d(tag, "$discardCard")
                        viewModel.onEvent(
                            GameInput.DiscardCard(
                                card = discardCard,
                                startOffset = uiState.localCoordinate.cardOffset,
                                targetOffset = uiState.localCoordinate.discardPileOffset
                            )
                        )
                    } ?: run {
                        // Handle the case where selectedCards is null or empty
                        viewModel.onEvent(GameInput.ShowValidationError("Select one card to discard."))
                    }
                }

                UserControlEvent.Swap -> {
                    selectedCards.let { cardsList ->
                        // Get the first and last cards
                        val firstCard = cardsList?.firstOrNull()
                        val lastCard = cardsList?.lastOrNull()
                        if (firstCard != null && lastCard != null) {
                            viewModel.onEvent(GameInput.SwapCards(cards = Pair(firstCard, lastCard)))
                        }
                    }.run {
                        viewModel.onEvent(GameInput.ShowValidationError("Select two cards to swap"))
                    }
                }
                UserControlEvent.SortRank -> viewModel.onEvent(GameInput.SortByRank)
                UserControlEvent.SortSuit -> viewModel.onEvent(GameInput.SortBySuit)
            }
        },
        modifier = modifier
    )
}