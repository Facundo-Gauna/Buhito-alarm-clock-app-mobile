package com.buhito.alarm.util

import android.content.Context

/** Ajustes sencillos de la app (SharedPreferences). Cada uno tiene su valor por defecto. */
object Prefs {
    private fun p(context: Context) = context.getSharedPreferences("buhito_prefs", Context.MODE_PRIVATE)

    // ---- Código QR para detener alarmas ----
    /** El texto del QR de este teléfono; se crea una vez y se mantiene (así el QR impreso sigue sirviendo). */
    fun qrCode(c: Context): String =
        p(c).getString("qr_code", null) ?: newQrCode(c)

    fun newQrCode(c: Context): String {
        val code = "buhito:" + java.util.UUID.randomUUID().toString()
        p(c).edit().putString("qr_code", code).apply()
        return code
    }

    // ---- Idioma y tutorial ----
    /** Etiqueta del idioma elegido ("es", "ja"...); vacío = el del teléfono. */
    fun language(c: Context): String = p(c).getString("language", "") ?: ""
    fun setLanguage(c: Context, tag: String) = p(c).edit().putString("language", tag).apply()

    fun onboardingDone(c: Context): Boolean = p(c).getBoolean("onboarding_done", false)
    fun setOnboardingDone(c: Context, done: Boolean) = p(c).edit().putBoolean("onboarding_done", done).apply()

    /** Modo estricto: una alarma con QR no ofrece posponer ni otra forma de apagarla. */
    fun qrStrict(c: Context): Boolean = p(c).getBoolean("qr_strict", false)
    fun setQrStrict(c: Context, on: Boolean) = p(c).edit().putBoolean("qr_strict", on).apply()

    // ---- Calendario ----
    fun calendarDays(c: Context): Int = p(c).getInt("cal_days", 30)
    fun setCalendarDays(c: Context, days: Int) = p(c).edit().putInt("cal_days", days).apply()

    // ---- Sueño automático ----
    fun sleepAuto(c: Context): Boolean = p(c).getBoolean("sleep_auto", true)
    fun setSleepAuto(c: Context, on: Boolean) = p(c).edit().putBoolean("sleep_auto", on).apply()

    /** Horas mínimas seguidas con la pantalla apagada para contarlo como sueño. */
    fun sleepMinHours(c: Context): Int = p(c).getInt("sleep_min_hours", 3)
    fun setSleepMinHours(c: Context, h: Int) = p(c).edit().putInt("sleep_min_hours", h).apply()

    /** Minutos que tardás en dormirte después de apagar la pantalla (se suman a la hora de dormir). */
    fun sleepDelayMin(c: Context): Int = p(c).getInt("sleep_delay_min", 10)
    fun setSleepDelayMin(c: Context, m: Int) = p(c).edit().putInt("sleep_delay_min", m).apply()

    /** Desde qué hora de la noche puede empezar un sueño (hasta las 6 de la madrugada). */
    fun sleepBedFromHour(c: Context): Int = p(c).getInt("sleep_bed_from", 20)
    fun setSleepBedFromHour(c: Context, h: Int) = p(c).edit().putInt("sleep_bed_from", h).apply()

    /** Hora de la mañana a la que se revisa y se avisa. */
    fun sleepCheckHour(c: Context): Int = p(c).getInt("sleep_check_hour", 9)
    fun setSleepCheckHour(c: Context, h: Int) = p(c).edit().putInt("sleep_check_hour", h).apply()

    /** Hasta cuándo ya se revisó el uso del teléfono (0 = nunca). */
    fun sleepLastScan(c: Context): Long = p(c).getLong("sleep_last_scan", 0L)
    fun setSleepLastScan(c: Context, t: Long) = p(c).edit().putLong("sleep_last_scan", t).apply()
}
