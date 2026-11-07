package com.example.aihub

import android.net.Uri

/** Проверка ссылок: разрешены только https-адреса с именем сайта и без логина в адресе. */
object Urls {

    /** Возвращает нормализованную https-ссылку или пустую строку, если ссылка недопустима. */
    fun safe(raw: String): String {
        var s = raw.trim()
        if (s.isEmpty() || s.length > 2048) return ""
        if (s.startsWith("http://", ignoreCase = true)) {
            s = "https://" + s.substring(7)
        } else if (!s.contains("://")) {
            s = "https://" + s
        }
        val uri = Uri.parse(s)
        if (!"https".equals(uri.scheme, ignoreCase = true)) return ""
        val host = uri.host ?: return ""
        if (host.isEmpty() || host.contains(" ") || uri.userInfo != null) return ""
        return s
    }

    /** Имя сайта из ссылки или null. */
    fun host(url: String): String? {
        val h = Uri.parse(url).host
        return if (h.isNullOrEmpty()) null else h
    }
}
