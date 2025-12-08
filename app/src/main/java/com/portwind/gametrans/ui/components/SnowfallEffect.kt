package com.portwind.gametrans.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.random.Random

data class Snowflake(
    var x: Float,
    var y: Float,
    val radius: Float,
    val speed: Float,
    val alpha: Float
)

@Composable
fun SnowfallEffect() {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp.value * configuration.densityDpi / 160f
    val screenHeight = configuration.screenHeightDp.dp.value * configuration.densityDpi / 160f

    val snowflakes = remember { mutableStateListOf<Snowflake>() }

    // Initialize snowflakes
    LaunchedEffect(Unit) {
        for (i in 0..50) {
            snowflakes.add(
                Snowflake(
                    x = Random.nextFloat() * screenWidth,
                    y = Random.nextFloat() * screenHeight,
                    radius = Random.nextFloat() * 5f + 2f,
                    speed = Random.nextFloat() * 2f + 1f,
                    alpha = Random.nextFloat() * 0.5f + 0.3f
                )
            )
        }
        
        while (true) {
            // Update positions
            for (snowflake in snowflakes) {
                snowflake.y += snowflake.speed
                if (snowflake.y > screenHeight) {
                    snowflake.y = -10f
                    snowflake.x = Random.nextFloat() * screenWidth
                }
            }
            delay(16L) // ~60 FPS
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        for (snowflake in snowflakes) {
            drawCircle(
                color = Color.White.copy(alpha = snowflake.alpha),
                radius = snowflake.radius,
                center = Offset(snowflake.x, snowflake.y)
            )
        }
    }
}
