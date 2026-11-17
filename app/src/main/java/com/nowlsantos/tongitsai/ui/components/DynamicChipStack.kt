package com.nowlsantos.tongitsai.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class ChipDenomination(
    val value: Int,
    val color: Color,
    val label: String
)

object ChipConfig {
    val CHIP_30 = ChipDenomination(30, Color(0xFF7B1FA2), "30") // Purple
    val CHIP_20 = ChipDenomination(20, Color(0xFF1E88E5), "20") // Blue
    val CHIP_10 = ChipDenomination(10, Color(0xFFE53935), "10") // Red
}

@Composable
fun DynamicChipStack(
    modifier: Modifier = Modifier,
    amount: Int,
    size: Dp = 34.dp
) {
    val total = amount.coerceAtLeast(0)

    val stackCount = when {
        total <= 0 -> 1
        total < 30 -> 1
        total < 60 -> 2
        total < 100 -> 3
        total < 180 -> 4
        total < 250 -> 5
        total < 350 -> 6
        total < 450 -> 7
        else -> 8
    }

    val chipsToRender = mutableListOf<ChipDenomination>()
    repeat(stackCount) { index ->
        val denom = when {
            index % 3 == 0 -> ChipConfig.CHIP_30
            index % 3 == 1 -> ChipConfig.CHIP_20
            else -> ChipConfig.CHIP_10
        }
        chipsToRender.add(denom)
    }

    Box(modifier = modifier) {
        chipsToRender.forEachIndexed { index, denom ->
            val offsetY = (-index * 4).dp
            ChipView(
                denomination = denom.label,
                chipColor = denom.color,
                size = size,
                modifier = Modifier.offset(y = offsetY)
            )
        }
    }
}
