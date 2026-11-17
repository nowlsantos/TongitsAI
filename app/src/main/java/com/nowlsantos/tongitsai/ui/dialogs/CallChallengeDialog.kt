package com.nowlsantos.tongitsai.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowlsantos.tongitsai.ui.components.FlexibleText
import com.nowlsantos.tongitsai.ui.theme.CrimsonRed
import com.nowlsantos.tongitsai.ui.theme.FlatBlack
import com.nowlsantos.tongitsai.ui.theme.PureWhite
import com.nowlsantos.tongitsai.ui.theme.SubtleGreen

@Composable
fun CallChallengeDialog(
    callerName: String,
    isInitial: Boolean,
    isNext: Boolean,
    message: String,
    onDismiss: () -> Unit,
    onAccept: () -> Unit,
    onFold: () -> Unit
) {
    AlertDialog(
        shape = RectangleShape,
        onDismissRequest = { /* Require explicit ACCEPT or FOLD button tap */ },
        containerColor = MaterialTheme.colorScheme.onPrimary,
        textContentColor = MaterialTheme.colorScheme.onBackground,
        title = {
            FlexibleText(
                text = "$callerName Makes A Call",
                modifier = Modifier.fillMaxWidth(),
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
            )
        },
        text = {
            FlexibleText(
                text = message,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 24.sp,
                fontWeight = FontWeight.Normal,
                //lineHeight = 34.sp,
                lineHeight = 26.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )
        },

        confirmButton = {
            if (!isInitial && !isNext) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        shape = RectangleShape,
                        modifier = Modifier.weight(1f),
                        onClick = onAccept,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        FlexibleText(
                            text = "ACCEPT",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    Button(
                        shape = RectangleShape,
                        modifier = Modifier.weight(1f),
                        onClick = onFold,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        FlexibleText(
                            text = "FOLD",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }
        },
        dismissButton = {
            if (!isInitial && isNext) {
                Button(
                    shape = RectangleShape,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onFold
                ) {
                    FlexibleText(
                        text = "NEXT ROUND",
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        },
        tonalElevation = 12.dp,
    )
}