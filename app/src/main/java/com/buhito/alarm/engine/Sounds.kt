package com.buhito.alarm.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import com.buhito.alarm.R
import java.io.File

object SoundUtil {
    fun defaultAlarmUri(): Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    fun resolve(soundUri: String, custom: Boolean): Uri = when {
        soundUri.isBlank() -> defaultAlarmUri()
        custom -> Uri.fromFile(File(soundUri))
        else -> Uri.parse(soundUri)
    }

    /** Devuelve un MediaPlayer ya preparado, o null si el audio no se puede reproducir. */
    fun createPlayer(context: Context, uri: Uri, loop: Boolean, usage: Int): MediaPlayer? {
        val player = MediaPlayer()
        return try {
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(usage)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            player.setDataSource(context, uri)
            player.isLooping = loop
            player.prepare()
            player
        } catch (e: Exception) {
            player.release()
            null
        }
    }
}

data class SystemTone(val name: String, val uri: String)

object SystemTones {
    /** Tonos de alarma del sistema (y, a continuación, los de llamada). Bloqueante. */
    fun load(context: Context): List<SystemTone> {
        val result = mutableListOf(
            SystemTone(context.getString(R.string.default_alarm_tone), ""),
        )
        val seen = mutableSetOf<String>()
        for (type in intArrayOf(RingtoneManager.TYPE_ALARM, RingtoneManager.TYPE_RINGTONE)) {
            val manager = RingtoneManager(context).apply { setType(type) }
            val cursor = manager.cursor ?: continue
            cursor.use { c ->
                while (c.moveToNext()) {
                    val title = c.getString(RingtoneManager.TITLE_COLUMN_INDEX) ?: continue
                    val uri = manager.getRingtoneUri(c.position).toString()
                    if (seen.add(uri)) result += SystemTone(title, uri)
                }
            }
        }
        return result
    }
}
