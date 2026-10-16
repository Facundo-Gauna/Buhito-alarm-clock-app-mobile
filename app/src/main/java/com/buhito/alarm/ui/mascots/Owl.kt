package com.buhito.alarm.ui.mascots

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import com.buhito.alarm.ui.theme.BuhitoColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Estados del búho:
 *  - Calm: parpadea, mira de lado a lado y se mece.
 *  - Ringing: se agita y aletea (alarma sonando).
 *  - Listening: inclina la cabeza y emite ondas (grabando tu voz).
 *  - Happy: ojos felices y alas arriba (celebración).
 *  - Sleepy: ojos cerrados y "z" flotando (no hay alarmas activas).
 */
enum class OwlMood { Calm, Ringing, Listening, Happy, Sleepy }

private val Body = Color(0xFF3B4C9B)
private val BodyDark = Color(0xFF2C3A7C)
private val FaceDisc = Color(0xFF5367BA)
private val Belly = Color(0xFF9DB4F2)
private val BellyLine = Color(0xFF7C95E0)
private val EyeWhite = Color(0xFFF4F7FF)
private val Pupil = Color(0xFF0B1230)
private val Beak = Color(0xFFFFC76B)
private val Feet = Color(0xFFE8A94B)
private val Blush = Color(0xFFFF9FB5)

private fun OwlMood.bobMs() = when (this) {
    OwlMood.Calm -> 1800
    OwlMood.Ringing -> 200
    OwlMood.Listening -> 1400
    OwlMood.Happy -> 420
    OwlMood.Sleepy -> 3200
}

private fun OwlMood.bobAmp() = when (this) {
    OwlMood.Sleepy -> 2f
    OwlMood.Happy -> 5f
    else -> 3f
}

private fun OwlMood.lookMs() = when (this) {
    OwlMood.Ringing -> 260
    OwlMood.Sleepy -> 6000
    else -> 3200
}

private fun OwlMood.flapMs() = when (this) {
    OwlMood.Ringing -> 170
    OwlMood.Happy -> 300
    else -> 1800
}

private fun OwlMood.wingAmp() = when (this) {
    OwlMood.Ringing -> 20f
    OwlMood.Happy -> 26f
    OwlMood.Listening -> 6f
    OwlMood.Sleepy -> 1.5f
    OwlMood.Calm -> 3f
}

/**
 * Búho dibujado con Canvas (sin imágenes).
 *
 * @param level  volumen de la voz (0..1), solo se usa en [OwlMood.Listening].
 * @param onClick si no es null, el búho reacciona al toque (salta y se pone contento).
 */
@Composable
fun OwlMascot(
    modifier: Modifier = Modifier,
    mood: OwlMood = OwlMood.Calm,
    level: Float = 0f,
    onClick: (() -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    val jump = remember { Animatable(0f) }
    var cheering by remember { mutableStateOf(false) }
    val shown = if (cheering) OwlMood.Happy else mood

    val transition = rememberInfiniteTransition(label = "owl")
    val blink by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 4200
                1f at 0
                1f at 3800
                0.08f at 3950
                1f at 4100
            },
        ),
        label = "blink",
    )
    val bobAmp = shown.bobAmp()
    val bob by transition.animateFloat(
        initialValue = -bobAmp,
        targetValue = bobAmp,
        animationSpec = infiniteRepeatable(tween(shown.bobMs(), easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bob",
    )
    val look by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(shown.lookMs(), easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "look",
    )
    val flap by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(shown.flapMs(), easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "flap",
    )
    val tilt by transition.animateFloat(
        initialValue = -7f,
        targetValue = 7f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "tilt",
    )
    val wave by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart),
        label = "wave",
    )

    val interaction = remember { MutableInteractionSource() }
    val clickModifier = if (onClick == null) {
        Modifier
    } else {
        Modifier.clickable(interactionSource = interaction, indication = null) {
            scope.launch {
                cheering = true
                jump.animateTo(-26f, tween(150, easing = FastOutSlowInEasing))
                jump.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
                delay(1100)
                cheering = false
            }
            onClick()
        }
    }

    Box(modifier.aspectRatio(1f).then(clickModifier)) {
        Canvas(Modifier.matchParentSize()) {
            val unit = size.minDimension / 200f
            scale(unit, unit, pivot = Offset.Zero) {
                translate(0f, bob + jump.value) {
                    rotate(if (shown == OwlMood.Listening) tilt else 0f, pivot = Offset(100f, 190f)) {
                        drawOwl(
                            mood = shown,
                            blink = blink,
                            look = look,
                            wingAngle = shown.wingAmp() * flap,
                            level = level.coerceIn(0f, 1f),
                            wave = wave,
                        )
                    }
                }
            }
        }
        if (shown == OwlMood.Sleepy) {
            FloatingGlyph("z", BuhitoColors.Moon, 0, 11, Modifier.align(Alignment.TopEnd).offset(x = (-10).dp, y = 4.dp))
            FloatingGlyph("z", BuhitoColors.Moon, 1000, 15, Modifier.align(Alignment.TopEnd).offset(x = 2.dp, y = 0.dp))
        }
    }
}

