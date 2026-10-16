package com.buhito.alarm.ui.mascots

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
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
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.unit.dp
import com.buhito.alarm.R
import com.buhito.alarm.ui.SpeechBubble
import com.buhito.alarm.ui.theme.BuhitoColors
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

private val Fur = Color(0xFF36457F)
private val FurLight = Color(0xFF4A5AA0)
private val Cream = Color(0xFFDDE6FF)
private val Pupil = Color(0xFF0B1230)
private val Pink = Color(0xFFFF9FB5)
private val Dial = Color(0xFF0F1838)

/**
 * Gatito que se asoma sobre un cronómetro. Si hay un cronómetro corriendo, sus ojitos siguen
 * la aguja, mueve la cola rápido y las orejas; si no, se duerme. Al tocarlo se pone contento.
 */
@Composable
fun StopwatchCat(running: Boolean, modifier: Modifier = Modifier) {
    val lines = stringArrayResource(R.array.sw_cat_lines)
    var line by remember { mutableStateOf("") }
    var pokes by remember { mutableIntStateOf(0) }
    var cheering by remember { mutableStateOf(false) }

    LaunchedEffect(pokes) {
        if (pokes > 0) {
            cheering = true
            delay(2600)
            cheering = false
        }
    }

    // La aguja da una vuelta cada 12 s mientras algo corre (si no, queda quieta).
    var hand by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(running) {
        if (running) {
            var last = withFrameNanos { it }
            while (true) {
                val now = withFrameNanos { it }
                hand = (hand + (now - last) / 12_000_000_000f) % 1f
                last = now
            }
        }
    }

    val transition = rememberInfiniteTransition(label = "swcat")
    val tail by transition.animateFloat(
        initialValue = if (running) -16f else -6f,
        targetValue = if (running) 18f else 8f,
        animationSpec = infiniteRepeatable(
            tween(if (running) 380 else 2600, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
        ),
        label = "tail",
    )
    val bounce by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (cheering) -5f else if (running) -1.5f else 0f,
        animationSpec = infiniteRepeatable(tween(if (cheering) 260 else 500), RepeatMode.Reverse),
        label = "bounce",
    )

    Box(
        modifier
            .aspectRatio(200f / 160f)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                line = lines.random()
                pokes++
            },
    ) {
        Canvas(Modifier.matchParentSize()) {
            val unit = size.width / 200f
            scale(unit, unit, pivot = Offset.Zero) {
                drawStopwatchCat(
                    awake = running || cheering,
                    happy = cheering,
                    hand = hand,
                    tail = tail,
                    headLift = bounce,
                )
            }
        }
        if (cheering) {
            FloatingGlyph("♥", Pink, 0, 16, Modifier.align(Alignment.TopEnd).offset(x = (-14).dp, y = 8.dp))
        } else if (!running) {
            FloatingGlyph("z", BuhitoColors.Moon, 0, 14, Modifier.align(Alignment.TopEnd).offset(x = (-14).dp, y = 8.dp))
            FloatingGlyph("z", BuhitoColors.Moon, 900, 18, Modifier.align(Alignment.TopEnd).offset(x = 2.dp, y = 2.dp))
        }
        AnimatedVisibility(
            visible = cheering,
            modifier = Modifier.align(Alignment.TopStart),
            enter = fadeIn(tween(160)) + scaleIn(tween(200), initialScale = 0.7f),
            exit = fadeOut(tween(250)),
        ) {
            SpeechBubble(line)
        }
    }
}

