package com.nowlsantos.tongitsai.ui.controls

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowlsantos.tongitsai.domain.fsm.game.GamePhase
import com.nowlsantos.tongitsai.presentation.GameInput
import com.nowlsantos.tongitsai.presentation.GameViewModel

@Composable
fun MessageControl(
    gamePhase: GamePhase,
    viewModel: GameViewModel,
    message: String,
    modifier: Modifier
) {
    if (gamePhase == GamePhase.SETUP) {
        Box(modifier.fillMaxSize()) {
            ControlButton(
                color = MaterialTheme.colorScheme.secondary,
                name = "Click Here To Start The Game".uppercase(),
                enabled = true,
                onClick = { viewModel.onEvent(GameInput.StartGame) },
                modifier
            )
        }
    } else {
        GameMessage(modifier, message)
    }
}

@Composable
private fun GameMessage(
    modifier: Modifier = Modifier,
    message: String
) {
    Box(
        modifier.fillMaxWidth().padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            fontSize = 24.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 32.sp,
            color = MaterialTheme.colorScheme.onPrimary,
            textAlign = TextAlign.Center,
            modifier = modifier
        )
    }
}