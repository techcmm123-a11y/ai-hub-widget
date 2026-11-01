package com.example.aihub

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.widget.TextView

object Ui {
    val CARD = 0xFF1C1C1E.toInt()
    val ROW = 0xFF2C2C2E.toInt()
    val TEXT = 0xFFF2F4F8.toInt()
    val SUB = 0xFF9AA3B2.toInt()
    val ACCENT = 0xFF8AB4F8.toInt()
    val OK = 0xFF3A9D5D.toInt()
    val DARK = 0xFF14161A.toInt()
    val RED = 0xFFE5604F.toInt()

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
