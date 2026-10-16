package com.buhito.alarm.ui

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.produceState
import com.buhito.alarm.BuhitoApp
import com.buhito.alarm.data.Alarm
import com.buhito.alarm.engine.AlarmService
import com.buhito.alarm.ui.theme.BuhitoTheme

/**
 * Pantalla de la alarma. Se abre sobre el bloqueo (notificación de pantalla completa),
 * enciende la pantalla y se cierra sola cuando el servicio deja de sonar.
 */
class AlarmActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(com.buhito.alarm.util.Locales.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        showOverLockScreen()

        val repository = (application as BuhitoApp).repository
        setContent {
            BuhitoTheme {
                val ringingId by AlarmService.ringingId.collectAsState()
                LaunchedEffect(ringingId) {
                    if (ringingId == null) finish()
                }
                val alarm by produceState<Alarm?>(null, ringingId) {
                    value = ringingId?.let { repository.get(it) }
                }
                BackHandler { /* la alarma se detiene o pospone con los botones */ }
                val customs by repository.customCategories.collectAsState(initial = emptyList())
                val chainRemaining by AlarmService.chainRemaining.collectAsState()
                AlarmScreen(
                    alarm = alarm,
                    customs = customs,
                    chainRemaining = chainRemaining,
                    onStop = { AlarmService.stop(this@AlarmActivity) },
                    onStopAll = { AlarmService.stop(this@AlarmActivity, all = true) },
                    onSnooze = { AlarmService.snooze(this@AlarmActivity, it) },
                )
            }
        }
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
