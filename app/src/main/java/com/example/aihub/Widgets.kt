package com.example.aihub

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import java.util.Random

object Widgets {

    const val ACTION_PREV = "com.example.aihub.PREV"
    const val ACTION_NEXT = "com.example.aihub.NEXT"
    const val ACTION_SHUFFLE = "com.example.aihub.SHUFFLE"
    const val MAX_SLOTS = 6

    fun updateAll(context: Context) {
        val m = AppWidgetManager.getInstance(context)
        val g = m.getAppWidgetIds(ComponentName(context, GroupsWidget::class.java))
        val r = m.getAppWidgetIds(ComponentName(context, RowsWidget::class.java))
        val c = m.getAppWidgetIds(ComponentName(context, ColumnsWidget::class.java))
        if (g.isEmpty() && r.isEmpty() && c.isEmpty()) return
        val cfg = ConfigStore.load(context)
        if (g.isNotEmpty()) {
            val v = groupsViews(context)
            for (id in g) m.updateAppWidget(id, v)
        }
        for (id in r) m.updateAppWidget(id, rowsViews(context, id, rowsCount(cfg, m, id)))
        for (id in c) m.updateAppWidget(id, colsViews(context, id, colsCount(cfg, m, id)))
    }

    private fun pending(context: Context, code: Int, intent: Intent): PendingIntent {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(
            context, code, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun settingsIntent(context: Context): Intent = Intent(context, MainActivity::class.java)

    /** Пакет, значок которого показываем на кнопке: выбранное приложение или веб-приложение входа. */
    private fun iconPkg(context: Context, e: EntryCfg): String? =
        if (e.pkg.isNotEmpty() && Apps.isInstalled(context, e.pkg)) e.pkg else null

    // ------------------------------------------------------------------
    // Общее для «Строк» и «Столбцов»
    // ------------------------------------------------------------------
    private class Item(val svc: ServiceCfg, val entry: EntryCfg, val multi: Boolean)

    private fun items(cfg: Config): List<Item> {
        val list = ArrayList<Item>()
        for (svc in cfg.services) {
            if (!svc.enabled) continue
            val es = cfg.entriesFor(svc.id)
            for (e in es) {
                if (e.inRows) list.add(Item(svc, e, es.size > 1))
            }
        }
        return list
    }

    private fun state(context: Context) =
        context.getSharedPreferences("rows_state", Context.MODE_PRIVATE)

    private fun pageOf(context: Context, id: Int): Int = state(context).getInt("page_" + id, 0)

    private fun seedOf(context: Context): Int = state(context).getInt("seed", 1)

    private fun pageCount(total: Int, perPage: Int): Int =
        if (total == 0) 1 else (total + perPage - 1) / perPage

    private fun navIntent(context: Context, cls: Class<*>, id: Int, action: String, k: Int): PendingIntent {
        val intent = Intent(context, cls)
        intent.action = action
        intent.data = Uri.parse("aihub://" + cls.simpleName + "/" + id + "/" + action)
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
        intent.putExtra("tok", token(context))
        return PendingIntent.getBroadcast(
            context, 1000 + id * 4 + k, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Секретная метка этой установки: команды листания принимаются только с ней, чужие приложения их не подделают. */
    private fun token(context: Context): String {
        val p = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val existing = p.getString("nav_token", null)
        if (existing != null) return existing
        val fresh = java.util.UUID.randomUUID().toString()
        p.edit().putString("nav_token", fresh).apply()
        return fresh
    }

    /** Истина, если команда листания или перемешивания пришла от самого виджета. */
    fun isOwnNav(context: Context, intent: Intent): Boolean {
        val a = intent.action
        if (a != ACTION_PREV && a != ACTION_NEXT && a != ACTION_SHUFFLE) return false
        return intent.getStringExtra("tok") == token(context)
    }

    fun shuffle(context: Context) {
        state(context).edit().putInt("seed", Random().nextInt(1000000)).apply()
        updateAll(context)
    }

    // ------------------------------------------------------------------
    // Виджет «Группы»: до 4 ИИ, у каждого до 3 входов.
    // ------------------------------------------------------------------
    fun groupsViews(context: Context): RemoteViews {
        val lc = L.wrap(context)
        val cfg = ConfigStore.load(context)
        val views = RemoteViews(context.packageName, R.layout.widget_groups)
        val sk = Skin.forced(context)
        if (sk != null) {
            views.setInt(R.id.root_groups, "setBackgroundResource", sk.cardRes)
            views.setTextColor(R.id.empty_groups, sk.sub)
        }
        val shown = cfg.services.filter { it.enabled && cfg.entriesFor(it.id).isNotEmpty() }.take(4)

        views.setViewVisibility(R.id.empty_groups, if (shown.isEmpty()) View.VISIBLE else View.GONE)
        views.setTextViewText(R.id.empty_groups, lc.getString(R.string.empty_hint))
        if (shown.isEmpty()) {
            views.setOnClickPendingIntent(R.id.empty_groups, pending(context, 90, settingsIntent(context)))
        }

        for (r in 0 until 4) {
            if (r >= shown.size) {
                views.setViewVisibility(Ids.row[r], View.GONE)
                continue
            }
            val svc = shown[r]
            val entries = cfg.entriesFor(svc.id).take(3)
            views.setViewVisibility(Ids.row[r], View.VISIBLE)
            views.setImageViewBitmap(Ids.tile[r], Tiles.render(context, svc, cfg, 126))
            views.setTextViewText(Ids.name[r], svc.title)
            if (sk != null) {
                views.setInt(Ids.row[r], "setBackgroundResource", sk.rowRes)
                views.setTextColor(Ids.name[r], sk.text)
            }
            views.setOnClickPendingIntent(
                Ids.tile[r],
                pending(context, 100 + r * 10 + 9, Open.intentFor(context, svc, entries[0]))
            )

            for (c in 0 until 3) {
                val chip = Ids.chip[r][c]
                if (c >= entries.size) {
                    views.setViewVisibility(chip, View.GONE)
                    continue
                }
                val e = entries[c]
                val label = Labels.of(lc, e)
                views.setViewVisibility(chip, View.VISIBLE)
                if (sk != null) {
                    views.setInt(chip, "setBackgroundResource", sk.chipRes)
                    views.setTextColor(Ids.chipText[r][c], sk.text)
                    views.setInt(Ids.chipApp[r][c], "setColorFilter", sk.text)
                }
                if (e.mode == "app") {
                    views.setViewVisibility(Ids.chipApp[r][c], View.VISIBLE)
                    views.setViewVisibility(Ids.chipWeb[r][c], View.GONE)
                    views.setViewVisibility(Ids.chipText[r][c], View.GONE)
                } else {
                    views.setViewVisibility(Ids.chipApp[r][c], View.GONE)
                    views.setViewVisibility(Ids.chipWeb[r][c], View.VISIBLE)
                    views.setInt(Ids.chipWeb[r][c], "setColorFilter", e.color)
                    views.setViewVisibility(Ids.chipText[r][c], View.VISIBLE)
                    views.setTextViewText(Ids.chipText[r][c], label)
                }
                views.setContentDescription(chip, svc.title + ": " + label)
                views.setOnClickPendingIntent(
                    chip,
                    pending(context, 100 + r * 10 + c, Open.intentFor(context, svc, e))
                )
            }
        }
        return views
    }

    // ------------------------------------------------------------------
    // Виджет «Строки»: 1 или 2 строки кнопок, страницы, режим «аквариум».
    // ------------------------------------------------------------------

    /** Сколько строк показывать: по настройке либо по высоте виджета (две, если он вытянут). */
    fun rowsCount(cfg: Config, manager: AppWidgetManager, id: Int): Int {
        if (cfg.rowsMode in 1..4) return cfg.rowsMode
        val h = manager.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
        return ((h + 8) / 72).coerceIn(1, 4)
    }

    fun turnRows(context: Context, id: Int, delta: Int) {
        val m = AppWidgetManager.getInstance(context)
        val cfg = ConfigStore.load(context)
        val rows = rowsCount(cfg, m, id)
        val pages = pageCount(items(cfg).size, rows * cfg.perRow)
        val next = ((pageOf(context, id) + delta) % pages + pages) % pages
        state(context).edit().putInt("page_" + id, next).apply()
        m.updateAppWidget(id, rowsViews(context, id, rows))
    }

    fun rowsViews(context: Context, id: Int, rows: Int): RemoteViews {
        val lc = L.wrap(context)
        val cfg = ConfigStore.load(context)
        val aq = cfg.aquarium
        val views = RemoteViews(
            context.packageName,
            if (aq) R.layout.widget_aquarium else R.layout.widget_rows
        )
        val all = items(cfg)
        val perRow = cfg.perRow
        val perPage = rows * perRow
        val pages = pageCount(all.size, perPage)
        val page = pageOf(context, id).coerceIn(0, pages - 1)
        val shown = all.drop(page * perPage).take(perPage)
        val seed = seedOf(context)
        val density = context.resources.displayMetrics.density
        val empty = all.isEmpty()

        val sk = Skin.forced(context)
        if (sk != null) {
            views.setInt(R.id.root_rows, "setBackgroundResource", sk.thinRes)
            views.setTextColor(R.id.empty_rows, sk.sub)
            views.setInt(R.id.nav_prev, "setColorFilter", sk.sub)
            views.setInt(R.id.nav_next, "setColorFilter", sk.sub)
            if (aq) views.setInt(R.id.nav_shuffle, "setColorFilter", sk.sub)
        }
        views.setViewVisibility(R.id.empty_rows, if (empty) View.VISIBLE else View.GONE)
        views.setTextViewText(R.id.empty_rows, lc.getString(R.string.empty_hint))
        if (empty) {
            views.setOnClickPendingIntent(R.id.empty_rows, pending(context, 92, settingsIntent(context)))
        }
        for (r in 0 until 4) {
            views.setViewVisibility(Ids.rowBox[r], if (empty || r >= rows) View.GONE else View.VISIBLE)
        }

        val paged = pages > 1
        views.setViewVisibility(R.id.nav_prev, if (paged) View.VISIBLE else View.GONE)
        views.setViewVisibility(R.id.nav_next, if (paged) View.VISIBLE else View.GONE)
        views.setContentDescription(R.id.nav_prev, lc.getString(R.string.nav_prev_desc))
        views.setContentDescription(R.id.nav_next, lc.getString(R.string.nav_next_desc))
        if (paged) {
            views.setOnClickPendingIntent(R.id.nav_prev, navIntent(context, RowsWidget::class.java, id, ACTION_PREV, 0))
            views.setOnClickPendingIntent(R.id.nav_next, navIntent(context, RowsWidget::class.java, id, ACTION_NEXT, 1))
        }
        if (aq) {
            views.setViewVisibility(R.id.nav_shuffle, if (empty) View.GONE else View.VISIBLE)
            views.setContentDescription(R.id.nav_shuffle, lc.getString(R.string.nav_shuffle_desc))
            views.setOnClickPendingIntent(
                R.id.nav_shuffle, navIntent(context, RowsWidget::class.java, id, ACTION_SHUFFLE, 2)
            )
        }

        for (r in 0 until 4) {
            for (s in 0 until MAX_SLOTS) {
                val slotId = if (aq) Ids.fl[r][s] else Ids.rt[r][s]
                val idx = r * perRow + s
                if (empty || r >= rows || s >= perRow || idx >= shown.size) {
                    views.setViewVisibility(slotId, View.GONE)
                    continue
                }
                val item = shown[idx]
                val label = Labels.of(lc, item.entry)
                val badge = if (item.multi) label.take(1).uppercase() else ""
                val bmp = Tiles.render(context, item.svc, cfg, 140, badge, item.entry.color, iconPkg(context, item.entry))
                views.setViewVisibility(slotId, View.VISIBLE)
                if (aq) {
                    views.setImageViewBitmap(Ids.fa[r][s], bmp)
                    views.setImageViewBitmap(Ids.fb[r][s], bmp)
                    val rnd = Random((seed + r * 31 + s * 7 + page * 13).toLong())
                    val top = (rnd.nextInt(9) * density).toInt()
                    val bottom = (8 * density).toInt() - top
                    views.setViewPadding(slotId, 0, top, 0, bottom)
                } else {
                    views.setImageViewBitmap(slotId, bmp)
                }
                views.setContentDescription(slotId, item.svc.title + ": " + label)
                views.setOnClickPendingIntent(
                    slotId,
                    pending(context, 300 + r * 10 + s, Open.intentFor(context, item.svc, item.entry))
                )
            }
        }
        return views
    }

    // ------------------------------------------------------------------
    // Виджет «Столбцы»: вертикальный, 1 или 2 столбца.
    // ------------------------------------------------------------------

    /** Сколько столбцов показывать: по настройке либо по ширине виджета (два, если он широкий). */
    fun colsCount(cfg: Config, manager: AppWidgetManager, id: Int): Int {
        if (cfg.colsMode == 1) return 1
        if (cfg.colsMode == 2) return 2
        val w = manager.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
        return if (w >= 110) 2 else 1
    }

    fun turnCols(context: Context, id: Int, delta: Int) {
        val m = AppWidgetManager.getInstance(context)
        val cfg = ConfigStore.load(context)
        val cols = colsCount(cfg, m, id)
        val pages = pageCount(items(cfg).size, cols * cfg.perCol)
        val next = ((pageOf(context, id) + delta) % pages + pages) % pages
        state(context).edit().putInt("page_" + id, next).apply()
        m.updateAppWidget(id, colsViews(context, id, cols))
    }

    fun colsViews(context: Context, id: Int, cols: Int): RemoteViews {
        val lc = L.wrap(context)
        val cfg = ConfigStore.load(context)
        val aq = cfg.aquarium
        val views = RemoteViews(
            context.packageName,
            if (aq) R.layout.widget_columns_aq else R.layout.widget_columns
        )
        val all = items(cfg)
        val perCol = cfg.perCol
        val perPage = cols * perCol
        val pages = pageCount(all.size, perPage)
        val page = pageOf(context, id).coerceIn(0, pages - 1)
        val shown = all.drop(page * perPage).take(perPage)
        val seed = seedOf(context)
        val density = context.resources.displayMetrics.density
        val empty = all.isEmpty()

        val sk = Skin.forced(context)
        if (sk != null) {
            views.setInt(R.id.root_cols, "setBackgroundResource", sk.thinRes)
            views.setTextColor(R.id.vempty, sk.sub)
            views.setInt(R.id.vnav_prev, "setColorFilter", sk.sub)
            views.setInt(R.id.vnav_next, "setColorFilter", sk.sub)
            if (aq) views.setInt(R.id.vnav_shuffle, "setColorFilter", sk.sub)
        }
        views.setViewVisibility(R.id.vempty, if (empty) View.VISIBLE else View.GONE)
        views.setTextViewText(R.id.vempty, lc.getString(R.string.empty_hint))
        if (empty) {
            views.setOnClickPendingIntent(R.id.vempty, pending(context, 93, settingsIntent(context)))
        }
        views.setViewVisibility(R.id.vcols, if (empty) View.GONE else View.VISIBLE)
        views.setViewVisibility(Ids.colBox[1], if (cols < 2) View.GONE else View.VISIBLE)

        val paged = pages > 1
        views.setViewVisibility(R.id.vnav, if (!empty && (paged || aq)) View.VISIBLE else View.GONE)
        views.setViewVisibility(R.id.vnav_prev, if (paged) View.VISIBLE else View.GONE)
        views.setViewVisibility(R.id.vnav_next, if (paged) View.VISIBLE else View.GONE)
        views.setContentDescription(R.id.vnav_prev, lc.getString(R.string.nav_prev_desc))
        views.setContentDescription(R.id.vnav_next, lc.getString(R.string.nav_next_desc))
        if (paged) {
            views.setOnClickPendingIntent(R.id.vnav_prev, navIntent(context, ColumnsWidget::class.java, id, ACTION_PREV, 0))
            views.setOnClickPendingIntent(R.id.vnav_next, navIntent(context, ColumnsWidget::class.java, id, ACTION_NEXT, 1))
        }
        if (aq) {
            views.setViewVisibility(R.id.vnav_shuffle, if (empty) View.GONE else View.VISIBLE)
            views.setContentDescription(R.id.vnav_shuffle, lc.getString(R.string.nav_shuffle_desc))
            views.setOnClickPendingIntent(
                R.id.vnav_shuffle, navIntent(context, ColumnsWidget::class.java, id, ACTION_SHUFFLE, 2)
            )
        }

        for (c in 0 until 2) {
            for (s in 0 until MAX_SLOTS) {
                val slotId = if (aq) Ids.vfl[c][s] else Ids.vt[c][s]
                val idx = c * perCol + s
                if (empty || c >= cols || s >= perCol || idx >= shown.size) {
                    views.setViewVisibility(slotId, View.GONE)
                    continue
                }
                val item = shown[idx]
                val label = Labels.of(lc, item.entry)
                val badge = if (item.multi) label.take(1).uppercase() else ""
                val bmp = Tiles.render(context, item.svc, cfg, 140, badge, item.entry.color, iconPkg(context, item.entry))
                views.setViewVisibility(slotId, View.VISIBLE)
                if (aq) {
                    views.setImageViewBitmap(Ids.vfa[c][s], bmp)
                    views.setImageViewBitmap(Ids.vfb[c][s], bmp)
                    val rnd = Random((seed + c * 31 + s * 7 + page * 13).toLong())
                    val left = (rnd.nextInt(9) * density).toInt()
                    val right = (8 * density).toInt() - left
                    views.setViewPadding(slotId, left, 0, right, 0)
                } else {
                    views.setImageViewBitmap(slotId, bmp)
                }
                views.setContentDescription(slotId, item.svc.title + ": " + label)
                views.setOnClickPendingIntent(
                    slotId,
                    pending(context, 400 + c * 10 + s, Open.intentFor(context, item.svc, item.entry))
                )
            }
        }
        return views
    }
}

class GroupsWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val v = Widgets.groupsViews(context)
        for (id in ids) manager.updateAppWidget(id, v)
    }
}

class RowsWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val cfg = ConfigStore.load(context)
        for (id in ids) {
            manager.updateAppWidget(id, Widgets.rowsViews(context, id, Widgets.rowsCount(cfg, manager, id)))
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        val cfg = ConfigStore.load(context)
        manager.updateAppWidget(
            appWidgetId,
            Widgets.rowsViews(context, appWidgetId, Widgets.rowsCount(cfg, manager, appWidgetId))
        )
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (!Widgets.isOwnNav(context, intent)) return
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        when (intent.action) {
            Widgets.ACTION_PREV -> if (id != AppWidgetManager.INVALID_APPWIDGET_ID) Widgets.turnRows(context, id, -1)
            Widgets.ACTION_NEXT -> if (id != AppWidgetManager.INVALID_APPWIDGET_ID) Widgets.turnRows(context, id, 1)
            Widgets.ACTION_SHUFFLE -> Widgets.shuffle(context)
        }
    }
}

class ColumnsWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val cfg = ConfigStore.load(context)
        for (id in ids) {
            manager.updateAppWidget(id, Widgets.colsViews(context, id, Widgets.colsCount(cfg, manager, id)))
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        val cfg = ConfigStore.load(context)
        manager.updateAppWidget(
            appWidgetId,
            Widgets.colsViews(context, appWidgetId, Widgets.colsCount(cfg, manager, appWidgetId))
        )
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (!Widgets.isOwnNav(context, intent)) return
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        when (intent.action) {
            Widgets.ACTION_PREV -> if (id != AppWidgetManager.INVALID_APPWIDGET_ID) Widgets.turnCols(context, id, -1)
            Widgets.ACTION_NEXT -> if (id != AppWidgetManager.INVALID_APPWIDGET_ID) Widgets.turnCols(context, id, 1)
            Widgets.ACTION_SHUFFLE -> Widgets.shuffle(context)
        }
    }
}
