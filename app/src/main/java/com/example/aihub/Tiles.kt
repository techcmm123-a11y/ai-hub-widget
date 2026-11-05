package com.example.aihub

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable

/** Рисует значок ИИ в выбранной форме: круглой или закруглённой. */
object Tiles {

    fun render(
        context: Context,
        svc: ServiceCfg,
        cfg: Config,
        size: Int,
        badge: String = "",
        badgeColor: Int = 0,
        iconPkg: String? = null
    ): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val pkg = iconPkg ?: Apps.packageFor(context, svc)
        val icon: Drawable? = if (cfg.realIcons) Apps.icon(context, pkg) else null
        if (icon != null) drawIcon(canvas, icon, size) else drawMono(context, canvas, svc, size)

        // Маска нужной формы со сглаженными краями.
        val maskBmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val maskCanvas = Canvas(maskBmp)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        fill.color = Color.WHITE
        val rect = RectF(0f, 0f, size.toFloat(), size.toFloat())
        if (cfg.shape == "round") {
            maskCanvas.drawOval(rect, fill)
        } else {
            maskCanvas.drawRoundRect(rect, size * 0.3f, size * 0.3f, fill)
        }
        val cut = Paint(Paint.ANTI_ALIAS_FLAG)
        cut.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        canvas.drawBitmap(maskBmp, 0f, 0f, cut)
        maskBmp.recycle()

        if (badge.isNotEmpty()) drawBadge(context, canvas, size, badge, badgeColor)
        return bmp
    }

    private fun drawIcon(canvas: Canvas, d: Drawable, size: Int) {
        if (d is AdaptiveIconDrawable) {
            // Слои адаптивного значка в 1,5 раза больше видимой области.
            val pad = (size * 0.25f).toInt()
            val bg = d.background
            val fg = d.foreground
            if (bg != null) {
                bg.setBounds(-pad, -pad, size + pad, size + pad)
                bg.draw(canvas)
            }
            if (fg != null) {
                fg.setBounds(-pad, -pad, size + pad, size + pad)
                fg.draw(canvas)
            }
        } else {
            d.setBounds(0, 0, size, size)
            d.draw(canvas)
        }
    }

    private fun blend(a: Int, b: Int, t: Float): Int {
        val r = (Color.red(a) * (1f - t) + Color.red(b) * t).toInt()
        val g = (Color.green(a) * (1f - t) + Color.green(b) * t).toInt()
        val bl = (Color.blue(a) * (1f - t) + Color.blue(b) * t).toInt()
        return Color.rgb(r, g, bl)
    }

    private fun drawMono(context: Context, canvas: Canvas, svc: ServiceCfg, size: Int) {
        val dark = Skin.isDark(context)
        val bg = Paint(Paint.ANTI_ALIAS_FLAG)
        bg.color = if (dark) svc.bg else blend(svc.fg, Color.WHITE, 0.80f)
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bg)
        val t = Paint(Paint.ANTI_ALIAS_FLAG)
        t.color = if (dark) svc.fg else blend(svc.fg, Color.BLACK, 0.50f)
        t.typeface = Typeface.DEFAULT_BOLD
        t.textAlign = Paint.Align.CENTER
        t.textSize = if (svc.mono.length >= 3) size * 0.26f else size * 0.34f
        canvas.drawText(svc.mono, size / 2f, size / 2f - (t.descent() + t.ascent()) / 2f, t)
    }

    private fun drawBadge(context: Context, canvas: Canvas, size: Int, letter: String, color: Int) {
        val r = size * 0.21f
        val cx = size - r
        val cy = size - r
        val ring = Paint(Paint.ANTI_ALIAS_FLAG)
        ring.color = if (Skin.isDark(context)) 0xFF15171B.toInt() else 0xFFFBFAF7.toInt()
        canvas.drawCircle(cx, cy, r + size * 0.03f, ring)
        val dot = Paint(Paint.ANTI_ALIAS_FLAG)
        dot.color = color
        canvas.drawCircle(cx, cy, r, dot)
        val t = Paint(Paint.ANTI_ALIAS_FLAG)
        t.color = 0xFF14161A.toInt()
        t.typeface = Typeface.DEFAULT_BOLD
        t.textAlign = Paint.Align.CENTER
        t.textSize = r * 1.15f
        canvas.drawText(letter, cx, cy - (t.descent() + t.ascent()) / 2f, t)
    }
}
