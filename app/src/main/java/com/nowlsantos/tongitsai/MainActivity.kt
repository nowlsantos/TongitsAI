package com.nowlsantos.tongitsai

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.nowlsantos.tongitsai.domain.model.FamousTrio
import com.nowlsantos.tongitsai.presentation.initializePlayerHandLayoutConfigs
import com.nowlsantos.tongitsai.ui.dialogs.ShowExitDialog
import com.nowlsantos.tongitsai.ui.screen.GameScreen
import com.nowlsantos.tongitsai.ui.screen.LobbyScreen
import com.nowlsantos.tongitsai.ui.theme.TongitsAITheme
import org.koin.compose.KoinContext

enum class Screen {
    LOBBY,
    GAME
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        enableEdgeToEdge()

        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        setContent {
            KoinContext {
                var currentScreen by remember { mutableStateOf(Screen.LOBBY) }
                var selectedCardBackRes by remember { mutableIntStateOf(R.drawable.backface_01) }
                var selectedTrio by remember { mutableStateOf(FamousTrio.PRESETS[0]) }
                var selectedBackgroundRes by remember { mutableIntStateOf(R.drawable.bg_green) }
                var showExitDialog by remember { mutableStateOf(false) }

                BackHandler {
                    if (currentScreen == Screen.GAME) {
                        currentScreen = Screen.LOBBY
                    } else {
                        showExitDialog = true
                    }
                }

                TongitsAITheme(
                    cardBackRes = selectedCardBackRes,
                    backgroundRes = selectedBackgroundRes
                ) {
                    LoadBackgroundImage(selectedBackgroundRes)
                    initializePlayerHandLayoutConfigs()

                    when (currentScreen) {
                        Screen.LOBBY -> {
                            LobbyScreen(
                                onStartGame = { trio, cardBackRes, backgroundRes ->
                                    selectedTrio = trio
                                    selectedCardBackRes = cardBackRes
                                    selectedBackgroundRes = backgroundRes
                                    currentScreen = Screen.GAME
                                }
                            )
                        }
                        Screen.GAME -> {
                            GameScreen(
                                cardBackRes = selectedCardBackRes,
                                selectedTrio = selectedTrio
                            )
                        }
                    }

                    if (showExitDialog) {
                        ShowExitDialog(
                            onDismiss = { showExitDialog = false }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadBackgroundImage(imageID: Int) {
    Image(
        painter = painterResource(imageID),
        contentDescription = "Tongits Background",
        contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxSize()
    )
}
