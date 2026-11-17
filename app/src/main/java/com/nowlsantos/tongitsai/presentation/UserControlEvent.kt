package com.nowlsantos.tongitsai.presentation

sealed class UserControlEvent {
    object Get : UserControlEvent()
    object Meld : UserControlEvent()
    object Chow : UserControlEvent()
    object Discard : UserControlEvent()
    object Sapaw : UserControlEvent()
    object Call : UserControlEvent()
    object Swap: UserControlEvent()
    object SortSuit : UserControlEvent()
    object SortRank : UserControlEvent()
}