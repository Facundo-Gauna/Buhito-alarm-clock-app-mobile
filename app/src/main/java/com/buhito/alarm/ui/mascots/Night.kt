package com.buhito.alarm.ui.mascots

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.util.lerp
import com.buhito.alarm.ui.theme.BuhitoColors
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private data class Star(val x: Float, val y: Float, val radius: Float, val phase: Float)

/** Cielo nocturno: degradé azul oscuro con estrellas que titilan suavemente. */
@Composable
fun NightBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(BuhitoColors.Night0, BuhitoColors.Night1, BuhitoColors.Night2),
                ),
            ),
    ) {
        Stars(Modifier.fillMaxSize())
        content()
    }
}

@Composable
private fun Stars(modifier: Modifier = Modifier, count: Int = 48) {
    val stars = remember {
        val random = Random(7)
        List(count) {
            Star(random.nextFloat(), random.nextFloat(), 0.7f + random.nextFloat() * 1.6f, random.nextFloat())
        }
    }
    val transition = rememberInfiniteTransition(label = "stars")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(5000, easing = LinearEasing), RepeatMode.Restart),
        label = "starTime",
    )
    // Una estrella fugaz cruza el cielo cada ~11 segundos.
    val meteor by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(11000, easing = LinearEasing), RepeatMode.Restart),
        label = "meteor",
    )
    Canvas(modifier) {
        for (s in stars) {
            val twinkle = 0.5f + 0.5f * sin((time + s.phase) * 2f * PI.toFloat())
            drawCircle(
                color = Color.White.copy(alpha = 0.15f + 0.65f * twinkle),
                radius = s.radius * density,
                center = Offset(s.x * size.width, s.y * size.height),
            )
        }
        if (meteor < 0.1f) {
            val t = meteor / 0.1f
            val head = Offset(lerp(0.92f, 0.52f, t) * size.width, lerp(0.05f, 0.28f, t) * size.height)
            val tail = Offset(head.x + 0.14f * size.width, head.y - 0.08f * size.height)
            val alpha = sin(PI.toFloat() * t)
            drawLine(
                color = Color.White.copy(alpha = 0.75f * alpha),
                start = tail,
                end = head,
                strokeWidth = 2f * density,
                cap = StrokeCap.Round,
            )
            drawCircle(Color.White.copy(alpha = alpha), radius = 2.4f * density, center = head)
        }
    }
}
