package com.nowlsantos.tongitsai.ui.dialogs

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowlsantos.tongitsai.ui.components.FlexibleText

@Composable
fun RoundWinnerDialog(
    gameRound: Int,
    message: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        shape = RectangleShape,
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.onPrimary,
        textContentColor = MaterialTheme.colorScheme.onBackground,
        title = {
            FlexibleText(
                text = "Round $gameRound Winner!",
                modifier = Modifier.fillMaxWidth(),
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        },
        text = {
            FlexibleText(
                text = message,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 24.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 34.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )
        },

        confirmButton = { },
        dismissButton = {
            Button(
                onClick = onDismiss,
                shape = RectangleShape,
                modifier = Modifier.fillMaxWidth()
            ) {
                FlexibleText(
                    text = "VIEW SCORE TALLY",
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        },
    )
}