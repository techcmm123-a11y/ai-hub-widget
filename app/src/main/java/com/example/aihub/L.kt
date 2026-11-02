package com.example.aihub

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/** Выбор языка: авто (русский, если телефон на русском, иначе английский), русский или английский. */
object L {
    private const val FILE = "settings"
    private const val KEY = "lang"

    fun pref(context: Context): String =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY, "auto") ?: "auto"

    fun setPref(context: Context, lang: String) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString(KEY, lang).apply()
    }

    /** Контекст с выбранным языком. При «авто» язык определяет система. */
    fun wrap(base: Context): Context {
        val lang = pref(base)
        if (lang == "auto") return base
        val conf = Configuration(base.resources.configuration)
        conf.setLocale(Locale(lang))
        return base.createConfigurationContext(conf)
    }
}
