package com.nowlsantos.tongitsai.domain.fsm.player

enum class PlayerPhase {
    IDLE,
    READY,
    CALL,
    GET,
    CHOW,
    MELD,
    SAPAW,
    DISCARD
}