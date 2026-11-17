package com.nowlsantos.tongitsai.domain.model

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.nowlsantos.tongitsai.R

@Immutable
data class CardBackOption(
    val id: String,
    val name: String,
    @param:DrawableRes val drawableRes: Int
)

@Immutable
data class BackgroundOption(
    val id: String,
    val name: String,
    @param:DrawableRes val drawableRes: Int
)

@Immutable
data class GameSettings(
    val trio: FamousTrio = FamousTrio.PRESETS[0],
    val cardBackRes: Int = R.drawable.card_back,
    val backgroundRes: Int = R.drawable.bg_blue
) {
    companion object {
        val CARD_BACKS = listOf(
            CardBackOption("splashy", "Forest Green", R.drawable.backface_01),
            CardBackOption("classic", "Ocean Blue", R.drawable.backface_02),
            CardBackOption("royal", "Royal flush", R.drawable.backface_03),
            CardBackOption("rebel", "Dark Side", R.drawable.backface_04)
        )

        val BACKGROUNDS = listOf(
            BackgroundOption("green", "Green Felt", R.drawable.bg_green),
            BackgroundOption("blue", "Blue Felt", R.drawable.bg_blue)
        )
    }
}
