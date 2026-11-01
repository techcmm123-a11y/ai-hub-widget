package com.example.aihub

import android.content.Context
import android.content.res.Configuration
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
        badgeColor: Int = 0
    ): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val icon: Drawable? = if (cfg.realIcons) Apps.icon(context, Apps.packageFor(context, svc)) else null
        if (icon != null) drawIcon(canvas, icon, size) else drawMono(canvas, svc, size)

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

    private fun drawMono(canvas: Canvas, svc: ServiceCfg, size: Int) {
        val bg = Paint(Paint.ANTI_ALIAS_FLAG)
        bg.color = svc.bg
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bg)
        val t = Paint(Paint.ANTI_ALIAS_FLAG)
        t.color = svc.fg
        t.typeface = Typeface.DEFAULT_BOLD
        t.textAlign = Paint.Align.CENTER
        t.textSize = if (svc.mono.length >= 3) size * 0.26f else size * 0.34f
        canvas.drawText(svc.mono, size / 2f, size / 2f - (t.descent() + t.ascent()) / 2f, t)
    }

    private fun drawBadge(context: Context, canvas: Canvas, size: Int, letter: String, color: Int) {
        val r = size * 0.21f
        val cx = size - r
        val cy = size - r
        val night = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        val ring = Paint(Paint.ANTI_ALIAS_FLAG)
        ring.color = if (night) 0xFF15171B.toInt() else 0xFFFBFAF7.toInt()
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
