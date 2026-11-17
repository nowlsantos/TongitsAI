package com.nowlsantos.tongitsai.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import com.nowlsantos.tongitsai.R

private val GreenForestColorScheme = darkColorScheme(
    primary = SubtleGreen,
    secondary = CrimsonRed,
    tertiary = CoolOrange,
    background = FlatBlack,
    surface = MatteGray,
    onPrimary = PureWhite,
    onSecondary = DarkGreen,
    onTertiary = DarkGray,
    onBackground = FlatBlack,
    onSurface = PureWhite
)

private val OceanBlueColorScheme = darkColorScheme(
    primary = TrueBlue,
    secondary = CrimsonRed,
    tertiary = CoolOrange,
    background = FlatBlack,
    surface = MatteGray,
    onPrimary = PureWhite,
    onSecondary = LightGray,
    onTertiary = DarkGray,
    onBackground = FlatBlack,
    onSurface = PureWhite
)

private val RoyalFlushColorScheme = darkColorScheme(
    primary = RoyalPurple,
    secondary = CrimsonRed,
    tertiary = CoolOrange,
    background = FlatBlack,
    surface = MatteGray,
    onPrimary = PureWhite,
    onSecondary = LightGray,
    onTertiary = DarkGray,
    onBackground = FlatBlack,
    onSurface = PureWhite
)

private val DarkSideColorScheme = darkColorScheme(
    primary = CrimsonRed,
    secondary = RoyalPurple,
    tertiary = CoolOrange,
    background = FlatBlack,
    surface = FlatBlack,
    onPrimary = PureWhite,
    onSecondary = PureWhite,
    onTertiary = DarkGray,
    onBackground = FlatBlack,
    onSurface = PureWhite
)

@Composable
fun TongitsAITheme(
    cardBackRes: Int = R.drawable.card_back,
    backgroundRes: Int = R.drawable.bg_blue,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        cardBackRes == R.drawable.backface_02 || backgroundRes == R.drawable.bg_blue -> OceanBlueColorScheme
        cardBackRes == R.drawable.backface_03 -> RoyalFlushColorScheme
        cardBackRes == R.drawable.backface_04 -> DarkSideColorScheme
        else -> GreenForestColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
