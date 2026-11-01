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
    var inRows: Boolean = true
)

class Config(
    var shape: String,
    var realIcons: Boolean,
    var aquarium: Boolean,
    val services: MutableList<ServiceCfg>,
    val entries: MutableList<EntryCfg>,
    var perRow: Int = 5,
    var rowsMode: Int = 0
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

        fun defaults(context: Context): Config {
            val services = mutableListOf<ServiceCfg>()
            val entries = mutableListOf<EntryCfg>()
            for (d in Catalog.list) {
                val svc = ServiceCfg(d.id, d.title, d.mono, d.bg, d.fg, d.url, false, false, "")
                val installed = Apps.packageFor(context, svc) != null
                svc.enabled = installed || d.id == "claude" || d.id == "chatgpt" || d.id == "gemini"
                services.add(svc)
                if (installed) {
                    entries.add(EntryCfg(newId(), d.id, "Приложение", "app", "", "", COLOR_APP))
                } else {
                    entries.add(EntryCfg(newId(), d.id, "Сайт", "web", "", "", COLOR_BLUE))
                }
            }
            return Config("rounded", true, false, services, entries)
        }

        fun fromJson(o: JSONObject): Config {
            val services = mutableListOf<ServiceCfg>()
            val sa = o.optJSONArray("services") ?: JSONArray()
            for (i in 0 until sa.length()) {
                val x = sa.getJSONObject(i)
                services.add(
                    ServiceCfg(
                        x.getString("id"), x.optString("title"), x.optString("mono"),
                        x.optInt("bg"), x.optInt("fg"), x.optString("url"),
                        x.optBoolean("enabled"), x.optBoolean("custom"), x.optString("pkg")
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
                entries.add(
                    EntryCfg(
                        x.getString("id"), x.getString("service"), x.optString("label"),
                        x.optString("mode", "web"), x.optString("browser"), x.optString("url"),
                        x.optInt("color", COLOR_BLUE), x.optBoolean("inRows", true)
                    )
                )
            }
            return Config(
                o.optString("shape", "rounded"), o.optBoolean("realIcons", true),
                o.optBoolean("aquarium", false), services, entries,
                o.optInt("perRow", 5).coerceIn(4, 6), o.optInt("rowsMode", 0).coerceIn(0, 2)
            )
        }
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
