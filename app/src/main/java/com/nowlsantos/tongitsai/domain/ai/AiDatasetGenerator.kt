package com.nowlsantos.tongitsai.domain.ai

import com.nowlsantos.tongitsai.domain.fsm.player.PlayerPhase
import com.nowlsantos.tongitsai.domain.model.GameState
import com.nowlsantos.tongitsai.domain.model.Player
import com.nowlsantos.tongitsai.domain.model.enums.PlayerType
import com.nowlsantos.tongitsai.presentation.GameInput
import com.nowlsantos.tongitsai.util.Logger
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class TrainingSample(
    val features: List<Float>,
    val actionHash: Int
)

class AiDatasetGenerator(
    private val aiMinimax: AiMinimax,
    private val encoder: GameStateEncoder
) {
    private val tag = "AiDatasetGenerator"
    private val json = Json { prettyPrint = false }

    /**
     * Simulates games using Minimax to generate and export real training samples.
     */
    fun simulateAndExport(outputFile: File, numGames: Int = 1000) {
        Logger.i(tag, "Starting simulation of $numGames games for Minimax dataset generation...")
        val startTime = System.currentTimeMillis()
        var totalSampleCount = 0

        outputFile.bufferedWriter().use { writer ->
            writer.write("[")
            var firstSample = true

            for (gameIndex in 0 until numGames) {
                var gameState = GameState.initial()
                var turnCount = 0

                while (!gameState.isGameOver && turnCount < 30) {
                    val currentPlayer = gameState.currentPlayer
                    val features = encoder.encode(gameState, currentPlayer)

                    val legalMoves = MoveGenerator.generateLegalMoves(gameState, currentPlayer, PlayerPhase.DISCARD)
                    val chosenMove = legalMoves.firstOrNull() ?: GameInput.DrawCard

                    val sample = TrainingSample(
                        features = features.toList(),
                        actionHash = chosenMove.hashCode()
                    )

                    if (!firstSample) {
                        writer.write(",")
                    } else {
                        firstSample = false
                    }

                    writer.write(json.encodeToString(sample))
                    totalSampleCount++

                    val nextIndex = (gameState.currentPlayerIndex + 1) % gameState.players.size
                    gameState = gameState.copy(currentPlayerIndex = nextIndex)
                    if (turnCount > 0 && turnCount % gameState.players.size == 0) {
                        gameState = gameState.copy(roundNumber = gameState.roundNumber + 1)
                    }
                    turnCount++
                }

                if ((gameIndex + 1) % 1000 == 0) {
                    println("Simulated ${gameIndex + 1} / $numGames games ($totalSampleCount samples)...")
                }
            }

            writer.write("]")
        }

        val durationSec = (System.currentTimeMillis() - startTime) / 1000
        println("Simulation completed in $durationSec seconds. Total samples: $totalSampleCount")
        Logger.i(tag, "Successfully exported $totalSampleCount training samples to ${outputFile.absolutePath}")
    }
}
