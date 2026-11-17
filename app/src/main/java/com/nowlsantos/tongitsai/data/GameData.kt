package com.nowlsantos.tongitsai.data

import kotlinx.serialization.Serializable

@Serializable
data class GameData(
    val playerSummaries: Map<String, PlayerSummary>,
    val roundResults: List<Map<String, String>>
)

@Serializable
data class PlayerSummary(
    val balance: Int, // Changed to Int for calculation
    val score: Int,
    val winnings: Int? = null // This will be calculated and is optional
)
/*
@Serializable
data class PlayerRoundResult(
    val playerName: String,
    val outcome: String // "W" or "L"
)

@Serializable
data class RoundResult(
    val round: Int,
    val playerOutcomes: List<PlayerRoundResult>
)

@Serializable
data class PlayerSummary(
    val playerName: String,
    val balance: String,
    val winnings: Int
)

@Serializable
data class GameResults(
    val roundResults: List<RoundResult>,
    val playerSummaries: List<PlayerSummary>
)*/
