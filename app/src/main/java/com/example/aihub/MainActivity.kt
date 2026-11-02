package com.example.aihub

import android.app.Activity
import android.app.AlertDialog
import android.appwidget.AppWidgetManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject

/** Главный экран: меню разделов. Каждый раздел открывается отдельно, чтобы настраивать постепенно. */
class MainActivity : Activity() {

    private lateinit var scroll: ScrollView
    private lateinit var root: LinearLayout
    private var section = "home"
    private val expanded = HashSet<String>()
    private val match = LinearLayout.LayoutParams.MATCH_PARENT
    private val wrap = LinearLayout.LayoutParams.WRAP_CONTENT

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(L.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        section = savedInstanceState?.getString("section") ?: "home"
        scroll = ScrollView(this)
        root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        val p = Ui.dp(this, 16)
        root.setPadding(p, p, p, p)
        scroll.addView(root)
        setContentView(scroll)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("section", section)
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (section != "home") open("home") else super.onBackPressed()
    }

    private fun open(target: String) {
        section = target
        render()
        scroll.post { scroll.scrollTo(0, 0) }
    }

    private fun rerenderKeepScroll() {
        val y = scroll.scrollY
        render()
        scroll.post { scroll.scrollTo(0, y) }
    }

    private fun render() {
        val cfg = ConfigStore.load(this)
        root.removeAllViews()
        when (section) {
            "my" -> sectionMy(cfg)
            "choose" -> sectionChoose(cfg)
            "look" -> sectionLook(cfg)
            "widgets" -> sectionWidgets(cfg)
            "lang" -> sectionLang()
            "backup" -> sectionBackup(cfg)
            "about" -> sectionAbout(cfg)
            else -> sectionHome()
        }
    }

    // ---------------------------------------------------------- общие элементы

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

    private fun cardBox(): LinearLayout {
        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.background = Ui.rounded(this, Ui.CARD, 16)
        val pad = Ui.dp(this, 12)
        card.setPadding(pad, pad, pad, pad)
        return card
    }

    private fun arrow(symbol: String, sizeDp: Int, onClick: () -> Unit): TextView {
        val t = Ui.label(this, symbol, 16f, Ui.SUB, true)
        t.gravity = Gravity.CENTER
        t.setOnClickListener { onClick() }
        val s = Ui.dp(this, sizeDp)
        t.layoutParams = LinearLayout.LayoutParams(s, s)
        return t
    }

    private fun segmented(options: List<String>, selected: Int, onPick: (Int) -> Unit): LinearLayout {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        val h = Ui.dp(this, 44)
        for (i in options.indices) {
            val sel = i == selected
            val b = button(options[i], if (sel) Ui.ACCENT else Ui.ROW, if (sel) Ui.DARK else Ui.TEXT)
            b.setOnClickListener { onPick(i) }
            val l = LinearLayout.LayoutParams(0, h, 1f)
            if (i > 0) l.marginStart = Ui.dp(this, 8)
            row.addView(b, l)
        }
        return row
    }

