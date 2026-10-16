package com.buhito.alarm.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.buhito.alarm.ui.mascots.OwlMascot
import com.buhito.alarm.ui.mascots.OwlMood
import com.buhito.alarm.ui.theme.BuhitoColors
import kotlin.random.Random

private class Confetti(
    val x: Float,
    val delay: Float,
    val drift: Float,
    val size: Float,
    val spin: Float,
    val color: Color,
)

/** Pantalla de festejo al guardar una alarma: el búho contento y una lluvia de confeti. */
@Composable
fun SavedCelebration(title: String, body: String, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(1500, easing = LinearEasing)) }

    val confetti = remember {
        val palette = listOf(
            BuhitoColors.Moon,
            BuhitoColors.Amber,
            Color(0xFFFF9FB5),
            Color.White,
            Color(0xFF7BE0B0),
        )
        val random = Random(11)
        List(36) {
            Confetti(
                x = random.nextFloat(),
                delay = random.nextFloat() * 0.35f,
                drift = (random.nextFloat() - 0.5f) * 0.25f,
                size = 0.6f + random.nextFloat() * 0.9f,
                spin = random.nextFloat() * 360f,
                color = palette[random.nextInt(palette.size)],
            )
        }
    }

    Box(
        modifier
            .fillMaxSize()
            .alpha((progress.value / 0.12f).coerceIn(0f, 1f))
            .background(BuhitoColors.Night0.copy(alpha = 0.94f))
            // bloquea toques mientras dura el festejo
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            for (c in confetti) {
                val t = ((progress.value - c.delay) / (1f - c.delay)).coerceIn(0f, 1f)
                if (t <= 0f) continue
                val x = (c.x + c.drift * t) * size.width
                val y = -20.dp.toPx() + (size.height * 0.95f) * t
                val w = 8.dp.toPx() * c.size
                val h = 14.dp.toPx() * c.size
                rotate(c.spin + 540f * t, pivot = Offset(x, y)) {
                    drawRect(
                        color = c.color.copy(alpha = 1f - 0.5f * t),
                        topLeft = Offset(x - w / 2f, y - h / 2f),
                        size = Size(w, h),
                    )
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            OwlMascot(Modifier.size(180.dp), mood = OwlMood.Happy)
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, start = 24.dp, end = 24.dp),
            )
        }
    }
}