private fun DrawScope.drawStopwatchCat(awake: Boolean, happy: Boolean, hand: Float, tail: Float, headLift: Float) {
    // cola (asoma detrás del cronómetro)
    rotate(tail, pivot = Offset(146f, 128f)) {
        val path = Path().apply {
            moveTo(146f, 128f)
            cubicTo(184f, 132f, 192f, 96f, 172f, 84f)
        }
        drawPath(path, Fur, style = Stroke(width = 13f, cap = StrokeCap.Round))
    }

    // cabeza + orejas (suben y bajan un poquito)
    val hy = headLift
    val leftEar = Path().apply { moveTo(70f, 46f + hy); lineTo(72f, 14f + hy); lineTo(95f, 30f + hy); close() }
    val rightEar = Path().apply { moveTo(130f, 46f + hy); lineTo(128f, 14f + hy); lineTo(105f, 30f + hy); close() }
    drawPath(leftEar, Fur)
    drawPath(rightEar, Fur)
    drawPath(
        Path().apply { moveTo(75f, 40f + hy); lineTo(76f, 22f + hy); lineTo(88f, 32f + hy); close() },
        Pink.copy(alpha = 0.55f),
    )
    drawPath(
        Path().apply { moveTo(125f, 40f + hy); lineTo(124f, 22f + hy); lineTo(112f, 32f + hy); close() },
        Pink.copy(alpha = 0.55f),
    )
    drawCircle(Fur, radius = 34f, center = Offset(100f, 60f + hy))

    // ojos
    val eyeY = 60f + hy
    if (happy || !awake) {
        // contento (^ ^) o dormido (u u)
        for (x in floatArrayOf(86f, 114f)) {
            drawArc(
                color = Cream,
                startAngle = if (happy) 180f else 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(x - 7f, eyeY - if (happy) 3f else 5f),
                size = Size(14f, 10f),
                style = Stroke(width = 3f, cap = StrokeCap.Round),
            )
        }
    } else {
        // despierto: ojos redondos que siguen la aguja
        val angle = hand * 2f * Math.PI.toFloat() - Math.PI.toFloat() / 2f
        val lx = cos(angle) * 2.4f
        val ly = sin(angle) * 2.4f
        for (x in floatArrayOf(86f, 114f)) {
            drawCircle(Cream, radius = 8f, center = Offset(x, eyeY))
            drawCircle(Pupil, radius = 5f, center = Offset(x + lx, eyeY + ly))
            drawCircle(Color.White, radius = 1.8f, center = Offset(x + lx - 1.5f, eyeY + ly - 1.8f))
        }
    }
    // cachetes, nariz y sonrisa
    drawOval(Pink.copy(alpha = 0.45f), Offset(70f, 66f + hy), Size(10f, 6f))
    drawOval(Pink.copy(alpha = 0.45f), Offset(120f, 66f + hy), Size(10f, 6f))
    drawPath(
        Path().apply { moveTo(96f, 68f + hy); lineTo(104f, 68f + hy); lineTo(100f, 73f + hy); close() },
        Pink,
    )
    val mouth = Stroke(width = 1.8f, cap = StrokeCap.Round)
    drawArc(Cream, 0f, 180f, false, Offset(94f, 73f + hy), Size(6f, 5f), style = mouth)
    drawArc(Cream, 0f, 180f, false, Offset(100f, 73f + hy), Size(6f, 5f), style = mouth)
    val whisker = Cream.copy(alpha = 0.5f)
    drawLine(whisker, Offset(76f, 70f + hy), Offset(56f, 66f + hy), strokeWidth = 1.6f, cap = StrokeCap.Round)
    drawLine(whisker, Offset(76f, 74f + hy), Offset(56f, 77f + hy), strokeWidth = 1.6f, cap = StrokeCap.Round)
    drawLine(whisker, Offset(124f, 70f + hy), Offset(144f, 66f + hy), strokeWidth = 1.6f, cap = StrokeCap.Round)
    drawLine(whisker, Offset(124f, 74f + hy), Offset(144f, 77f + hy), strokeWidth = 1.6f, cap = StrokeCap.Round)

    // cronómetro (tapa la parte de abajo de la cabeza: el gato "se asoma")
    val c = Offset(100f, 118f)
    drawCircle(BuhitoColors.Amber, radius = 7f, center = Offset(134f, 84f))  // botoncito
    drawCircle(BuhitoColors.Moon, radius = 40f, center = c)                 // aro
    drawCircle(Dial, radius = 35f, center = c)                              // esfera
    for (i in 0 until 12) {
        val a = i * (Math.PI.toFloat() / 6f)
        val inner = if (i % 3 == 0) 27f else 30f
        drawLine(
            BuhitoColors.InkDim,
            Offset(c.x + cos(a) * inner, c.y + sin(a) * inner),
            Offset(c.x + cos(a) * 33f, c.y + sin(a) * 33f),
            strokeWidth = if (i % 3 == 0) 2.4f else 1.4f,
            cap = StrokeCap.Round,
        )
    }
    val a = hand * 2f * Math.PI.toFloat() - Math.PI.toFloat() / 2f
    drawLine(
        BuhitoColors.Amber,
        c,
        Offset(c.x + cos(a) * 26f, c.y + sin(a) * 26f),
        strokeWidth = 3f,
        cap = StrokeCap.Round,
    )
    drawCircle(BuhitoColors.Amber, radius = 3.5f, center = c)

    // patitas sobre el aro
    drawOval(FurLight, Offset(64f, 90f), Size(24f, 13f))
    drawOval(FurLight, Offset(112f, 90f), Size(24f, 13f))
}
