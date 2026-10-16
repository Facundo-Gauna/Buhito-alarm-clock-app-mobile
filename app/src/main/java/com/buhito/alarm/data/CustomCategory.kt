package com.buhito.alarm.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Categoría creada por el usuario (las de fábrica están en [Category]). */
@Entity(tableName = "custom_categories")
data class CustomCategory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String,
    val color: Long,
)

@Dao
interface CustomCategoryDao {
    @Query("SELECT * FROM custom_categories ORDER BY id")
    fun observeAll(): Flow<List<CustomCategory>>

    @Query("SELECT * FROM custom_categories")
    suspend fun getAll(): List<CustomCategory>

    @Insert
    suspend fun insert(category: CustomCategory): Long

    @Delete
    suspend fun delete(category: CustomCategory)

    @Query("DELETE FROM custom_categories")
    suspend fun deleteAll()
}

/**
 * Una categoría lista para mostrar (de fábrica o propia). [key] es lo que se guarda en la alarma:
 * el nombre del enum para las de fábrica, o "custom_<id>" para las propias.
 */
data class CategoryInfo(val key: String, val emoji: String, val color: Long, val name: String)

fun customKey(id: Long) = "custom_$id"

/** Todas las categorías disponibles: primero las de fábrica, después las propias. */
fun allCategories(context: Context, customs: List<CustomCategory>): List<CategoryInfo> =
    Category.entries.map { CategoryInfo(it.name, it.emoji, it.color, context.getString(it.nameRes)) } +
        customs.map { CategoryInfo(customKey(it.id), it.emoji, it.color, it.name) }

/** Busca una categoría por su clave; si ya no existe (se borró), cae en "General". */
fun resolveCategory(context: Context, key: String, customs: List<CustomCategory>): CategoryInfo {
    val all = allCategories(context, customs)
    return all.firstOrNull { it.key == key } ?: all.first()
}
