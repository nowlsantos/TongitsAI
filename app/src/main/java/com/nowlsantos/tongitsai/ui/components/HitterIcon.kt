package com.nowlsantos.tongitsai.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowlsantos.tongitsai.R
import com.nowlsantos.tongitsai.ui.theme.BrightYellow

@Composable
fun HitterIcon(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.hitter),
            contentDescription = "Hitter icon",
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(28.dp)
        )

        Text(
            text = " Hitter",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = BrightYellow,
        )
    }
}
