package com.buhito.alarm

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.buhito.alarm.engine.SleepScheduler
import com.buhito.alarm.ui.BuhitoRoot
import com.buhito.alarm.ui.theme.BuhitoTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(com.buhito.alarm.util.Locales.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        val app = application as BuhitoApp
        val repository = app.repository
        // Por las dudas, deja programadas todas las alarmas activas cada vez que se abre la app.
        lifecycleScope.launch { repository.rescheduleAll() }
        // Sueño automático: revisa lo nuevo al abrir la app y deja programada la revisión de cada mañana.
        lifecycleScope.launch { app.sleep.detectNew() }
        SleepScheduler.schedule(this)
        val startTab = intent.getIntExtra(EXTRA_TAB, 0)

        setContent {
            BuhitoTheme { BuhitoRoot(repository, app.stopwatches, app.backup, app.sleep, startTab) }
        }
    }

    companion object {
        const val EXTRA_TAB = "tab"
        const val TAB_SLEEP = 2
    }
}
