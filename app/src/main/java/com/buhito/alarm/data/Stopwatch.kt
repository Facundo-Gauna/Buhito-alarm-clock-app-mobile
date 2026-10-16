package com.buhito.alarm.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Un cronómetro guardado. No se guarda "el tiempo que va", sino dos datos que permiten
 * calcularlo siempre: lo acumulado hasta la última pausa y desde cuándo está corriendo.
 * Así sigue contando aunque cierres la app.
 */
@Entity(tableName = "stopwatches")
data class Stopwatch(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val accumulatedMs: Long = 0,
    /** Momento (reloj de pared) en que empezó a correr; 0 si está en pausa. */
    val runningSince: Long = 0,
    /** Tiempos totales de cada vuelta, separados por coma ("12300,40100"). */
    val laps: String = "",
    val createdAt: Long = System.currentTimeMillis(),
) {
    val running: Boolean get() = runningSince > 0

    fun elapsed(now: Long): Long = accumulatedMs + if (running) (now - runningSince).coerceAtLeast(0) else 0

    fun lapList(): List<Long> = if (laps.isBlank()) emptyList() else laps.split(",").mapNotNull { it.toLongOrNull() }
}

@Dao
interface StopwatchDao {
    @Query("SELECT * FROM stopwatches ORDER BY createdAt")
    fun observeAll(): Flow<List<Stopwatch>>

    @Insert
    suspend fun insert(stopwatch: Stopwatch): Long

    @Update
    suspend fun update(stopwatch: Stopwatch)

    @Delete
    suspend fun delete(stopwatch: Stopwatch)

    @Query("SELECT * FROM stopwatches")
    suspend fun getAll(): List<Stopwatch>

    @Query("DELETE FROM stopwatches")
    suspend fun deleteAll()
}