internal fun DrawScope.drawOwl(
    mood: OwlMood,
    blink: Float,
    look: Float,
    wingAngle: Float,
    level: Float,
    wave: Float,
) {
    // ondas de sonido (solo al escuchar)
    if (mood == OwlMood.Listening) {
        for (i in 0..1) {
            val p = (wave + i * 0.5f) % 1f
            val r = 54f + 24f * p
            val color = BuhitoColors.Moon.copy(alpha = 0.55f * (1f - p))
            val stroke = Stroke(width = 3f, cap = StrokeCap.Round)
            drawArc(color, -40f, 80f, false, Offset(100f - r, 92f - r), Size(r * 2f, r * 2f), style = stroke)
            drawArc(color, 140f, 80f, false, Offset(100f - r, 92f - r), Size(r * 2f, r * 2f), style = stroke)
        }
    }

    // patas
    drawOval(Feet, Offset(70f, 180f), Size(24f, 11f))
    drawOval(Feet, Offset(106f, 180f), Size(24f, 11f))

    // alas (detrás del cuerpo)
    rotate(-wingAngle, pivot = Offset(46f, 112f)) {
        drawOval(BodyDark, Offset(28f, 96f), Size(36f, 76f))
    }
    rotate(wingAngle, pivot = Offset(154f, 112f)) {
        drawOval(BodyDark, Offset(136f, 96f), Size(36f, 76f))
    }

    // plumas de las orejas
    val leftEar = Path().apply {
        moveTo(48f, 74f); lineTo(54f, 26f); lineTo(92f, 54f); close()
    }
    val rightEar = Path().apply {
        moveTo(152f, 74f); lineTo(146f, 26f); lineTo(108f, 54f); close()
    }
    drawPath(leftEar, Body)
    drawPath(rightEar, Body)

    // cuerpo + panza
    drawOval(Body, Offset(38f, 40f), Size(124f, 146f))
    drawOval(Belly, Offset(64f, 112f), Size(72f, 70f))
    for (row in 0..2) {
        for (col in 0..1) {
            drawArc(
                color = BellyLine,
                startAngle = 20f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(78f + col * 24f, 128f + row * 16f),
                size = Size(20f, 14f),
                style = Stroke(width = 3f, cap = StrokeCap.Round),
            )
        }
    }

    // cara
    drawCircle(FaceDisc, radius = 31f, center = Offset(76f, 92f))
    drawCircle(FaceDisc, radius = 31f, center = Offset(124f, 92f))

    // ojos
    val eyeStroke = Stroke(width = 4.5f, cap = StrokeCap.Round)
    for (cx in floatArrayOf(76f, 124f)) {
        val cy = 92f
        when (mood) {
            // dormido: arco hacia abajo (ojo cerrado)
            OwlMood.Sleepy ->
                drawArc(Pupil, 10f, 160f, false, Offset(cx - 14f, cy - 8f), Size(28f, 16f), style = eyeStroke)
            // contento: arco hacia arriba (^ ^)
            OwlMood.Happy ->
                drawArc(Pupil, 190f, 160f, false, Offset(cx - 14f, cy - 4f), Size(28f, 16f), style = eyeStroke)
            else -> {
                val ry = 22f * blink
                drawOval(EyeWhite, Offset(cx - 22f, cy - ry), Size(44f, ry * 2f))
                val px = cx + look * 4f
                val pw = 11f + if (mood == OwlMood.Listening) 2f + 3f * level else 0f
                val ph = pw * blink
                drawOval(Pupil, Offset(px - pw, cy - ph), Size(pw * 2f, ph * 2f))
                if (blink > 0.5f) drawCircle(EyeWhite, radius = 3.5f, center = Offset(px - 4f, cy - 4f))
            }
        }
    }

    // pico y mejillas
    val beak = Path().apply {
        moveTo(91f, 104f); lineTo(109f, 104f); lineTo(100f, 124f); close()
    }
    drawPath(beak, Beak)
    val blush = if (mood == OwlMood.Happy) 0.65f else 0.35f
    drawCircle(Blush.copy(alpha = blush), radius = 7f, center = Offset(58f, 116f))
    drawCircle(Blush.copy(alpha = blush), radius = 7f, center = Offset(142f, 116f))
}
