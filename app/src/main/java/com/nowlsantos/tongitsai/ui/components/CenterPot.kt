package com.nowlsantos.tongitsai.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowlsantos.tongitsai.domain.fsm.game.GamePhase
import com.nowlsantos.tongitsai.presentation.GameViewModel
import com.nowlsantos.tongitsai.ui.theme.CrimsonRed

@Composable
fun CenterPot(
    centerFee: Int,
    roundNumber: Int,
    gamePhase: GamePhase,
    viewModel: GameViewModel,
    modifier: Modifier
) {
    Column(
        modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ){
        HorizontalDivider(
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f),
            thickness = 1.dp
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            //DynamicChipStack(amount = centerFee, size = 32.dp)
            Text(
                text = "Center Pot - P$centerFee",
                fontSize = 26.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f),
            thickness = 1.dp
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Round : $roundNumber",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )

                val buttonState by viewModel.buttonState.collectAsState()
                val isPlayStateHumanTurn = gamePhase == GamePhase.PLAYING && (buttonState.buttons[com.nowlsantos.tongitsai.domain.fsm.human.ButtonType.SWAP] == true)
                val isRoundEnd = gamePhase == GamePhase.ROUND_END
                val isScoreTallyEnabled = isPlayStateHumanTurn || isRoundEnd

                Button(
                    onClick = { viewModel.onShowGameResult() },
                    enabled = isScoreTallyEnabled,
                    shape = RectangleShape,
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = CrimsonRed,
                        disabledContainerColor = MaterialTheme.colorScheme.onTertiary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.4f),
                    ),
                    modifier = modifier.fillMaxWidth().weight(1f)
                ) {
                    Text(
                        text = "Score Tally",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Normal,
                        color = if (isScoreTallyEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.4f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}