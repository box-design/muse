package com.muse.app.data

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.palette.graphics.Palette
import com.muse.app.data.model.ColorSwatch
import com.muse.app.util.Colors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PaletteExtractor {

    suspend fun extract(resolver: ContentResolver, uri: Uri, count: Int = 6): List<ColorSwatch> =
        withContext(Dispatchers.IO) {
            runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                val rawW = if (bounds.outWidth > 0) bounds.outWidth else 1000
                val rawH = if (bounds.outHeight > 0) bounds.outHeight else 1000
                var sample = 1
                var dim = maxOf(rawW, rawH)
                while (dim / 2 >= 220) {
                    sample *= 2
                    dim /= 2
                }
                val opts = BitmapFactory.Options().apply { inSampleSize = sample }
                val bmp: Bitmap? = resolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, opts)
                }
                if (bmp == null) {
                    emptyList()
                } else {
                    val palette = Palette.Builder(bmp).maximumColorCount(24).generate()
                    val merged = mergeNear(palette.swatches.map { ColorSwatch(it.rgb, it.population) })
                    merged.sortedByDescending { Colors.luminance(it.argb) }.take(count)
                }
            }.getOrDefault(emptyList())
        }

    private fun mergeNear(input: List<ColorSwatch>): List<ColorSwatch> {
        val sorted = input.sortedByDescending { it.population }
        val out = mutableListOf<ColorSwatch>()
        for (s in sorted) {
            val near = out.find { Colors.distance(it.argb, s.argb) < 52f }
            if (near == null) {
                out += s
            } else {
                val total = (near.population + s.population).coerceAtLeast(1)
                val wB = s.population.toFloat() / total
                out[out.indexOf(near)] = near.copy(argb = Colors.blend(near.argb, s.argb, wB))
            }
        }
        return out
    }
}
