package com.buhito.alarm.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buhito.alarm.R
import androidx.compose.ui.graphics.Color
import com.buhito.alarm.data.Alarm
import com.buhito.alarm.data.CustomCategory
import com.buhito.alarm.data.resolveCategory
import com.buhito.alarm.ui.mascots.NightBackground
import com.buhito.alarm.ui.mascots.OwlMascot
import com.buhito.alarm.ui.mascots.OwlMood
import com.buhito.alarm.util.timeParts

@Composable
fun AlarmScreen(
    alarm: Alarm?,
    customs: List<CustomCategory>,
    chainRemaining: Int,
    onStop: () -> Unit,
    onStopAll: () -> Unit,
    onSnooze: (Int) -> Unit,
) {
    val context = LocalContext.current
    var scanning by remember { mutableStateOf(false) }
    // El QR solo se pide si hay permiso de cámara; si no, se cae al botón de mantener (nunca se queda sin poder apagarla).
    val qrReady = alarm?.qrStop == true && com.buhito.alarm.util.cameraGranted(context)
    // Modo estricto (Ajustes): con QR no hay posponer ni otra forma de apagarla.
    val strict = com.buhito.alarm.util.qrOnly(context, alarm)
    if (scanning) {
        QrScanScreen(
            expected = com.buhito.alarm.util.Prefs.qrCode(context),
            onSuccess = { scanning = false; if (strict) onStopAll() else onStop() },
            onCancel = { scanning = false },
        )
        return
    }
    NightBackground {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                PulseRings(Modifier.size(320.dp))
                val shake = rememberInfiniteTransition(label = "shake")
                val angle by shake.animateFloat(
                    initialValue = -4f,
                    targetValue = 4f,
                    animationSpec = infiniteRepeatable(tween(180, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                    label = "angle",
                )
                OwlMascot(Modifier.size(210.dp).rotate(angle), mood = OwlMood.Ringing)
            }

            if (alarm != null) {
                val category = resolveCategory(context, alarm.category, customs)
                val parts = timeParts(context, alarm.hour, alarm.minute)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = parts.main,
                        fontSize = 76.sp,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    if (parts.suffix.isNotEmpty()) {
                        Text(
                            text = parts.suffix,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 8.dp, bottom = 16.dp),
                        )
                    }
                }
                Text(
                    text = "${category.emoji} ${category.name}".uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(category.color),
                )
                Text(
                    text = alarm.label.ifBlank { stringResource(R.string.alarm_default_title) },
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                )
            }

            Column(Modifier.padding(top = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (!strict) {
                    Text(
                        text = stringResource(R.string.snooze).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        Modifier.padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        for (minutes in intArrayOf(5, 10, 15)) {
                            Pill(
                                text = stringResource(R.string.minutes_short, minutes),
                                selected = false,
                                onClick = { onSnooze(minutes) },
                            )
                        }
                    }
                }
                if (qrReady) {
                    Button(
                        onClick = { scanning = true },
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary,
                        ),
                        modifier = Modifier.padding(top = 24.dp).fillMaxWidth().height(72.dp),
                    ) {
                        Text("📷 " + stringResource(R.string.qr_scan_button), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                } else if (alarm?.confirmStop == true || alarm?.qrStop == true) {
                    HoldToStopButton(onComplete = onStop)
                } else {
                    Button(
                        onClick = onStop,
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary,
                        ),
                        modifier = Modifier
                            .padding(top = 24.dp)
                            .fillMaxWidth()
                            .height(72.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.stop),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                if (chainRemaining > 0) {
                    Text(
                        text = pluralStringResource(R.plurals.chain_remaining, chainRemaining, chainRemaining),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    if (!strict) TextButton(onClick = onStopAll) { Text(stringResource(R.string.stop_all_chain)) }
                }
            }
        }
    }
}

/** Botón que hay que mantener presionado un par de segundos: evita apagar la alarma dormido. */
@Composable
private fun HoldToStopButton(onComplete: () -> Unit) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val shape = RoundedCornerShape(28.dp)
    Box(
        Modifier
            .padding(top = 24.dp)
            .fillMaxWidth()
            .height(72.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        val job = scope.launch {
                            progress.animateTo(1f, tween(1800, easing = LinearEasing))
                            onComplete()
                        }
                        tryAwaitRelease()
                        job.cancel()
                        scope.launch { progress.snapTo(0f) }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth(progress.value)
                .height(72.dp)
                .background(MaterialTheme.colorScheme.secondary),
        )
        Text(
            text = stringResource(R.string.hold_to_stop),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

/** Ondas que se expanden desde el búho, como un latido luminoso. */
@Composable
private fun PulseRings(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "rings")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
        label = "ringProgress",
    )
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        val maxRadius = size.minDimension / 2f
        for (i in 0..2) {
            val p = (progress + i / 3f) % 1f
            drawCircle(
                color = color.copy(alpha = 0.35f * (1f - p)),
                radius = maxRadius * (0.45f + 0.55f * p),
                center = Offset(size.width / 2f, size.height / 2f),
                style = Stroke(width = 3.dp.toPx()),
            )
        }
    }
}
