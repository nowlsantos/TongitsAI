package com.nowlsantos.tongitsai.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

data class ConfettiParticle(
    var normX: Float, // 0.0 to 1.0
    var normY: Float, // 0.0 to 1.0
    val color: Color,
    val size: Float,
    val speedY: Float, // fraction of height per frame
    val speedX: Float  // fraction of width per frame
)

@Composable
fun ConfettiView(
    modifier: Modifier = Modifier
) {
    val colors = listOf(
        Color.Red,
        Color.Green,
        Color.Blue,
        Color.Yellow,
        Color.Magenta,
        Color.Cyan,
        Color.White,
        Color(0xFFFFD700), // Gold
        Color(0xFFFF69B4)  // Hot Pink
    )

    val particles = remember {
        mutableStateListOf<ConfettiParticle>().apply {
            repeat(120) {
                add(
                    ConfettiParticle(
                        normX = Random.nextFloat(),
                        normY = Random.nextFloat(), // Distributed across full screen height
                        color = colors.random(),
                        size = Random.nextFloat() * 14f + 6f,
                        speedY = Random.nextFloat() * 0.012f + 0.004f, // Fall speed
                        speedX = Random.nextFloat() * 0.004f - 0.002f  // Drift speed
                    )
                )
            }
        }
    }

    var tick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(16.milliseconds) // ~60fps loop
            tick++
            particles.forEach { p ->
                p.normY += p.speedY
                p.normX += p.speedX
                if (p.normY > 1.05f) {
                    p.normY = -0.05f
                    p.normX = Random.nextFloat()
                }
                if (p.normX < 0f) p.normX = 1f
                if (p.normX > 1f) p.normX = 0f
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        tick // Read state to force canvas redraw on every frame tick
        val width = size.width
        val height = size.height
        particles.forEach { p ->
            drawCircle(
                color = p.color,
                radius = p.size,
                center = Offset(p.normX * width, p.normY * height)
            )
        }
    }
}
