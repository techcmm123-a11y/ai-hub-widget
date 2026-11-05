package com.example.aihub

import android.content.Context
import android.content.res.Configuration

/** Тема оформления: как в системе, светлая или тёмная. */
object Skin {
    private const val FILE = "settings"
    private const val KEY = "theme"

    fun pref(context: Context): String =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY, "auto") ?: "auto"

    fun setPref(context: Context, theme: String) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString(KEY, theme).apply()
    }

    fun isDark(context: Context): Boolean = when (pref(context)) {
        "light" -> false
        "dark" -> true
        else -> (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
    }

    /** Ресурсы и цвета для виджетов при принудительной теме. */
    class Palette(
        val text: Int,
        val sub: Int,
        val cardRes: Int,
        val thinRes: Int,
        val rowRes: Int,
        val chipRes: Int
    )

    /** null при «как в системе»: тогда виджеты берут цвета из ресурсов с ночным вариантом. */
    fun forced(context: Context): Palette? = when (pref(context)) {
        "light" -> Palette(
            0xFF16181D.toInt(), 0xFF566070.toInt(),
            R.drawable.card_bg_light, R.drawable.thin_bg_light, R.drawable.row_bg_light, R.drawable.chip_bg_light
        )
        "dark" -> Palette(
            0xFFF2F4F8.toInt(), 0xFF9AA3B2.toInt(),
            R.drawable.card_bg_dark, R.drawable.thin_bg_dark, R.drawable.row_bg_dark, R.drawable.chip_bg_dark
        )
        else -> null
    }
}
