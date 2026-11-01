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

/** Экран «Мои ИИ»: сервисы, их входы, порядок, вид и резервная копия. */
class MainActivity : Activity() {

    private lateinit var root: LinearLayout
    private val match = LinearLayout.LayoutParams.MATCH_PARENT
    private val wrap = LinearLayout.LayoutParams.WRAP_CONTENT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scroll = ScrollView(this)
        root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        val p = Ui.dp(this, 16)
        root.setPadding(p, p, p, p)
        scroll.addView(root)
        setContentView(scroll)
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun lp(w: Int, h: Int, topDp: Int = 0): LinearLayout.LayoutParams {
        val l = LinearLayout.LayoutParams(w, h)
        l.topMargin = Ui.dp(this, topDp)
        return l
    }

    private fun render() {
        val cfg = ConfigStore.load(this)
        root.removeAllViews()

        root.addView(Ui.label(this, "Мои ИИ", 26f, Ui.TEXT, true))
        root.addView(
            Ui.label(
                this,
                "Включайте сервисы, добавляйте несколько входов (приложение, браузер, аккаунты) " +
                    "и расставляйте их по порядку.",
                13f, Ui.SUB
            ),
            lp(match, wrap, 6)
        )

        for (i in cfg.services.indices) {
            root.addView(serviceCard(cfg, i), lp(match, wrap, 10))
        }

        val add = button("+ Добавить свой ИИ", Ui.ROW, Ui.TEXT)
        add.setOnClickListener { addCustomDialog(cfg) }
        root.addView(add, lp(match, Ui.dp(this, 48), 14))

        root.addView(appearanceCard(cfg), lp(match, wrap, 14))
        root.addView(widgetsCard(), lp(match, wrap, 14))
        root.addView(backupCard(cfg), lp(match, wrap, 14))
        root.addView(infoCard(cfg), lp(match, wrap, 14))
    }

    // ---------------------------------------------------------- общие элементы

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

    // ---------------------------------------------------------- сервисы и входы

