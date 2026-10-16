package com.buhito.alarm.util

import com.buhito.alarm.data.SleepRecord
import java.time.LocalDate
import java.time.YearMonth

/** Resumen de un conjunto de registros de sueño (solo los terminados). */
data class SleepSummary(
    val count: Int,
    val totalMs: Long,
    val avgMs: Long,
    val avgBedMinute: Int,   // hora promedio de dormir, en minutos desde las 00:00
    val avgWakeMinute: Int,  // hora promedio de despertar
    val avgQuality: Float?,  // null si ninguno fue valorado
    val longest: SleepRecord,
    val shortest: SleepRecord,
)

/** Día (en hora local) en que se despertó: ahí se cuenta la noche. */
fun SleepRecord.wakeDate(): LocalDate = TimeUtil.toLocal(wakeAt).toLocalDate()

private fun minuteOfDay(millis: Long): Int {
    val t = TimeUtil.toLocal(millis)
    return t.hour * 60 + t.minute
}

fun summarize(records: List<SleepRecord>): SleepSummary? {
    val done = records.filter { !it.open && it.durationMs > 0 }
    if (done.isEmpty()) return null
    val total = done.sumOf { it.durationMs }
    // Dormir pasada la medianoche (00:30) debe promediar con 23:30, no con las 12:00:
    // las horas de la mañana se cuentan como "del día siguiente" antes de promediar.
    val bed = done.map { val m = minuteOfDay(it.sleepAt); if (m < 12 * 60) m + 1440 else m }.average().toInt() % 1440
    val wake = done.map { minuteOfDay(it.wakeAt) }.average().toInt()
    val rated = done.filter { it.quality > 0 }
    return SleepSummary(
        count = done.size,
        totalMs = total,
        avgMs = total / done.size,
        avgBedMinute = bed,
        avgWakeMinute = wake,
        avgQuality = if (rated.isEmpty()) null else rated.map { it.quality }.average().toFloat(),
        longest = done.maxBy { it.durationMs },
        shortest = done.minBy { it.durationMs },
    )
}

fun List<SleepRecord>.inMonth(month: YearMonth): List<SleepRecord> =
    filter { !it.open && YearMonth.from(it.wakeDate()) == month }

fun List<SleepRecord>.inYear(year: Int): List<SleepRecord> =
    filter { !it.open && it.wakeDate().year == year }

/** Horas dormidas por cada día del mes (índice 0 = día 1). Si hay varias siestas el mismo día, se suman. */
fun List<SleepRecord>.hoursPerDay(month: YearMonth): List<Float> {
    val perDay = FloatArray(month.lengthOfMonth())
    for (r in inMonth(month)) perDay[r.wakeDate().dayOfMonth - 1] += r.durationMs / 3_600_000f
    return perDay.toList()
}

/** Promedio de horas por noche registrada, para cada mes del año (null = sin datos). */
fun List<SleepRecord>.avgHoursPerMonth(year: Int): List<Float?> =
    (1..12).map { m ->
        summarize(inMonth(YearMonth.of(year, m)))?.let { it.avgMs / 3_600_000f }
    }

/** "7h 32min" */
fun formatSleep(ms: Long): String {
    val totalMin = (ms / 60_000L).toInt()
    return "${totalMin / 60}h ${"%02d".format(totalMin % 60)}min"
}

/** Minutos desde las 00:00 -> "23:40" (o "11:40 PM" según el teléfono). */
fun minuteText(context: android.content.Context, minute: Int): String =
    clockText(context, (minute / 60) % 24, minute % 60)
