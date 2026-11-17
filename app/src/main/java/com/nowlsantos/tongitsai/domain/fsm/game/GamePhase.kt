package com.nowlsantos.tongitsai.domain.fsm.game

enum class GamePhase {
    SETUP,
    DEALING,
    PLAYING,
    CALL_CHALLENGE,
    REVEAL,
    ROUND_END,
    GAME_OVER
}

enum class RevealType {
    TONGITS,
    NORMAL,
    CHALLENGE,
    ALL_FOLDS,
    NONE
}