package com.example.aihub

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.TextView

object Ui {
    /** Тёмная ли сейчас тема. Задаётся при открытии каждого экрана. */
    var dark: Boolean = true

    val BG: Int get() = if (dark) 0xFF121212.toInt() else 0xFFF4F1EA.toInt()
    val CARD: Int get() = if (dark) 0xFF1C1C1E.toInt() else 0xFFFFFFFF.toInt()
    val ROW: Int get() = if (dark) 0xFF2C2C2E.toInt() else 0xFFEFEBE2.toInt()
    val TEXT: Int get() = if (dark) 0xFFF2F4F8.toInt() else 0xFF1B1D22.toInt()
    val SUB: Int get() = if (dark) 0xFF9AA3B2.toInt() else 0xFF5E6676.toInt()
    val ACCENT: Int get() = if (dark) 0xFF8AB4F8.toInt() else 0xFF2F6FDE.toInt()
    val OK: Int get() = if (dark) 0xFF3A9D5D.toInt() else 0xFF2E8B57.toInt()
    val RED: Int get() = if (dark) 0xFFE5604F.toInt() else 0xFFC23B2E.toInt()

    /** Цвет текста на кнопках акцентного цвета. */
    val ON_ACCENT: Int get() = if (dark) 0xFF14161A.toInt() else 0xFFFFFFFF.toInt()

    /** Тёмный текст для цветных меток и значков. */
    val DARK: Int = 0xFF14161A.toInt()

    /** Выбирает тему окна. Вызывать в начале onCreate, до super.onCreate. */
    fun apply(activity: Activity) {
        dark = Skin.isDark(activity)
        activity.setTheme(if (dark) R.style.AppTheme else R.style.AppThemeLight)
    }

    /** Цвета системных панелей под тему. Вызывать после setContentView. */
    @Suppress("DEPRECATION")
    fun bars(activity: Activity) {
        val w = activity.window
        w.statusBarColor = BG
        w.navigationBarColor = BG
        var flags = w.decorView.systemUiVisibility
        val light = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        flags = if (dark) flags and light.inv() else flags or light
        w.decorView.systemUiVisibility = flags
    }

    /** Диалог в цвете текущей темы. */
    fun dialog(context: Context): AlertDialog.Builder {
        dark = Skin.isDark(context)
        return AlertDialog.Builder(
            context,
            if (dark) android.R.style.Theme_Material_Dialog_Alert else android.R.style.Theme_Material_Light_Dialog_Alert
        )
    }

    fun dp(context: Context, v: Int): Int =
        (v * context.resources.displayMetrics.density + 0.5f).toInt()

    fun rounded(context: Context, color: Int, radiusDp: Int): GradientDrawable {
        val d = GradientDrawable()
        d.setColor(color)
        d.cornerRadius = dp(context, radiusDp).toFloat()
        return d
    }

    fun oval(color: Int): GradientDrawable {
        val d = GradientDrawable()
        d.shape = GradientDrawable.OVAL
        d.setColor(color)
        return d
    }

    fun label(context: Context, s: String, sizeSp: Float, color: Int, bold: Boolean = false): TextView {
        val t = TextView(context)
        t.text = s
        t.textSize = sizeSp
        t.setTextColor(color)
        if (bold) t.setTypeface(null, Typeface.BOLD)
        return t
    }
}
