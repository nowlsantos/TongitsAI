package com.nowlsantos.tongitsai.util

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.nowlsantos.tongitsai.domain.model.LocalCoordinate
import com.nowlsantos.tongitsai.domain.model.Meld
import com.nowlsantos.tongitsai.domain.model.PlayingCard
import com.nowlsantos.tongitsai.domain.model.enums.PlayerPosition
import com.nowlsantos.tongitsai.presentation.HandOrientation
import com.nowlsantos.tongitsai.presentation.PLAYER_HAND_LAYOUT_CONFIGS
import com.nowlsantos.tongitsai.util.GameConstants
import com.nowlsantos.tongitsai.util.GameConstants.CARD_HEIGHT
import com.nowlsantos.tongitsai.util.GameConstants.CARD_WIDTH
import com.nowlsantos.tongitsai.util.GameConstants.SCREEN_WIDTH

/**
 * Placeholder for your calculateTargetOffset function.
 * This function needs to return the screen coordinates where a card should end up
 * for a specific player and its position in their hand.
 * You MUST implement the actual logic for this based on your UI layout.
 */
fun calculateTargetOffset(
    playerPosition: PlayerPosition,
    cardIndexInHand: Int, // The index of the card within this player's hand
    localCoordinate: LocalCoordinate // Contains layout info for player areas
): DpOffset {
    val config = PLAYER_HAND_LAYOUT_CONFIGS[playerPosition]
        ?: throw IllegalStateException("No hand layout config found for player position: $playerPosition")

    // Get the base offset for the player's hand area (top-left of their hand Box)
    // localCoordinate properties are now DpOffset
    val baseOffset = when (playerPosition) {
        PlayerPosition.BOTTOM -> localCoordinate.bottomHandOffset
        PlayerPosition.RIGHT -> localCoordinate.rightHandOffset
        PlayerPosition.LEFT -> localCoordinate.leftHandOffset
    }

    // Since initialOffsetDp and cardOverlapDp are already Dp, no conversion is needed
    val initialOffset = config.initialOffsetDp
    val cardOverlap = config.cardOverlapDp

    return when (config.orientation) {
        HandOrientation.HORIZONTAL -> {
            val offsetX = baseOffset.x + initialOffset + (cardOverlap * cardIndexInHand.toFloat())
            val offsetY = baseOffset.y
            DpOffset(offsetX, offsetY)
        }
        HandOrientation.VERTICAL -> {
            //val offsetX = baseOffset.x
            //val offsetY = baseOffset.y + initialOffset + (cardOverlap * cardIndexInHand.toFloat())
            DpOffset(baseOffset.x, baseOffset.y)
        }
    }
}

/**
 * Calculates the width of a meld based on the number of cards.
 * This mirrors the logic in the PlayerMeld composable.
 * @param meld The meld to calculate the width for.
 * @return The calculated width as a Dp value.
 */
private fun getMeldWidth(meld: Meld): Dp {
    val overlap = GameConstants.getDynamicOverlap(meld.cards.size)
    return CARD_WIDTH + (overlap * (meld.cards.size - 1))
}

/**
 * Calculates the target offset for a sapaw card based on the dynamic `Arrangement.SpaceEvenly` layout.
 * This function now requires the total width of the container to correctly distribute the melds.
 * @param baseAreaOffset The base offset of the player's meld area (the top-left of the Row).
 * @param allMeldsOfTargetPlayer A list of all melds of the target player.
 * @param meldIndex The index of the meld being targeted.
 * @param targetMeld The specific meld being targeted for the sapaw.
 * @return The Offset for the center of the card to be placed.
 */
fun calculateSapawDropOffset(
    baseAreaOffset: DpOffset,
    allMeldsOfTargetPlayer: List<Meld>,
    meldIndex: Int,
    targetMeld: Meld
): DpOffset {
    // Step 1: Calculate the total width of all melds for the target player
    val totalMeldsWidth = allMeldsOfTargetPlayer.sumOf { getMeldWidth(it).value.toDouble() }.dp

    // Step 2: Calculate the total available space to be distributed
    val totalSpacing = SCREEN_WIDTH / 2 - totalMeldsWidth

    // Step 3: Calculate the width of a single space segment
    val numMelds = allMeldsOfTargetPlayer.size
    val individualSpacing = if (numMelds > 0) totalSpacing / (numMelds + 1) else 0.dp

    // Step 4: Calculate the x-position of the target meld's start
    var xOffsetForTargetMeld = baseAreaOffset.x + individualSpacing
    for (i in 0 until meldIndex) {
        xOffsetForTargetMeld += getMeldWidth(allMeldsOfTargetPlayer[i]) + individualSpacing
    }

    // Step 5: Calculate the x-position of the new card within the meld
    val targetMeldForOffset = allMeldsOfTargetPlayer[meldIndex]
    val overlap = GameConstants.getDynamicOverlap(targetMeldForOffset.cards.size)
    val cardPositionInMeld = targetMeldForOffset.cards.size
    val cardOffsetInMeld = overlap * cardPositionInMeld

    val finalX = xOffsetForTargetMeld + cardOffsetInMeld + (CARD_WIDTH / 2)
    val finalY = baseAreaOffset.y + (CARD_HEIGHT / 2)

    return DpOffset(x = finalX, y = finalY)
}

fun calculateAISapawDropOffset(
    playerMelds: List<Meld>,
    targetMeldIndex: Int,
    sapawCard: PlayingCard,
): DpOffset {
    // ✅ Fixed: Convert each Dp to Float for summing
    //val totalMeldsWidth = playerMelds.sumOf { getMeldWidth(it).value }.dp
    val totalMeldsWidth = playerMelds.sumOf { getMeldWidth(it).value.toDouble() }.dp


    // ✅ Avoid negative spacing
    val totalSpacing = SCREEN_WIDTH / 2 - totalMeldsWidth

    val spacingBetweenMelds = if (playerMelds.size > 1) {
        totalSpacing / (playerMelds.size + 1)
    } else {
        totalSpacing / 2
    }

    var xOffset = spacingBetweenMelds
    for (i in 0 until targetMeldIndex) {
        xOffset += getMeldWidth(playerMelds[i]) + spacingBetweenMelds
    }

    val targetMeld = playerMelds[targetMeldIndex]

    val insertIndex = targetMeld.cards.indexOfFirst { it.rank.ordinal > sapawCard.rank.ordinal }
        .takeIf { it != -1 } ?: targetMeld.cards.size

    val overlap = GameConstants.getDynamicOverlap(targetMeld.cards.size)
    val cardOffsetInsideMeld = overlap * insertIndex

    xOffset += cardOffsetInsideMeld

    val yOffset = CARD_HEIGHT / 2

    val sapawOffset: DpOffset = DpOffset(xOffset, yOffset)
    return sapawOffset
}
