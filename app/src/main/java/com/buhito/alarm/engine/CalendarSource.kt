package com.buhito.alarm.engine

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Un evento del calendario del teléfono que todavía no empezó. */
data class CalendarEvent(val key: String, val title: String, val beginMillis: Long)

/** Lee los próximos eventos del calendario del teléfono (solo lectura, nada sale del dispositivo). */
object CalendarSource {
    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    /** Eventos con hora (no de día completo) de los próximos [days] días, ordenados por inicio (máx. 300). */
    suspend fun upcoming(context: Context, days: Int): List<CalendarEvent> = withContext(Dispatchers.IO) {
        if (!hasPermission(context)) return@withContext emptyList()
        val now = System.currentTimeMillis()
        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, now)
        ContentUris.appendId(builder, now + days * 24L * 60 * 60_000L)
        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.ALL_DAY,
        )
        val result = ArrayList<CalendarEvent>()
        runCatching {
            context.contentResolver.query(builder.build(), projection, null, null, "${CalendarContract.Instances.BEGIN} ASC")
        }.getOrNull()?.use { c ->
            while (c.moveToNext() && result.size < 300) {
                val begin = c.getLong(2)
                if (c.getInt(3) == 1 || begin <= now) continue
                val title = c.getString(1)?.takeIf { it.isNotBlank() } ?: continue
                result += CalendarEvent("${c.getLong(0)}-$begin", title, begin)
            }
        }
        result
    }
}
