package com.buhito.alarm.engine

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.buhito.alarm.BuhitoApp
import com.buhito.alarm.util.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

/** Programa la revisión diaria del sueño (a la hora de la mañana que elija el usuario). */
object SleepScheduler {
    private const val REQUEST = 7001
    const val ACTION_CHECK = "com.buhito.alarm.action.SLEEP_CHECK"

    private fun pending(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST,
        Intent(context, SleepCheckReceiver::class.java).setAction(ACTION_CHECK),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /** Deja programada la próxima revisión (o la cancela si la detección está apagada). */
    fun schedule(context: Context) {
        val manager = context.getSystemService(AlarmManager::class.java)
        if (!Prefs.sleepAuto(context)) {
            manager.cancel(pending(context))
            return
        }
        val now = ZonedDateTime.now()
        var at = now.withHour(Prefs.sleepCheckHour(context)).withMinute(0).withSecond(0).withNano(0)
        if (!at.isAfter(now)) at = at.plusDays(1)
        // No necesita permiso de alarmas exactas: unos minutos de diferencia no importan.
        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toInstant().toEpochMilli(), pending(context))
    }
}

/** Se ejecuta cada mañana: busca sueños nuevos, avisa al usuario y reprograma la siguiente revisión. */
class SleepCheckReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != SleepScheduler.ACTION_CHECK) return
        val pending = goAsync()
        val sleep = (context.applicationContext as BuhitoApp).sleep
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val found = sleep.detectNew()
                if (found.isNotEmpty()) Notifications.sleepDetected(com.buhito.alarm.util.Locales.wrap(context), found)
            } finally {
                SleepScheduler.schedule(context)
                pending.finish()
            }
        }
    }
}
