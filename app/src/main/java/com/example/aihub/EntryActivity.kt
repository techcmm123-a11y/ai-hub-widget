package com.example.aihub

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

/** Экран «Вход»: название, способ открытия (приложение или сайт в браузере), цвет метки. */
class EntryActivity : Activity() {

    private lateinit var cfg: Config
    private lateinit var svc: ServiceCfg
    private var entry: EntryCfg? = null
    private var mode = "app"
    private var color = Config.COLOR_BLUE
    private var browsers: List<Browsers.Info> = emptyList()

    private lateinit var labelField: EditText
    private lateinit var urlField: EditText
    private lateinit var browserSpinner: Spinner
    private lateinit var appBox: LinearLayout
    private lateinit var webBox: LinearLayout
    private lateinit var appInfo: TextView
    private lateinit var modeApp: TextView
    private lateinit var modeWeb: TextView
    private lateinit var inRowsSwitch: Switch
    private val swatches = ArrayList<TextView>()

    private val match = LinearLayout.LayoutParams.MATCH_PARENT
    private val wrap = LinearLayout.LayoutParams.WRAP_CONTENT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cfg = ConfigStore.load(this)
        val sid = intent.getStringExtra("service")
        val found = if (sid == null) null else cfg.service(sid)
        if (found == null) {
            finish()
            return
        }
        svc = found
        val eid = intent.getStringExtra("entry")
        entry = if (eid == null) null else cfg.entries.firstOrNull { it.id == eid }
        browsers = Browsers.installed(this)
        mode = entry?.mode ?: (if (Apps.packageFor(this, svc) != null) "app" else "web")
        color = entry?.color ?: Config.COLOR_BLUE
        setContentView(buildUi())
        applyMode()
        refreshAppInfo()
        refreshSwatches()
    }

    private fun lp(w: Int, h: Int, topDp: Int = 0): LinearLayout.LayoutParams {
        val l = LinearLayout.LayoutParams(w, h)
        l.topMargin = Ui.dp(this, topDp)
        return l
    }

    private fun button(text: String, bg: Int, fg: Int): TextView {
        val t = Ui.label(this, text, 15f, fg, true)
        t.gravity = Gravity.CENTER
        t.background = Ui.rounded(this, bg, 14)
        t.isClickable = true
        return t
    }

    private fun buildUi(): View {
        val scroll = ScrollView(this)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        val p = Ui.dp(this, 16)
        root.setPadding(p, p, p, p)
        scroll.addView(root)

        root.addView(Ui.label(this, if (entry == null) "Новый вход" else "Изменить вход", 24f, Ui.TEXT, true))
        root.addView(Ui.label(this, svc.title, 14f, Ui.SUB), lp(match, wrap, 2))

        root.addView(Ui.label(this, "Название на кнопке", 12f, Ui.SUB), lp(match, wrap, 16))
        labelField = EditText(this)
        labelField.hint = "Например: Личный, Работа"
        labelField.setText(entry?.label ?: "")
        labelField.inputType = InputType.TYPE_CLASS_TEXT
        root.addView(labelField, lp(match, wrap))

        root.addView(Ui.label(this, "Как открыть", 12f, Ui.SUB), lp(match, wrap, 12))
        val modes = LinearLayout(this)
        modes.orientation = LinearLayout.HORIZONTAL
        val h = Ui.dp(this, 44)
        modeApp = button("Приложение", Ui.ROW, Ui.TEXT)
        modeApp.setOnClickListener {
            mode = "app"
            applyMode()
        }
        modeWeb = button("Сайт в браузере", Ui.ROW, Ui.TEXT)
        modeWeb.setOnClickListener {
            mode = "web"
            applyMode()
        }
        modes.addView(modeApp, LinearLayout.LayoutParams(0, h, 1f))
        val second = LinearLayout.LayoutParams(0, h, 1f)
        second.marginStart = Ui.dp(this, 8)
        modes.addView(modeWeb, second)
        root.addView(modes, lp(match, wrap, 4))

        // Блок «Приложение»
        appBox = LinearLayout(this)
        appBox.orientation = LinearLayout.VERTICAL
        appInfo = Ui.label(this, "", 14f, Ui.TEXT)
        appBox.addView(appInfo, lp(match, wrap, 12))
        val pick = Ui.label(this, "Выбрать другое приложение", 14f, Ui.ACCENT, true)
        pick.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 10))
        pick.setOnClickListener { pickApp() }
        appBox.addView(pick)
        root.addView(appBox)

        // Блок «Сайт»
        webBox = LinearLayout(this)
        webBox.orientation = LinearLayout.VERTICAL
        webBox.addView(Ui.label(this, "Браузер (в каждом браузере свой вход в аккаунт)", 12f, Ui.SUB), lp(match, wrap, 12))
        browserSpinner = Spinner(this)
        val names = ArrayList<String>()
        names.add("Браузер по умолчанию")
        for (b in browsers) names.add(b.label)
        browserSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, names)
        val saved = entry?.browserPkg ?: ""
        val idx = browsers.indexOfFirst { it.pkg == saved }
        browserSpinner.setSelection(if (idx >= 0) idx + 1 else 0)
        webBox.addView(browserSpinner, lp(match, wrap))
        webBox.addView(Ui.label(this, "Ссылка (необязательно)", 12f, Ui.SUB), lp(match, wrap, 12))
        urlField = EditText(this)
        urlField.hint = svc.url
        urlField.setText(entry?.url ?: "")
        urlField.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
        webBox.addView(urlField, lp(match, wrap))
        root.addView(webBox)

        // Цвет метки
        root.addView(Ui.label(this, "Цвет метки", 12f, Ui.SUB), lp(match, wrap, 16))
        val colors = LinearLayout(this)
        colors.orientation = LinearLayout.HORIZONTAL
        for (i in Config.PALETTE.indices) {
            val sw = TextView(this)
            val size = Ui.dp(this, 36)
            val l = LinearLayout.LayoutParams(size, size)
            l.marginEnd = Ui.dp(this, 12)
            sw.setOnClickListener {
                color = Config.PALETTE[i]
                refreshSwatches()
            }
            swatches.add(sw)
            colors.addView(sw, l)
        }
        root.addView(colors, lp(match, wrap, 8))

        inRowsSwitch = Switch(this)
        inRowsSwitch.text = "Показывать в виджете «Строки»"
        inRowsSwitch.setTextColor(Ui.TEXT)
        inRowsSwitch.isChecked = entry?.inRows ?: true
        root.addView(inRowsSwitch, lp(match, wrap, 16))

        // Кнопки
        val actions = LinearLayout(this)
        actions.orientation = LinearLayout.HORIZONTAL
        val test = button("Проверить", Ui.ROW, Ui.TEXT)
        test.setOnClickListener { testOpen() }
        val save = button("Сохранить", Ui.OK, Ui.DARK)
        save.setOnClickListener { save() }
        val bh = Ui.dp(this, 48)
        actions.addView(test, LinearLayout.LayoutParams(0, bh, 1f))
        val sl = LinearLayout.LayoutParams(0, bh, 1f)
        sl.marginStart = Ui.dp(this, 8)
        actions.addView(save, sl)
        root.addView(actions, lp(match, wrap, 24))

        if (entry != null) {
            val del = Ui.label(this, "Удалить вход", 14f, Ui.RED, true)
            del.gravity = Gravity.CENTER
            del.setPadding(0, Ui.dp(this, 14), 0, Ui.dp(this, 6))
            del.setOnClickListener { deleteEntry() }
            root.addView(del)
        }
        val cancel = Ui.label(this, "Отмена", 14f, Ui.SUB)
        cancel.gravity = Gravity.CENTER
        cancel.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 10))
        cancel.setOnClickListener { finish() }
        root.addView(cancel)
        return scroll
    }

    private fun applyMode() {
        val appSel = mode == "app"
        modeApp.background = Ui.rounded(this, if (appSel) Ui.ACCENT else Ui.ROW, 14)
        modeApp.setTextColor(if (appSel) Ui.DARK else Ui.TEXT)
        modeWeb.background = Ui.rounded(this, if (appSel) Ui.ROW else Ui.ACCENT, 14)
        modeWeb.setTextColor(if (appSel) Ui.TEXT else Ui.DARK)
        appBox.visibility = if (appSel) View.VISIBLE else View.GONE
        webBox.visibility = if (appSel) View.GONE else View.VISIBLE
    }

    private fun refreshAppInfo() {
        val name = Apps.label(this, Apps.packageFor(this, svc))
        appInfo.text = if (name != null) {
            "Найдено: " + name
        } else {
            "Приложение не найдено. Выберите его вручную или используйте сайт."
        }
    }

    private fun refreshSwatches() {
        for (i in swatches.indices) {
            val c = Config.PALETTE[i]
            val d = Ui.oval(c)
            if (c == color) d.setStroke(Ui.dp(this, 3), Ui.TEXT)
            swatches[i].background = d
        }
    }

    private fun pickApp() {
        val apps = Apps.launchable(this)
        val names = apps.map { it.label }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Приложение для " + svc.title)
            .setItems(names) { _, which ->
                svc.pkg = apps[which].pkg
                refreshAppInfo()
            }
            .show()
    }

    private fun normalizeUrl(s: String): String {
        if (s.isEmpty()) return ""
        return if (s.startsWith("http")) s else "https://" + s
    }

    private fun collect(): EntryCfg {
        val typed = labelField.text.toString().trim()
        val label = if (typed.isEmpty()) (if (mode == "app") "Приложение" else "Сайт") else typed
        val pos = browserSpinner.selectedItemPosition
        val browser = if (mode == "web" && pos > 0) browsers[pos - 1].pkg else ""
        val url = if (mode == "web") normalizeUrl(urlField.text.toString().trim()) else ""
        return EntryCfg(entry?.id ?: Config.newId(), svc.id, label, mode, browser, url, color, inRowsSwitch.isChecked)
    }

    private fun testOpen() {
        try {
            startActivity(Open.intentFor(this, svc, collect()))
        } catch (e: Exception) {
            Toast.makeText(this, "Не удалось открыть", Toast.LENGTH_SHORT).show()
        }
    }

    private fun save() {
        val e = collect()
        val old = entry
        if (old == null) {
            cfg.entries.add(e)
        } else {
            val i = cfg.entries.indexOf(old)
            if (i >= 0) cfg.entries[i] = e
        }
        ConfigStore.save(this, cfg)
        finish()
    }

    private fun deleteEntry() {
        val old = entry
        if (old != null) cfg.entries.remove(old)
        ConfigStore.save(this, cfg)
        finish()
    }
}
