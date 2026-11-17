package com.nowlsantos.tongitsai.util

import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

object GameConstants {
    val OVERLAP_FRACTION = 32.dp
    val REGULAR_OVERLAP_FRACTION = 48.dp
    val CARD_WIDTH = 85.dp
    val CARD_HEIGHT = 118.dp

    fun getDynamicOverlap(meldSize: Int): androidx.compose.ui.unit.Dp {
        return when {
            meldSize == 4 -> 30.dp
            meldSize == 5 -> 28.dp
            meldSize >= 6 -> 26.dp
            else -> 32.dp
        }
    }

    val SCREEN_WIDTH = 1333.dp
    val SCREEN_HEIGHT = 800.dp
    val ADD_OFFSET = DpOffset(x = CARD_WIDTH, y = 26.dp)

    const val BALANCE = 500 //300
    const val CENTER_FEE = 10 //10

    // Payment points
    const val NORMAL_DEDUCTION = 20 //10
    const val CHALLENGE_DEDUCTION = 50 //20
    const val TONGITS_DEDUCTION = 50 //30

    // Other potential constants
    const val CHALLENGER_FEE = 50 //20
    const val FOLDEE_FEE = 20 //10
    const val NO_MELDS = 20 //10

    // Generic Player IDs for dynamic naming & DI sync
    const val ID_HUMAN = "player_human"
    const val ID_AI_1 = "player_ai_1"
    const val ID_AI_2 = "player_ai_2"
}
