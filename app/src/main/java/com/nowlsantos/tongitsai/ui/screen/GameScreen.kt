package com.nowlsantos.tongitsai.ui.screen

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nowlsantos.tongitsai.R
import com.nowlsantos.tongitsai.domain.fsm.game.GamePhase
import com.nowlsantos.tongitsai.domain.model.FamousTrio
import com.nowlsantos.tongitsai.domain.model.LocalCoordinate
import com.nowlsantos.tongitsai.domain.model.PlayingCard
import com.nowlsantos.tongitsai.domain.model.enums.PlayerPosition
import com.nowlsantos.tongitsai.presentation.GameInput
import com.nowlsantos.tongitsai.presentation.GameViewModel
import com.nowlsantos.tongitsai.presentation.UiEffect
import com.nowlsantos.tongitsai.ui.animations.GameAnimator
import com.nowlsantos.tongitsai.ui.components.ConfettiView
import com.nowlsantos.tongitsai.ui.controls.GameControls
import com.nowlsantos.tongitsai.ui.dialogs.DialogManager
import com.nowlsantos.tongitsai.ui.players.PlayerSeat
import com.nowlsantos.tongitsai.ui.theme.FlatBlack
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@Composable
fun GameScreen(
    viewModel: GameViewModel = koinViewModel(),
    cardBackRes: Int = R.drawable.card_back,
    selectedTrio: FamousTrio = FamousTrio.PRESETS[0]
) {
    LaunchedEffect(selectedTrio) {
        viewModel.setTrio(selectedTrio)
    }

    // Collect UI State and Effects
    val uiState by viewModel.gameUiState.collectAsStateWithLifecycle()

    // State for local coordinates (used for animations)
    var localCoordinate by remember { mutableStateOf(LocalCoordinate()) }

    // Source offsets
    var allPlayersCardOffsets by remember { mutableStateOf(emptyMap<PlayerPosition, Map<PlayingCard, DpOffset>>()) }
    var meldingTargetOffset by remember { mutableStateOf(DpOffset.Zero) }

    // Collect the game data as a state to trigger recomposition on changes.
    val gameData by viewModel.gameData.collectAsState()

    // Ui specific helpers
    var message by remember { mutableStateOf("Welcome to Tongits!") }

    // State for the call challenge dialog
    var challengeDialog by remember { mutableStateOf(false) }
    var initialDialog by remember { mutableStateOf(false) }
    var callerName by remember { mutableStateOf("") }
    var callerId by remember { mutableStateOf("") }
    var challengerId by remember { mutableStateOf("") }
    var winnerDialog by remember { mutableStateOf(false) }
    var gameResultDialog by remember { mutableStateOf(false) }
    var isNext by remember { mutableStateOf(false) }
    var showConfettiTest by remember { mutableStateOf(false) }

    val isGameOver = uiState.gamePhase == GamePhase.GAME_OVER || showConfettiTest

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collectLatest { effect ->
            when (effect) {
                is UiEffect.ShowMessage -> {
                    message = effect.message
                }
                is UiEffect.InitialMessage -> {
                    message = effect.message
                    initialDialog = true
                }
                is UiEffect.ShowError -> {
                    message = effect.message
                }
                is UiEffect.ShowWinner -> {
                    message = effect.message
                    winnerDialog = true
                    initialDialog = false
                }
                is UiEffect.ShowGameOver -> {
                    message = "Game has ended. ${effect.message} is the overall winner!"
                }
                is UiEffect.ResetDialog -> {
                    initialDialog = false
                    challengeDialog = false
                    winnerDialog = false
                    gameResultDialog = false
                    isNext = false
                }
                is UiEffect.PlayerCall -> {
                    callerId = effect.callerId
                    message = effect.message
                    isNext = false
                    initialDialog = false
                    challengeDialog = true
                }
                is UiEffect.AskCallChallenge -> {
                    callerId = effect.callerId
                    callerName = effect.callerName
                    challengerId = effect.challengerId
                    message = effect.message
                    initialDialog = false
                    isNext = false
                    challengeDialog = true
                }
                is UiEffect.GameResultDialog -> {
                    gameResultDialog = true
                    winnerDialog = false
                }
                is UiEffect.ClearMessage -> { message = "" }
                else -> {}
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .border(3.dp, FlatBlack),
        contentAlignment = Alignment.TopStart
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.weight(1f)) {
                // Left player
                uiState.uiPlayers.firstOrNull { it.position == PlayerPosition.LEFT }?.let { leftPlayer ->
                    PlayerSeat(
                        uiPlayer = leftPlayer,
                        uiState = uiState,
                        viewModel = viewModel,
                        onCardClick = null,
                        cardBackRes = cardBackRes,
                        onMeldClickedForSapaw = { meldSourceOffset, position, meldIndex ->
                            viewModel.onEvent(GameInput.SelectSapawTarget(meldSourceOffset, position, meldIndex))
                        },
                        onPlayerCoordinates = { offset ->
                            localCoordinate = localCoordinate.copy(leftHandOffset = offset)
                            viewModel.onEvent(GameInput.UpdateLocalCoordinates(PlayerPosition.LEFT, localCoordinate.leftHandOffset))
                        },
                        onCardOffsetsChanged = { cardOffsets ->
                            allPlayersCardOffsets = allPlayersCardOffsets.toMutableMap().apply {
                                this[PlayerPosition.LEFT] = cardOffsets
                            }
                            viewModel.onEvent(GameInput.UpdateAllPlayersCardOffsets(PlayerPosition.LEFT, cardOffsets))
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Right player
                uiState.uiPlayers.firstOrNull { it.position == PlayerPosition.RIGHT }?.let { rightPlayer ->
                    PlayerSeat(
                        uiPlayer = rightPlayer,
                        uiState = uiState,
                        viewModel = viewModel,
                        onCardClick = null,
                        cardBackRes = cardBackRes,
                        onMeldClickedForSapaw = { meldSourceOffset, position, meldIndex ->
                            viewModel.onEvent(GameInput.SelectSapawTarget(meldSourceOffset, position, meldIndex))
                        },
                        onPlayerCoordinates = { offset ->
                            localCoordinate = localCoordinate.copy(rightHandOffset = offset)
                            viewModel.onEvent(GameInput.UpdateLocalCoordinates(PlayerPosition.RIGHT, localCoordinate.rightHandOffset))
                        },
                        onCardOffsetsChanged = { cardOffsets ->
                            allPlayersCardOffsets = allPlayersCardOffsets.toMutableMap().apply {
                                this[PlayerPosition.RIGHT] = cardOffsets
                            }
                            viewModel.onEvent(GameInput.UpdateAllPlayersCardOffsets(PlayerPosition.RIGHT, cardOffsets))
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Row(Modifier.weight(1f)) {
                // Bottom player (Human)
                uiState.uiPlayers.firstOrNull { it.position == PlayerPosition.BOTTOM }?.let { bottomPlayer ->
                    PlayerSeat(
                        uiPlayer = bottomPlayer,
                        uiState = uiState,
                        viewModel = viewModel,
                        cardBackRes = cardBackRes,
                        onCardClick = { card, offset ->
                            localCoordinate = localCoordinate.copy(cardOffset = offset)
                            viewModel.onEvent(GameInput.CardSelection(card, offset))
                        },
                        onMeldClickedForSapaw = { meldSourceOffset, position, meldIndex ->
                            viewModel.onEvent(GameInput.SelectSapawTarget(meldSourceOffset, position, meldIndex))
                        },
                        onPlayerCoordinates = { offset ->
                            localCoordinate = localCoordinate.copy(bottomHandOffset = offset)
                            viewModel.onEvent(GameInput.UpdateLocalCoordinates(PlayerPosition.BOTTOM, localCoordinate.bottomHandOffset))
                        },
                        onCardOffsetsChanged = { cardOffsets ->
                            allPlayersCardOffsets = allPlayersCardOffsets.toMutableMap().apply {
                                this[PlayerPosition.BOTTOM] = cardOffsets
                            }
                            viewModel.onEvent(GameInput.UpdateAllPlayersCardOffsets(PlayerPosition.BOTTOM, cardOffsets))
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Game Controls UI
                GameControls(
                    uiState = uiState,
                    viewModel = viewModel,
                    message = message,
                    meldingTargetOffset = meldingTargetOffset,
                    allPlayersCardOffsets = allPlayersCardOffsets,
                    cardBackRes = cardBackRes,
                    onDeckCoordinates = { offset ->
                        localCoordinate = localCoordinate.copy(deckOffset = offset)
                        viewModel.onEvent(GameInput.UpdateDeckOffset(offset))
                    },
                    onDiscardCoordinates = { offset ->
                        localCoordinate = localCoordinate.copy(discardPileOffset = offset)
                        viewModel.onEvent(GameInput.UpdateDiscardPileOffset(offset))
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Animation layer
        GameAnimator(
            localCoordinate = localCoordinate,
            uiState = uiState,
            viewModel = viewModel,
            cardBackRes = cardBackRes
        )

        // Confetti layer (On top of game board and animations)
        if (isGameOver) {
            ConfettiView(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(50f)
            )
        }

        // Dialogs layer
        DialogManager(
            viewModel = viewModel,
            gameRound = uiState.roundNumber,
            gamePhase = uiState.gamePhase,
            callerName = callerName,
            challengerId = challengerId,
            callerId = callerId,
            challengeDialog = challengeDialog,
            initialDialog = initialDialog,
            isNext = isNext,
            winnerDialog = winnerDialog,
            gameResultDialog = gameResultDialog,
            message = message,
            gameData = gameData,
            onDismissCallChallenge = {
                initialDialog = false
                challengeDialog = false
                winnerDialog = false
                gameResultDialog = false
            }
        )

        // Confetti layer & New Game Button (Only appear automatically when game ends)
        if (uiState.gamePhase == GamePhase.GAME_OVER || showConfettiTest) {
            ConfettiView(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(50f)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(100f),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        showConfettiTest = false
                        viewModel.restartGame()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.padding(bottom = 32.dp)
                ) {
                    Text(
                        text = "New Game",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}