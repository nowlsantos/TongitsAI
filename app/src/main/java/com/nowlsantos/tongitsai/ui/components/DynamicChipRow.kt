package com.nowlsantos.tongitsai.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowlsantos.tongitsai.ui.theme.PureWhite

@Composable
fun DynamicChipRow(
    modifier: Modifier = Modifier,
    amount: Int,
    size: Dp = 24.dp
) {
    val total = amount.coerceAtLeast(0)

    // Subdivide 300 total into 30, 20, and 10 chips
    val c30 = (total / 30).coerceAtMost(3) // e.g. 3 of 30 (= 90)
    val after30 = total - (c30 * 30)
    val c20 = (after30 / 20).coerceAtMost(5) // e.g. up to 5 of 20
    val after20 = after30 - (c20 * 20)
    val c10 = (after20 / 10).coerceAtMost(15) // remainder in 10s

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (c30 > 0) {
            ChipWithCount(denomination = "30", color = Color(0xFF7B1FA2), count = c30, size = size)
        }
        if (c20 > 0) {
            ChipWithCount(denomination = "20", color = Color(0xFF1E88E5), count = c20, size = size)
        }
        if (c10 > 0 || (c30 == 0 && c20 == 0)) {
            ChipWithCount(denomination = "10", color = Color(0xFFE53935), count = c10.coerceAtLeast(1), size = size)
        }
    }
}

@Composable
fun ChipWithCount(
    denomination: String,
    color: Color,
    count: Int,
    size: Dp
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        ChipView(
            denomination = denomination,
            chipColor = color,
            size = size
        )
        if (count > 1) {
            Text(
                text = "×$count",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = PureWhite
            )
        }
    }
}
