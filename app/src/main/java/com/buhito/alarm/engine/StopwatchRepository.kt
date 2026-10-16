package com.buhito.alarm.engine

import com.buhito.alarm.data.Stopwatch
import com.buhito.alarm.data.StopwatchDao

/** Operaciones de los cronómetros: crear, iniciar/pausar, vuelta, reiniciar y borrar. */
class StopwatchRepository(private val dao: StopwatchDao) {
    val stopwatches = dao.observeAll()

    suspend fun create(name: String) {
        dao.insert(Stopwatch(name = name.trim()))
    }

    suspend fun toggle(sw: Stopwatch) {
        val now = System.currentTimeMillis()
        dao.update(
            if (sw.running) sw.copy(accumulatedMs = sw.elapsed(now), runningSince = 0)
            else sw.copy(runningSince = now),
        )
    }

    suspend fun lap(sw: Stopwatch) {
        if (!sw.running) return
        val total = sw.elapsed(System.currentTimeMillis())
        dao.update(sw.copy(laps = (sw.lapList() + total).joinToString(",")))
    }

    suspend fun reset(sw: Stopwatch) {
        dao.update(sw.copy(accumulatedMs = 0, runningSince = 0, laps = ""))
    }

    suspend fun delete(sw: Stopwatch) = dao.delete(sw)
}
