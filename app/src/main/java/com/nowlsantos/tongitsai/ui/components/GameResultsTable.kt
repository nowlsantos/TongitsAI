package com.nowlsantos.tongitsai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowlsantos.tongitsai.data.GameData
import com.nowlsantos.tongitsai.ui.theme.CoolOrange

@Composable
fun GameResultsTable(
    gameData: GameData,
    modifier: Modifier = Modifier
) {
    // Dynamically get the player names from the summaries
    val playerNames = gameData.playerSummaries.keys.toList()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.onPrimary)
    ) {
        Text(
            text = "Game Results",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(10.dp)
        )

        // Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                text = "ROUND",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .weight(1f)
                    .padding(10.dp)
                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f))
            )
            // Map player names to Text Composables
            playerNames.forEach { name ->
                Text(
                    text = name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .weight(1f)
                        .padding(10.dp)
                        .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f))
                )
            }
        }

        // Round Results Rows (Scrollable)
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
        ) {
            itemsIndexed(gameData.roundResults) { index, round ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Text(
                        text = round["Round"] ?: "${index + 1}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .weight(1f)
                            .padding(10.dp)
                            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f))
                    )
                    // Map player names to Text Composables for round outcomes
                    playerNames.forEach { name ->
                        Text(
                            text = round[name] ?: "",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .weight(1f)
                                .padding(10.dp)
                                .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f))
                        )
                    }
                }
            }
        }

        val playerWins = mutableMapOf<String, Int>()
        playerNames.forEach { playerWins[it] = 0 }
        gameData.roundResults.forEach { round ->
            round.forEach { (name, result) ->
                if (result == "WIN" || result == "W") {
                    playerWins[name] = (playerWins[name] ?: 0) + 1
                }
            }
        }

        // Player Summaries (WINS) - Fixed at bottom
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Text(
                text = "WINS",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .weight(1f)
                    .padding(10.dp)
                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f))
            )
            playerNames.forEach { name ->
                Text(
                    text = (playerWins[name] ?: 0).toString(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .weight(1f)
                        .padding(10.dp)
                        .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f))
                )
            }
        }

        // Player Summaries (BALANCE) - Fixed at bottom
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CoolOrange),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Text(
                text = "BALANCE",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .weight(1f)
                    .padding(10.dp)
                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f))
            )
            playerNames.forEach { name ->
                Text(
                    text = gameData.playerSummaries[name]?.balance.toString(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .weight(1f)
                        .padding(10.dp)
                        .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f))
                )
            }
        }
    }
}
