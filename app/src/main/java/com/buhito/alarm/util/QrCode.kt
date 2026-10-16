package com.buhito.alarm.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.content.ContextCompat
import com.buhito.alarm.data.Alarm
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

/** Dibuja el texto como un código QR (negro sobre blanco, para que cualquier cámara lo lea). */
fun qrBitmap(text: String, size: Int = 768): Bitmap {
    val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, mapOf(EncodeHintType.MARGIN to 2))
    val pixels = IntArray(size * size) { i ->
        if (matrix.get(i % size, i / size)) Color.BLACK else Color.WHITE
    }
    return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
}

fun cameraGranted(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

/**
 * true = esta alarma solo se apaga con el QR: la alarma lo pide, el modo estricto está encendido y hay cámara
 * permitida (sin permiso de cámara no se bloquea, para que nunca quede imposible de apagar).
 */
fun qrOnly(context: Context, alarm: Alarm?): Boolean =
    alarm?.qrStop == true && Prefs.qrStrict(context) && cameraGranted(context)
