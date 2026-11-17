package com.nowlsantos.tongitsai.domain.model

import androidx.compose.runtime.Immutable
import com.nowlsantos.tongitsai.domain.model.enums.ChallengerType
import com.nowlsantos.tongitsai.domain.model.enums.PlayerPosition
import com.nowlsantos.tongitsai.domain.model.enums.PlayerType

@Immutable
data class UiPlayer(
    val id: String,
    val name: String,
    val type: PlayerType,
    val position: PlayerPosition,
    val balance: Int,
    val score: Int,
    val isHitter: Boolean,
    val isFaceUp: Boolean,
    val displayCards: List<PlayingCard>,
    val selectedCards: Set<PlayingCard> = emptySet(),
    val melds: List<Meld> = emptyList(),
    val challengerType: ChallengerType = ChallengerType.NONE
)
