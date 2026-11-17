package com.nowlsantos.tongitsai.domain.model

import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.nowlsantos.tongitsai.domain.model.enums.PlayerPosition

data class LocalCoordinate(
    // Player hand area positions, keyed by their fixed UI position
    val bottomHandOffset: DpOffset = DpOffset(0.dp, 0.dp),
    val leftHandOffset: DpOffset = DpOffset(0.dp, 0.dp),
    val rightHandOffset: DpOffset = DpOffset(0.dp, 0.dp),

    val discardPileOffset: DpOffset = DpOffset(0.dp, 0.dp),
    val deckOffset: DpOffset = DpOffset(0.dp, 0.dp),
    val cardOffset: DpOffset = DpOffset(0.dp, 0.dp),

    val meldAreaOffsets: Map<PlayerPosition, DpOffset> = emptyMap(),
    val allPlayersCardOffsets: Map<PlayerPosition, Map<PlayingCard, DpOffset>> = emptyMap(),
)

/**
 * Extension function to easily get the hand offset based on the player's position.
 */
fun LocalCoordinate.getHandOffset(playerPosition: PlayerPosition): DpOffset {
    return when (playerPosition) {
        PlayerPosition.BOTTOM -> bottomHandOffset
        PlayerPosition.LEFT -> leftHandOffset
        PlayerPosition.RIGHT -> rightHandOffset
    }
}