package com.nowlsantos.tongitsai.domain.model

import androidx.compose.runtime.Immutable
import com.nowlsantos.tongitsai.domain.model.enums.ChallengerType
import com.nowlsantos.tongitsai.domain.model.enums.PlayerPosition
import com.nowlsantos.tongitsai.domain.model.enums.PlayerType
import com.nowlsantos.tongitsai.util.GameConstants

@Immutable
data class Player(
    val id: String,
    val name: String,
    val type: PlayerType,
    val cards: List<PlayingCard>,
    val position: PlayerPosition,
    val selectedCards: Set<PlayingCard> = emptySet(),
    val melds: List<Meld> = emptyList(),
    val score: Int = 0,
    val balance: Int = GameConstants.BALANCE,
    val hitCount: Int = 0,
    val isHitter: Boolean = false,
    val challengerType: ChallengerType = ChallengerType.NONE,
    val firstMove: Boolean = false
) {
    // inside Player.kt
    companion object {
        fun initialize(
            humanName: String = "Luke",
            aiNames: List<String> = listOf("Lea", "Hans")
        ): List<Player> = listOf(
            Player(
                id = GameConstants.ID_HUMAN,
                name = humanName,
                type = PlayerType.HUMAN,
                cards = emptyList(),
                position = PlayerPosition.BOTTOM,
                firstMove = true
            ),
            Player(
                id = GameConstants.ID_AI_1,
                name = aiNames[0],
                type = PlayerType.AI,
                cards = emptyList(),
                position = PlayerPosition.RIGHT
            ),
            Player(
                id = GameConstants.ID_AI_2,
                name = aiNames[1],
                type = PlayerType.AI,
                cards = emptyList(),
                position = PlayerPosition.LEFT
            )
        )
    }
}
