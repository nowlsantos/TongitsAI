package com.nowlsantos.tongitsai.validator

import com.nowlsantos.tongitsai.domain.model.GameState
import com.nowlsantos.tongitsai.domain.model.GameUiState
import com.nowlsantos.tongitsai.domain.model.Meld
import com.nowlsantos.tongitsai.domain.model.Player
import com.nowlsantos.tongitsai.domain.model.PlayingCard
import com.nowlsantos.tongitsai.domain.model.enums.PlayerType

sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()
    data class SuccessWithData<T>(val data: T) : ValidationResult()
}

fun ValidationResult.isError(): Boolean = this is ValidationResult.Error
fun ValidationResult.isSuccess(): Boolean = this is ValidationResult.Success

class GameValidator {
    fun validateCall(player: Player): ValidationResult {
        // Should have at least one meld
        if (player.melds.isEmpty()) {
            return ValidationResult.Error("Cannot make a call without a meld.")
        }

        // Not allowed to make a call when meld has sapaw
        player.melds.forEach { it ->
            if (it.hasSapaw) {
                return ValidationResult.Error("Cannot make a call. Your meld has sapaw.")
            }
        }
        return ValidationResult.Success
    }

    fun validateMeld(uiState: GameUiState, meld: Meld): ValidationResult {
        val humanPlayer = uiState.players.firstOrNull { it.type == PlayerType.HUMAN }
        humanPlayer?.let {
            // A meld must contain at least 3 cards.
            if (meld.cards.size < 3) {
                return ValidationResult.Error("A meld must contain at least 3 cards.")
            }

            // Check if the cards form a valid Tongits meld (set of same rank or run of same suit).
            if (!meld.isValid()) {
                return ValidationResult.Error("The selected cards do not form a valid meld.")
            }
        }

        return ValidationResult.Success
    }

    fun validateChow(uiState: GameUiState, selectedCards: List<PlayingCard>, chowCard: PlayingCard): ValidationResult {
        if (uiState.discardPile.cards.isEmpty()) {
            return ValidationResult.Error("The discard pile is empty. Cannot chow a card.")
        }

        if (selectedCards.isEmpty()) {
            return ValidationResult.Error("You need to select cards to form a chow with the discarded card.")
        }

        // Check if the combined cards form a valid meld
        val potentialMeld = Meld((selectedCards + chowCard).toList())
        val meldValidation = validateMeld(uiState, potentialMeld)
        if (meldValidation.isError()) {
            //return meldValidation
            return ValidationResult.Error("The selected cards do not form a valid chow.")
        }

        // Additional chow-specific validation
        // For chow, the meld must be a run (straight) of the same suit
        val meld = Meld(potentialMeld.cards)
        if (!meld.isValid()) {
            return ValidationResult.Error("Chow can only be used to form straights (runs) of the same suit.")
        }
        return ValidationResult.Success
    }

    fun validateSapaw(
        uiState: GameUiState,
        sapawCard: PlayingCard?,
        targetMeld: Meld
    ): ValidationResult {
        // No selected target meld
        if (uiState.selectedSapawTarget == null) {
            return ValidationResult.Error("No selected target meld.")
        }

        if (sapawCard == null) {
            return ValidationResult.Error("No selected card to Sapaw.")
        }

        // Check if the card can actually be added to the target meld
        if (!targetMeld.canAdd(sapawCard)) {
            return ValidationResult.Error("The card '$sapawCard' is not a valid sapaw card for this meld.")
        }

        return ValidationResult.Success
    }

    fun validateDiscardCard(
        gameState: GameState,
        discarded: PlayingCard,
        selectedCards: List<PlayingCard>
    ): ValidationResult {
        if (selectedCards.isEmpty()) {
            return ValidationResult.Error("No selected card to discard.")
        }

        if (selectedCards.size != 1) {
            return ValidationResult.Error("You can only discard one card.")
        }

        if (isSapawToExistingMelds(gameState, discarded)) {
            return ValidationResult.Error("Discarding a valid sapaw card is prohibited.")
        }

        return ValidationResult.Success
    }

    private fun isSapawToExistingMelds(gameState: GameState, cardToDiscard: PlayingCard): Boolean {
        // Flatten the list of all players' melds into a single list of melds.
        val allMeldsOnTable = gameState.players.flatMap { it.melds }

        // Iterate through all melds and check if the card can be added to any of them.
        for (meld in allMeldsOnTable) {
            if (meld.canAdd(cardToDiscard)) {
                // Found a valid meld to lay off on, so the card is a valid sapaw.
                return true
            }
        }

        // If no valid meld was found after checking all of them, it's not a sapaw.
        return false
    }

    fun validateSwap(player: Player): ValidationResult {
        if (player.selectedCards.isEmpty()) {
            return ValidationResult.Error("No selected card to swap.")
        }

        if (player.selectedCards.size != 2) {
            return ValidationResult.Error("You need to select two cards to swap.")
        }

        return ValidationResult.Success
    }
}