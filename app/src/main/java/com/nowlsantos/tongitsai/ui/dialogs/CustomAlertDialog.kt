package com.nowlsantos.tongitsai.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nowlsantos.tongitsai.ui.theme.FlatBlack

@Composable
fun CustomAlertDialog(
    onDismissRequest: () -> Unit,
    //title: @Composable () -> Unit,
    text: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)? = null,
    scrimColor: Color = FlatBlack.copy(alpha = 0.5f)
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = scrimColor),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.35f)
                .fillMaxHeight(0.35f),
            shape = MaterialTheme.shapes.extraLarge, // Modern, larger rounded corners
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Dialog Title
                //title()
                Spacer(Modifier.height(8.dp))
                // Dialog Text
                text()
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    // Optional Dismiss Button
                    dismissButton?.invoke()
                    Spacer(Modifier.width(8.dp))
                    // Confirm Button
                    confirmButton()
                }
            }
        }
    }
}