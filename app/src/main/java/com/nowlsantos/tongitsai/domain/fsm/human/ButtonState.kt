package com.nowlsantos.tongitsai.domain.fsm.human

data class ButtonState(
    val buttons: Map<ButtonType, Boolean>,
    val isActionPhase: Boolean = false
) {
    companion object {
        val ALL_DISABLED = ButtonState(
            buttons = mapOf(
                ButtonType.SWAP to false,
                ButtonType.SORT_RANK to false,
                ButtonType.SORT_SUIT to false,
                ButtonType.CHOW to false,
                ButtonType.DRAW to false,
                ButtonType.DISCARD to false,
                ButtonType.MELD to false,
                ButtonType.SAPAW to false,
                ButtonType.CALL to false,
            ),
            isActionPhase = false
        )
    }
}