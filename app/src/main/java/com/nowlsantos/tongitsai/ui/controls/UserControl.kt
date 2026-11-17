package com.nowlsantos.tongitsai.ui.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowlsantos.tongitsai.domain.fsm.human.ButtonState
import com.nowlsantos.tongitsai.domain.fsm.human.ButtonType
import com.nowlsantos.tongitsai.presentation.UserControlEvent
import com.nowlsantos.tongitsai.ui.theme.*

@Composable
fun UserControls(
    buttonState: ButtonState,
    onClick: (UserControlEvent) -> Unit,
    modifier: Modifier
) {
    Column(modifier.fillMaxSize()) {
        Row(
            modifier
                .weight(1f)
                .fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!buttonState.isActionPhase) {
                ControlButton(
                    color = CoolOrange,
                    name = "DRAW",
                    enabled = buttonState.buttons[ButtonType.DRAW] ?: false,
                    onClick = { onClick(UserControlEvent.Get) },
                    modifier.weight(1f)
                )

                ControlButton(
                    color = SubtleGreen,
                    name = "CHOW",
                    enabled = buttonState.buttons[ButtonType.CHOW] ?: false,
                    onClick = { onClick(UserControlEvent.Chow) },
                    modifier.weight(1f)
                )

                ControlButton(
                    color = TealDrop,
                    name = "CALL",
                    enabled = buttonState.buttons[ButtonType.CALL] ?: false,
                    onClick = { onClick(UserControlEvent.Call) },
                    modifier.weight(1f)
                )
            } else {
                ControlButton(
                    color = CrimsonRed,
                    name = "DISCARD",
                    enabled = buttonState.buttons[ButtonType.DISCARD] ?: false,
                    onClick = { onClick(UserControlEvent.Discard) },
                    Modifier.weight(1f)
                )

                ControlButton(
                    color = SubtleGreen,
                    name = "MELD",
                    enabled = buttonState.buttons[ButtonType.MELD] ?: false,
                    onClick = { onClick(UserControlEvent.Meld) },
                    Modifier.weight(1f)
                )

                ControlButton(
                    color = TealDrop,
                    name = "SAPAW",
                    enabled = buttonState.buttons[ButtonType.SAPAW] ?: false,
                    onClick = { onClick(UserControlEvent.Sapaw) },
                    Modifier.weight(1f)
                )
            }
        }

        Row(
            modifier
                .weight(1f)
                .fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ControlButton(
                color = RoyalPurple,
                name = "RANK",
                enabled = buttonState.buttons[ButtonType.SORT_RANK] ?: false,
                onClick = { onClick(UserControlEvent.SortRank) },
                modifier.weight(1f)
            )

            ControlButton(
                color = LovelyPink,
                name = "SUIT",
                enabled = buttonState.buttons[ButtonType.SORT_SUIT] ?: false,
                onClick = { onClick(UserControlEvent.SortSuit) },
                modifier.weight(1f)
            )

            ControlButton(
                color = RareIndigo,
                name = "SWAP",
                enabled = buttonState.buttons[ButtonType.SWAP] ?: false,
                onClick = { onClick(UserControlEvent.Swap) },
                modifier.weight(1f)
            )
        }
    }
}

@Composable
fun ControlButton(
    color: Color,
    name: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier
) {
    // Track the last click time to prevent double clicks
    var lastClickTime by remember { mutableLongStateOf(0L) }

    Button(
        onClick = {
            val currentTime = System.currentTimeMillis()
            // Only allow clicks if at least 500ms have passed since the last click
            if (currentTime - lastClickTime > 500) {
                lastClickTime = currentTime
                onClick()
            }
        },
        enabled = enabled,
        shape = RectangleShape,
        colors = ButtonDefaults.elevatedButtonColors(
            containerColor = color,
            disabledContainerColor = MatteGray,
            disabledContentColor = LightGray,
        ),
        modifier = modifier.fillMaxSize().padding(4.dp)
    ) {
        Text(
            text = name,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = if (enabled) PureWhite else LightGray,
        )
    }
}