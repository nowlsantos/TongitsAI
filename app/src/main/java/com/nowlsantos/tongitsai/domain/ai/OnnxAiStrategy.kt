package com.nowlsantos.tongitsai.domain.ai

import com.nowlsantos.tongitsai.domain.fsm.player.PlayerStateMachine
import com.nowlsantos.tongitsai.domain.model.GameState
import com.nowlsantos.tongitsai.domain.model.Player
import com.nowlsantos.tongitsai.presentation.GameInput
import com.nowlsantos.tongitsai.util.Logger

class OnnxAiStrategy(
    private val onnxAiEngine: OnnxAiEngine,
    private val encoder: GameStateEncoder,
    private val fallbackMinimax: AiMinimax
) : AiStrategy {
    private val tag = "OnnxAiStrategy"

    override suspend fun chooseMove(
        gameState: GameState,
        player: Player,
        playerStateMachine: PlayerStateMachine
    ): GameInput? {
        // If ONNX model is not loaded, fall back to Minimax
        if (!onnxAiEngine.isModelLoaded()) {
            return fallbackMinimax.chooseMove(gameState, player, playerStateMachine)
        }

        return try {
            val inputFeatures = encoder.encode(gameState, player)
            val probabilities = onnxAiEngine.evaluate(inputFeatures)

            if (probabilities == null || probabilities.isEmpty()) {
                return fallbackMinimax.chooseMove(gameState, player, playerStateMachine)
            }

            val currentPhase = playerStateMachine.currentPhase.value
            if (currentPhase == com.nowlsantos.tongitsai.domain.fsm.player.PlayerPhase.CHOW ||
                currentPhase == com.nowlsantos.tongitsai.domain.fsm.player.PlayerPhase.MELD ||
                currentPhase == com.nowlsantos.tongitsai.domain.fsm.player.PlayerPhase.SAPAW ||
                currentPhase == com.nowlsantos.tongitsai.domain.fsm.player.PlayerPhase.CALL) {
                return fallbackMinimax.chooseMove(gameState, player, playerStateMachine)
            }

            val legalMoves = MoveGenerator.generateLegalMoves(gameState, player, currentPhase)
            if (legalMoves.isEmpty()) {
                return fallbackMinimax.chooseMove(gameState, player, playerStateMachine)
            }

            // Select move corresponding to highest probability among legal moves
            legalMoves.maxByOrNull { move: GameInput ->
                val actionIndex = Math.abs(move.hashCode()) % probabilities.size
                probabilities[actionIndex]
            } ?: fallbackMinimax.chooseMove(gameState, player, playerStateMachine)

        } catch (e: Exception) {
            Logger.e(tag, "Error in OnnxAiStrategy: ${e.message}, falling back to Minimax", e)
            fallbackMinimax.chooseMove(gameState, player, playerStateMachine)
        }
    }
}
