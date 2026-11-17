package com.nowlsantos.tongitsai.domain.fsm.animation

import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.nowlsantos.tongitsai.domain.model.GameState
import com.nowlsantos.tongitsai.domain.model.GameUiState
import com.nowlsantos.tongitsai.domain.model.Meld
import com.nowlsantos.tongitsai.domain.model.Player
import com.nowlsantos.tongitsai.domain.model.PlayingCard
import com.nowlsantos.tongitsai.domain.model.enums.AnimationType
import com.nowlsantos.tongitsai.domain.model.enums.PlayerPosition
import com.nowlsantos.tongitsai.presentation.GameInput
import com.nowlsantos.tongitsai.presentation.UiEffect
import com.nowlsantos.tongitsai.util.GameConstants.CARD_WIDTH
import com.nowlsantos.tongitsai.util.GameConstants.REGULAR_OVERLAP_FRACTION
import com.nowlsantos.tongitsai.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class GameAnimationState(
    internal val externalScope: CoroutineScope,
    private val gameState: MutableStateFlow<GameState>,
    private val uiState: StateFlow<GameUiState>,
    private val emitUiEffect: (UiEffect) -> Unit,
    private val handleGameInput: (GameInput) -> Unit,
    private val onDiscardCompleteCallback: (PlayingCard, player: Player) -> Unit,
    private val onDrawCompleteCallback: (drawnCard: PlayingCard, player: Player) -> Unit,
    private val onMeldCompleteCallback: (meld: Meld, player: Player) -> Unit,
    private val onSapawCompleteCallback: (
        sapawCard: PlayingCard, meldIndex: Int, position: PlayerPosition, player: Player
    ) -> Unit,
) {
    private val tag = "GameAnimationState"
    private val _pendingAnimationAction = MutableStateFlow<PendingAction?>(null)

    private val cardsDealtPerPlayer: MutableMap<PlayerPosition, Int> = mutableMapOf()
    private var playersInDealOrder: List<Player> = emptyList()
    private var currentDealingRoundPlayerIndex = 0

    init {
        Logger.d(tag, "GameAnimationState initialized.")
    }

    fun setPendingAnimationAction(action: PendingAction?) {
        externalScope.launch {
            _pendingAnimationAction.value = action

            action?.let { pendingAction ->
                when (pendingAction) {
                    is PendingAction.CardDealt -> {
                        emitUiEffect(
                            UiEffect.StartCardAnimation(
                                card = pendingAction.card,
                                type = pendingAction.type,
                                cardIndexInHand = pendingAction.cardIndexInHand,
                                playerPosition = pendingAction.playerPosition,
                                startOffset = pendingAction.startOffset,
                                targetOffset = pendingAction.targetOffset
                            )
                        )
                    }

                    is PendingAction.CardDrawn -> {
                        emitUiEffect(
                            UiEffect.StartCardAnimation(
                                card = pendingAction.card,
                                type = pendingAction.type,
                                cardIndexInHand = pendingAction.cardIndexInHand,
                                playerPosition = pendingAction.playerPosition,
                                startOffset = pendingAction.startOffset,
                                targetOffset = pendingAction.targetOffset
                            )
                        )
                    }

                    is PendingAction.CardDiscarded -> {
                        emitUiEffect(
                            UiEffect.StartDiscardAnimation(
                                card = pendingAction.card,
                                startOffset = pendingAction.startOffset,
                                targetOffset = pendingAction.targetOffset
                            )
                        )
                    }

                    is PendingAction.MeldAnimated -> {
                        emitUiEffect(
                            UiEffect.StartMeldAnimation(
                                meldedCards = pendingAction.meld.cards,
                                sourceCardOffsets = pendingAction.sourceCardOffsets,
                                targetMeldCenterOffset = pendingAction.targetMeldCenterOffset
                            )
                        )
                    }

                    is PendingAction.SingleChowAnimated -> {
                        emitUiEffect(
                            UiEffect.StartSingleChowAnimation(
                                chowCard = pendingAction.chowCard,
                                discardPileOffset = pendingAction.discardPileOffset,
                                targetOffset = pendingAction.targetOffset
                            )
                        )
                    }

                    is PendingAction.ChowCardAnimated -> {
                        emitUiEffect(
                            UiEffect.StartChowCardAnimation(
                                discardCard = pendingAction.discardCard,
                                discardPileOffset = pendingAction.discardPileOffset,
                                targetMeldCenterOffset = pendingAction.targetMeldCenterOffset,
                                cardIndexInHand = pendingAction.targetPlayer.cards.size,
                                playerPosition = pendingAction.playerPosition
                            )
                        )
                    }

                    is PendingAction.SapawCardAnimated -> {
                        emitUiEffect(
                            UiEffect.StartSapawCardAnimation(
                                card = pendingAction.sapawCard,
                                startOffset = pendingAction.startOffset,
                                playerPosition = pendingAction.playerPosition,
                                targetMeldIndex = pendingAction.targetMeldIndex,
                                targetMeldOffset = pendingAction.targetMeldDropOffset
                            )
                        )
                    }

                    is PendingAction.DealComplete -> {
                        Logger.d(tag, "DealComplete action set. Signalling GameStateMachine about dealing completion.")
                        _pendingAnimationAction.value = null
                        externalScope.launch {
                            handleGameInput(GameInput.DealingCardsComplete)
                        }
                    }
                }
            }
        }
    }

    fun prepareDealingSequence(players: List<Player>) {
        Logger.d(tag, "prepareDealingSequence received players: ${players.map { "${it.name}(${it.position}, firstMove=${it.firstMove})" }}")

        cardsDealtPerPlayer.clear()
        players.forEach { player ->
            cardsDealtPerPlayer[player.position] = 0
        }

        val dealerPlayer = players.firstOrNull { it.firstMove } ?: players[0]
        val numPlayers = players.size
        val dealerIndexInPlayers = players.indexOfFirst { it.id == dealerPlayer.id }.coerceAtLeast(0)

        val reorderedPlayers = mutableListOf<Player>()
        var currentIndex = dealerIndexInPlayers

        for (i in 0 until numPlayers) {
            val playerToAdd = players[currentIndex]
            reorderedPlayers.add(playerToAdd)
            currentIndex = (currentIndex + 1) % numPlayers
        }

        playersInDealOrder = reorderedPlayers
        currentDealingRoundPlayerIndex = 0
        Logger.d(tag, "Preparing dealing sequence. Opening Player: ${dealerPlayer.name}(${dealerPlayer.position}). " +
                "Actual dealing order: ${playersInDealOrder.map { "${it.name}(${it.position}, firstMove=${it.firstMove})" }}")
        startNextDealCardAnimation()
    }

    private fun startNextDealCardAnimation() {
        if (_pendingAnimationAction.value != null) return

        val currentPlayerState = gameState.value

        val totalExpectedCards = currentPlayerState.players.sumOf { player ->
            (if (player.firstMove) 13 else 12).toInt()
        }

        val totalCardsCurrentlyDealt = cardsDealtPerPlayer.values.sum()

        if (totalCardsCurrentlyDealt >= totalExpectedCards) {
            //Logger.d(tag, "All expected cards dealt. Dealing sequence complete.")
            setPendingAnimationAction(PendingAction.DealComplete)
            return
        }

        var nextPlayerToDeal: Player? = null
        var attempts = 0
        val maxAttempts = playersInDealOrder.size * 2

        while (nextPlayerToDeal == null && attempts < maxAttempts) {
            val candidatePlayer = playersInDealOrder[currentDealingRoundPlayerIndex % playersInDealOrder.size]
            val freshCandidate = currentPlayerState.players.firstOrNull { it.id == candidatePlayer.id } ?: candidatePlayer
            val cardsDealtToCandidate = cardsDealtPerPlayer[freshCandidate.position] ?: 0

            val maxCardsForCandidate = if (freshCandidate.firstMove) 13 else 12

            if (cardsDealtToCandidate < maxCardsForCandidate) {
                nextPlayerToDeal = freshCandidate
            }

            currentDealingRoundPlayerIndex++
            attempts++
        }

        if (nextPlayerToDeal == null) {
            Logger.e(tag, "Could not find a player to deal to, but not all cards dealt. Logic error or insufficient deck. Forcing DealComplete.")
            setPendingAnimationAction(PendingAction.DealComplete)
            return
        }

        var dealtCard: PlayingCard? = null
        gameState.update { currentGameState ->
            if (currentGameState.deck.cards.isNotEmpty()) {
                val (card, newDeck) = currentGameState.deck.draw()
                dealtCard = card
                currentGameState.copy(deck = newDeck)
            } else {
                currentGameState
            }
        }

        if (dealtCard != null) {
            val cardIndexForAnimation = cardsDealtPerPlayer[nextPlayerToDeal.position] ?: 0

            val targetOffset = when (nextPlayerToDeal.position) {
                PlayerPosition.LEFT -> uiState.value.localCoordinate.leftHandOffset
                PlayerPosition.RIGHT -> uiState.value.localCoordinate.rightHandOffset
                PlayerPosition.BOTTOM -> uiState.value.localCoordinate.bottomHandOffset
            }

            setPendingAnimationAction(
                PendingAction.CardDealt(
                    card = dealtCard,
                    startOffset = uiState.value.localCoordinate.deckOffset,
                    targetOffset = targetOffset,
                    playerPosition = nextPlayerToDeal.position,
                    cardIndexInHand = cardIndexForAnimation,
                    type = AnimationType.DEAL_CARD
                )
            )
            cardsDealtPerPlayer[nextPlayerToDeal.position] = (cardsDealtPerPlayer[nextPlayerToDeal.position] ?: 0) + 1
        } else {
            Logger.w(tag, "No more cards in the deck, but more cards expected. Deck exhausted prematurely. Forcing DealComplete.")
            setPendingAnimationAction(PendingAction.DealComplete)
        }
    }

    fun handleAnimationCompletionSignal() {
        externalScope.launch {
            _pendingAnimationAction.value?.let { pendingAction ->
                when (pendingAction) {
                    is PendingAction.CardDealt -> {
                        gameState.update { currentGameState ->
                            val updatedPlayers = currentGameState.players.map { player ->
                                if (player.position == pendingAction.playerPosition) {
                                    player.copy(cards = (player.cards + pendingAction.card).toMutableList())
                                } else {
                                    player
                                }
                            }
                            currentGameState.copy(
                                players = updatedPlayers
                            )
                        }

                        _pendingAnimationAction.value = null

                        externalScope.launch {
                            delay(60.milliseconds)
                            startNextDealCardAnimation()
                        }
                    }

                    is PendingAction.CardDrawn -> {
                        Logger.d(tag, "DrawCard animation finished.")
                        onDrawCompleteCallback.invoke(pendingAction.card, pendingAction.targetPlayer)
                        _pendingAnimationAction.value = null
                    }

                    is PendingAction.CardDiscarded -> {
                        Logger.d(tag, "CardDiscarded animation finished for ${pendingAction.card}")
                        onDiscardCompleteCallback.invoke(pendingAction.card, pendingAction.targetPlayer)
                        _pendingAnimationAction.value = null
                    }

                    is PendingAction.MeldAnimated -> {
                        Logger.d(tag, "MeldAnimated animation finished.")
                        onMeldCompleteCallback.invoke(pendingAction.meld, pendingAction.targetPlayer)
                        _pendingAnimationAction.value = null
                    }

                    is PendingAction.SingleChowAnimated -> {
                        Logger.d(tag, "SingleChow animation finished.")
                        val handCardsSourceOffsets = pendingAction.sourceCardOffsets
                        val targetMeldCenterOffset = pendingAction.targetMeldCenterOffset
                        val sortedChowMeldCards = (pendingAction.selectedCards).sortedBy { it.rank.sequentialValue }

                        setPendingAnimationAction(
                            PendingAction.MeldAnimated(
                                meld = Meld(sortedChowMeldCards),
                                sourceCardOffsets = handCardsSourceOffsets,
                                targetMeldCenterOffset = targetMeldCenterOffset,
                                playerPosition = pendingAction.playerPosition,
                                targetPlayer = pendingAction.targetPlayer
                            )
                        )

                        gameState.update { currentGameState ->
                            val updatedPlayers = currentGameState.players.map { player ->
                                if (player.position == pendingAction.playerPosition) {
                                    val updatedMelds = player.melds.map { meld ->
                                        if (meld.hasSapaw) {
                                            meld.copy(hasSapaw = false) // Return a new Meld object with hasSapaw set to false
                                        } else {
                                            meld // Otherwise, return the original meld object
                                        }
                                    }
                                    player.copy(melds = updatedMelds)
                                } else {
                                    player
                                }
                            }
                            currentGameState.copy(players = updatedPlayers)
                        }
                    }

                    is PendingAction.ChowCardAnimated -> {
                        Logger.d(tag, "ChowCard animation (discard to meld area) finished.")
                        val handCardsSourceOffsets = pendingAction.sourceCardOffsets
                        val targetMeldCenterOffset = pendingAction.targetMeldCenterOffset
                        val baseHandOffset = uiState.value.localCoordinate.bottomHandOffset

                        // The following calculation is now done with Dp values
                        val cardXOffset = baseHandOffset.x + CARD_WIDTH + (REGULAR_OVERLAP_FRACTION * pendingAction.targetPlayer.cards.size)
                        val cardYOffset = baseHandOffset.y + 80.dp

                        val discardSourceOffset = mutableMapOf<PlayingCard, DpOffset>()
                        val targetOffset = DpOffset(cardXOffset, cardYOffset)
                        discardSourceOffset[pendingAction.discardCard] = targetOffset
                        val newCardSourceOffsets = handCardsSourceOffsets + discardSourceOffset

                        val sortedChowMeldCards = (pendingAction.selectedCards + pendingAction.discardCard).sortedBy { it.rank.sequentialValue }

                        gameState.update { currentGameState ->
                            val updatedPlayers = currentGameState.players.map { player ->
                                if (player.position == pendingAction.playerPosition) {
                                    val newHand = player.cards.filter { card -> !sortedChowMeldCards.contains(card) }
                                    val updatedMelds = player.melds.map { meld ->
                                        if (meld.hasSapaw) {
                                            meld.copy(hasSapaw = false) // Return a new Meld object with hasSapaw set to false
                                        } else {
                                            meld // Otherwise, return the original meld object
                                        }
                                    }
                                    player.copy(
                                        cards = newHand,
                                        melds = updatedMelds
                                    )
                                } else {
                                    player
                                }
                            }
                            currentGameState.copy(players = updatedPlayers)
                        }

                        setPendingAnimationAction(
                            PendingAction.MeldAnimated(
                                meld = Meld(sortedChowMeldCards),
                                sourceCardOffsets = newCardSourceOffsets,
                                targetMeldCenterOffset = targetMeldCenterOffset,
                                playerPosition = pendingAction.playerPosition,
                                targetPlayer = pendingAction.targetPlayer
                            )
                        )
                    }

                    is PendingAction.SapawCardAnimated -> {
                        Logger.d(tag, "SapawCard animation finished.")
                        onSapawCompleteCallback.invoke(
                            pendingAction.sapawCard,
                            pendingAction.targetMeldIndex,
                            pendingAction.playerPosition,
                            pendingAction.targetPlayer
                        )
                        _pendingAnimationAction.value = null
                    }

                    is PendingAction.DealComplete -> {
                        Logger.d(tag, "DealComplete pending action received in handleAnimationCompletionSignal " +
                                "but already processed logic in setPendingAnimationAction.")
                        _pendingAnimationAction.value = null
                    }
                }
            } ?: run {
                Logger.w(tag, "handleAnimationCompletionSignal called but no pending animation action was found.")
            }
        }
    }
}