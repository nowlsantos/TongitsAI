package com.nowlsantos.tongitsai.presentation

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nowlsantos.tongitsai.domain.model.enums.PlayerPosition

// Defines how a player's hand is laid out
data class HandLayoutConfig(
    val initialOffsetDp: Dp, // Initial padding/offset from the hand area's origin
    val cardOverlapDp: Dp,    // How much each subsequent card overlaps the previous one
    val orientation: HandOrientation // Whether cards spread horizontally or vertically
)

enum class HandOrientation { HORIZONTAL, VERTICAL }

// Central map to store the layout config for each player position
val PLAYER_HAND_LAYOUT_CONFIGS = mutableMapOf<PlayerPosition, HandLayoutConfig>()

// Call this function once at your application's start (e.g., in your Application class's onCreate)
fun initializePlayerHandLayoutConfigs() {
    // Luke (PlayerPosition.BOTTOM)
    // From Luke.kt: No initial padding, overlapOffset = 48.dp
    PLAYER_HAND_LAYOUT_CONFIGS[PlayerPosition.BOTTOM] = HandLayoutConfig(
        initialOffsetDp = 25.dp,
        //cardOverlapDp = 48.dp, // This is a fixed value from Luke.kt
        cardOverlapDp = 46.dp, // This is a fixed value from Luke.kt
        orientation = HandOrientation.HORIZONTAL
    )

    // Anakin (PlayerPosition.RIGHT)
    // From Anakin.kt: padding(start = 25.dp), overlapOffset = 48.dp
    PLAYER_HAND_LAYOUT_CONFIGS[PlayerPosition.RIGHT] = HandLayoutConfig(
        initialOffsetDp = 25.dp,
        cardOverlapDp = 46.dp, // This is a fixed value from Anakin.kt
        orientation = HandOrientation.HORIZONTAL
    )

    // Obiwan (PlayerPosition.LEFT)
    // From Obiwan.kt: padding(start = 25.dp), overlapOffset = 48.dp
    PLAYER_HAND_LAYOUT_CONFIGS[PlayerPosition.LEFT] = HandLayoutConfig(
        initialOffsetDp = 25.dp,
        cardOverlapDp = 46.dp, // This is a fixed value from Obiwan.kt
        orientation = HandOrientation.HORIZONTAL
    )
}