package com.example.aihub

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable

object Apps {

    class App(val pkg: String, val label: String)

    private var cache: List<App>? = null
    private var cacheAt = 0L

    /** Все приложения, у которых есть значок запуска (кроме самого ИИ-запуска). Список кэшируется на 15 секунд. */
    fun launchable(context: Context): List<App> {
        val now = System.currentTimeMillis()
        val cached = cache
        if (cached != null && now - cacheAt < 15000L) return cached
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val list = pm.queryIntentActivities(intent, 0)
            .map { App(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
            .filter { it.pkg != context.packageName }
            .distinctBy { it.pkg }
            .sortedBy { it.label.lowercase() }
        cache = list
        cacheAt = now
        return list
    }

    fun isInstalled(context: Context, pkg: String): Boolean =
        pkg.isNotEmpty() && context.packageManager.getLaunchIntentForPackage(pkg) != null

    /** Пакет приложения сервиса: выбранный вручную, известный или найденный по названию. */
    fun packageFor(context: Context, svc: ServiceCfg): String? {
        if (isInstalled(context, svc.pkg)) return svc.pkg
        val def = Catalog.byId(svc.id)
        if (def != null) {
            for (p in def.pkgs) {
                if (isInstalled(context, p)) return p
            }
        }
        // Поиск по названию только для основных ИИ и своих сервисов: короткие названия
        // (Bolt, Opal, Pika) легко совпадают с посторонними приложениями.
        val labelMatchAllowed = svc.custom || (def != null && def.pkgs.isNotEmpty())
        if (!labelMatchAllowed) return null
        val re = Regex("(^|[^\\p{L}\\p{N}])" + Regex.escape(svc.title) + "([^\\p{L}\\p{N}]|$)", RegexOption.IGNORE_CASE)
        val found = launchable(context).firstOrNull { re.containsMatchIn(it.label) }
        return found?.pkg
    }

    fun label(context: Context, pkg: String?): String? {
        if (pkg == null) return null
        return try {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        } catch (e: Exception) {
            null
        }
    }

    fun icon(context: Context, pkg: String?): Drawable? {
        if (pkg == null) return null
        return try {
            context.packageManager.getApplicationIcon(pkg)
        } catch (e: Exception) {
            null
        }
    }
}
