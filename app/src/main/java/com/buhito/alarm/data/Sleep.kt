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
 * Un período de sueño. [wakeAt] = 0 significa que la persona sigue durmiendo (sesión abierta).
 * [auto] marca los detectados solos. [quality] es la valoración de 1 a 5 al despertar (0 = sin valorar).
 * La noche se cuenta en el día en que te despertaste.
 */
@Entity(tableName = "sleep_records")
data class SleepRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sleepAt: Long,
    val wakeAt: Long = 0,
    val quality: Int = 0,
    /** true = lo detectó la app (por la pantalla del teléfono) y todavía no lo corregiste a mano. */
    val auto: Boolean = false,
) {
    val open: Boolean get() = wakeAt == 0L
    val durationMs: Long get() = if (open) 0 else (wakeAt - sleepAt).coerceAtLeast(0)
}

@Dao
interface SleepDao {
    @Query("SELECT * FROM sleep_records ORDER BY sleepAt DESC")
    fun observeAll(): Flow<List<SleepRecord>>

    @Query("SELECT * FROM sleep_records")
    suspend fun getAll(): List<SleepRecord>

    @Query("SELECT * FROM sleep_records WHERE wakeAt = 0 ORDER BY sleepAt DESC LIMIT 1")
    suspend fun getOpen(): SleepRecord?

    @Insert
    suspend fun insert(record: SleepRecord): Long

    @Update
    suspend fun update(record: SleepRecord)

    @Delete
    suspend fun delete(record: SleepRecord)

    @Query("DELETE FROM sleep_records")
    suspend fun deleteAll()
}
