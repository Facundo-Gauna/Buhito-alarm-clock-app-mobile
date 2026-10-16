package com.buhito.alarm.engine

import android.content.Context
import com.buhito.alarm.data.Alarm
import com.buhito.alarm.data.AlarmDao
import com.buhito.alarm.data.AudioStorage
import com.buhito.alarm.data.CustomCategory
import com.buhito.alarm.data.CustomCategoryDao
import com.buhito.alarm.data.HistoryDao
import com.buhito.alarm.data.HistoryEntry
import com.buhito.alarm.data.RingResult
import com.buhito.alarm.data.customKey
import com.buhito.alarm.data.Template
import com.buhito.alarm.data.TemplateDao
import com.buhito.alarm.util.TimeUtil
import com.buhito.alarm.util.nextRepeating
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/** Único punto de entrada para crear/editar alarmas: mantiene la base de datos y el AlarmManager sincronizados. */
class AlarmRepository(
    private val context: Context,
    private val dao: AlarmDao,
    private val templateDao: TemplateDao,
    private val categoryDao: CustomCategoryDao,
    private val historyDao: HistoryDao,
    private val scheduler: AlarmScheduler,
) {
    val alarms: Flow<List<Alarm>> = dao.observeAll()
    val templates: Flow<List<Template>> = templateDao.observeAll()
    val customCategories: Flow<List<CustomCategory>> = categoryDao.observeAll()
    val history: Flow<List<HistoryEntry>> = historyDao.observeRecent()

    suspend fun get(id: Long): Alarm? = dao.get(id)

    suspend fun save(alarm: Alarm): Alarm {
        val old = if (alarm.id != 0L) dao.get(alarm.id) else null
        val id = if (alarm.id == 0L) dao.insert(alarm) else alarm.also { dao.update(it) }.id
        val saved = alarm.copy(id = id)
        if (saved.enabled) scheduler.schedule(saved) else scheduler.cancel(id)
        // Si cambió el audio propio, borramos la copia anterior.
        if (old != null && old.customSound && (!saved.customSound || old.soundUri != saved.soundUri)) {
            AudioStorage.delete(old.soundUri)
        }
        return saved
    }

    suspend fun setEnabled(alarm: Alarm, enabled: Boolean) {
        if (!enabled) {
            save(alarm.copy(enabled = false, chainLeft = 0))
            return
        }
        // Al reactivar, vuelve a la próxima ocurrencia de su hora.
        val trigger = when {
            alarm.repeatDays != 0 -> nextRepeating(alarm.hour, alarm.minute, alarm.repeatDays)
            alarm.triggerAt > System.currentTimeMillis() -> alarm.triggerAt
            else -> TimeUtil.nextOccurrence(alarm.hour, alarm.minute)
        }
        save(alarm.copy(enabled = true, triggerAt = trigger, chainLeft = 0))
    }

    suspend fun delete(alarm: Alarm) {
        scheduler.cancel(alarm.id)
        dao.delete(alarm)
        if (alarm.customSound) AudioStorage.delete(alarm.soundUri)
    }

    /** Datos de un timbre que acaba de empezar. */
    class RingStart(val alarm: Alarm, val historyId: Long, val chainRemaining: Int)

    /**
     * La alarma acaba de empezar a sonar. Deja todo listo para después:
     *  - si se repite por días, queda programada para el próximo día; si era de una sola vez, se desactiva;
     *  - cuenta en qué timbre de la secuencia está (ver [continueChain]);
     *  - anota la vez en el historial.
     * Devuelve la alarma tal como estaba al sonar, o null si ya no existe.
     */
    suspend fun startRing(id: Long): RingStart? {
        val alarm = dao.get(id) ?: return null
        val left = when {
            alarm.chainLeft < 0 -> 0                    // viene de posponer: no inicia secuencia
            alarm.chainLeft > 0 -> alarm.chainLeft - 1  // timbre de seguimiento de una secuencia
            else -> alarm.chainCount                    // primer timbre: quedan chainCount más
        }
        if (alarm.repeatDays != 0) {
            save(alarm.copy(triggerAt = nextRepeating(alarm.hour, alarm.minute, alarm.repeatDays), chainLeft = left))
        } else {
            dao.update(alarm.copy(enabled = false, chainLeft = left))
        }
        historyDao.closeDangling()
        historyDao.deleteBefore(System.currentTimeMillis() - 90L * 24 * 60 * 60_000L)
        val historyId = historyDao.insert(
            HistoryEntry(
                alarmId = alarm.id,
                label = alarm.label,
                hour = alarm.hour,
                minute = alarm.minute,
                category = alarm.category,
                firedAt = System.currentTimeMillis(),
            ),
        )
        return RingStart(alarm, historyId, left)
    }

    /** Terminó el timbre: anota qué pasó. Si queda secuencia, programa el siguiente timbre. */
    suspend fun finishRing(historyId: Long, result: String, alarmId: Long?, continueChain: Boolean) {
        if (historyId != 0L) historyDao.setResult(historyId, result)
        if (alarmId == null) return
        val alarm = dao.get(alarmId) ?: return
        if (continueChain && alarm.chainLeft > 0) {
            save(alarm.copy(enabled = true, triggerAt = System.currentTimeMillis() + alarm.chainGapMin * 60_000L))
        } else if (alarm.chainLeft != 0 && result != RingResult.SNOOZED) {
            dao.update(alarm.copy(chainLeft = 0)) // secuencia cancelada
        }
    }

    /** Posponer: la misma alarma se reprograma dentro de [minutes] minutos (y cancela lo que quedaba de la secuencia). */
    suspend fun snooze(id: Long, minutes: Int) {
        val alarm = dao.get(id) ?: return
        save(alarm.copy(enabled = true, triggerAt = System.currentTimeMillis() + minutes * 60_000L, chainLeft = -1))
    }

    /** Tras un reinicio Android borra las alarmas: las volvemos a programar. */
    suspend fun rescheduleAll() {
        val now = System.currentTimeMillis()
        for (alarm in dao.getAll().filter { it.enabled }) {
            when {
                alarm.triggerAt > now -> scheduler.schedule(alarm)
                // Se perdió mientras el teléfono estaba apagado: las repetidas pasan al próximo día.
                alarm.repeatDays != 0 ->
                    save(alarm.copy(triggerAt = nextRepeating(alarm.hour, alarm.minute, alarm.repeatDays)))
                else -> dao.update(alarm.copy(enabled = false))
            }
        }
    }

    // ---- Plantillas ----

    suspend fun saveTemplate(template: Template) {
        val toSave = if (template.customSound) {
            // La plantilla se queda con su propia copia del audio.
            val copy = withContext(Dispatchers.IO) { AudioStorage.duplicate(context, template.soundUri) }
            if (copy != null) template.copy(soundUri = copy)
            else template.copy(soundUri = "", soundName = "", customSound = false)
        } else {
            template
        }
        templateDao.insert(toSave)
    }

    suspend fun deleteTemplate(template: Template) {
        templateDao.delete(template)
        if (template.customSound) AudioStorage.delete(template.soundUri)
    }

    /** Borra los audios copiados que ya no usa ninguna alarma ni plantilla. */
    suspend fun deleteOrphanSounds() {
        val used = dao.getAll().filter { it.customSound }.map { it.soundUri } +
            templateDao.getAll().filter { it.customSound }.map { it.soundUri }
        AudioStorage.deleteExcept(context, used.toSet())
    }

    // ---- Categorías propias ----

    suspend fun addCategory(name: String, emoji: String, color: Long) {
        categoryDao.insert(CustomCategory(name = name.trim(), emoji = emoji, color = color))
    }

    suspend fun deleteCategory(category: CustomCategory) {
        val key = customKey(category.id)
        dao.resetCategory(key)
        templateDao.resetCategory(key)
        categoryDao.delete(category)
    }

    // ---- Calendario ----

    /**
     * Crea una alarma de una sola vez por cada evento, [minutesBefore] minutos antes de que empiece
     * (las que ya pasaron se omiten). Devuelve cuántas creó.
     */
    suspend fun createFromCalendar(events: List<CalendarEvent>, minutesBefore: Int, category: String): Int {
        val now = System.currentTimeMillis()
        var created = 0
        for (e in events) {
            val at = e.beginMillis - minutesBefore * 60_000L
            if (at <= now) continue
            val time = TimeUtil.toLocal(at)
            save(
                Alarm(
                    label = e.title.take(60),
                    hour = time.hour,
                    minute = time.minute,
                    triggerAt = TimeUtil.triggerFor(time.toLocalDate(), time.hour, time.minute),
                    category = category,
                ),
            )
            created++
        }
        return created
    }

    // ---- Historial ----

    suspend fun clearHistory() = historyDao.clear()
}
