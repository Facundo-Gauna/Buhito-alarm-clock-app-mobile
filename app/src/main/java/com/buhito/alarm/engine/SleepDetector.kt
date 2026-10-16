package com.buhito.alarm.engine

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process

/**
 * Detecta cuándo dormís mirando cuándo estuvo apagada la pantalla del teléfono: un apagón largo y
 * seguido, de noche, es sueño. Usa el "Acceso de uso" de Android, que guarda el historial de la pantalla
 * (así no hace falta ningún servicio corriendo todo el día ni gasta batería).
 */
object SleepDetector {
    /** Los eventos de pantalla del historial de uso existen desde Android 9 (API 28). */
    val supported: Boolean get() = Build.VERSION.SDK_INT >= 28

    data class Span(val start: Long, val end: Long)

    fun hasAccess(context: Context): Boolean {
        val ops = context.getSystemService(AppOpsManager::class.java)
        val op = "android:get_usage_stats"
        val mode = if (Build.VERSION.SDK_INT >= 29) {
            ops.unsafeCheckOpNoThrow(op, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            ops.checkOpNoThrow(op, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Tramos con la pantalla apagada que ya terminaron (la pantalla volvió a encenderse). */
    fun screenOffSpans(context: Context, from: Long, to: Long): List<Span> {
        val manager = context.getSystemService(UsageStatsManager::class.java)
        val events = manager.queryEvents(from, to)
        val event = UsageEvents.Event()
        val spans = ArrayList<Span>()
        var offStart = -1L
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                SCREEN_NON_INTERACTIVE -> if (offStart < 0) offStart = event.timeStamp
                SCREEN_INTERACTIVE -> {
                    if (offStart >= 0) spans += Span(offStart, event.timeStamp)
                    offStart = -1L
                }
            }
        }
        return spans
    }

    /** Une apagones separados por encendidos cortos (mirar la hora, una notificación). */
    fun merge(spans: List<Span>, maxGapMs: Long): List<Span> {
        val merged = ArrayList<Span>()
        for (s in spans.sortedBy { it.start }) {
            val last = merged.lastOrNull()
            if (last != null && s.start - last.end <= maxGapMs) merged[merged.lastIndex] = Span(last.start, maxOf(last.end, s.end))
            else merged += s
        }
        return merged
    }

    // Valores de UsageEvents.Event.SCREEN_INTERACTIVE / SCREEN_NON_INTERACTIVE (API 28).
    private const val SCREEN_INTERACTIVE = 15
    private const val SCREEN_NON_INTERACTIVE = 16
}
