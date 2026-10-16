package com.buhito.alarm.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/** Un idioma de la lista: [tag] vacío = el del teléfono; [name] se muestra en su propio idioma. */
data class Language(val tag: String, val name: String)

/** Idioma de la app: se guarda en Prefs y se aplica envolviendo el contexto de cada pantalla/servicio. */
object Locales {
    val all = listOf(
        Language("en", "English"),
        Language("es", "Español"),
        Language("pt", "Português"),
        Language("fr", "Français"),
        Language("de", "Deutsch"),
        Language("it", "Italiano"),
        Language("ru", "Русский"),
        Language("zh", "中文"),
        Language("ja", "日本語"),
        Language("hi", "हिन्दी"),
        Language("ar", "العربية"),
    )

    /** Devuelve [base] con el idioma elegido; si no se eligió ninguno, queda el del teléfono. */
    fun wrap(base: Context): Context {
        val tag = Prefs.language(base)
        if (tag.isBlank()) return base
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return base.createConfigurationContext(config)
    }
}
