package com.example.aihub

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

object Browsers {

    class Info(val pkg: String, val label: String)

    /** Установленные браузеры (приложения, обрабатывающие обычные https-ссылки). */
    fun installed(context: Context): List<Info> {
        val probe = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com"))
            .addCategory(Intent.CATEGORY_BROWSABLE)
        val pm = context.packageManager
        return pm.queryIntentActivities(probe, PackageManager.MATCH_ALL)
            .map { Info(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
            .distinctBy { it.pkg }
            .sortedBy { it.label.lowercase() }
    }

    /** Открывает сайт в выбранном браузере, а если он не выбран или удалён, в браузере по умолчанию. */
    fun siteIntent(context: Context, url: String, browserPkg: String): Intent {
        if (browserPkg.isNotEmpty()) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).setPackage(browserPkg)
            if (intent.resolveActivity(context.packageManager) != null) return intent
        }
        val fallback = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_BROWSER)
        fallback.data = Uri.parse(url)
        return fallback
    }
}
