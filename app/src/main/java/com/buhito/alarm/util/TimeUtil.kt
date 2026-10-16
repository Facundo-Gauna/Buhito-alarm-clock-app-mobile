package com.buhito.alarm.util

import android.content.Context
import android.text.format.DateFormat
import com.buhito.alarm.R
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

object TimeUtil {
    fun toLocal(millis: Long): ZonedDateTime =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())

    fun triggerFor(date: LocalDate, hour: Int, minute: Int): Long =
        date.atTime(hour, minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    /** Próxima vez (hoy si todavía no pasó, si no mañana) que el reloj marca hour:minute. */
    fun nextOccurrence(hour: Int, minute: Int): Long {
        val now = ZonedDateTime.now()
        var t = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!t.isAfter(now)) t = t.plusDays(1)
        return t.toInstant().toEpochMilli()
    }

    /** Fecha elegida + hora; si ya pasó, se corre a la próxima ocurrencia de esa hora. */
    fun effectiveTrigger(date: LocalDate, hour: Int, minute: Int): Long {
        val t = triggerFor(date, hour, minute)
        return if (t > System.currentTimeMillis()) t else nextOccurrence(hour, minute)
    }
}

/** Días de la semana como bits: lunes = bit 0 ... domingo = bit 6. */
object Days {
    const val ALL = 127
    const val WEEKDAYS = 31
    const val WEEKEND = 96

    fun bit(index: Int): Int = 1 shl index
    fun has(mask: Int, index: Int): Boolean = (mask and bit(index)) != 0
}

/** Próxima vez, después de [after], que cae en uno de los días de [days] a la hora indicada. */
fun nextRepeating(hour: Int, minute: Int, days: Int, after: Long = System.currentTimeMillis()): Long {
    val zone = ZoneId.systemDefault()
    val today = Instant.ofEpochMilli(after).atZone(zone).toLocalDate()
    for (offset in 0L..7L) {
        val date = today.plusDays(offset)
        if (!Days.has(days, date.dayOfWeek.value - 1)) continue
        val millis = date.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()
        if (millis > after) return millis
    }
    return TimeUtil.nextOccurrence(hour, minute)
}

fun clockText(context: Context, hour: Int, minute: Int): String {
    val p = timeParts(context, hour, minute)
    return if (p.suffix.isEmpty()) p.main else "${p.main} ${p.suffix}"
}

/** "Todos los días", "Lunes a viernes", "Lun, Mié, Vie"... */
fun daysSummary(context: Context, mask: Int): String = when (mask) {
    0 -> ""
    Days.ALL -> context.getString(R.string.every_day)
    Days.WEEKDAYS -> context.getString(R.string.weekdays)
    Days.WEEKEND -> context.getString(R.string.weekends)
    else -> (0..6).filter { Days.has(mask, it) }.joinToString(", ") {
        DayOfWeek.of(it + 1).getDisplayName(TextStyle.SHORT, Locale.getDefault())
    }
}

data class TimeParts(val main: String, val suffix: String)

fun timeParts(context: Context, hour: Int, minute: Int): TimeParts =
    if (DateFormat.is24HourFormat(context)) {
        TimeParts("%02d:%02d".format(hour, minute), "")
    } else {
        val h12 = if (hour % 12 == 0) 12 else hour % 12
        TimeParts("%d:%02d".format(h12, minute), if (hour < 12) "AM" else "PM")
    }

fun timeText(context: Context, millis: Long): String {
    val t = TimeUtil.toLocal(millis)
    val p = timeParts(context, t.hour, t.minute)
    return if (p.suffix.isEmpty()) p.main else "${p.main} ${p.suffix}"
}

fun dayLabel(context: Context, millis: Long): String = dayLabel(context, TimeUtil.toLocal(millis).toLocalDate())

fun dayLabel(context: Context, date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today -> context.getString(R.string.today)
        today.plusDays(1) -> context.getString(R.string.tomorrow)
        else -> DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault()).format(date)
    }
}

fun formatDuration(context: Context, millis: Long): String {
    val totalMin = (millis / 60_000L).coerceAtLeast(1)
    val days = totalMin / (24 * 60)
    val hours = (totalMin / 60) % 24
    val minutes = totalMin % 60
    return when {
        days > 0 -> context.getString(R.string.dur_days_hours, days.toInt(), hours.toInt())
        hours > 0 -> context.getString(R.string.dur_hours_minutes, hours.toInt(), minutes.toInt())
        else -> context.getString(R.string.dur_minutes, minutes.toInt())
    }
}

fun formatSeconds(context: Context, seconds: Int): String = when {
    seconds < 60 -> context.getString(R.string.dur_seconds, seconds)
    seconds % 60 == 0 -> context.getString(R.string.dur_minutes, seconds / 60)
    else -> context.getString(R.string.dur_min_sec, seconds / 60, seconds % 60)
}

/** Tiempo de cronómetro: "05:09.4" o, pasada la hora, "1:05:09.4". */
fun formatStopwatch(ms: Long): String {
    val total = ms.coerceAtLeast(0)
    val tenths = (total / 100) % 10
    val seconds = (total / 1000) % 60
    val minutes = (total / 60_000) % 60
    val hours = total / 3_600_000
    return if (hours > 0) {
        String.format(Locale.ROOT, "%d:%02d:%02d.%d", hours, minutes, seconds, tenths)
    } else {
        String.format(Locale.ROOT, "%02d:%02d.%d", minutes, seconds, tenths)
    }
}
