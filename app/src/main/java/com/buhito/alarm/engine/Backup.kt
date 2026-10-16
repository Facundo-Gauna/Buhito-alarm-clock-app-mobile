package com.buhito.alarm.engine

import android.content.Context
import android.net.Uri
import com.buhito.alarm.data.Alarm
import com.buhito.alarm.data.AppDatabase
import com.buhito.alarm.data.CustomCategory
import com.buhito.alarm.data.SleepRecord
import com.buhito.alarm.data.Stopwatch
import com.buhito.alarm.data.Template
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Copia de seguridad en un único .zip: `data.json` con alarmas, plantillas, categorías propias y
 * cronómetros, más una carpeta `sounds/` con los audios propios. Sin librerías extra (org.json y java.util.zip
 * vienen con Android). Restaurar REEMPLAZA lo que hay en la app.
 */
class Backup(
    private val context: Context,
    private val database: AppDatabase,
    private val repository: AlarmRepository,
    private val scheduler: AlarmScheduler,
) {
    /** Devuelve cuántas alarmas se guardaron. */
    suspend fun export(target: Uri): Int = withContext(Dispatchers.IO) {
        val alarms = database.alarmDao().getAll()
        val templates = database.templateDao().getAll()
        val categories = database.customCategoryDao().getAll()
        val stopwatches = database.stopwatchDao().getAll()
        val sleeps = database.sleepDao().getAll()

        val out = context.contentResolver.openOutputStream(target) ?: error("No se pudo abrir el destino")
        ZipOutputStream(out.buffered()).use { zip ->
            val written = HashSet<String>()
            fun addSound(path: String): String {
                val file = File(path)
                val name = file.name
                if (file.exists() && written.add(name)) {
                    zip.putNextEntry(ZipEntry("sounds/$name"))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
                return name
            }

            val root = JSONObject()
            root.put("version", 1)
            root.put("alarms", JSONArray().apply {
                for (a in alarms) put(JSONObject().apply {
                    put("id", a.id); put("label", a.label); put("hour", a.hour); put("minute", a.minute)
                    put("triggerAt", a.triggerAt); put("repeatDays", a.repeatDays); put("category", a.category)
                    put("enabled", a.enabled); put("soundName", a.soundName); put("customSound", a.customSound)
                    put("soundUri", if (a.customSound) addSound(a.soundUri) else a.soundUri)
                    put("volume", a.volume); put("loop", a.loop); put("durationSec", a.durationSec)
                    put("vibrate", a.vibrate); put("confirmStop", a.confirmStop); put("qrStop", a.qrStop); put("speakLabel", a.speakLabel)
                    put("chainCount", a.chainCount); put("chainGapMin", a.chainGapMin)
                })
            })
            root.put("templates", JSONArray().apply {
                for (t in templates) put(JSONObject().apply {
                    put("name", t.name); put("label", t.label); put("hour", t.hour); put("minute", t.minute)
                    put("repeatDays", t.repeatDays); put("category", t.category); put("soundName", t.soundName)
                    put("customSound", t.customSound)
                    put("soundUri", if (t.customSound) addSound(t.soundUri) else t.soundUri)
                    put("volume", t.volume); put("loop", t.loop); put("durationSec", t.durationSec)
                    put("vibrate", t.vibrate)
                })
            })
            root.put("categories", JSONArray().apply {
                for (c in categories) put(JSONObject().apply {
                    put("id", c.id); put("name", c.name); put("emoji", c.emoji); put("color", c.color)
                })
            })
            root.put("stopwatches", JSONArray().apply {
                for (w in stopwatches) put(JSONObject().apply {
                    put("name", w.name); put("accumulatedMs", w.accumulatedMs); put("runningSince", w.runningSince)
                    put("laps", w.laps); put("createdAt", w.createdAt)
                })
            })
            root.put("sleep", JSONArray().apply {
                for (r in sleeps) put(JSONObject().apply {
                    put("sleepAt", r.sleepAt); put("wakeAt", r.wakeAt); put("quality", r.quality); put("auto", r.auto)
                })
            })
            zip.putNextEntry(ZipEntry("data.json"))
            zip.write(root.toString().toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        alarms.size
    }

    /** Reemplaza todo con el contenido del .zip. Devuelve cuántas alarmas se restauraron. */
    suspend fun restore(source: Uri): Int = withContext(Dispatchers.IO) {
        // 1) Leer el zip: el JSON a memoria y los audios a archivos nuevos (nombre viejo -> ruta nueva).
        var json: JSONObject? = null
        val sounds = HashMap<String, String>()
        val soundDir = File(context.filesDir, "sounds").apply { mkdirs() }
        val input = context.contentResolver.openInputStream(source) ?: error("No se pudo abrir el archivo")
        ZipInputStream(input.buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                when {
                    entry.name == "data.json" -> json = JSONObject(String(zip.readBytes(), Charsets.UTF_8))
                    entry.name.startsWith("sounds/") && !entry.isDirectory -> {
                        val oldName = entry.name.removePrefix("sounds/").substringAfterLast('/')
                        val target = File(soundDir, "${UUID.randomUUID()}.${oldName.substringAfterLast('.', "audio")}")
                        target.outputStream().use { zip.copyTo(it) }
                        sounds[oldName] = target.absolutePath
                    }
                }
            }
        }
        val root = json ?: error("El archivo no es una copia de Buhito")

        fun soundPath(obj: JSONObject): Pair<String, Boolean> {
            val custom = obj.optBoolean("customSound")
            if (!custom) return obj.optString("soundUri") to false
            val path = sounds[obj.optString("soundUri")]
            return if (path != null) path to true else "" to false // falta el audio: queda la alarma predeterminada
        }

        val alarms = (0 until root.optJSONArray("alarms")!!.length()).map { i ->
            val o = root.getJSONArray("alarms").getJSONObject(i)
            val (uri, custom) = soundPath(o)
            Alarm(
                id = o.getLong("id"), label = o.optString("label"), hour = o.getInt("hour"), minute = o.getInt("minute"),
                triggerAt = o.getLong("triggerAt"), repeatDays = o.optInt("repeatDays"),
                category = o.optString("category", "GENERAL"), enabled = o.optBoolean("enabled", true),
                soundUri = uri, soundName = if (custom || uri.isNotBlank()) o.optString("soundName") else "",
                customSound = custom, volume = o.optInt("volume", 100), loop = o.optBoolean("loop", true),
                durationSec = o.optInt("durationSec", 120), vibrate = o.optBoolean("vibrate", true),
                confirmStop = o.optBoolean("confirmStop"), qrStop = o.optBoolean("qrStop"), speakLabel = o.optBoolean("speakLabel"),
                chainCount = o.optInt("chainCount"), chainGapMin = o.optInt("chainGapMin", 10),
            )
        }
        val templates = (0 until (root.optJSONArray("templates")?.length() ?: 0)).map { i ->
            val o = root.getJSONArray("templates").getJSONObject(i)
            val (uri, custom) = soundPath(o)
            Template(
                name = o.getString("name"), label = o.optString("label"), hour = o.getInt("hour"),
                minute = o.getInt("minute"), repeatDays = o.optInt("repeatDays"),
                category = o.optString("category", "GENERAL"), soundUri = uri,
                soundName = if (custom || uri.isNotBlank()) o.optString("soundName") else "", customSound = custom,
                volume = o.optInt("volume", 100), loop = o.optBoolean("loop", true),
                durationSec = o.optInt("durationSec", 120), vibrate = o.optBoolean("vibrate", true),
            )
        }
        val categories = (0 until (root.optJSONArray("categories")?.length() ?: 0)).map { i ->
            val o = root.getJSONArray("categories").getJSONObject(i)
            CustomCategory(id = o.getLong("id"), name = o.getString("name"), emoji = o.getString("emoji"), color = o.getLong("color"))
        }
        val stopwatches = (0 until (root.optJSONArray("stopwatches")?.length() ?: 0)).map { i ->
            val o = root.getJSONArray("stopwatches").getJSONObject(i)
            Stopwatch(
                name = o.getString("name"), accumulatedMs = o.optLong("accumulatedMs"),
                runningSince = o.optLong("runningSince"), laps = o.optString("laps"),
                createdAt = o.optLong("createdAt", System.currentTimeMillis()),
            )
        }

        val sleeps = (0 until (root.optJSONArray("sleep")?.length() ?: 0)).map { i ->
            val o = root.getJSONArray("sleep").getJSONObject(i)
            SleepRecord(sleepAt = o.getLong("sleepAt"), wakeAt = o.optLong("wakeAt"), quality = o.optInt("quality"), auto = o.optBoolean("auto"))
        }

        // 2) Reemplazar: cancelar las alarmas actuales, vaciar las tablas y cargar lo restaurado.
        for (old in database.alarmDao().getAll()) scheduler.cancel(old.id)
        database.alarmDao().deleteAll()
        database.templateDao().deleteAll()
        database.customCategoryDao().deleteAll()
        database.stopwatchDao().deleteAll()
        database.sleepDao().deleteAll()
        for (c in categories) database.customCategoryDao().insert(c)
        for (a in alarms) database.alarmDao().insert(a)
        for (t in templates) database.templateDao().insert(t)
        for (w in stopwatches) database.stopwatchDao().insert(w)
        for (r in sleeps) database.sleepDao().insert(r)

        // 3) Programar las alarmas y borrar los audios de antes que ya no se usan.
        repository.rescheduleAll()
        repository.deleteOrphanSounds()
        alarms.size
    }
}
