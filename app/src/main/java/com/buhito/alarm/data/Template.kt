package com.buhito.alarm.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Una alarma "preconfigurada" (hora, sonido y comportamiento) para crear alarmas nuevas con un toque. */
@Entity(tableName = "templates")
data class Template(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val label: String = "",
    val hour: Int,
    val minute: Int,
    val repeatDays: Int = 0,
    val category: String = Category.GENERAL.name,
    val soundUri: String = "",
    val soundName: String = "",
    val customSound: Boolean = false,
    val volume: Int = 100,
    val loop: Boolean = true,
    val durationSec: Int = 120,
    val vibrate: Boolean = true,
)

@Dao
interface TemplateDao {
    @Query("SELECT * FROM templates ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<Template>>

    @Query("SELECT * FROM templates")
    suspend fun getAll(): List<Template>

    @Insert
    suspend fun insert(template: Template): Long

    @Delete
    suspend fun delete(template: Template)

    @Query("DELETE FROM templates")
    suspend fun deleteAll()

    @Query("UPDATE templates SET category = 'GENERAL' WHERE category = :key")
    suspend fun resetCategory(key: String)
}
