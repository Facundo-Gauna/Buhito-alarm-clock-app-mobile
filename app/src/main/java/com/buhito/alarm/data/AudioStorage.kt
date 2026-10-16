package com.buhito.alarm.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.IOException
import java.util.UUID

/**
 * Copia los audios elegidos por el usuario a la carpeta interna de la app, así la alarma
 * sigue sonando aunque el archivo original se mueva o se borre.
 */
object AudioStorage {
    data class Imported(val path: String, val displayName: String)

    private fun dir(context: Context): File =
        File(context.filesDir, "sounds").apply { mkdirs() }

    /** Bloqueante: llamar desde un hilo de IO. */
    fun import(context: Context, uri: Uri): Imported {
        val resolver = context.contentResolver
        var name = "audio"
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (i >= 0) c.getString(i)?.let { name = it }
            }
        }
        val ext = name.substringAfterLast('.', "").lowercase().takeIf { it.length in 1..5 } ?: "audio"
        val target = File(dir(context), "${UUID.randomUUID()}.$ext")
        val input = resolver.openInputStream(uri) ?: throw IOException("Cannot open $uri")
        input.use { source -> target.outputStream().use { source.copyTo(it) } }
        return Imported(target.absolutePath, name.substringBeforeLast('.', name))
    }

    /** Archivo nuevo, dentro de la carpeta interna, donde se guarda una grabación de voz. */
    fun newRecordingFile(context: Context): File = File(dir(context), "rec-${UUID.randomUUID()}.m4a")

    /**
     * Copia un audio ya guardado a un archivo nuevo, así una alarma y una plantilla
     * nunca comparten archivo (borrar una no rompe la otra). Bloqueante.
     */
    fun duplicate(context: Context, path: String): String? = runCatching {
        val source = File(path)
        val target = File(dir(context), "${UUID.randomUUID()}.${source.extension.ifEmpty { "audio" }}")
        source.copyTo(target)
        target.absolutePath
    }.getOrNull()

    fun delete(path: String) {
        runCatching { File(path).delete() }
    }

    /** Borra los audios copiados que ya no usa ninguna alarma. */
    fun deleteExcept(context: Context, keep: Set<String>) {
        dir(context).listFiles()?.filter { it.absolutePath !in keep }?.forEach { it.delete() }
    }
}
