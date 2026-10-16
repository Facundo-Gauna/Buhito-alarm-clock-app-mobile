package com.buhito.alarm.engine

import android.content.Context
import com.buhito.alarm.data.SleepDao
import com.buhito.alarm.data.SleepRecord
import com.buhito.alarm.util.Prefs
import com.buhito.alarm.util.TimeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Registro del sueño: detección automática por la pantalla del teléfono + carga/edición manual. */
class SleepRepository(private val context: Context, private val dao: SleepDao) {
    val records = dao.observeAll()

    /**
     * Busca sueños nuevos desde la última revisión y los guarda como automáticos.
     * Devuelve los que encontró (vacío si está apagado, sin permiso o no había nada nuevo).
     *
     * Solo cuentan los apagones de pantalla que terminaron después de la última revisión, así lo que
     * borrás a mano no vuelve a aparecer. La primera vez mira la última semana.
     */
    suspend fun detectNew(): List<SleepRecord> = withContext(Dispatchers.IO) {
        if (!SleepDetector.supported || !Prefs.sleepAuto(context) || !SleepDetector.hasAccess(context)) {
            return@withContext emptyList()
        }
        val now = System.currentTimeMillis()
        val last = Prefs.sleepLastScan(context).takeIf { it > 0 } ?: (now - 7 * DAY)
        val minMs = Prefs.sleepMinHours(context) * HOUR
        val delayMs = Prefs.sleepDelayMin(context) * 60_000L
        val bedFrom = Prefs.sleepBedFromHour(context)

        // Se mira un poco antes de la última revisión para ver dónde empezó un sueño que estaba en curso.
        val spans = SleepDetector.merge(SleepDetector.screenOffSpans(context, last - 18 * HOUR, now), 15 * 60_000L)
        val existing = dao.getAll()
        val found = spans
            .filter { s ->
                val length = s.end - s.start
                val startHour = TimeUtil.toLocal(s.start).hour
                length in minMs..(16 * HOUR) &&
                    s.end > last &&
                    (startHour >= bedFrom || startHour < 6) &&
                    existing.none { it.sleepAt < s.end && it.wakeAt > s.start } // ya cargada a mano
            }
            .map { SleepRecord(sleepAt = it.start + delayMs, wakeAt = it.end, auto = true) }
            .filter { it.wakeAt - it.sleepAt >= 60_000L }
        for (r in found) dao.insert(r)
        Prefs.setSleepLastScan(context, now)
        found
    }

    suspend fun save(record: SleepRecord) {
        if (record.id == 0L) dao.insert(record) else dao.update(record)
    }

    suspend fun delete(record: SleepRecord) = dao.delete(record)

    private companion object {
        const val HOUR = 3_600_000L
        const val DAY = 24 * HOUR
    }
}
