package com.buhito.alarm.ui.mascots

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buhito.alarm.R
import com.buhito.alarm.ui.SpeechBubble
import com.buhito.alarm.ui.theme.BuhitoColors
import kotlinx.coroutines.delay

private val Fur = Color(0xFF36457F)
private val FurDark = Color(0xFF28345F)
private val FurLight = Color(0xFF4A5AA0)
private val EyeLine = Color(0xFFDDE6FF)
private val Pupil = Color(0xFF0B1230)
private val Pink = Color(0xFFFF9FB5)

/**
 * Gatito dormido: respira, mueve la cola y le salen "z" flotando.
 * Al tocarlo se despierta unos segundos: abre los ojos, mueve las orejas, ronronea
 * (corazoncitos) y dice algo; después vuelve a dormirse.
 */
@Composable
fun CatMascot(modifier: Modifier = Modifier) {
    val lines = stringArrayResource(R.array.cat_lines)
    var awake by remember { mutableStateOf(false) }
    var line by remember { mutableStateOf("") }
    var pokes by remember { mutableIntStateOf(0) }
    var happy by remember { mutableStateOf(false) } // ojitos felices ^ ^ mientras ronronea

    LaunchedEffect(pokes) {
        if (pokes > 0) {
            happy = false
            awake = true
            delay(1500)   // primero abre los ojitos redondos con calma...
            happy = true  // ...y después sonríe con los ojos cerrados (^ ^)
            delay(2300)
            awake = false
            happy = false
        }
    }
    // Los ojos se abren despacito (sin pestañeo brusco) y la cabecita se inclina con ternura.
    val eyeOpen by animateFloatAsState(
        targetValue = if (awake) 1f else 0f,
        animationSpec = tween(650, easing = FastOutSlowInEasing),
        label = "eyeOpen",
    )
    val tilt by animateFloatAsState(
        targetValue = if (awake) -7f else 0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 120f),
        label = "tilt",
    )

    val transition = rememberInfiniteTransition(label = "cat")
    val breath by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            tween(if (awake) 600 else 2200, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
        ),
        label = "breath",
    )
    val tail by transition.animateFloat(
        initialValue = if (awake) -14f else -7f,
        targetValue = if (awake) 16f else 9f,
        animationSpec = infiniteRepeatable(
            tween(if (awake) 700 else 2800, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
        ),
        label = "tail",
    )
    val twitch by transition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 1600
                0f at 0
                0f at 1000
                1f at 1100
                0f at 1250
                1f at 1350
                0f at 1500
            },
        ),
        label = "twitch",
    )

    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .aspectRatio(240f / 160f)
            .clickable(interactionSource = interaction, indication = null) {
                line = lines.random()
                pokes++
            },
    ) {
        Canvas(Modifier.matchParentSize()) {
            val unit = size.width / 240f
            scale(unit, unit, pivot = Offset.Zero) {
                drawCat(
                    breath = breath,
                    tailAngle = tail,
                    eyeOpen = eyeOpen,
                    happy = happy,
                    tilt = tilt,
                    earTwitch = if (awake) twitch else 0f,
                )
            }
        }
        if (awake) {
            FloatingGlyph("♥", Pink, 0, 16, Modifier.align(Alignment.TopEnd).offset(x = (-26).dp, y = 6.dp))
            FloatingGlyph("♥", Pink, 700, 12, Modifier.align(Alignment.TopEnd).offset(x = (-6).dp, y = 10.dp))
        } else {
            FloatingGlyph("z", BuhitoColors.Moon, 0, 14, Modifier.align(Alignment.TopEnd).offset(x = (-28).dp, y = 6.dp))
            FloatingGlyph("z", BuhitoColors.Moon, 900, 18, Modifier.align(Alignment.TopEnd).offset(x = (-8).dp, y = 0.dp))
        }
        AnimatedVisibility(
            visible = awake,
            modifier = Modifier.align(Alignment.TopCenter),
            enter = fadeIn(tween(160)) + scaleIn(tween(200), initialScale = 0.7f),
            exit = fadeOut(tween(250)),
        ) {
            SpeechBubble(line)
        }
    }
}

/** Letra o símbolo que sube flotando y se desvanece (las "z" del sueño, corazoncitos...). */
@Composable
internal fun FloatingGlyph(
    glyph: String,
    color: Color,
    delayMillis: Int,
    size: Int,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "glyph")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 2700 + delayMillis
                0f at 0 using LinearEasing
                0f at delayMillis using LinearEasing
                1f at (2700 + delayMillis) using LinearEasing
            },
        ),
        label = "glyphProgress",
    )
    Text(
        text = glyph,
        color = color,
        fontSize = size.sp,
        modifier = modifier.graphicsLayer {
            translationY = -34.dp.toPx() * progress
            translationX = 8.dp.toPx() * progress
            alpha = (if (progress < 0.2f) progress / 0.2f else (1f - progress) / 0.8f).coerceIn(0f, 1f)
        },
    )
}

