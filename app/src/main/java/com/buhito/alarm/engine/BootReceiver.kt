package com.buhito.alarm.engine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.buhito.alarm.BuhitoApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Android borra las alarmas al apagar el teléfono (o al actualizar la app): las reprogramamos. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return
        val pending = goAsync()
        val repository = (context.applicationContext as BuhitoApp).repository
        CoroutineScope(Dispatchers.IO).launch {
            try {
                repository.rescheduleAll()
                SleepScheduler.schedule(context)
            } finally {
                pending.finish()
            }
        }
    }
}
