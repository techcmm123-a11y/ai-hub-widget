package com.example.aihub

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable

object Apps {

    class App(val pkg: String, val label: String)

    /** Основные ИИ: для них названия ищутся в подписях приложений свободнее. */
    private val CORE = setOf(
        "claude", "chatgpt", "gemini", "perplexity", "grok", "deepseek", "copilot", "mistral", "meta"
    )

    /** Слишком общие названия: по подписи не ищем, только по известному имени пакета или ручному выбору. */
    private val NO_LABEL_MATCH = setOf(
        "bolt", "opal", "stitch", "jules", "windsurf", "cursor", "flux", "gamma", "otter",
        "fathom", "consensus", "playground", "luma", "pika", "devin", "codex", "flow"
    )

    private var cache: List<App>? = null
    private var cacheAt = 0L

    /** Сбросить запомненный список приложений, чтобы следующая проверка увидела свежие установки. */
    fun refresh() {
        cache = null
    }

    /** Все приложения, у которых есть значок запуска (кроме самого AI Start). Список кэшируется на 15 секунд. */
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

    private fun labelMatches(svc: ServiceCfg, label: String): Boolean {
        val title = Regex.escape(svc.title)
        val loose = svc.custom || svc.id in CORE
        val re = if (loose) {
            // Слово целиком в любом месте подписи: «Microsoft Copilot», «Perplexity: Ask Anything».
            Regex("(^|[^\\p{L}\\p{N}])" + title + "([^\\p{L}\\p{N}]|$)", RegexOption.IGNORE_CASE)
        } else {
            // Подпись начинается с названия: «Poe - Fast AI Chat», «Kimi».
            Regex("^" + title + "([^\\p{L}\\p{N}].*)?$", RegexOption.IGNORE_CASE)
        }
        return re.containsMatchIn(label.trim())
    }

    /** Пакет приложения сервиса: выбранный вручную, известный или найденный по названию. */
    fun packageFor(context: Context, svc: ServiceCfg): String? {
        if (isInstalled(context, svc.pkg)) return svc.pkg
        val def = Catalog.byId(svc.id)
        if (def != null) {
            for (p in def.pkgs) {
                if (isInstalled(context, p)) return p
            }
        }
        if (!svc.custom && svc.id in NO_LABEL_MATCH) return null
        val found = launchable(context).firstOrNull { labelMatches(svc, it.label) }
        return found?.pkg
    }

    /** Пакет для входа: выбранный в самом входе либо найденный для сервиса (веб-приложения только выбранные). */
    fun packageForEntry(context: Context, svc: ServiceCfg, e: EntryCfg): String? {
        if (e.pkg.isNotEmpty() && isInstalled(context, e.pkg)) return e.pkg
        if (e.mode == "pwa") return null
        return packageFor(context, svc)
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
