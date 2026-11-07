package com.example.aihub

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Сервис (ИИ) в списке пользователя. */
class ServiceCfg(
    val id: String,
    var title: String,
    val mono: String,
    val bg: Int,
    val fg: Int,
    var url: String,
    var enabled: Boolean,
    val custom: Boolean,
    var pkg: String
)

/** Вход: способ открыть сервис (приложение или сайт в браузере), возможно под своим аккаунтом. */
class EntryCfg(
    val id: String,
    val serviceId: String,
    var label: String,
    var mode: String,
    var browserPkg: String,
    var url: String,
    var color: Int,
    var inRows: Boolean = true,
    var pkg: String = ""
)

class Config(
    var shape: String,
    var realIcons: Boolean,
    var aquarium: Boolean,
    val services: MutableList<ServiceCfg>,
    val entries: MutableList<EntryCfg>,
    var perRow: Int = 5,
    var rowsMode: Int = 0,
    var perCol: Int = 5,
    var colsMode: Int = 0
) {
    fun entriesFor(serviceId: String): List<EntryCfg> = entries.filter { it.serviceId == serviceId }

    fun service(id: String): ServiceCfg? = services.firstOrNull { it.id == id }

    fun toJson(): JSONObject {
        val o = JSONObject()
        o.put("shape", shape)
        o.put("realIcons", realIcons)
        o.put("aquarium", aquarium)
        o.put("perRow", perRow)
        o.put("rowsMode", rowsMode)
        o.put("perCol", perCol)
        o.put("colsMode", colsMode)
        val sa = JSONArray()
        for (s in services) {
            val x = JSONObject()
            x.put("id", s.id)
            x.put("title", s.title)
            x.put("mono", s.mono)
            x.put("bg", s.bg)
            x.put("fg", s.fg)
            x.put("url", s.url)
            x.put("enabled", s.enabled)
            x.put("custom", s.custom)
            x.put("pkg", s.pkg)
            sa.put(x)
        }
        o.put("services", sa)
        val ea = JSONArray()
        for (e in entries) {
            val x = JSONObject()
            x.put("id", e.id)
            x.put("service", e.serviceId)
            x.put("label", e.label)
            x.put("mode", e.mode)
            x.put("browser", e.browserPkg)
            x.put("url", e.url)
            x.put("color", e.color)
            x.put("inRows", e.inRows)
            x.put("pkg", e.pkg)
            ea.put(x)
        }
        o.put("entries", ea)
        return o
    }

    companion object {
        val COLOR_APP = 0xFF8A8A8E.toInt()
        val COLOR_BLUE = 0xFF4C9AFF.toInt()
        val COLOR_AMBER = 0xFFF2B84B.toInt()
        val COLOR_RED = 0xFFE5604F.toInt()
        val COLOR_GREEN = 0xFF3A9D5D.toInt()
        val COLOR_PURPLE = 0xFFC9A0F0.toInt()
        val COLOR_WHITE = 0xFFF2F4F8.toInt()

        val PALETTE = intArrayOf(
            COLOR_BLUE, COLOR_AMBER, COLOR_WHITE, COLOR_RED, COLOR_GREEN, COLOR_PURPLE
        )

        fun newId(): String = UUID.randomUUID().toString().substring(0, 8)

        /** Создаёт вход по умолчанию, если у сервиса их ещё нет. */
        fun ensureEntry(context: Context, cfg: Config, svc: ServiceCfg) {
            if (cfg.entriesFor(svc.id).isNotEmpty()) return
            val def = Catalog.byId(svc.id)
            val hasApp = Apps.packageFor(context, svc) != null
            val preferApp = hasApp || (def != null && def.pkgs.isNotEmpty())
            cfg.entries.add(
                EntryCfg(
                    newId(), svc.id, "", if (preferApp) "app" else "web", "", "",
                    if (preferApp) COLOR_APP else COLOR_BLUE
                )
            )
        }

        fun defaults(context: Context): Config {
            val services = mutableListOf<ServiceCfg>()
            val cfg = Config("rounded", true, false, services, mutableListOf())
            for (d in Catalog.list) {
                val svc = ServiceCfg(d.id, d.title, d.mono, d.bg, d.fg, d.url, false, false, "")
                val knownInstalled = d.pkgs.any { Apps.isInstalled(context, it) }
                svc.enabled = knownInstalled || d.id == "claude" || d.id == "chatgpt" || d.id == "gemini"
                services.add(svc)
                if (svc.enabled) ensureEntry(context, cfg, svc)
            }
            return cfg
        }

        fun fromJson(o: JSONObject): Config {
            val services = mutableListOf<ServiceCfg>()
            val sa = o.optJSONArray("services") ?: JSONArray()
            for (i in 0 until sa.length()) {
                val x = sa.getJSONObject(i)
                val id = x.getString("id")
                val custom = x.optBoolean("custom")
                val def = if (custom) null else Catalog.byId(id)
                // Для встроенных сервисов название, цвета и адрес берём из актуального каталога.
                services.add(
                    ServiceCfg(
                        id,
                        def?.title ?: x.optString("title"),
                        def?.mono ?: x.optString("mono"),
                        def?.bg ?: x.optInt("bg"),
                        def?.fg ?: x.optInt("fg"),
                        def?.url ?: Urls.safe(x.optString("url")),
                        x.optBoolean("enabled"), custom, x.optString("pkg")
                    )
                )
            }
            for (d in Catalog.list) {
                if (services.none { it.id == d.id }) {
                    services.add(ServiceCfg(d.id, d.title, d.mono, d.bg, d.fg, d.url, false, false, ""))
                }
            }
            val entries = mutableListOf<EntryCfg>()
            val ea = o.optJSONArray("entries") ?: JSONArray()
            for (i in 0 until ea.length()) {
                val x = ea.getJSONObject(i)
                var label = x.optString("label")
                var mode = x.optString("mode", "web")
                val serviceId = x.getString("service")
                val browser = x.optString("browser")
                val url = Urls.safe(x.optString("url"))
                var color = x.optInt("color", COLOR_BLUE)
                // Старые версии сохраняли подпись по умолчанию как текст: переводим в «пусто».
                val wasDefaultSite = (label == "Сайт" || label == "Site") && mode == "web" &&
                    browser.isEmpty() && url.isEmpty()
                if (label == "Приложение" || label == "Сайт" || label == "App" || label == "Site") label = ""
                // Вход «Сайт» по умолчанию мог появиться, когда приложения ещё не было на телефоне.
                // Для ИИ с приложением превращаем его во вход «Приложение»: оно ищется в момент нажатия.
                if (wasDefaultSite && (Catalog.byId(serviceId)?.pkgs?.isNotEmpty() == true)) {
                    mode = "app"
                    color = COLOR_APP
                }
                entries.add(
                    EntryCfg(
                        x.getString("id"), serviceId, label, mode, browser, url,
                        color, x.optBoolean("inRows", true), x.optString("pkg")
                    )
                )
            }
            return Config(
                o.optString("shape", "rounded"), o.optBoolean("realIcons", true),
                o.optBoolean("aquarium", false), services, entries,
                o.optInt("perRow", 5).coerceIn(4, 6), o.optInt("rowsMode", 0).coerceIn(0, 4),
                o.optInt("perCol", 5).coerceIn(3, 6), o.optInt("colsMode", 0).coerceIn(0, 2)
            )
        }
    }
}

/** Подписи входов: пустая подпись заменяется названием по умолчанию на текущем языке. */
object Labels {
    fun of(context: Context, e: EntryCfg): String {
        if (e.label.isNotEmpty()) return e.label
        return context.getString(if (e.mode == "app") R.string.label_app else R.string.label_site)
    }
}

object ConfigStore {
    private const val FILE = "cfg"
    private const val KEY = "json"

    fun load(context: Context): Config {
        val p = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val raw = p.getString(KEY, null)
        if (raw != null) {
            try {
                return Config.fromJson(JSONObject(raw))
            } catch (e: Exception) {
                // повреждённые данные: начинаем заново
            }
        }
        val fresh = Config.defaults(context)
        persist(context, fresh)
        return fresh
    }

    private fun persist(context: Context, cfg: Config) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putString(KEY, cfg.toJson().toString()).apply()
    }

    /** Сохраняет настройки и обновляет все виджеты. */
    fun save(context: Context, cfg: Config) {
        persist(context, cfg)
        Widgets.updateAll(context)
    }
}
