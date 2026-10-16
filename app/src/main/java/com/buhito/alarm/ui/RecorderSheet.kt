package com.buhito.alarm.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.buhito.alarm.R
import com.buhito.alarm.engine.VoiceRecorder
import com.buhito.alarm.ui.mascots.OwlMascot
import com.buhito.alarm.ui.mascots.OwlMood
import com.buhito.alarm.ui.theme.BuhitoColors
import kotlinx.coroutines.delay
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sin

private const val MAX_SECONDS = 120

/**
 * Hoja para grabar un mensaje de voz. El búho "escucha" mientras grabás.
 * Al tocar "Usar este", devuelve la ruta del archivo (ya guardado en la carpeta interna) y un nombre.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecorderSheet(
    preview: SoundPreview,
    onUse: (path: String, name: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val recorder = remember { VoiceRecorder(context.applicationContext) }
    var recording by remember { mutableStateOf(false) }
    var recorded by remember { mutableStateOf<File?>(null) }
    var seconds by remember { mutableIntStateOf(0) }
    var errorRes by remember { mutableStateOf<Int?>(null) }
    var used by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun begin() {
        preview.stop()
        recorded?.delete()
        recorded = null
        if (recorder.start()) {
            errorRes = null
            recording = true
        } else {
            errorRes = R.string.record_failed
        }
    }

    fun finish() {
        recording = false
        val file = recorder.stop()
        if (file == null) errorRes = R.string.record_failed else recorded = file
    }

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) begin() else errorRes = R.string.mic_denied
    }

    fun onRecordTap() {
        if (recording) {
            finish()
            return
        }
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) begin() else micLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    // Cronómetro: corta solo al llegar al máximo.
    LaunchedEffect(recording) {
        if (recording) {
            seconds = 0
            while (recording && seconds < MAX_SECONDS) {
                delay(1000)
                seconds++
            }
            if (recording) finish()
        }
    }

    // Volumen de la voz, para animar al búho y las barras.
    val level by produceState(0f, recording) {
        if (!recording) {
            value = 0f
            return@produceState
        }
        while (true) {
            value = (recorder.maxAmplitude() / 12000f).coerceIn(0f, 1f)
            delay(70)
        }
    }

    // Al cerrar la hoja: se corta la grabación y se borra el archivo si no se usó.
    DisposableEffect(Unit) {
        onDispose {
            recorder.cancel()
            preview.stop()
            if (!used) recorded?.delete()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.recorder_title), style = MaterialTheme.typography.titleLarge)
            OwlMascot(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .size(150.dp),
                mood = if (recording) OwlMood.Listening else OwlMood.Calm,
                level = level,
            )

            Text(
                text = "%d:%02d".format(seconds / 60, seconds % 60),
                style = MaterialTheme.typography.headlineMedium,
                color = if (recording) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LevelBars(level = if (recording) level else 0f, modifier = Modifier.padding(vertical = 8.dp))

            val file = recorded
            if (file != null && !recording) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    PreviewButton(
                        playing = preview.playingKey == "rec",
                        onClick = { preview.toggle("rec", Uri.fromFile(file)) },
                    )
                    TextButton(onClick = {
                        preview.stop()
                        file.delete()
                        recorded = null
                        seconds = 0
                    }) { Text(stringResource(R.string.record_again)) }
                }
                Button(
                    onClick = {
                        preview.stop()
                        val stamp = DateTimeFormatter.ofPattern("d MMM HH:mm", Locale.getDefault())
                            .format(LocalDateTime.now())
                        used = true
                        onUse(file.absolutePath, context.getString(R.string.voice_note_name, stamp))
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 16.dp)
                        .height(52.dp),
                ) { Text(stringResource(R.string.use_recording), style = MaterialTheme.typography.labelLarge) }
            } else {
                RecordButton(recording = recording, onClick = ::onRecordTap)
                Text(
                    text = stringResource(
                        when {
                            errorRes != null -> errorRes!!
                            recording -> R.string.recording
                            else -> R.string.record_tap
                        },
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (errorRes != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp, bottom = 24.dp),
                )
            }
        }
    }
}

@Composable
private fun RecordButton(recording: Boolean, onClick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "rec")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (recording) 1.12f else 1f,
        animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Reverse),
        label = "recPulse",
    )
    Box(
        Modifier
            .padding(top = 8.dp)
            .size((84 * pulse).dp)
            .clip(CircleShape)
            .background(BuhitoColors.Danger)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (recording) {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White),
            )
        } else {
            Box(
                Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color.White),
            )
        }
    }
}

/** Barritas que bailan según el volumen de la voz. */
@Composable
private fun LevelBars(level: Float, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "bars")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.2832f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
        label = "barsPhase",
    )
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier.fillMaxWidth().height(44.dp)) {
        val count = 21
        val gap = 6.dp.toPx()
        val barWidth = 5.dp.toPx()
        val total = count * barWidth + (count - 1) * gap
        val startX = (size.width - total) / 2f
        for (i in 0 until count) {
            val wobble = 0.35f + 0.65f * abs(sin(i * 0.9f + phase))
            val height = 4.dp.toPx() + (size.height - 4.dp.toPx()) * level * wobble
            val x = startX + i * (barWidth + gap)
            drawRoundRect(
                color = color.copy(alpha = 0.35f + 0.65f * level),
                topLeft = Offset(x, (size.height - height) / 2f),
                size = Size(barWidth, height),
                cornerRadius = CornerRadius(barWidth / 2f),
            )
        }
    }
}
