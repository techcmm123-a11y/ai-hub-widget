package com.example.aihub

import android.content.Context
import android.content.Intent
import android.net.Uri

object Open {

    /**
     * Намерение для кнопок виджетов: промежуточный экран, который в момент нажатия сам проверяет,
     * установлено ли приложение. Так кнопка работает сразу после установки, без обновления виджета.
     */
    fun intentFor(context: Context, svc: ServiceCfg, e: EntryCfg): Intent =
        Intent(context, LaunchActivity::class.java)
            .setData(Uri.parse("aistart://open/" + svc.id + "/" + e.id))
            .putExtra("service", svc.id)
            .putExtra("entry", e.id)

    /**
     * Что открыть прямо сейчас: приложение либо сайт в выбранном браузере.
     * Если приложения нет, открывается окно выбора: Google Play или сайт.
     */
    fun resolveNow(context: Context, svc: ServiceCfg, e: EntryCfg): Intent {
        if (e.mode == "app" || e.mode == "pwa") {
            val pkg = Apps.packageForEntry(context, svc, e)
            val launch = if (pkg != null) context.packageManager.getLaunchIntentForPackage(pkg) else null
            if (launch != null) return launch
            return Intent(context, ChooserActivity::class.java)
                .putExtra("service", svc.id)
                .putExtra("browser", e.browserPkg)
                .putExtra("url", e.url)
                .putExtra("kind", e.mode)
        }
        val url = if (e.url.isNotEmpty()) e.url else svc.url
        return Browsers.siteIntent(context, url, e.browserPkg)
    }
}
