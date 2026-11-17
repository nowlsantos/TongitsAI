package com.nowlsantos.tongitsai.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowlsantos.tongitsai.domain.model.FamousTrio
import com.nowlsantos.tongitsai.domain.model.GameSettings
import com.nowlsantos.tongitsai.ui.theme.BrightYellow
import com.nowlsantos.tongitsai.ui.theme.CoolOrange
import com.nowlsantos.tongitsai.ui.theme.DarkGreen
import com.nowlsantos.tongitsai.ui.theme.PureWhite
import com.nowlsantos.tongitsai.ui.theme.SubtleGreen
import com.nowlsantos.tongitsai.ui.theme.Transparent

@Composable
fun LobbyScreen(
    onStartGame: (FamousTrio, Int, Int) -> Unit
) {
    var selectedTrio by remember { mutableStateOf(FamousTrio.PRESETS[0]) }
    var selectedCardBack by remember { mutableStateOf(GameSettings.CARD_BACKS[0]) }
    var selectedBackground by remember { mutableStateOf(GameSettings.BACKGROUNDS[0]) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkGreen),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Tongits Lobby",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = BrightYellow.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 40.dp)
        )

        Row(modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween)
        {
            // First Column
            Column(modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(vertical = 20.dp),
                //.background(PureWhite.copy(alpha = 0.1f)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                // Section 1: Famous Trios Selection
                Text(
                    text = "Choose Your Famous Trio",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Normal,
                    color = PureWhite,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp)
                )

                Column(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Splits your presets into chunks of 3 for the 3 rows
                    FamousTrio.PRESETS.chunked(3).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowItems.forEach { trio ->
                                val isSelected = trio == selectedTrio
                                Card(
                                    modifier = Modifier
                                        .weight(1f) // Each card takes an equal 1/3 of the horizontal width
                                        .height(180.dp)
                                        .clickable { selectedTrio = trio }
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) BrightYellow.copy(alpha = 0.8f) else PureWhite.copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(12.dp)
                                        ),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) CoolOrange.copy(alpha = 0.4f) else DarkGreen
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = trio.groupTitle,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BrightYellow
                                        )
                                        Text("You: ${trio.humanName}", fontSize = 18.sp, color = PureWhite)
                                        Text("AI 1: ${trio.ai1Name}", fontSize = 18.sp, color = PureWhite.copy(alpha = 0.8f))
                                        Text("AI 2: ${trio.ai2Name}", fontSize = 18.sp, color = PureWhite.copy(alpha = 0.8f))
                                    }
                                }
                            }

                            // Handles any uneven trailing rows if your list isn't an exact multiple of 3
                            repeat(3 - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // Second Column
            Column(modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(vertical = 30.dp),
                //.background(BrightYellow.copy(alpha = 0.2f)),
                verticalArrangement = Arrangement.Top,
            ) {
                // Section 2: Card Back Selection
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Choose Card Back Style",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Normal,
                        color = PureWhite,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(10.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        GameSettings.CARD_BACKS.forEach { cardBack ->
                            val isSelected = cardBack == selectedCardBack
                            Card(
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(180.dp)
                                    .clickable { selectedCardBack = cardBack },
                                /*.border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) BrightYellow else PureWhite.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(8.dp)
                                ),*/
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) PureWhite.copy(alpha = 0.2f) else Transparent
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = cardBack.drawableRes),
                                        contentDescription = cardBack.name,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .size(100.dp, 120.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = cardBack.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PureWhite,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(60.dp))

                // Section 3: Screen Background Selection
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Choose Screen Background",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Normal,
                        color = PureWhite,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        GameSettings.BACKGROUNDS.forEach { background ->
                            val isSelected = background == selectedBackground
                            Card(
                                modifier = Modifier
                                    .width(200.dp)
                                    .height(160.dp)
                                    .clickable { selectedBackground = background },
                                    /*.border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) BrightYellow else PureWhite.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(8.dp)
                                    ),*/
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) PureWhite.copy(alpha = 0.2f) else Transparent
                                )
                            ) {
                                Column(
                                    modifier = Modifier .fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    Image(
                                        painter = painterResource(id = background.drawableRes),
                                        contentDescription = background.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(160.dp, 110.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                    )
                                    Text(
                                        text = background.name,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PureWhite,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(60.dp))

                Button(
                    onClick = { onStartGame(selectedTrio, selectedCardBack.drawableRes, selectedBackground.drawableRes) },
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SubtleGreen,
                        contentColor = PureWhite
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                ) {
                    Text(
                        text = "Let's Play Tongits",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Normal,
                        color = PureWhite
                    )
                }
            }
        }
    }
}
