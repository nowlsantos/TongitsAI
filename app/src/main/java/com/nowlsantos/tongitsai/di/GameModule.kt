package com.nowlsantos.tongitsai.di

import com.nowlsantos.tongitsai.data.GameRepository
import com.nowlsantos.tongitsai.domain.ai.AiHandRepository
import com.nowlsantos.tongitsai.domain.ai.AiMinimax
import com.nowlsantos.tongitsai.domain.ai.AiStrategy
import com.nowlsantos.tongitsai.domain.ai.GameStateEncoder
import com.nowlsantos.tongitsai.domain.ai.OnnxAiEngine
import com.nowlsantos.tongitsai.domain.ai.OnnxAiStrategy
import com.nowlsantos.tongitsai.domain.fsm.game.GameStateMachine
import com.nowlsantos.tongitsai.domain.fsm.player.PlayerStateMachine
import com.nowlsantos.tongitsai.domain.model.GameState
import com.nowlsantos.tongitsai.domain.model.GameUiState
import com.nowlsantos.tongitsai.domain.model.LocalCoordinate
import com.nowlsantos.tongitsai.domain.model.Player
import com.nowlsantos.tongitsai.domain.model.enums.PlayerPosition
import com.nowlsantos.tongitsai.domain.model.enums.PlayerType
import com.nowlsantos.tongitsai.domain.usecase.GameHistoryUseCase
import com.nowlsantos.tongitsai.presentation.GameViewModel
import com.nowlsantos.tongitsai.util.GameConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val gameModule = module {

    single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    single<MutableStateFlow<GameState>> { MutableStateFlow(GameState.initial()) }

    single<MutableStateFlow<GameUiState>> {
        val initialGameState = GameState.initial()
        val initialGameUiState = GameUiState.fromGameState(
            gs = initialGameState,
            localCoordinate = LocalCoordinate(),
            meldAreaCoordinates = emptyMap(),
        )
        MutableStateFlow(initialGameUiState)
    }

    single<StateFlow<GameUiState>> { get<MutableStateFlow<GameUiState>>() }

    single {
        GameStateMachine(
            externalScope = get(),
            uiState = get()
        )
    }

    single { GameRepository(get()) }

    single {
        GameHistoryUseCase(
            repository = get(),
            gameStateMachine = get(),
            scope = get()
        )
    }

    single { OnnxAiEngine(androidContext()) }
    single { GameStateEncoder() }
    single { AiMinimax() }
    single<AiStrategy> { OnnxAiStrategy(get(), get(), get()) }
    single { AiHandRepository }

    viewModel {
        GameViewModel(
            gameStateMachine = get(),
            _gameUiState = get(),
            gameHistoryUseCase = get()
        )
    }

    single(named("p1")) {
        Player(
            id = GameConstants.ID_HUMAN,
            name = "Player1",
            type = PlayerType.HUMAN,
            cards = emptyList(),
            position = PlayerPosition.BOTTOM,
            isHitter = false
        )
    }

    /* AI #1 – Anakin */
    single(named("p2"), createdAtStart = true) {
        PlayerStateMachine(
            player = Player(
                id = GameConstants.ID_AI_1,
                name = "Player2",
                type = PlayerType.AI,
                cards = emptyList(),
                position = PlayerPosition.RIGHT,
                isHitter = false
            ),
            externalScope = get(),
            strategy = get(),
            gameStateMachine = get(),
            aiHandRepository = get()
        )
    }

    /* AI #2 – Obiwan */
    single(named("p3"), createdAtStart = true) {
        PlayerStateMachine(
            player = Player(
                id = GameConstants.ID_AI_2,
                name = "Player3",
                type = PlayerType.AI,
                cards = emptyList(),
                position = PlayerPosition.LEFT,
                isHitter = false
            ),
            externalScope = get(),
            strategy = get(),
            gameStateMachine = get(),
            aiHandRepository = get()
        )
    }
}
