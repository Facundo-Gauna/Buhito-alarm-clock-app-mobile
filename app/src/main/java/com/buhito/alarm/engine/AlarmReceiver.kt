package com.buhito.alarm.engine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Lo dispara el AlarmManager a la hora exacta; solo arranca el servicio que hace sonar la alarma. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE) return
        val id = intent.getLongExtra(AlarmService.EXTRA_ID, -1L)
        if (id >= 0) AlarmService.fire(context, id)
    }

    companion object {
        const val ACTION_FIRE = "com.buhito.alarm.action.FIRE"
    }
}
