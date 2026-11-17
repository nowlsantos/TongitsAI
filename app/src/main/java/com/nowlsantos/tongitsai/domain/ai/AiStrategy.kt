package com.nowlsantos.tongitsai.domain.ai

import com.nowlsantos.tongitsai.domain.fsm.player.PlayerStateMachine
import com.nowlsantos.tongitsai.domain.model.GameState
import com.nowlsantos.tongitsai.domain.model.Meld
import com.nowlsantos.tongitsai.domain.model.Player
import com.nowlsantos.tongitsai.domain.model.PlayingCard
import com.nowlsantos.tongitsai.presentation.GameInput

interface AiStrategy {
    suspend fun chooseMove(
        gameState: GameState,
        player: Player,
        playerStateMachine: PlayerStateMachine
    ): GameInput?
}