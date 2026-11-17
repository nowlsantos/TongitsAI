package com.nowlsantos.tongitsai.domain.ai

import com.nowlsantos.tongitsai.domain.model.GameState
import com.nowlsantos.tongitsai.domain.model.Meld
import com.nowlsantos.tongitsai.domain.model.Player
import com.nowlsantos.tongitsai.domain.model.PlayingCard
import java.util.concurrent.ConcurrentHashMap

object AiHandRepository {
    private val secretMeldsByPlayer = ConcurrentHashMap<String, List<Meld>>()

    fun getAiHand(player: Player): AiHand {
        val secrets = secretMeldsByPlayer[player.id] ?: emptyList()
        // The player's public hand does not include secret cards, so remainingCards is just the public hand.
        return AiHand(player, secrets, player.cards)
    }

    fun getSecretMelds(player: Player): List<Meld> {
        return getAiHand(player).secretMelds
    }

    fun addSecretMeld(playerId: String, meld: Meld) {
        secretMeldsByPlayer.compute(playerId) { _, current ->
            val list = current ?: emptyList()
            if (list.none { it.cards.toSet() == meld.cards.toSet() }) {
                list + meld
            } else {
                list
            }
        }
    }

    fun removeSecretMeld(playerId: String, meld: Meld) {
        secretMeldsByPlayer.computeIfPresent(playerId) { _, current ->
            current.filterNot { it.cards.toSet() == meld.cards.toSet() }
        }
    }

    fun updateAiHand(playerId: String, updatedAiHand: AiHand) {
        secretMeldsByPlayer[playerId] = updatedAiHand.secretMelds
    }

    fun clearForPlayer(playerId: String) {
        secretMeldsByPlayer.remove(playerId)
    }

    fun resetAll(players: List<Player>) {
        secretMeldsByPlayer.clear()
        players.forEach { secretMeldsByPlayer[it.id] = emptyList() }
    }

    fun syncWithGameState(gameState: GameState) {
        for (player in gameState.players) {
            val visibleMeldSets = player.melds.map { it.cards.toSet() }.toSet()
            secretMeldsByPlayer.computeIfPresent(player.id) { _, current ->
                current.filterNot { secret -> visibleMeldSets.contains(secret.cards.toSet()) }
            }
        }
    }

    /**
     * New function to remove secret melds containing cards used in a public move (CHOW, MELD, SAPAW).
     */
    fun updateSecretsAfterPublicMove(playerId: String, publicCards: Set<PlayingCard>) {
        if (publicCards.isEmpty()) return
        secretMeldsByPlayer.computeIfPresent(playerId) { _, current ->
            current.filterNot { secretMeld ->
                secretMeld.cards.any { it in publicCards }
            }
        }
    }

    fun debugSecrets(playerId: String): List<Meld> =
        secretMeldsByPlayer[playerId] ?: emptyList()
}