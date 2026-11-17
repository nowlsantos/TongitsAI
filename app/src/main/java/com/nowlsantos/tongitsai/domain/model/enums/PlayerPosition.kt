package com.nowlsantos.tongitsai.domain.model.enums

/**
 * Represents the typical positions of players on a game screen,
 * often relative to the local player. Using cardinal directions for clarity
 * and consistency with animation calculations.
 */
enum class PlayerPosition {
    BOTTOM, RIGHT, LEFT;

    fun next(): PlayerPosition = when(this) {
        BOTTOM -> RIGHT
        RIGHT -> LEFT
        LEFT -> BOTTOM
    }
}