    private fun moveService(cfg: Config, from: Int, to: Int) {
        if (to < 0 || to >= cfg.services.size) return
        java.util.Collections.swap(cfg.services, from, to)
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

    private fun serviceCard(cfg: Config, index: Int): View {
        val svc = cfg.services[index]
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

        header.addView(arrow("▲", 40) { moveService(cfg, index, index - 1) })
        header.addView(arrow("▼", 40) { moveService(cfg, index, index + 1) })

        val sw = Switch(this)
        sw.isChecked = svc.enabled
        sw.setOnCheckedChangeListener { _, on ->
            svc.enabled = on
            ConfigStore.save(this, cfg)
        }
        header.addView(sw)
        card.addView(header)

        for (e in cfg.entriesFor(svc.id)) {
            card.addView(entryRow(cfg, svc, e), lp(match, wrap, 6))
        }

        val addEntry = Ui.label(this, "+ Вход", 14f, Ui.ACCENT, true)
        addEntry.gravity = Gravity.CENTER
        val ap = Ui.dp(this, 10)
        addEntry.setPadding(ap, ap, ap, ap)
        addEntry.setOnClickListener {
            startActivity(Intent(this, EntryActivity::class.java).putExtra("service", svc.id))
        }
        card.addView(addEntry, lp(match, wrap, 4))

        if (svc.custom) {
            val del = Ui.label(this, "Удалить этот ИИ", 13f, Ui.RED)
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
        val hidden = if (e.inRows) "" else " · скрыт в строках"
        if (e.mode == "app") {
            val name = Apps.label(this, Apps.packageFor(this, svc))
            return "Приложение: " + (name ?: "не найдено, откроется сайт") + hidden
        }
        val browserPkg = if (e.browserPkg.isEmpty()) null else e.browserPkg
        val where = Apps.label(this, browserPkg) ?: "браузер по умолчанию"
        val extra = if (e.url.isNotEmpty()) " · " + e.url.removePrefix("https://") else ""
        return "Сайт · " + where + extra + hidden
    }

    private fun entryRow(cfg: Config, svc: ServiceCfg, e: EntryCfg): View {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.background = Ui.rounded(this, Ui.ROW, 10)
        val pad = Ui.dp(this, 10)
        row.setPadding(pad, Ui.dp(this, 8), Ui.dp(this, 2), Ui.dp(this, 8))

        val badge = Ui.label(this, e.label.take(1).uppercase(), 12f, Ui.DARK, true)
        badge.gravity = Gravity.CENTER
        badge.background = Ui.oval(e.color)
        row.addView(badge, LinearLayout.LayoutParams(Ui.dp(this, 28), Ui.dp(this, 28)))

        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.addView(Ui.label(this, e.label, 14f, Ui.TEXT, true))
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
        name.hint = "Название"
        val url = EditText(this)
        url.hint = "Ссылка, например example.com"
        url.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
        box.addView(name)
        box.addView(url)
        AlertDialog.Builder(this)
            .setTitle("Свой ИИ")
            .setView(box)
            .setPositiveButton("Добавить") { _, _ ->
                val n = name.text.toString().trim()
                var u = url.text.toString().trim()
                if (n.isNotEmpty() && u.isNotEmpty()) {
                    if (!u.startsWith("http")) u = "https://" + u
                    val id = "c_" + Config.newId()
                    val mono = n.take(2).replaceFirstChar { it.uppercase() }
                    cfg.services.add(
                        ServiceCfg(id, n, mono, 0xFF2C2C31.toInt(), 0xFFE8E8ED.toInt(), u, true, true, "")
                    )
                    cfg.entries.add(EntryCfg(Config.newId(), id, "Сайт", "web", "", "", Config.COLOR_BLUE))
                    ConfigStore.save(this, cfg)
                    render()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    // ---------------------------------------------------------- вид и виджеты

    private fun appearanceCard(cfg: Config): View {
        val card = cardBox()
        card.addView(Ui.label(this, "Вид значков", 17f, Ui.TEXT, true))
        card.addView(
            segmented(listOf("Закруглённые", "Круглые"), if (cfg.shape == "round") 1 else 0) { i ->
                cfg.shape = if (i == 1) "round" else "rounded"
                ConfigStore.save(this, cfg)
                render()
            },
            lp(match, wrap, 8)
        )

        val real = Switch(this)
        real.text = "Значки из установленных приложений"
        real.setTextColor(Ui.TEXT)
        real.isChecked = cfg.realIcons
        real.setOnCheckedChangeListener { _, on ->
            cfg.realIcons = on
            ConfigStore.save(this, cfg)
            root.post { render() }
        }
        card.addView(real, lp(match, wrap, 12))

        card.addView(Ui.label(this, "Виджет «Строки»", 17f, Ui.TEXT, true), lp(match, wrap, 18))
        card.addView(Ui.label(this, "Кнопок в строке", 12f, Ui.SUB), lp(match, wrap, 8))
        card.addView(
            segmented(listOf("4", "5", "6"), cfg.perRow - 4) { i ->
                cfg.perRow = 4 + i
                ConfigStore.save(this, cfg)
                render()
            },
            lp(match, wrap, 4)
        )
        card.addView(Ui.label(this, "Строк", 12f, Ui.SUB), lp(match, wrap, 10))
        card.addView(
            segmented(listOf("Авто по высоте", "1", "2"), cfg.rowsMode) { i ->
                cfg.rowsMode = i
                ConfigStore.save(this, cfg)
                render()
            },
            lp(match, wrap, 4)
        )

        val aq = Switch(this)
        aq.text = "Режим «аквариум»"
        aq.setTextColor(Ui.TEXT)
        aq.isChecked = cfg.aquarium
        aq.setOnCheckedChangeListener { _, on ->
            cfg.aquarium = on
            ConfigStore.save(this, cfg)
        }
        card.addView(aq, lp(match, wrap, 14))
        card.addView(
            Ui.label(
                this,
                "Кнопки покачиваются в воде, у каждой свой ритм. Кнопка с перекрещенными стрелками " +
                    "перемешивает расположение, а при большом числе кнопок страницы листаются " +
                    "стрелками по бокам. Какие входы показывать в строках, выбирается в самом входе.",
                12f, Ui.SUB
            ),
            lp(match, wrap, 4)
        )
        return card
    }

    private fun pin(cls: Class<*>) {
        val m = AppWidgetManager.getInstance(this)
        if (m.isRequestPinAppWidgetSupported) {
            m.requestPinAppWidget(ComponentName(this, cls), null, null)
        } else {
            Toast.makeText(
                this,
                "Оболочка не поддерживает быстрое добавление. Добавьте виджет с главного экрана.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun widgetsCard(): View {
        val card = cardBox()
        card.addView(Ui.label(this, "Добавить виджет на экран", 17f, Ui.TEXT, true))
        val b1 = button("«Строки» (1–2 строки кнопок)", Ui.ROW, Ui.TEXT)
        b1.setOnClickListener { pin(RowsWidget::class.java) }
        card.addView(b1, lp(match, Ui.dp(this, 46), 8))
        val b2 = button("«Группы» (ряд на каждый ИИ)", Ui.ROW, Ui.TEXT)
        b2.setOnClickListener { pin(GroupsWidget::class.java) }
        card.addView(b2, lp(match, Ui.dp(this, 46), 8))
        card.addView(
            Ui.label(
                this,
                "Если окно не появилось, добавьте виджет вручную: долгое нажатие на главном экране, «Виджеты», «ИИ-запуск».",
                12f, Ui.SUB
            ),
            lp(match, wrap, 8)
        )
        return card
    }

    // ---------------------------------------------------------- резервная копия

    private fun backupCard(cfg: Config): View {
        val card = cardBox()
        card.addView(Ui.label(this, "Резервная копия настроек", 17f, Ui.TEXT, true))
        card.addView(
            Ui.label(
                this,
                "Копирует настройки в буфер обмена как текст. Сохраните его в заметках, чтобы восстановить после переустановки.",
                12f, Ui.SUB
            ),
            lp(match, wrap, 4)
        )
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        val h = Ui.dp(this, 46)
        val copy = button("Копировать", Ui.ROW, Ui.TEXT)
        copy.setOnClickListener {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("ИИ-запуск", cfg.toJson().toString()))
            Toast.makeText(this, "Настройки скопированы", Toast.LENGTH_SHORT).show()
        }
        val paste = button("Вставить", Ui.ROW, Ui.TEXT)
        paste.setOnClickListener { pasteDialog() }
        row.addView(copy, LinearLayout.LayoutParams(0, h, 1f))
        val second = LinearLayout.LayoutParams(0, h, 1f)
        second.marginStart = Ui.dp(this, 8)
        row.addView(paste, second)
        card.addView(row, lp(match, wrap, 10))
        return card
    }

    private fun pasteDialog() {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        val p = Ui.dp(this, 20)
        box.setPadding(p, p / 2, p, 0)
        val field = EditText(this)
        field.hint = "Вставьте скопированные настройки"
        field.minLines = 3
        box.addView(field)
        AlertDialog.Builder(this)
            .setTitle("Восстановить настройки")
            .setView(box)
            .setPositiveButton("Восстановить") { _, _ ->
                try {
                    val restored = Config.fromJson(JSONObject(field.text.toString().trim()))
                    ConfigStore.save(this, restored)
                    render()
                    Toast.makeText(this, "Настройки восстановлены", Toast.LENGTH_SHORT).show()
                } catch (ex: Exception) {
                    Toast.makeText(this, "Не удалось прочитать настройки", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    // ---------------------------------------------------------- диагностика

    private fun infoCard(cfg: Config): View {
        val card = cardBox()
        card.addView(Ui.label(this, "О приложении", 17f, Ui.TEXT, true))
        card.addView(
            Ui.label(
                this,
                "Версия 1.1. Приложение без разрешений, не использует сеть и ничего не собирает. " +
                    "Установленных приложений с запуском: " + Apps.launchable(this).size + ".",
                12f, Ui.SUB
            ),
            lp(match, wrap, 4)
        )
        card.addView(Ui.label(this, "Какие приложения найдены", 13f, Ui.TEXT, true), lp(match, wrap, 10))
        for (svc in cfg.services) {
            if (!svc.enabled) continue
            val pkg = Apps.packageFor(this, svc)
            val line = svc.title + ": " + (pkg ?: "не найдено, откроется сайт")
            card.addView(Ui.label(this, line, 12f, Ui.SUB), lp(match, wrap, 2))
        }
        return card
    }
}
