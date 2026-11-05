package com.example.aihub

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/** Выбор языка: авто (язык телефона, если он поддерживается, иначе английский) или один из списка. */
object L {
    private const val FILE = "settings"
    private const val KEY = "lang"

    /** Языки интерфейса: код и название на самом языке. «Авто» добавляется отдельно. */
    val LANGS: List<Pair<String, String>> = listOf(
        "en" to "English",
        "ru" to "Русский",
        "es" to "Español",
        "pt" to "Português",
        "fr" to "Français",
        "de" to "Deutsch",
        "it" to "Italiano",
        "tr" to "Türkçe",
        "pl" to "Polski",
        "id" to "Bahasa Indonesia",
        "vi" to "Tiếng Việt",
        "zh" to "简体中文",
        "ja" to "日本語",
        "ko" to "한국어",
        "ar" to "العربية",
        "hi" to "हिन्दी"
    )

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
