package com.buhito.alarm.engine

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import com.buhito.alarm.data.AudioStorage
import java.io.File

/** Graba la voz a un archivo .m4a dentro de la carpeta interna de la app. */
class VoiceRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var file: File? = null

    fun start(): Boolean {
        val target = AudioStorage.newRecordingFile(context)
        val r = createRecorder()
        return try {
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(128_000)
            r.setAudioSamplingRate(44_100)
            r.setOutputFile(target.absolutePath)
            r.prepare()
            r.start()
            recorder = r
            file = target
            true
        } catch (e: Exception) {
            r.release()
            target.delete()
            false
        }
    }

    /** Termina la grabación y devuelve el archivo, o null si falló (por ejemplo, fue demasiado corta). */
    fun stop(): File? {
        val r = recorder ?: return null
        val result = file
        recorder = null
        file = null
        return try {
            r.stop()
            r.release()
            result
        } catch (e: RuntimeException) {
            r.release()
            result?.delete()
            null
        }
    }

    /** Corta y descarta lo grabado. */
    fun cancel() {
        stop()?.delete()
    }

    /** Volumen reciente captado por el micrófono (0..32767), para animar al búho. */
    fun maxAmplitude(): Int = runCatching { recorder?.maxAmplitude ?: 0 }.getOrDefault(0)

    private fun createRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= 31) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
}
