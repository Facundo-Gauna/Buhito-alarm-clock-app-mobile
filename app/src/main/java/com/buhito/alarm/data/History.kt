package com.buhito.alarm.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Resultados posibles de una alarma que sonó. */
object RingResult {
    const val RINGING = "RINGING"
    const val STOPPED = "STOPPED"
    const val SNOOZED = "SNOOZED"
    const val MISSED = "MISSED"
}

/** Una vez que sonó una alarma, y qué pasó con ella. Se guarda una copia de los datos por si la alarma se borra. */
@Entity(tableName = "history")
data class HistoryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val alarmId: Long,
    val label: String,
    val hour: Int,
    val minute: Int,
    val category: String,
    val firedAt: Long,
    val result: String = RingResult.RINGING,
)

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY firedAt DESC LIMIT 300")
    fun observeRecent(): Flow<List<HistoryEntry>>

    @Insert
    suspend fun insert(entry: HistoryEntry): Long

    @Query("UPDATE history SET result = :result WHERE id = :id")
    suspend fun setResult(id: Long, result: String)

    /** Lo que quedó "sonando" sin cerrarse (el servicio murió) cuenta como sin atender. */
    @Query("UPDATE history SET result = 'MISSED' WHERE result = 'RINGING'")
    suspend fun closeDangling()

    @Query("DELETE FROM history")
    suspend fun clear()

    @Query("DELETE FROM history WHERE firedAt < :before")
    suspend fun deleteBefore(before: Long)
}
