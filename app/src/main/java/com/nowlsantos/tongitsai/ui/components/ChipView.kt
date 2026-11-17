package com.nowlsantos.tongitsai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowlsantos.tongitsai.ui.theme.BrightYellow
import com.nowlsantos.tongitsai.ui.theme.PureWhite

@Composable
fun ChipView(
    denomination: String = "100",
    chipColor: Color = Color(0xFFE53935), // Red chip by default
    size: Dp = 40.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(chipColor)
            .border(2.dp, PureWhite, CircleShape)
            .border(4.dp, chipColor.copy(alpha = 0.6f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size * 0.7f)
                .clip(CircleShape)
                .border(1.dp, BrightYellow, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = denomination,
                color = PureWhite,
                fontSize = (size.value * 0.3f).sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
