package com.example.aihub

import android.app.Activity
import android.content.Context
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

/** Экран «Вход»: название, способ открытия (приложение, сайт или веб-приложение), цвет метки. */
class EntryActivity : Activity() {

    private lateinit var cfg: Config
    private lateinit var svc: ServiceCfg
    private var entry: EntryCfg? = null
    private var mode = "app"
    private var color = Config.COLOR_BLUE
    private var entryPkg = ""
    private var browsers: List<Browsers.Info> = emptyList()

    private lateinit var labelField: EditText
    private lateinit var urlField: EditText
    private lateinit var browserSpinner: Spinner
    private lateinit var appBox: LinearLayout
    private lateinit var webBox: LinearLayout
    private lateinit var pwaTop: LinearLayout
    private lateinit var pwaBottom: LinearLayout
    private lateinit var appInfo: TextView
    private lateinit var pwaInfo: TextView
    private lateinit var modeApp: TextView
    private lateinit var modeWeb: TextView
    private lateinit var modePwa: TextView
    private lateinit var inRowsSwitch: Switch
    private val swatches = ArrayList<TextView>()

    private val match = LinearLayout.LayoutParams.MATCH_PARENT
    private val wrap = LinearLayout.LayoutParams.WRAP_CONTENT

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(L.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Ui.apply(this)
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
        entryPkg = entry?.pkg ?: ""
        setContentView(buildUi())
        Ui.bars(this)
        applyMode()
        refreshInfo()
        refreshSwatches()
    }

    override fun onResume() {
        super.onResume()
        if (!::appInfo.isInitialized) return
        Apps.refresh()
        refreshInfo()
    }

    private fun lp(w: Int, h: Int, topDp: Int = 0): LinearLayout.LayoutParams {
        val l = LinearLayout.LayoutParams(w, h)
        l.topMargin = Ui.dp(this, topDp)
        return l
    }

    private fun button(text: String, bg: Int, fg: Int, size: Float = 15f): TextView {
        val t = Ui.label(this, text, size, fg, true)
        t.gravity = Gravity.CENTER
        t.background = Ui.rounded(this, bg, 14)
        t.isClickable = true
        return t
    }

    private fun box(): LinearLayout {
        val b = LinearLayout(this)
        b.orientation = LinearLayout.VERTICAL
        return b
    }

