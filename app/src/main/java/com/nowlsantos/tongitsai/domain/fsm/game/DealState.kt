package com.nowlsantos.tongitsai.domain.fsm.game

import com.nowlsantos.tongitsai.domain.fsm.animation.GameAnimationState
import com.nowlsantos.tongitsai.presentation.GameInput
import com.nowlsantos.tongitsai.presentation.UiEffect
import com.nowlsantos.tongitsai.util.Logger

class DealState : GamePhaseState {
    private val tag = "DealState"
    private var gameAnimationState: GameAnimationState? = null

    override fun onEnter(stateMachine: GameStateMachine) {
        Logger.d("DealState", "Entering Dealing phase")
        gameAnimationState = stateMachine.gameAnimationState

        // Pass the required coordinates from GameStateMachine's uiState
        gameAnimationState?.prepareDealingSequence(stateMachine.gameState.value.players)

        stateMachine.emitUiEffect(
            UiEffect.ShowMessage("Now dealing cards for round ${stateMachine.gameState.value.roundNumber}")
        )
    }

    override suspend fun handleInput(stateMachine: GameStateMachine, input: GameInput) {
        when (input) {
            GameInput.CardAnimationFinished -> {
                gameAnimationState?.handleAnimationCompletionSignal()
            }

            GameInput.DealingCardsComplete -> {
                Logger.d(tag, "Received DealingCardsComplete. Transitioning to PlayState.")
                stateMachine.transitionTo(GamePhase.PLAYING)
            }
            else -> {
                Logger.w(tag, "Input $input not handled in DealState.")
            }
        }
    }

    override fun onExit(stateMachine: GameStateMachine) {
        Logger.d("DealState", "Exiting Dealing phase")
    }
}