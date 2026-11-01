package com.example.aihub

import android.content.Context
import android.content.Intent

object Open {

    /** Намерение для открытия входа: приложение либо сайт в выбранном браузере. */
    fun intentFor(context: Context, svc: ServiceCfg, e: EntryCfg): Intent {
        if (e.mode == "app") {
            val pkg = Apps.packageFor(context, svc)
            val launch = if (pkg != null) context.packageManager.getLaunchIntentForPackage(pkg) else null
            if (launch != null) return launch
        }
        val url = if (e.url.isNotEmpty()) e.url else svc.url
        return Browsers.siteIntent(context, url, e.browserPkg)
    }
}
