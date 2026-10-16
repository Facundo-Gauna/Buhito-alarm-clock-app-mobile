package com.buhito.alarm.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.buhito.alarm.engine.SoundUtil

/** Reproduce una vista previa de un sonido (una sola vez, sin bucle). */
class SoundPreview(private val context: Context) {
    private var player: MediaPlayer? = null

    /** Clave de lo que está sonando ahora (null si nada). */
    var playingKey by mutableStateOf<String?>(null)
        private set

    fun toggle(key: String, uri: Uri, volume: Float = 1f) {
        if (playingKey == key) stop() else play(key, uri, volume)
    }

    fun play(key: String, uri: Uri, volume: Float = 1f) {
        stop()
        val p = SoundUtil.createPlayer(context, uri, loop = false, usage = AudioAttributes.USAGE_MEDIA) ?: return
        p.setVolume(volume, volume)
        p.setOnCompletionListener { stop() }
        p.start()
        player = p
        playingKey = key
    }

    fun stop() {
        val p = player
        if (p != null) {
            runCatching { p.stop() }
            p.release()
        }
        player = null
        playingKey = null
    }
}

@Composable
fun rememberSoundPreview(): SoundPreview {
    val context = LocalContext.current
    val preview = remember { SoundPreview(context.applicationContext) }
    DisposableEffect(preview) { onDispose { preview.stop() } }
    return preview
}