    private fun buildUi(): View {
        val scroll = ScrollView(this)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        val p = Ui.dp(this, 16)
        root.setPadding(p, p, p, p)
        scroll.addView(root)

        root.addView(
            Ui.label(this, getString(if (entry == null) R.string.entry_new else R.string.entry_edit), 24f, Ui.TEXT, true)
        )
        root.addView(Ui.label(this, svc.title, 14f, Ui.SUB), lp(match, wrap, 2))

        root.addView(Ui.label(this, getString(R.string.entry_name), 12f, Ui.SUB), lp(match, wrap, 16))
        labelField = EditText(this)
        labelField.hint = getString(R.string.entry_name_hint)
        labelField.setText(entry?.label ?: "")
        labelField.inputType = InputType.TYPE_CLASS_TEXT
        root.addView(labelField, lp(match, wrap))

        // Способ открытия: приложение, сайт, веб-приложение
        root.addView(Ui.label(this, getString(R.string.how_open), 12f, Ui.SUB), lp(match, wrap, 12))
        val modes = LinearLayout(this)
        modes.orientation = LinearLayout.HORIZONTAL
        val h = Ui.dp(this, 48)
        modeApp = button(getString(R.string.mode_app), Ui.ROW, Ui.TEXT, 13f)
        modeApp.setOnClickListener { mode = "app"; applyMode(); refreshInfo() }
        modeWeb = button(getString(R.string.mode_web), Ui.ROW, Ui.TEXT, 13f)
        modeWeb.setOnClickListener { mode = "web"; applyMode() }
        modePwa = button(getString(R.string.mode_pwa), Ui.ROW, Ui.TEXT, 13f)
        modePwa.setOnClickListener { mode = "pwa"; applyMode(); refreshInfo() }
        modes.addView(modeApp, LinearLayout.LayoutParams(0, h, 1f))
        val l2 = LinearLayout.LayoutParams(0, h, 1f)
        l2.marginStart = Ui.dp(this, 8)
        modes.addView(modeWeb, l2)
        val l3 = LinearLayout.LayoutParams(0, h, 1.3f)
        l3.marginStart = Ui.dp(this, 8)
        modes.addView(modePwa, l3)
        root.addView(modes, lp(match, wrap, 4))

        // Блок «Приложение»
        appBox = box()
        appInfo = Ui.label(this, "", 14f, Ui.TEXT)
        appBox.addView(appInfo, lp(match, wrap, 12))
        val pick = Ui.label(this, getString(R.string.pick_app), 14f, Ui.ACCENT, true)
        pick.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 10))
        pick.setOnClickListener { pickApp() }
        appBox.addView(pick)
        root.addView(appBox)

        // Блок «Веб-приложение»: пояснение
        pwaTop = box()
        pwaTop.addView(Ui.label(this, getString(R.string.pwa_help), 13f, Ui.SUB), lp(match, wrap, 12))
        root.addView(pwaTop)

        // Блок «Сайт» (нужен и для установки веб-приложения): браузер и ссылка
        webBox = box()
        webBox.addView(Ui.label(this, getString(R.string.browser_label), 12f, Ui.SUB), lp(match, wrap, 12))
        browserSpinner = Spinner(this)
        val names = ArrayList<String>()
        names.add(getString(R.string.browser_default))
        for (b in browsers) names.add(b.label)
        browserSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, names)
        val saved = entry?.browserPkg ?: ""
        val idx = browsers.indexOfFirst { it.pkg == saved }
        browserSpinner.setSelection(if (idx >= 0) idx + 1 else 0)
        webBox.addView(browserSpinner, lp(match, wrap))
        webBox.addView(Ui.label(this, getString(R.string.link_optional), 12f, Ui.SUB), lp(match, wrap, 12))
        urlField = EditText(this)
        urlField.hint = svc.url
        urlField.setText(entry?.url ?: "")
        urlField.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
        webBox.addView(urlField, lp(match, wrap))
        root.addView(webBox)

        // Блок «Веб-приложение»: установка и выбор
        pwaBottom = box()
        val install = button(getString(R.string.pwa_open_site), Ui.ROW, Ui.TEXT)
        install.setOnClickListener { openSiteToInstall() }
        pwaBottom.addView(install, lp(match, Ui.dp(this, 46), 14))
        pwaInfo = Ui.label(this, "", 14f, Ui.TEXT)
        pwaBottom.addView(pwaInfo, lp(match, wrap, 12))
        val pickWeb = Ui.label(this, getString(R.string.pwa_pick), 14f, Ui.ACCENT, true)
        pickWeb.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 10))
        pickWeb.setOnClickListener { pickWebApp() }
        pwaBottom.addView(pickWeb)
        root.addView(pwaBottom)

        // Цвет метки
        root.addView(Ui.label(this, getString(R.string.color_label), 12f, Ui.SUB), lp(match, wrap, 16))
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
        inRowsSwitch.text = getString(R.string.show_in_widgets)
        inRowsSwitch.setTextColor(Ui.TEXT)
        inRowsSwitch.isChecked = entry?.inRows ?: true
        root.addView(inRowsSwitch, lp(match, wrap, 16))

        // Кнопки
        val actions = LinearLayout(this)
        actions.orientation = LinearLayout.HORIZONTAL
        val test = button(getString(R.string.test), Ui.ROW, Ui.TEXT)
        test.setOnClickListener { testOpen() }
        val save = button(getString(R.string.save), Ui.OK, Ui.ON_ACCENT)
        save.setOnClickListener { save() }
        val bh = Ui.dp(this, 48)
        actions.addView(test, LinearLayout.LayoutParams(0, bh, 1f))
        val sl = LinearLayout.LayoutParams(0, bh, 1f)
        sl.marginStart = Ui.dp(this, 8)
        actions.addView(save, sl)
        root.addView(actions, lp(match, wrap, 24))

        if (entry != null) {
            val del = Ui.label(this, getString(R.string.delete_entry), 14f, Ui.RED, true)
            del.gravity = Gravity.CENTER
            del.setPadding(0, Ui.dp(this, 14), 0, Ui.dp(this, 6))
            del.setOnClickListener { deleteEntry() }
            root.addView(del)
        }
        val cancel = Ui.label(this, getString(R.string.cancel), 14f, Ui.SUB)
        cancel.gravity = Gravity.CENTER
        cancel.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 10))
        cancel.setOnClickListener { finish() }
        root.addView(cancel)
        return scroll
    }

    private fun applyMode() {
        val appSel = mode == "app"
        val webSel = mode == "web"
        val pwaSel = mode == "pwa"
        modeApp.background = Ui.rounded(this, if (appSel) Ui.ACCENT else Ui.ROW, 14)
        modeApp.setTextColor(if (appSel) Ui.ON_ACCENT else Ui.TEXT)
        modeWeb.background = Ui.rounded(this, if (webSel) Ui.ACCENT else Ui.ROW, 14)
        modeWeb.setTextColor(if (webSel) Ui.ON_ACCENT else Ui.TEXT)
        modePwa.background = Ui.rounded(this, if (pwaSel) Ui.ACCENT else Ui.ROW, 14)
        modePwa.setTextColor(if (pwaSel) Ui.ON_ACCENT else Ui.TEXT)
        appBox.visibility = if (appSel) View.VISIBLE else View.GONE
        pwaTop.visibility = if (pwaSel) View.VISIBLE else View.GONE
        webBox.visibility = if (webSel || pwaSel) View.VISIBLE else View.GONE
        pwaBottom.visibility = if (pwaSel) View.VISIBLE else View.GONE
    }

    private fun refreshInfo() {
        val appPkg = if (entryPkg.isNotEmpty() && Apps.isInstalled(this, entryPkg)) entryPkg
        else Apps.packageFor(this, svc)
        val name = Apps.label(this, appPkg)
        appInfo.text = if (name != null) getString(R.string.app_found, name) else getString(R.string.app_missing)

        val webName = if (entryPkg.isNotEmpty() && Apps.isInstalled(this, entryPkg)) Apps.label(this, entryPkg) else null
        pwaInfo.text = if (webName != null) getString(R.string.pwa_chosen, webName) else getString(R.string.pwa_none)
    }

    private fun refreshSwatches() {
        for (i in swatches.indices) {
            val c = Config.PALETTE[i]
            val d = Ui.oval(c)
            if (c == color) d.setStroke(Ui.dp(this, 3), Ui.TEXT)
            swatches[i].background = d
        }
    }

    /** Выбор приложения для этого входа: автоматически или любое установленное (например, копия приложения). */
    private fun pickApp() {
        val apps = Apps.launchable(this)
        val names = ArrayList<String>()
        names.add(getString(R.string.pick_auto))
        for (a in apps) names.add(a.label)
        Ui.dialog(this)
            .setTitle(getString(R.string.pick_app_title, svc.title))
            .setItems(names.toTypedArray()) { _, which ->
                entryPkg = if (which == 0) "" else apps[which - 1].pkg
                refreshInfo()
            }
            .show()
    }

    /** Выбор установленного веб-приложения: сначала веб-приложения браузера, затем все приложения. */
    private fun pickWebApp() {
        val all = Apps.launchable(this)
        val web = all.filter { it.pkg.contains("webapk", ignoreCase = true) }
        if (web.isEmpty()) {
            pickFromList(all)
            return
        }
        val names = ArrayList<String>()
        for (a in web) names.add(a.label)
        names.add(getString(R.string.pwa_pick_all))
        Ui.dialog(this)
            .setTitle(R.string.pwa_pick_title)
            .setItems(names.toTypedArray()) { _, which ->
                if (which == web.size) {
                    pickFromList(all)
                } else {
                    entryPkg = web[which].pkg
                    refreshInfo()
                }
            }
            .show()
    }

    private fun pickFromList(list: List<Apps.App>) {
        val names = list.map { it.label }.toTypedArray()
        Ui.dialog(this)
            .setTitle(R.string.pwa_pick_title)
            .setItems(names) { _, which ->
                entryPkg = list[which].pkg
                refreshInfo()
            }
            .show()
    }

    private fun selectedBrowser(): String {
        val pos = browserSpinner.selectedItemPosition
        return if (pos > 0) browsers[pos - 1].pkg else ""
    }

    private fun openSiteToInstall() {
        val url = Urls.safe(urlField.text.toString())
        try {
            startActivity(Browsers.siteIntent(this, if (url.isNotEmpty()) url else svc.url, selectedBrowser()))
        } catch (e: Exception) {
            Toast.makeText(this, R.string.open_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun collect(): EntryCfg {
        val label = labelField.text.toString().trim()
        val browser = if (mode == "app") "" else selectedBrowser()
        val rawUrl = urlField.text.toString().trim()
        val url = if (mode == "app") "" else Urls.safe(rawUrl)
        if (mode != "app" && rawUrl.isNotEmpty() && url.isEmpty()) {
            Toast.makeText(this, R.string.invalid_link, Toast.LENGTH_LONG).show()
        }
        val pkg = if (mode == "web") "" else entryPkg
        return EntryCfg(
            entry?.id ?: Config.newId(), svc.id, label, mode, browser, url, color,
            inRowsSwitch.isChecked, pkg
        )
    }

    private fun testOpen() {
        try {
            startActivity(Open.resolveNow(this, svc, collect()))
        } catch (e: Exception) {
            Toast.makeText(this, R.string.open_failed, Toast.LENGTH_SHORT).show()
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
