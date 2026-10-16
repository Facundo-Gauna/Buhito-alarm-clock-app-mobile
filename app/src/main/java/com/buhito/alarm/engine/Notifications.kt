package com.buhito.alarm.engine

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import com.buhito.alarm.MainActivity
import com.buhito.alarm.R
import com.buhito.alarm.data.Alarm
import com.buhito.alarm.data.SleepRecord
import com.buhito.alarm.util.TimeUtil
import com.buhito.alarm.util.formatSleep
import com.buhito.alarm.ui.AlarmActivity
import com.buhito.alarm.ui.mascots.OwlMood
import com.buhito.alarm.ui.mascots.owlBitmap
import com.buhito.alarm.util.clockText

object Notifications {
    const val CHANNEL_ALARM = "alarm"
    const val CHANNEL_MISSED = "missed"
    const val CHANNEL_SLEEP = "sleep"

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ALARM,
                context.getString(R.string.channel_alarm),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.channel_alarm_desc)
                setSound(null, null)          // el sonido lo reproduce el servicio, no la notificación
                enableVibration(false)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_SLEEP,
                context.getString(R.string.channel_sleep),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_MISSED,
                context.getString(R.string.channel_missed),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
    }

    /**
     * Notificación de alarma sonando. [frame] es el cuadro actual del búho animado (el servicio
     * la vuelve a publicar con distintos cuadros para que el búho "se mueva"); [line] es la frase.
     */
    /** Avisa que se detectó sueño nuevo, para que el usuario lo revise y lo corrija si hace falta. */
    fun sleepDetected(context: Context, found: List<SleepRecord>) {
        val open = PendingIntent.getActivity(
            context,
            1,
            Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_TAB, MainActivity.TAB_SLEEP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val text = if (found.size == 1) {
            val r = found[0]
            val bed = TimeUtil.toLocal(r.sleepAt)
            val wake = TimeUtil.toLocal(r.wakeAt)
            context.getString(
                R.string.sleep_notif_one,
                clockText(context, bed.hour, bed.minute),
                clockText(context, wake.hour, wake.minute),
                formatSleep(r.durationMs),
            )
        } else {
            context.getString(R.string.sleep_notif_many, found.size)
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_SLEEP)
            .setSmallIcon(R.drawable.ic_stat_owl)
            .setColor(0xFF7C95E0.toInt())
            .setLargeIcon(owlBitmap(160, OwlMood.Sleepy))
            .setContentTitle(context.getString(R.string.sleep_notif_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text + "\n" + context.getString(R.string.sleep_notif_hint)))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(3000, notification)
    }

    fun ringing(context: Context, alarm: Alarm?, frame: Bitmap? = null, line: String? = null): Notification {
        val screen = PendingIntent.getActivity(
            context,
            0,
            Intent(context, AlarmActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val title = alarm?.label?.takeIf { it.isNotBlank() } ?: context.getString(R.string.alarm_default_title)
        return NotificationCompat.Builder(context, CHANNEL_ALARM)
            .setSmallIcon(R.drawable.ic_stat_owl)
            .setColor(0xFF7C95E0.toInt())
            .setLargeIcon(frame)
            .setContentTitle(title)
            .setContentText(line ?: context.getString(R.string.notif_wake_up))
            .setSubText(alarm?.let { clockText(context, it.hour, it.minute) })
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setContentIntent(screen)
            .setFullScreenIntent(screen, true)
            .apply {
                // Modo estricto con QR: sin posponer ni detener desde la notificación, solo abrir la pantalla.
                if (com.buhito.alarm.util.qrOnly(context, alarm)) {
                    addAction(0, "📷 " + context.getString(R.string.open_alarm), screen)
                    return@apply
                }
                addAction(0, "💤 " + context.getString(R.string.snooze_n, 5), serviceAction(context, AlarmService.ACTION_SNOOZE, 5))
                addAction(0, "💤 " + context.getString(R.string.snooze_n, 10), serviceAction(context, AlarmService.ACTION_SNOOZE, 10))
                // Con "confirmar para detener", el botón abre la pantalla (ahí hay que mantener presionado).
                if ((alarm?.confirmStop == true || alarm?.qrStop == true)) addAction(0, "👆 " + context.getString(R.string.open_alarm), screen)
                else addAction(0, "⏹ " + context.getString(R.string.stop), serviceAction(context, AlarmService.ACTION_STOP, 0))
            }
            .build()
    }

    fun missed(context: Context, alarm: Alarm) {
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val title = alarm.label.takeIf { it.isNotBlank() } ?: context.getString(R.string.alarm_default_title)
        val notification = NotificationCompat.Builder(context, CHANNEL_MISSED)
            .setSmallIcon(R.drawable.ic_stat_owl)
            .setColor(0xFF7C95E0.toInt())
            .setLargeIcon(owlBitmap(160, OwlMood.Sleepy))
            .setContentTitle(title)
            .setContentText(context.getString(R.string.missed_text, clockText(context, alarm.hour, alarm.minute)))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify(2000 + alarm.id.toInt(), notification)
    }

    private fun serviceAction(context: Context, action: String, minutes: Int): PendingIntent =
        PendingIntent.getService(
            context,
            100 + minutes + if (action == AlarmService.ACTION_STOP) 1 else 0,
            Intent(context, AlarmService::class.java)
                .setAction(action)
                .putExtra(AlarmService.EXTRA_MINUTES, minutes),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
}
