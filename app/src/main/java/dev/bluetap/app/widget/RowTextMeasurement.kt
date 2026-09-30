package dev.bluetap.app.widget

import android.graphics.Paint
import android.graphics.Typeface

/** One measurement implementation for both renderers, including Dot Mono and font scaling. */
fun rowTextMeasurer(density: Float, fontScale: Float, preset: WidgetPreset): (String, Int) -> Float {
    val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = if (preset == WidgetPreset.DOT_MONO) Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        else Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.DEFAULT }
    return { text, size ->
        val paint = if (size == 14) namePaint else statusPaint
        paint.textSize = size * density * fontScale
        paint.measureText(text) / density
    }
}
