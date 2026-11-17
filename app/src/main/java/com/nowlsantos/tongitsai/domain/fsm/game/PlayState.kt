package com.nowlsantos.tongitsai.domain.fsm.game

import androidx.compose.ui.unit.DpOffset
import com.nowlsantos.tongitsai.domain.fsm.animation.GameAnimationState
import com.nowlsantos.tongitsai.domain.fsm.animation.PendingAction
import com.nowlsantos.tongitsai.domain.model.GameUiState
import com.nowlsantos.tongitsai.domain.model.Meld
import com.nowlsantos.tongitsai.domain.model.Player
import com.nowlsantos.tongitsai.domain.model.PlayingCard
import com.nowlsantos.tongitsai.domain.model.enums.AnimationType
import com.nowlsantos.tongitsai.domain.model.enums.MoveType
import com.nowlsantos.tongitsai.domain.model.enums.PlayerPosition
import com.nowlsantos.tongitsai.domain.model.enums.PlayerType
import com.nowlsantos.tongitsai.domain.model.getHandOffset
import com.nowlsantos.tongitsai.presentation.GameInput
import com.nowlsantos.tongitsai.presentation.UiEffect
import com.nowlsantos.tongitsai.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class PlayState(private val gameAnimationState: GameAnimationState) : GamePhaseState {
    private val tag = "PlayState"

    override fun onEnter(stateMachine: GameStateMachine) {
        Logger.d(tag, "Entering PlayState. Game is now in playing phase.")

        val currentPlayer = stateMachine.gameState.value.currentPlayer
        Logger.d(tag, "CurrentPlayer:: ${currentPlayer.name} | isHitter:: ${currentPlayer.isHitter} | FirstMove:: ${currentPlayer.firstMove}")

        // Signaling the players state machine for their first move
        stateMachine.broadcastDecision(currentPlayer, MoveType.NEXT_TURN)
    }

    override suspend fun handleInput(stateMachine: GameStateMachine, input: GameInput) {
        when (input) {
            is GameInput.DiscardCard -> handleDiscardCard(
                stateMachine,
                input.card,
                input.startOffset,
                input.targetOffset
            )
            is GameInput.MeldCards -> handleMeldCards(
                stateMachine,
                input.cards,
                input.sourceCardOffsets,
                input.targetMeldCenterOffset
            )
            is GameInput.SingleChowCard -> handleSingleChowCard(
                stateMachine,
                input.chowCard,
                input.disCardPileOffset,
                input.targetOffset,
                input.selectedCards,
                input.sourceCardOffsets,
                input.targetMeldCenterOffset
            )
            is GameInput.ChowCard -> handleChowCard(
                stateMachine,
                input.chowCard,
                input.disCardPileOffset,
                input.selectedCards,
                input.sourceCardOffsets
            )
            is GameInput.SapawCard -> handleSapawCard(
                stateMachine,
                input.sapawCard,
                input.startOffset,
                input.sapawTarget
            )
            is GameInput.RoundReveal -> { handleCardReveal(stateMachine, input.playerIds) }
            is GameInput.MakeCall -> { handlePlayerCall(stateMachine, input.caller) }
            is GameInput.SwapCards -> { handleSwapCards(stateMachine, input.cards) }
            GameInput.DrawCard -> handleDrawCard(stateMachine)
            GameInput.SortByRank -> handleSortByRank(stateMachine)
            GameInput.SortBySuit -> handleSortBySuit(stateMachine)
            GameInput.CardAnimationFinished -> { handleCardAnimationFinished() }
            GameInput.RoundEnd -> { handleRoundEnd(stateMachine) }
            else -> { Logger.w(tag, "Input $input not handled in PlayState.") }
        }
    }

    private fun updateUiStateSapawTarget(context: GameStateMachine) {
        val uiState = context.uiState as MutableStateFlow<GameUiState>
        uiState.update {
            it.copy(selectedSapawTarget = null)
        }
    }

    private fun handlePlayerCall(context: GameStateMachine, caller: Player) {
        Logger.d(tag, "Received MakeCall input")

        // Move to ChallengeState
        context.transitionTo(GamePhase.CALL_CHALLENGE)
    }

    private fun handleRoundEnd(context: GameStateMachine) {
        Logger.d(tag, "Received RoundEnd input.")
        context.transitionTo(GamePhase.ROUND_END)
    }

    private fun handleCardReveal(context: GameStateMachine, playerIDs: List<String>) {
        Logger.d(tag, "Received Reveal input. | $playerIDs")
        context.transitionTo(GamePhase.REVEAL)
    }

    private fun handleCardAnimationFinished() {
        gameAnimationState.handleAnimationCompletionSignal()
    }

    private fun handleDiscardCard(
        context: GameStateMachine,
        card: PlayingCard,
        startOffset: DpOffset,
        targetOffset: DpOffset
    ) {
        Logger.d(tag, "Attempting to handle DiscardCard: $card | StartOffset:: $startOffset | TargetOffset:: $targetOffset")
        val gameState = context.gameState.value
        val currentPlayer = gameState.currentPlayer

        currentPlayer.let {
            val newPlayerCards = it.cards.toMutableList().apply { remove(card) }
            val updatedPlayer = it.copy(
                cards = newPlayerCards,
                selectedCards = emptySet(),
                firstMove = false
            )

            val updatedPlayers = gameState.players.map { player ->
                if (player.name == updatedPlayer.name) updatedPlayer else player
            }

            context.updateState { currentState ->
                currentState.copy(players = updatedPlayers)
            }

            context.emitUiEffect(UiEffect.ShowMessage("${currentPlayer.name} discarded $card"))

            gameAnimationState.setPendingAnimationAction(
                PendingAction.CardDiscarded(
                    card = card,
                    startOffset = startOffset,
                    targetOffset = targetOffset,
                    targetPlayer = it
                )
            )
        }
    }

    private fun handleDrawCard(context: GameStateMachine) {
        val gameState = context.gameState.value
        val deck = gameState.deck

        if (deck.cards.isEmpty()) {
            Logger.w(tag, "Attempted to draw card from empty deck in PlayState.")
            context.emitUiEffect(UiEffect.ShowError("Deck is empty. Cannot draw card."))
            return
        }

        val currentPlayer = gameState.players.firstOrNull { it.name == gameState.currentPlayer.name }
        currentPlayer?.let {
            if (it.cards.size >= 13) {
                Logger.w(tag, "Player ${it.name} already has ${it.cards.size} cards. Cannot draw card twice in a single turn.")
                return
            }

            // Update the UiState SapawTarget
            updateUiStateSapawTarget(context)

            val (drawnCard, updatedDeck ) = deck.draw()
            context.updateState { currentGameState ->
                currentGameState.copy(deck = updatedDeck)
            }

            val gameUiState = context.uiState.value
            val baseHandOffset = gameUiState.localCoordinate.getHandOffset(it.position)

            context.emitUiEffect(UiEffect.ShowMessage("${currentPlayer.name} draws a card."))

            gameAnimationState.setPendingAnimationAction(
                PendingAction.CardDrawn(
                    card = drawnCard,
                    playerPosition = it.position,
                    cardIndexInHand = it.cards.size,
                    startOffset = context.uiState.value.localCoordinate.deckOffset,
                    targetOffset = baseHandOffset,
                    type = AnimationType.GET_CARD,
                    targetPlayer = it
                )
            )
        }
    }

    private fun handleMeldCards(
        context: GameStateMachine,
        selectedCards: List<PlayingCard>,
        sourceCardOffsets: Map<PlayingCard, DpOffset>,
        targetMeldCenterOffset: DpOffset,
    ) {
        val gameState = context.gameState.value
        val currentPlayer = gameState.players.firstOrNull { it.name == gameState.currentPlayer.name }
        val sortedSelectedCards = selectedCards.sortedWith(compareBy({ it.suit.ordinal }, { it.rank.sequentialValue }))
        val potentialMeld: Meld
        try {
            potentialMeld = Meld(cards = sortedSelectedCards, owner = currentPlayer?.id)
            Logger.d(tag, "PotentialMeldOwner:: ${potentialMeld.owner}")
        } catch (e: IllegalArgumentException) {
            Logger.e(tag, "Error creating potential meld: ${e.message}")
            context.emitUiEffect(UiEffect.ShowError("Invalid cards for a meld: ${e.message}"))
            return
        }

        currentPlayer?.let {
            val meldCenterAreaOffset: DpOffset? = if (it.type == PlayerType.HUMAN) {
                context.uiState.value.meldAreaCoordinates[it.position]
            } else {
                targetMeldCenterOffset
            }

            context.updateState { currentGameState ->
                val updatePlayers = currentGameState.players.map { player ->
                    if (player.position == it.position) {
                        val newHand = player.cards.filter { card ->
                            !potentialMeld.cards.contains(card)
                        }
                        player.copy(
                            cards = newHand
                        )
                    } else {
                        player
                    }
                }
                currentGameState.copy(players = updatePlayers)
            }

            gameAnimationState.setPendingAnimationAction(
                PendingAction.MeldAnimated(
                    meld = Meld(sortedSelectedCards, owner = currentPlayer.id),
                    sourceCardOffsets = sourceCardOffsets,
                    targetMeldCenterOffset = meldCenterAreaOffset!!,
                    playerPosition = it.position,
                    targetPlayer = it
                )
            )
        }
    }

    private fun handleSingleChowCard(
        context: GameStateMachine,
        chowCard: PlayingCard,
        discardPileOffset: DpOffset,
        targetOffset: DpOffset,
        selectedCards: List<PlayingCard>,
        sourceCardOffsets: Map<PlayingCard, DpOffset>,
        targetMeldCenterOffset: DpOffset,
    ) {
        val gameState = context.gameState.value
        val currentPlayer = gameState.players.firstOrNull { it.name == gameState.currentPlayer.name }

        currentPlayer?.let { it ->
            // Update the UiState SapawTarget
            updateUiStateSapawTarget(context)

            context.updateState { currentGameState ->
                val updatedPlayers = currentGameState.players.map { p ->
                    if (p.id == currentPlayer.id) {
                        val updatedMelds = p.melds.map { if (it.hasSapaw) it.copy(hasSapaw = false) else it }
                        p.copy(melds = updatedMelds)
                    } else {
                        p
                    }
                }
                currentGameState.copy(
                    discardPile = currentGameState.discardPile.removeTop(),
                    players = updatedPlayers
                )
            }
            context.emitUiEffect(UiEffect.ShowMessage("${currentPlayer.name} chows $chowCard"))

            val combinedSourceOffsets = sourceCardOffsets + mapOf(chowCard to discardPileOffset)
            val sortedChowMeldCards = selectedCards.sortedBy { it.rank.sequentialValue }

            gameAnimationState.setPendingAnimationAction(
                PendingAction.MeldAnimated(
                    meld = Meld(sortedChowMeldCards),
                    sourceCardOffsets = combinedSourceOffsets,
                    targetMeldCenterOffset = targetMeldCenterOffset,
                    playerPosition = it.position,
                    targetPlayer = it
                )
            )
        }
    }

    private fun handleChowCard(
        context: GameStateMachine,
        chowCard: PlayingCard,
        discardPileOffset: DpOffset,
        selectedCards: List<PlayingCard>,
        sourceCardOffsets: Map<PlayingCard, DpOffset>,
    ) {
        val gameState = context.gameState.value
        val currentPlayer = gameState.players.firstOrNull { it.name == gameState.currentPlayer.name }
        currentPlayer?.let { it ->
            // Update the UiState SapawTarget
            updateUiStateSapawTarget(context)

            context.updateState { currentGameState ->
                val updatedPlayers = currentGameState.players.map { p ->
                    if (p.id == it.id) {
                        val updatedMelds = p.melds.map { m -> if (m.hasSapaw) m.copy(hasSapaw = false) else m }
                        p.copy(melds = updatedMelds)
                    } else {
                        p
                    }
                }
                currentGameState.copy(
                    discardPile = currentGameState.discardPile.removeTop(),
                    players = updatedPlayers
                )
            }

            context.emitUiEffect(UiEffect.ShowMessage("${it.name} chows $chowCard"))

            val meldCenterAreaOffset = context.uiState.value.meldAreaCoordinates[it.position]
            meldCenterAreaOffset?.let {
                val combinedSourceOffsets = sourceCardOffsets + mapOf(chowCard to discardPileOffset)
                val sortedChowMeldCards = (selectedCards + chowCard).sortedBy { it.rank.sequentialValue }

                gameAnimationState.setPendingAnimationAction(
                    PendingAction.MeldAnimated(
                        meld = Meld(sortedChowMeldCards),
                        sourceCardOffsets = combinedSourceOffsets,
                        targetMeldCenterOffset = meldCenterAreaOffset,
                        playerPosition = currentPlayer.position,
                        targetPlayer = currentPlayer
                    )
                )
            }
        }
    }

    private fun handleSapawCard(
        context: GameStateMachine,
        sapawCard: PlayingCard,
        startOffset: DpOffset,
        sapawTarget: Triple<DpOffset, PlayerPosition, Int>
    ) {
        val gameState = context.gameState.value
        val currentPlayer = gameState.players.firstOrNull { it.id == gameState.currentPlayer.id }
        val targetPlayer = gameState.players.firstOrNull { it.position == sapawTarget.second }
        if (targetPlayer == null) {
            Logger.e(tag, "handleSapawCard: Target player for Sapaw not found at position ${sapawTarget.second}.")
            context.emitUiEffect(UiEffect.ShowError("Error: Target player not found for Sapaw."))
            return
        }

        val targetMeld = targetPlayer.melds.getOrNull(sapawTarget.third)
        if (targetMeld == null) {
            context.emitUiEffect(UiEffect.ShowError("Invalid target meld index for Sapaw."))
            Logger.e(tag, "handleSapawCard: Invalid target meld index ${sapawTarget.third} for player ${targetPlayer.name}.")
            return
        }

        currentPlayer?.let {
            val currentPlayerCards = it.cards.toMutableList().apply { remove(sapawCard) }
            val updatedPlayer = it.copy(
                cards = currentPlayerCards,
                selectedCards = emptySet()
            )

            val updatedPlayersList = gameState.players.map { player ->
                if (player.name == updatedPlayer.name) updatedPlayer else player
            }

            context.updateState { currentState ->
                currentState.copy(
                    players = updatedPlayersList,
                    lastDiscardedCard = sapawCard
                )
            }
            val targetMeldOwner = gameState.players.firstOrNull { currentPlayer.id == targetMeld.owner }
            context.emitUiEffect(UiEffect.ShowMessage("${currentPlayer.name} sapaws a card to ${targetMeldOwner?.name}"))
            Logger.d(tag, "AFTER CARD REMOVAL (local variable): newPlayerCards size is ${currentPlayerCards.size}.")

            gameAnimationState.setPendingAnimationAction(
                PendingAction.SapawCardAnimated(
                    sapawCard = sapawCard,
                    startOffset = startOffset,
                    playerPosition = sapawTarget.second,
                    targetMeldIndex = sapawTarget.third,
                    targetMeldDropOffset = sapawTarget.first,
                    targetPlayer = it
                )
            )
        }
    }

    private fun handleSwapCards(
        context: GameStateMachine,
        cardsToSwap: Pair<PlayingCard, PlayingCard>
    ) {
        val gameState = context.gameState.value
        Logger.d(tag, "cardsToSwap: $cardsToSwap | Player: ${gameState.currentPlayer.name}")

        context.updateState { currentGameState ->
            val updatedPlayers = currentGameState.players.map { player ->
                // The human player is now the only one whose state is updated.
                // This is correct because the onEvent function handles the check.
                val firstCard = cardsToSwap.first
                val secondCard = cardsToSwap.second

                val firstIndex = player.cards.indexOf(firstCard)
                val secondIndex = player.cards.indexOf(secondCard)

                val newCards = player.cards.toMutableList()
                if (firstIndex != -1 && secondIndex != -1) {
                    newCards[firstIndex] = secondCard
                    newCards[secondIndex] = firstCard
                }

                player.copy(cards = newCards, selectedCards = emptySet())
            }
            currentGameState.copy(players = updatedPlayers)
        }
    }

    private fun handleSortByRank(context: GameStateMachine) {
        val gameState = context.gameState.value
        val currentPlayer = gameState.players.firstOrNull { it.name == gameState.currentPlayer.name }
        currentPlayer?.let { it ->
            val updatedPlayers = gameState.players.map { player ->
                if (player.name == it.name) {
                    player.copy(
                        cards = player.cards.sortedWith(compareBy({ it.rank.sequentialValue }, { it.suit.ordinal })) as MutableList<PlayingCard>
                    )
                } else {
                    player
                }
            }

            context.updateState {
                it.copy(
                    players = updatedPlayers
                )
            }
            Logger.d(tag, "Player hand sorted by rank.")
        }
    }

    private fun handleSortBySuit(context: GameStateMachine) {
        val gameState = context.gameState.value
        val currentPlayer = gameState.players.firstOrNull { it.name == gameState.currentPlayer.name }
        currentPlayer?.let { it ->
            val updatedPlayers = gameState.players.map { player ->
                if (player.name == it.name) {
                    player.copy(
                        cards = player.cards.sortedWith(compareBy({ it.suit.ordinal }, { it.rank.sequentialValue })) as MutableList<PlayingCard>
                    )
                } else {
                    player
                }
            }
            context.updateState {
                it.copy(
                    players = updatedPlayers
                )
            }
            Logger.d(tag, "Player hand sorted by suit.")
        }
    }

    override fun onExit(stateMachine: GameStateMachine) {
        Logger.d(tag, "Exiting PlayState.")
    }
}