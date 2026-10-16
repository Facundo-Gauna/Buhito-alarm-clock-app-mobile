package com.buhito.alarm.engine

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.buhito.alarm.MainActivity
import com.buhito.alarm.data.Alarm
import com.buhito.alarm.util.Permissions

/**
 * Usa setAlarmClock: es la forma que Android reserva para alarmas "de reloj". Sale de Doze,
 * suena a la hora exacta y muestra la próxima alarma en la pantalla de bloqueo.
 * Requiere el permiso de alarmas exactas (ver AndroidManifest).
 */
class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    /** Devuelve false (sin cerrar la app) si Android no nos deja programar alarmas exactas. */
    fun schedule(alarm: Alarm): Boolean {
        if (!Permissions.exactAlarmsAllowed(context)) return false
        val show = PendingIntent.getActivity(
            context,
            alarm.id.toInt(),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return try {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(alarm.triggerAt, show),
                firePendingIntent(alarm.id),
            )
            true
        } catch (e: SecurityException) {
            false
        }
    }

    fun cancel(id: Long) {
        alarmManager.cancel(firePendingIntent(id))
    }

    private fun firePendingIntent(id: Long): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            id.toInt(),
            Intent(context, AlarmReceiver::class.java)
                .setAction(AlarmReceiver.ACTION_FIRE)
                .putExtra(AlarmService.EXTRA_ID, id),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
}
