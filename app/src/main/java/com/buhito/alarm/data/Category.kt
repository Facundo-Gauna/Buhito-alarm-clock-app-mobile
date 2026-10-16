package com.buhito.alarm.data

import androidx.annotation.StringRes
import com.buhito.alarm.R

/** Categoría de una alarma: un emoji, un color propio y su nombre traducido. */
enum class Category(val emoji: String, val color: Long, @StringRes val nameRes: Int) {
    GENERAL("🔔", 0xFF8CA9FF, R.string.cat_general),
    STUDY("📚", 0xFF7EC8FF, R.string.cat_study),
    GYM("💪", 0xFFFF9F7A, R.string.cat_gym),
    WORK("💼", 0xFFB59CFF, R.string.cat_work),
    HEALTH("🩺", 0xFF7BE0B0, R.string.cat_health),
    PERSONAL("⭐", 0xFFFFC76B, R.string.cat_personal);

    companion object {
        fun from(name: String): Category = entries.firstOrNull { it.name == name } ?: GENERAL
    }
}
