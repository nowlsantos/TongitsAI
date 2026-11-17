package com.nowlsantos.tongitsai.domain.model.enums

enum class PlayerType {
    HUMAN,
    AI
}

enum class ChallengerType {
    CALLER,
    CALLEE,
    LAST_CALLEE,
    FOLDEE,
    LAST_FOLDEE,
    NONE
}

enum class PaymentMode {
    NONE,
    NORMAL,
    CHALLENGE,
    TONGITS
}