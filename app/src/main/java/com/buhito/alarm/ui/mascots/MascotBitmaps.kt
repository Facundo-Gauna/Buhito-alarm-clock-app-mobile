package com.buhito.alarm.ui.mascots

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * Dibuja el búho en un Bitmap, para usarlo fuera de Compose (por ejemplo en las notificaciones,
 * que no pueden mostrar composables). Reutiliza exactamente el mismo dibujo que la app.
 */
fun owlBitmap(
    sizePx: Int,
    mood: OwlMood,
    blink: Float = 1f,
    wingAngle: Float = 0f,
    look: Float = 0f,
): Bitmap {
    val image = ImageBitmap(sizePx, sizePx)
    val area = Size(sizePx.toFloat(), sizePx.toFloat())
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(image), area) {
        // fondo azul noche redondeado para que el búho se lea bien sobre cualquier notificación
        drawCircle(Color(0xFF16204A))
        val unit = sizePx / 200f
        scale(unit * 0.92f, unit * 0.92f, pivot = Offset.Zero) {
            drawOwl(
                mood = mood,
                blink = blink,
                look = look,
                wingAngle = wingAngle,
                level = 0f,
                wave = 0f,
            )
        }
    }
    return image.asAndroidBitmap()
}
