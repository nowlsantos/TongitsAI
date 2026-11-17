package com.nowlsantos.tongitsai.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.nowlsantos.tongitsai.R
import com.nowlsantos.tongitsai.domain.model.PlayingCard
import com.nowlsantos.tongitsai.util.GameConstants.CARD_HEIGHT
import com.nowlsantos.tongitsai.util.GameConstants.CARD_WIDTH

@Composable
fun BackFaceView(
    card: PlayingCard,
    modifier: Modifier = Modifier,
    cardBackRes: Int = R.drawable.backface_01,
) {
    Image(
        painter = painterResource(id = cardBackRes),
        contentDescription = "Card back",
        contentScale = ContentScale.Fit,
        modifier = modifier.size(CARD_WIDTH, CARD_HEIGHT) // Use passed dimensions
    )
}