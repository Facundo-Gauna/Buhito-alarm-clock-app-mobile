package com.buhito.alarm.engine

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.buhito.alarm.BuhitoApp
import com.buhito.alarm.R
import com.buhito.alarm.data.Alarm
import com.buhito.alarm.data.RingResult
import com.buhito.alarm.ui.mascots.OwlMood
import com.buhito.alarm.ui.mascots.owlBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Servicio en primer plano que hace sonar la alarma: reproduce el audio en bucle, vibra
 * y se corta solo cuando se cumple la duración configurada. La pantalla de alarma
 * (AlarmActivity) observa [ringingId] para saber cuándo cerrarse.
 */
class AlarmService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val handler = Handler(Looper.getMainLooper())
    private val timeout = Runnable { onTimeout() }
    private val animate = Runnable { nextOwlFrame() }
    private val speak = Runnable { speakLabel() }

    // Búho animado de la notificación: unos pocos cuadros que se van alternando.
    private var owlFrames: List<Bitmap> = emptyList()
    private var frameIndex = 0
    private var ringLine: String? = null

    private lateinit var repository: AlarmRepository
    private var current: Alarm? = null
    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var focusRequest: AudioFocusRequest? = null
    private var historyId = 0L
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(com.buhito.alarm.util.Locales.wrap(newBase))
    }

    override fun onCreate() {
        super.onCreate()
        repository = (application as BuhitoApp).repository
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_FIRE -> startRinging(intent.getLongExtra(EXTRA_ID, -1L))
            ACTION_STOP -> endRing(RingResult.STOPPED, stopChain = intent.getBooleanExtra(EXTRA_ALL, false))
            ACTION_SNOOZE -> snooze(intent.getIntExtra(EXTRA_MINUTES, 5))
            else -> stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun startRinging(id: Long) {
        releaseResources()
        current = null
        historyId = 0L
        chainRemaining.value = 0
        ringingId.value = id

        // Hay que llamar a startForeground enseguida; el contenido real llega cuando carga la alarma.
        val type = if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0
        ServiceCompat.startForeground(this, NOTIFICATION_ID, Notifications.ringing(this, null), type)
        acquireWakeLock()

        scope.launch {
            val start = repository.startRing(id)
            if (start == null) {
                stopRinging()
                return@launch
            }
            val alarm = start.alarm
            current = alarm
            historyId = start.historyId
            chainRemaining.value = start.chainRemaining
            ringLine = resources.getStringArray(R.array.notif_ring_lines).random()
            owlFrames = listOf(
                owlBitmap(192, OwlMood.Ringing, blink = 1f, wingAngle = 0f, look = -1f),
                owlBitmap(192, OwlMood.Ringing, blink = 1f, wingAngle = 18f, look = 0f),
                owlBitmap(192, OwlMood.Ringing, blink = 0.1f, wingAngle = 0f, look = 1f),
                owlBitmap(192, OwlMood.Ringing, blink = 1f, wingAngle = -18f, look = 0f),
            )
            frameIndex = 0
            publishFrame()
            handler.postDelayed(animate, FRAME_MS)
            requestAudioFocus()
            startPlayback(alarm)
            startVibration(alarm)
            startSpeaking(alarm)
            handler.postDelayed(timeout, alarm.durationSec * 1000L)
        }
    }

    /** Si la alarma tiene "leer la etiqueta", la dice al empezar y la repite cada 20 s. */
    private fun startSpeaking(alarm: Alarm) {
        if (!alarm.speakLabel || alarm.label.isBlank()) return
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build(),
                )
                ttsReady = true
                handler.postDelayed(speak, 1500)
            }
        }
    }

    private fun speakLabel() {
        val text = current?.label?.takeIf { it.isNotBlank() } ?: return
        if (!ttsReady) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "label")
        handler.postDelayed(speak, 20_000)
    }

    private fun publishFrame() {
        val alarm = current ?: return
        val frame = owlFrames.getOrNull(frameIndex)
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, Notifications.ringing(this, alarm, frame, ringLine))
    }

    private fun nextOwlFrame() {
        if (current == null || owlFrames.isEmpty()) return
        frameIndex = (frameIndex + 1) % owlFrames.size
        publishFrame()
        handler.postDelayed(animate, FRAME_MS)
    }

    private fun startPlayback(alarm: Alarm) {
        val candidates = listOf(
            SoundUtil.resolve(alarm.soundUri, alarm.customSound),
            SoundUtil.defaultAlarmUri(), // si el audio falla, suena la alarma del sistema
        )
        for (uri in candidates) {
            val p = SoundUtil.createPlayer(this, uri, loop = alarm.loop, usage = AudioAttributes.USAGE_ALARM)
            if (p != null) {
                val volume = alarm.volume / 100f
                p.setVolume(volume, volume)
                // Sin repetición: el audio suena una vez, pero la alarma (pantalla, vibración y
                // notificación) sigue activa hasta que se cumple la duración o el usuario la detiene.
                if (!alarm.loop) p.setOnCompletionListener { abandonAudioFocus() }
                p.start()
                player = p
                return
            }
        }
    }

    private fun startVibration(alarm: Alarm) {
        if (!alarm.vibrate) return
        val v = if (Build.VERSION.SDK_INT >= 31) {
            getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Vibrator::class.java)
        }
        if (!v.hasVibrator()) return
        val effect = VibrationEffect.createWaveform(longArrayOf(0, 600, 400, 600, 1200), 0)
        if (Build.VERSION.SDK_INT >= 33) {
            v.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(
                effect,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
        }
        vibrator = v
    }

    private fun requestAudioFocus() {
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setOnAudioFocusChangeListener { }
            .build()
        getSystemService(AudioManager::class.java).requestAudioFocus(request)
        focusRequest = request
    }

    private fun abandonAudioFocus() {
        focusRequest?.let { getSystemService(AudioManager::class.java).abandonAudioFocusRequest(it) }
        focusRequest = null
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(PowerManager::class.java)
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "buhito:alarm").apply {
            acquire(10 * 60 * 1000L)
        }
    }

    private fun onTimeout() {
        current?.let { Notifications.missed(this, it) }
        endRing(RingResult.MISSED)
    }

    private fun snooze(minutes: Int) {
        val id = ringingId.value
        if (id == null) {
            stopRinging()
            return
        }
        scope.launch {
            repository.snooze(id, minutes)
            Toast.makeText(this@AlarmService, getString(R.string.snoozed_toast, minutes), Toast.LENGTH_SHORT).show()
            endRing(RingResult.SNOOZED)
        }
    }

    /**
     * Termina el timbre: corta sonido y vibración enseguida, anota el resultado en el historial
     * y, si la alarma tiene secuencia, programa el siguiente timbre (salvo [stopChain]).
     */
    private fun endRing(result: String, stopChain: Boolean = false) {
        val alarm = current
        val hid = historyId
        current = null
        historyId = 0L
        releaseResources()
        ringingId.value = null
        scope.launch {
            repository.finishRing(hid, result, alarm?.id, continueChain = !stopChain && result != RingResult.SNOOZED)
            ServiceCompat.stopForeground(this@AlarmService, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun stopRinging() {
        releaseResources()
        ringingId.value = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun releaseResources() {
        handler.removeCallbacks(timeout)
        handler.removeCallbacks(animate)
        handler.removeCallbacks(speak)
        tts?.let {
            it.stop()
            it.shutdown()
        }
        tts = null
        ttsReady = false
        player?.let {
            runCatching { it.stop() }
            it.release()
        }
        player = null
        vibrator?.cancel()
        vibrator = null
        abandonAudioFocus()
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    override fun onDestroy() {
        releaseResources()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_FIRE = "com.buhito.alarm.action.SERVICE_FIRE"
        const val ACTION_STOP = "com.buhito.alarm.action.STOP"
        const val ACTION_SNOOZE = "com.buhito.alarm.action.SNOOZE"
        const val EXTRA_ID = "alarm_id"
        const val EXTRA_MINUTES = "minutes"
        const val EXTRA_ALL = "stop_all"
        private const val NOTIFICATION_ID = 1001
        private const val FRAME_MS = 900L

        /** Id de la alarma que está sonando ahora (null si no suena ninguna). */
        val ringingId = MutableStateFlow<Long?>(null)

        /** Cuántos timbres más de la secuencia faltan después del que suena ahora. */
        val chainRemaining = MutableStateFlow(0)

        fun fire(context: Context, id: Long) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, AlarmService::class.java).setAction(ACTION_FIRE).putExtra(EXTRA_ID, id),
            )
        }

        /** [all] = true también cancela los timbres que faltan de la secuencia. */
        fun stop(context: Context, all: Boolean = false) {
            context.startService(
                Intent(context, AlarmService::class.java).setAction(ACTION_STOP).putExtra(EXTRA_ALL, all),
            )
        }

        fun snooze(context: Context, minutes: Int) {
            context.startService(
                Intent(context, AlarmService::class.java)
                    .setAction(ACTION_SNOOZE)
                    .putExtra(EXTRA_MINUTES, minutes),
            )
        }
    }
}
