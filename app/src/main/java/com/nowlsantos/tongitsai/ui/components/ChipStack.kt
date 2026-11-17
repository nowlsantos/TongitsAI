package com.nowlsantos.tongitsai.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ChipStack(
    count: Int = 4,
    chipColor: Color = Color(0xFF1E88E5), // Blue chip by default
    size: Dp = 36.dp,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        val stackCount = count.coerceIn(1, 6)
        for (i in 0 until stackCount) {
            // Offset each chip slightly upwards to create a 3D stack effect
            val offsetY = (-i * 5).dp
            ChipView(
                denomination = "${(i + 1) * 10}",
                chipColor = chipColor,
                size = size,
                modifier = Modifier.offset(y = offsetY)
            )
        }
    }
}
