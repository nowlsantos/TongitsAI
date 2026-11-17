package com.nowlsantos.tongitsai.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.File

class GameRepository(
    private val context: Context
) {

    private val json = Json { prettyPrint = true } // For a human-readable file

    init {
        // Purge old saved score tally data on fresh debug launches so every run starts clean
        try {
            val file = File(context.filesDir, "game_results.json")
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {}
    }

    suspend fun saveGameData(gameData: GameData) = withContext(Dispatchers.IO) {
        val jsonString = json.encodeToString(gameData)
        val file = File(context.filesDir, "game_results.json")
        file.writeText(jsonString)
    }

    suspend fun loadGameData(): GameData? = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, "game_results.json")
        if (!file.exists()) return@withContext null

        val jsonString = file.readText()
        return@withContext json.decodeFromString<GameData>(jsonString)
    }
}