private fun DrawScope.drawCat(
    breath: Float,
    tailAngle: Float,
    eyeOpen: Float,
    happy: Boolean,
    tilt: Float,
    earTwitch: Float,
) {
    // cola
    rotate(tailAngle, pivot = Offset(52f, 118f)) {
        val tail = Path().apply {
            moveTo(52f, 118f)
            cubicTo(16f, 122f, 8f, 84f, 32f, 72f)
        }
        drawPath(tail, Fur, style = Stroke(width = 14f, cap = StrokeCap.Round))
    }

    // cuerpo (respira)
    scale(1f, breath, pivot = Offset(115f, 142f)) {
        drawOval(Fur, Offset(40f, 72f), Size(150f, 70f))
        for (i in 0..2) {
            val x = 78f + i * 24f
            drawLine(FurDark, Offset(x, 78f), Offset(x + 6f, 94f), strokeWidth = 6f, cap = StrokeCap.Round)
        }
    }

    // patitas
    drawOval(FurLight, Offset(146f, 128f), Size(34f, 15f))
    drawOval(FurLight, Offset(112f, 130f), Size(30f, 13f))

    // cabeza (todo lo de acá se inclina junto)
    rotate(tilt, pivot = Offset(176f, 130f)) {
        // orejas (la izquierda se mueve cuando está despierto)
        rotate(-earTwitch * 12f, pivot = Offset(160f, 80f)) {
            val leftEar = Path().apply { moveTo(146f, 86f); lineTo(152f, 52f); lineTo(174f, 74f); close() }
            drawPath(leftEar, Fur)
            val leftInner = Path().apply { moveTo(152f, 80f); lineTo(155f, 62f); lineTo(167f, 75f); close() }
            drawPath(leftInner, Pink.copy(alpha = 0.55f))
        }
        val rightEar = Path().apply { moveTo(206f, 86f); lineTo(200f, 52f); lineTo(178f, 74f); close() }
        drawPath(rightEar, Fur)
        val rightInner = Path().apply { moveTo(200f, 80f); lineTo(197f, 62f); lineTo(185f, 75f); close() }
        drawPath(rightInner, Pink.copy(alpha = 0.55f))

        // cabeza
        drawCircle(Fur, radius = 34f, center = Offset(176f, 104f))

        // ojos
        if (eyeOpen < 0.1f || happy) {
            // dormido: curvita hacia abajo (u). Feliz: curvita hacia arriba (^).
            val startAngle = if (happy) 180f else 0f
            for (x in floatArrayOf(158f, 182f)) {
                drawArc(
                    color = EyeLine,
                    startAngle = startAngle,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(x, if (happy) 99f else 98f),
                    size = Size(14f, 10f),
                    style = Stroke(width = 3f, cap = StrokeCap.Round),
                )
            }
        } else {
            // ojos grandes, redondos y brillantes: se "inflan" despacito al abrirse
            for (x in floatArrayOf(165f, 189f)) {
                val h = 15f * eyeOpen
                drawOval(Pupil, Offset(x - 7.5f, 102f - h / 2), Size(15f, h))
                if (eyeOpen > 0.6f) {
                    drawCircle(Color.White, radius = 2.8f, center = Offset(x - 2f, 99.5f))
                    drawCircle(Color.White.copy(alpha = 0.85f), radius = 1.4f, center = Offset(x + 2.5f, 104.5f))
                }
            }
        }
        // cachetes rosados
        val blush = Pink.copy(alpha = 0.5f * maxOf(eyeOpen, if (happy) 1f else 0f))
        drawOval(blush, Offset(150f, 108f), Size(11f, 6.5f))
        drawOval(blush, Offset(191f, 108f), Size(11f, 6.5f))

        // nariz, boquita y bigotes
        val nose = Path().apply { moveTo(172f, 111f); lineTo(180f, 111f); lineTo(176f, 116f); close() }
        drawPath(nose, Pink)
        if (eyeOpen > 0.5f) {
            val mouth = Stroke(width = 1.8f, cap = StrokeCap.Round)
            drawArc(EyeLine, 0f, 180f, false, Offset(170f, 117f), Size(6f, 5f), style = mouth)
            drawArc(EyeLine, 0f, 180f, false, Offset(176f, 117f), Size(6f, 5f), style = mouth)
        }
        val whisker = EyeLine.copy(alpha = 0.55f)
        drawLine(whisker, Offset(160f, 115f), Offset(138f, 110f), strokeWidth = 1.8f, cap = StrokeCap.Round)
        drawLine(whisker, Offset(160f, 119f), Offset(138f, 121f), strokeWidth = 1.8f, cap = StrokeCap.Round)
        drawLine(whisker, Offset(192f, 115f), Offset(214f, 110f), strokeWidth = 1.8f, cap = StrokeCap.Round)
        drawLine(whisker, Offset(192f, 119f), Offset(214f, 121f), strokeWidth = 1.8f, cap = StrokeCap.Round)
    }
}
