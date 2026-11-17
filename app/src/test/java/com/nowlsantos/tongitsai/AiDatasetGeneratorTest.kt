package com.nowlsantos.tongitsai

import com.nowlsantos.tongitsai.domain.ai.AiDatasetGenerator
import com.nowlsantos.tongitsai.domain.ai.AiMinimax
import com.nowlsantos.tongitsai.domain.ai.GameStateEncoder
import org.junit.Test
import java.io.File

class AiDatasetGeneratorTest {

    @Test
    fun generateDataset() {
        val aiMinimax = AiMinimax()
        val encoder = GameStateEncoder()
        val generator = AiDatasetGenerator(aiMinimax, encoder)

        val outputFile = File("training_dataset.json")
        generator.simulateAndExport(outputFile, numGames = 50000)
    }
}