    private fun header(title: String) {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        val back = Ui.label(this, "‹", 30f, Ui.ACCENT, true)
        back.gravity = Gravity.CENTER
        back.contentDescription = getString(R.string.back)
        back.setOnClickListener { open("home") }
        row.addView(back, LinearLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 44)))
        row.addView(Ui.label(this, title, 24f, Ui.TEXT, true))
        root.addView(row)
    }

    private fun caption(text: String, topDp: Int = 8): TextView {
        val t = Ui.label(this, text, 12f, Ui.SUB)
        root.addView(t, lp(match, wrap, topDp))
        return t
    }

    // ---------------------------------------------------------- главное меню

    private fun menuRow(title: Int, sub: Int, target: String): View {
        val card = cardBox()
        card.orientation = LinearLayout.HORIZONTAL
        card.gravity = Gravity.CENTER_VERTICAL
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.addView(Ui.label(this, getString(title), 17f, Ui.TEXT, true))
        col.addView(Ui.label(this, getString(sub), 12f, Ui.SUB))
        card.addView(col, LinearLayout.LayoutParams(0, wrap, 1f))
        card.addView(Ui.label(this, "›", 24f, Ui.SUB))
        card.setOnClickListener { open(target) }
        return card
    }

    private fun sectionHome() {
        root.addView(Ui.label(this, getString(R.string.app_name), 28f, Ui.TEXT, true))
        caption(getString(R.string.home_hint), 4)
        root.addView(menuRow(R.string.menu_choose, R.string.menu_choose_sub, "choose"), lp(match, wrap, 14))
        root.addView(menuRow(R.string.menu_my, R.string.menu_my_sub, "my"), lp(match, wrap, 8))
        root.addView(menuRow(R.string.menu_look, R.string.menu_look_sub, "look"), lp(match, wrap, 8))
        root.addView(menuRow(R.string.menu_widgets, R.string.menu_widgets_sub, "widgets"), lp(match, wrap, 8))
        root.addView(menuRow(R.string.menu_lang, R.string.menu_lang_sub, "lang"), lp(match, wrap, 8))
        root.addView(menuRow(R.string.menu_backup, R.string.menu_backup_sub, "backup"), lp(match, wrap, 8))
        root.addView(menuRow(R.string.menu_about, R.string.menu_about_sub, "about"), lp(match, wrap, 8))
    }

    // ---------------------------------------------------------- «Мои ИИ»

    private fun moveService(cfg: Config, svc: ServiceCfg, delta: Int) {
        val idx = cfg.services.indexOf(svc)
        var j = idx + delta
        while (j >= 0 && j < cfg.services.size && !cfg.services[j].enabled) j += delta
        if (idx < 0 || j < 0 || j >= cfg.services.size) return
        java.util.Collections.swap(cfg.services, idx, j)
        ConfigStore.save(this, cfg)
        render()
    }

    private fun moveEntry(cfg: Config, e: EntryCfg, delta: Int) {
        val idxs = cfg.entries.indices.filter { cfg.entries[it].serviceId == e.serviceId }
        val pos = idxs.indexOfFirst { cfg.entries[it].id == e.id }
        val target = pos + delta
        if (pos < 0 || target < 0 || target >= idxs.size) return
        java.util.Collections.swap(cfg.entries, idxs[pos], idxs[target])
        ConfigStore.save(this, cfg)
        render()
    }

    private fun sectionMy(cfg: Config) {
        header(getString(R.string.menu_my))
        caption(getString(R.string.my_intro), 4)
        val enabled = cfg.services.filter { it.enabled }
        if (enabled.isEmpty()) {
            caption(getString(R.string.my_empty), 14)
            val go = button(getString(R.string.go_choose), Ui.ACCENT, Ui.DARK)
            go.setOnClickListener { open("choose") }
            root.addView(go, lp(match, Ui.dp(this, 48), 10))
        }
        for (svc in enabled) {
            root.addView(serviceCard(cfg, svc), lp(match, wrap, 10))
        }
        val add = button(getString(R.string.add_custom), Ui.ROW, Ui.TEXT)
        add.setOnClickListener { addCustomDialog(cfg) }
        root.addView(add, lp(match, Ui.dp(this, 48), 14))
    }

    private fun serviceCard(cfg: Config, svc: ServiceCfg): View {
        val card = cardBox()

        val header = LinearLayout(this)
        header.orientation = LinearLayout.HORIZONTAL
        header.gravity = Gravity.CENTER_VERTICAL

        val icon = ImageView(this)
        icon.setImageBitmap(Tiles.render(this, svc, cfg, 96))
        header.addView(icon, LinearLayout.LayoutParams(Ui.dp(this, 40), Ui.dp(this, 40)))

        val title = Ui.label(this, svc.title, 17f, Ui.TEXT, true)
        val tl = LinearLayout.LayoutParams(0, wrap, 1f)
        tl.marginStart = Ui.dp(this, 12)
        header.addView(title, tl)

        header.addView(arrow("▲", 40) { moveService(cfg, svc, -1) })
        header.addView(arrow("▼", 40) { moveService(cfg, svc, 1) })

        val sw = Switch(this)
        sw.isChecked = svc.enabled
        sw.setOnCheckedChangeListener { _, on ->
            svc.enabled = on
            ConfigStore.save(this, cfg)
            root.post { render() }
        }
        header.addView(sw)
        card.addView(header)

        for (e in cfg.entriesFor(svc.id)) {
            card.addView(entryRow(cfg, svc, e), lp(match, wrap, 6))
        }

        val addEntry = Ui.label(this, getString(R.string.add_entry), 14f, Ui.ACCENT, true)
        addEntry.gravity = Gravity.CENTER
        val ap = Ui.dp(this, 10)
        addEntry.setPadding(ap, ap, ap, ap)
        addEntry.setOnClickListener {
            startActivity(Intent(this, EntryActivity::class.java).putExtra("service", svc.id))
        }
        card.addView(addEntry, lp(match, wrap, 4))

        if (svc.custom) {
            val del = Ui.label(this, getString(R.string.delete_ai), 13f, Ui.RED)
            del.gravity = Gravity.CENTER
            del.setPadding(ap, ap, ap, ap)
            del.setOnClickListener {
                cfg.services.remove(svc)
                cfg.entries.removeAll { it.serviceId == svc.id }
                ConfigStore.save(this, cfg)
                render()
            }
            card.addView(del)
        }
        return card
    }

    private fun summary(svc: ServiceCfg, e: EntryCfg): String {
        val hidden = if (e.inRows) "" else getString(R.string.sum_hidden)
        if (e.mode == "app") {
            val name = Apps.label(this, Apps.packageFor(this, svc)) ?: getString(R.string.sum_app_missing)
            return getString(R.string.sum_app, name) + hidden
        }
        val browserPkg = if (e.browserPkg.isEmpty()) null else e.browserPkg
        val where = Apps.label(this, browserPkg) ?: getString(R.string.browser_default)
        val extra = if (e.url.isNotEmpty()) " · " + e.url.removePrefix("https://") else ""
        return getString(R.string.sum_web, where) + extra + hidden
    }

    private fun entryRow(cfg: Config, svc: ServiceCfg, e: EntryCfg): View {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.background = Ui.rounded(this, Ui.ROW, 10)
        val pad = Ui.dp(this, 10)
        row.setPadding(pad, Ui.dp(this, 8), Ui.dp(this, 2), Ui.dp(this, 8))

        val label = Labels.of(this, e)
        val badge = Ui.label(this, label.take(1).uppercase(), 12f, Ui.DARK, true)
        badge.gravity = Gravity.CENTER
        badge.background = Ui.oval(e.color)
        row.addView(badge, LinearLayout.LayoutParams(Ui.dp(this, 28), Ui.dp(this, 28)))

        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.addView(Ui.label(this, label, 14f, Ui.TEXT, true))
        col.addView(Ui.label(this, summary(svc, e), 12f, Ui.SUB))
        val cl = LinearLayout.LayoutParams(0, wrap, 1f)
        cl.marginStart = Ui.dp(this, 10)
        row.addView(col, cl)

        row.addView(arrow("▲", 36) { moveEntry(cfg, e, -1) })
        row.addView(arrow("▼", 36) { moveEntry(cfg, e, 1) })

        row.setOnClickListener {
            startActivity(
                Intent(this, EntryActivity::class.java)
                    .putExtra("service", svc.id)
                    .putExtra("entry", e.id)
            )
        }
        return row
    }

    private fun addCustomDialog(cfg: Config) {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        val p = Ui.dp(this, 20)
        box.setPadding(p, p / 2, p, 0)
        val name = EditText(this)
        name.hint = getString(R.string.custom_name)
        val url = EditText(this)
        url.hint = getString(R.string.custom_url)
        url.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
        box.addView(name)
        box.addView(url)
        AlertDialog.Builder(this)
            .setTitle(R.string.custom_title)
            .setView(box)
            .setPositiveButton(R.string.add) { _, _ ->
                val n = name.text.toString().trim()
                var u = url.text.toString().trim()
                if (n.isNotEmpty() && u.isNotEmpty()) {
                    if (!u.startsWith("http")) u = "https://" + u
                    val id = "c_" + Config.newId()
                    val mono = n.take(2).replaceFirstChar { it.uppercase() }
                    cfg.services.add(
                        ServiceCfg(id, n, mono, 0xFF2C2C31.toInt(), 0xFFE8E8ED.toInt(), u, true, true, "")
                    )
                    cfg.entries.add(EntryCfg(Config.newId(), id, "", "web", "", "", Config.COLOR_BLUE))
                    ConfigStore.save(this, cfg)
                    render()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    // ---------------------------------------------------------- «Выбор ИИ»: каталог по категориям

    private fun sectionChoose(cfg: Config) {
        header(getString(R.string.menu_choose))
        caption(getString(R.string.choose_hint), 4)
        for (cat in Catalog.cats) {
            val list = cfg.services.filter { Catalog.byId(it.id)?.cat == cat }
            if (list.isEmpty()) continue
            val isOpen = expanded.contains(cat)
            val chosen = list.count { it.enabled }

            val head = cardBox()
            head.orientation = LinearLayout.HORIZONTAL
            head.gravity = Gravity.CENTER_VERTICAL
            val col = LinearLayout(this)
            col.orientation = LinearLayout.VERTICAL
            col.addView(Ui.label(this, Catalog.catTitle(this, cat), 17f, Ui.TEXT, true))
            col.addView(Ui.label(this, getString(R.string.cat_count, chosen, list.size), 12f, Ui.SUB))
            head.addView(col, LinearLayout.LayoutParams(0, wrap, 1f))
            head.addView(Ui.label(this, if (isOpen) "▾" else "▸", 20f, Ui.SUB))
            head.setOnClickListener {
                if (isOpen) expanded.remove(cat) else expanded.add(cat)
                rerenderKeepScroll()
            }
            root.addView(head, lp(match, wrap, 8))

            if (isOpen) {
                for (svc in list) {
                    root.addView(chooseRow(cfg, svc), lp(match, wrap, 4))
                }
            }
        }
    }

    private fun chooseRow(cfg: Config, svc: ServiceCfg): View {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.background = Ui.rounded(this, Ui.ROW, 12)
        val pad = Ui.dp(this, 10)
        row.setPadding(pad, Ui.dp(this, 8), pad, Ui.dp(this, 8))

        val icon = ImageView(this)
        icon.setImageBitmap(Tiles.render(this, svc, cfg, 96))
        row.addView(icon, LinearLayout.LayoutParams(Ui.dp(this, 36), Ui.dp(this, 36)))

        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.addView(Ui.label(this, svc.title, 15f, Ui.TEXT, true))
        if (Apps.packageFor(this, svc) != null) {
            col.addView(Ui.label(this, getString(R.string.tag_installed), 12f, Ui.SUB))
        }
        val cl = LinearLayout.LayoutParams(0, wrap, 1f)
        cl.marginStart = Ui.dp(this, 12)
        row.addView(col, cl)

        val sw = Switch(this)
        sw.isChecked = svc.enabled
        sw.setOnCheckedChangeListener { _, on ->
            svc.enabled = on
            if (on) Config.ensureEntry(this, cfg, svc)
            ConfigStore.save(this, cfg)
            root.post { rerenderKeepScroll() }
        }
        row.addView(sw)
        return row
    }

    // ---------------------------------------------------------- «Оформление»

    private fun sectionLook(cfg: Config) {
        header(getString(R.string.menu_look))
        val card = cardBox()
        card.addView(Ui.label(this, getString(R.string.look_shape), 17f, Ui.TEXT, true))
        card.addView(
            segmented(
                listOf(getString(R.string.shape_rounded), getString(R.string.shape_round)),
                if (cfg.shape == "round") 1 else 0
            ) { i ->
                cfg.shape = if (i == 1) "round" else "rounded"
                ConfigStore.save(this, cfg)
                render()
            },
            lp(match, wrap, 8)
        )
        val real = Switch(this)
        real.text = getString(R.string.real_icons)
        real.setTextColor(Ui.TEXT)
        real.isChecked = cfg.realIcons
        real.setOnCheckedChangeListener { _, on ->
            cfg.realIcons = on
            ConfigStore.save(this, cfg)
            root.post { render() }
        }
        card.addView(real, lp(match, wrap, 14))
        card.addView(Ui.label(this, getString(R.string.real_icons_help), 12f, Ui.SUB), lp(match, wrap, 4))
        root.addView(card, lp(match, wrap, 10))
    }

    // ---------------------------------------------------------- «Виджеты»

    private fun pin(cls: Class<*>) {
        val m = AppWidgetManager.getInstance(this)
        if (m.isRequestPinAppWidgetSupported) {
            m.requestPinAppWidget(ComponentName(this, cls), null, null)
        } else {
            Toast.makeText(this, R.string.pin_unsupported, Toast.LENGTH_LONG).show()
        }
    }

    private fun sectionWidgets(cfg: Config) {
        header(getString(R.string.menu_widgets))

        val add = cardBox()
        add.addView(Ui.label(this, getString(R.string.w_pin_title), 17f, Ui.TEXT, true))
        val b1 = button(getString(R.string.w_pin_rows), Ui.ROW, Ui.TEXT)
        b1.setOnClickListener { pin(RowsWidget::class.java) }
        add.addView(b1, lp(match, Ui.dp(this, 46), 8))
        val b2 = button(getString(R.string.w_pin_cols), Ui.ROW, Ui.TEXT)
        b2.setOnClickListener { pin(ColumnsWidget::class.java) }
        add.addView(b2, lp(match, Ui.dp(this, 46), 8))
        val b3 = button(getString(R.string.w_pin_groups), Ui.ROW, Ui.TEXT)
        b3.setOnClickListener { pin(GroupsWidget::class.java) }
        add.addView(b3, lp(match, Ui.dp(this, 46), 8))
        add.addView(Ui.label(this, getString(R.string.w_pin_help), 12f, Ui.SUB), lp(match, wrap, 8))
        root.addView(add, lp(match, wrap, 10))

        val rows = cardBox()
        rows.addView(Ui.label(this, getString(R.string.w_rows_title), 17f, Ui.TEXT, true))
        rows.addView(Ui.label(this, getString(R.string.per_row), 12f, Ui.SUB), lp(match, wrap, 8))
        rows.addView(
            segmented(listOf("4", "5", "6"), cfg.perRow - 4) { i ->
                cfg.perRow = 4 + i
                ConfigStore.save(this, cfg)
                render()
            },
            lp(match, wrap, 4)
        )
        rows.addView(Ui.label(this, getString(R.string.rows_count), 12f, Ui.SUB), lp(match, wrap, 10))
        rows.addView(
            segmented(listOf(getString(R.string.auto_height), "1", "2"), cfg.rowsMode) { i ->
                cfg.rowsMode = i
                ConfigStore.save(this, cfg)
                render()
            },
            lp(match, wrap, 4)
        )
        root.addView(rows, lp(match, wrap, 10))

        val cols = cardBox()
        cols.addView(Ui.label(this, getString(R.string.w_cols_title), 17f, Ui.TEXT, true))
        cols.addView(Ui.label(this, getString(R.string.per_col), 12f, Ui.SUB), lp(match, wrap, 8))
        cols.addView(
            segmented(listOf("3", "4", "5", "6"), cfg.perCol - 3) { i ->
                cfg.perCol = 3 + i
                ConfigStore.save(this, cfg)
                render()
            },
            lp(match, wrap, 4)
        )
        cols.addView(Ui.label(this, getString(R.string.cols_count), 12f, Ui.SUB), lp(match, wrap, 10))
        cols.addView(
            segmented(listOf(getString(R.string.auto_width), "1", "2"), cfg.colsMode) { i ->
                cfg.colsMode = i
                ConfigStore.save(this, cfg)
                render()
            },
            lp(match, wrap, 4)
        )
        root.addView(cols, lp(match, wrap, 10))

        val aqCard = cardBox()
        val aq = Switch(this)
        aq.text = getString(R.string.aquarium)
        aq.setTextColor(Ui.TEXT)
        aq.isChecked = cfg.aquarium
        aq.setOnCheckedChangeListener { _, on ->
            cfg.aquarium = on
            ConfigStore.save(this, cfg)
        }
        aqCard.addView(aq)
        aqCard.addView(Ui.label(this, getString(R.string.aquarium_help), 12f, Ui.SUB), lp(match, wrap, 8))
        root.addView(aqCard, lp(match, wrap, 10))
    }

    // ---------------------------------------------------------- «Язык»

    private fun sectionLang() {
        header(getString(R.string.menu_lang))
        val codes = arrayOf("auto", "ru", "en")
        val current = codes.indexOf(L.pref(this)).coerceAtLeast(0)
        val card = cardBox()
        card.addView(
            segmented(
                listOf(getString(R.string.lang_auto), getString(R.string.lang_ru), getString(R.string.lang_en)),
                current
            ) { i ->
                L.setPref(this, codes[i])
                Widgets.updateAll(this)
                recreate()
            }
        )
        card.addView(Ui.label(this, getString(R.string.lang_help), 12f, Ui.SUB), lp(match, wrap, 10))
        root.addView(card, lp(match, wrap, 10))
    }

    // ---------------------------------------------------------- «Резервная копия»

    private fun sectionBackup(cfg: Config) {
        header(getString(R.string.menu_backup))
        val card = cardBox()
        card.addView(Ui.label(this, getString(R.string.backup_help), 12f, Ui.SUB))
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        val h = Ui.dp(this, 46)
        val copy = button(getString(R.string.copy), Ui.ROW, Ui.TEXT)
        copy.setOnClickListener {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText(getString(R.string.app_name), cfg.toJson().toString()))
            Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show()
        }
        val paste = button(getString(R.string.paste), Ui.ROW, Ui.TEXT)
        paste.setOnClickListener { pasteDialog() }
        row.addView(copy, LinearLayout.LayoutParams(0, h, 1f))
        val second = LinearLayout.LayoutParams(0, h, 1f)
        second.marginStart = Ui.dp(this, 8)
        row.addView(paste, second)
        card.addView(row, lp(match, wrap, 10))
        root.addView(card, lp(match, wrap, 10))
    }

    private fun pasteDialog() {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        val p = Ui.dp(this, 20)
        box.setPadding(p, p / 2, p, 0)
        val field = EditText(this)
        field.hint = getString(R.string.restore_hint)
        field.minLines = 3
        box.addView(field)
        AlertDialog.Builder(this)
            .setTitle(R.string.restore_title)
            .setView(box)
            .setPositiveButton(R.string.restore) { _, _ ->
                try {
                    val restored = Config.fromJson(JSONObject(field.text.toString().trim()))
                    ConfigStore.save(this, restored)
                    render()
                    Toast.makeText(this, R.string.restored, Toast.LENGTH_SHORT).show()
                } catch (ex: Exception) {
                    Toast.makeText(this, R.string.restore_failed, Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    // ---------------------------------------------------------- «О приложении и поддержка»

    private fun sectionAbout(cfg: Config) {
        header(getString(R.string.menu_about))

        val about = cardBox()
        about.addView(Ui.label(this, getString(R.string.app_name), 20f, Ui.TEXT, true))
        val version = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: ""
        } catch (e: Exception) {
            ""
        }
        about.addView(Ui.label(this, getString(R.string.about_version, version), 13f, Ui.SUB), lp(match, wrap, 2))
        about.addView(Ui.label(this, getString(R.string.about_text), 14f, Ui.TEXT), lp(match, wrap, 10))
        about.addView(Ui.label(this, getString(R.string.about_privacy), 13f, Ui.SUB), lp(match, wrap, 10))
        root.addView(about, lp(match, wrap, 10))

        val donate = cardBox()
        donate.addView(Ui.label(this, getString(R.string.donate_title), 17f, Ui.TEXT, true))
        donate.addView(Ui.label(this, getString(R.string.donate_text), 13f, Ui.SUB), lp(match, wrap, 6))
        val db = button(getString(R.string.donate_button), Ui.ACCENT, Ui.DARK)
        db.setOnClickListener {
            if (Links.DONATE_URL.isEmpty()) {
                Toast.makeText(this, R.string.donate_unset, Toast.LENGTH_LONG).show()
            } else {
                try {
                    startActivity(Browsers.siteIntent(this, Links.DONATE_URL, ""))
                } catch (e: Exception) {
                    Toast.makeText(this, R.string.open_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }
        donate.addView(db, lp(match, Ui.dp(this, 48), 12))
        root.addView(donate, lp(match, wrap, 10))

        val info = cardBox()
        info.addView(Ui.label(this, getString(R.string.found_apps), 15f, Ui.TEXT, true))
        info.addView(
            Ui.label(this, getString(R.string.apps_count, Apps.launchable(this).size), 12f, Ui.SUB),
            lp(match, wrap, 4)
        )
        for (svc in cfg.services) {
            if (!svc.enabled) continue
            val pkg = Apps.packageFor(this, svc)
            val line = svc.title + ": " + (pkg ?: getString(R.string.not_found))
            info.addView(Ui.label(this, line, 12f, Ui.SUB), lp(match, wrap, 2))
        }
        root.addView(info, lp(match, wrap, 10))
    }
}
