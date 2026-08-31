package com.muse.app.util

import android.graphics.Color
import java.util.Locale

object Colors {

    fun hex(argb: Int): String = String.format(
        Locale.US, "#%02X%02X%02X",
        (argb shr 16) and 0xFF, (argb shr 8) and 0xFF, argb and 0xFF
    )

    fun luminance(argb: Int): Float {
        val r = Color.red(argb) / 255f
        val g = Color.green(argb) / 255f
        val b = Color.blue(argb) / 255f
        return 0.2126f * r + 0.7152f * g + 0.0722f * b
    }

    fun readableInk(backgroundArgb: Int): Int =
        if (luminance(backgroundArgb) > 0.42f) 0xFF161512.toInt() else 0xFFF6F3EC.toInt()

    fun distance(a: Int, b: Int): Float {
        val dr = Color.red(a) - Color.red(b)
        val dg = Color.green(a) - Color.green(b)
        val db = Color.blue(a) - Color.blue(b)
        return kotlin.math.sqrt((dr * dr + dg * dg + db * db).toFloat())
    }

    fun blend(a: Int, b: Int, weightB: Float): Int {
        val w = weightB.coerceIn(0f, 1f)
        fun mix(ca: Int, cb: Int): Int = (ca + ((cb - ca) * w)).toInt()
        return Color.rgb(
            mix(Color.red(a), Color.red(b)),
            mix(Color.green(a), Color.green(b)),
            mix(Color.blue(a), Color.blue(b))
        )
    }

    fun hsvOf(argb: Int): FloatArray {
        val hsv = FloatArray(3)
        Color.colorToHSV(argb, hsv)
        return hsv
    }

    fun fromHsv(h: Float, s: Float, v: Float): Int =
        Color.HSVToColor(floatArrayOf(h.coerceIn(0f, 360f), s.coerceIn(0f, 1f), v.coerceIn(0f, 1f)))
}
