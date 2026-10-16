package com.buhito.alarm.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Una alarma. [hour] y [minute] son la hora "de reloj" (no cambian al posponer);
 * [triggerAt] es la próxima vez que suena (o la hora a la que se pospuso).
 *
 * @param repeatDays Días de la semana en bits: bit 0 = lunes ... bit 6 = domingo. 0 = una sola vez.
 * @param soundUri   Uri de un tono del sistema, o ruta del archivo copiado a la carpeta interna cuando
 *                   [customSound] es true (audio elegido o grabación). Vacío = alarma predeterminada.
 * @param volume     10..100, porcentaje sobre el volumen de alarma del teléfono.
 * @param loop       true = el sonido se repite durante la duración; false = suena una vez (la pantalla sigue).
 * @param confirmStop true = para detenerla hay que mantener presionado el botón.
 * @param qrStop     true = para detenerla hay que escanear el código QR de Buhito (si no hay cámara, se mantiene el botón).
 * @param speakLabel true = el teléfono lee la etiqueta en voz alta mientras suena.
 * @param chainCount timbres extra de la secuencia después del primero (0 = sin secuencia).
 * @param chainGapMin minutos entre un timbre de la secuencia y el siguiente.
 * @param chainLeft  uso interno: timbres de la secuencia que faltan (-1 = el próximo viene de posponer).
 */
@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String = "",
    val hour: Int,
    val minute: Int,
    val triggerAt: Long,
    val repeatDays: Int = 0,
    val category: String = Category.GENERAL.name,
    val enabled: Boolean = true,
    val soundUri: String = "",
    val soundName: String = "",
    val customSound: Boolean = false,
    val volume: Int = 100,
    val loop: Boolean = true,
    val durationSec: Int = 120,
    val vibrate: Boolean = true,
    val confirmStop: Boolean = false,
    val speakLabel: Boolean = false,
    val qrStop: Boolean = false,
    val chainCount: Int = 0,
    val chainGapMin: Int = 10,
    val chainLeft: Int = 0,
)
