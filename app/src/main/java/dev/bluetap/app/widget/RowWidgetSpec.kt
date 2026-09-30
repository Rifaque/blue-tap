package dev.bluetap.app.widget

import kotlin.math.ceil

enum class RowHeightClass { SLIM, SPACIOUS }

/** Shared by RemoteViews and the preview. Heights reserve font padding and host rounding. */
data class RowWidgetSpec(
    val heightClass: RowHeightClass,
    val artworkSize: Int,
    val horizontalPadding: Int,
    val verticalPadding: Int,
    val artworkTextGap: Int,
    val name: String,
    val nameMaxLines: Int,
    val nameTextSize: Int = 14,
    val statusTextSize: Int = 11,
    val nameHeight: Int,
    val statusHeight: Int,
    val textGap: Int,
    val secondary: String?,
    val availableHeight: Int,
) {
    val showName get() = nameMaxLines > 0
    val showStatus get() = secondary != null
    val textHeight get() = nameHeight + if (showStatus) textGap + statusHeight else 0
}

fun isOneRowWidget(width: Float, height: Float) = width >= 110 && height < 125

/** measure returns dp at the supplied sp size. Runtime and preview use the same Android Paint. */
fun rowWidgetSpec(width: Float, height: Float, name: String, a: WidgetAppearance,
    state: WidgetDeviceStatus = WidgetDeviceStatus(), configured: Boolean = true, fontScale: Float = 1f,
    measure: (String, Int) -> Float = { text, sp -> text.length * sp * .6f * fontScale }): RowWidgetSpec {
    val scale = fontScale.coerceAtLeast(.5f)
    val slim = height < 72
    val verticalPadding = if (height < 64) 2 else 4
    val available = (height - verticalPadding * 2 - 4).toInt().coerceAtLeast(0) // 4dp rounding/headroom.
    var padding = if (width < 320) 8 else 12
    val gap = if (!a.showArtwork) 0 else if (width < 320) 8 else 12
    var art = if (!a.showArtwork) 0 else minOf(available, when {
        width < 220 -> 36
        width < 320 -> 44
        else -> 52
    })
    fun textWidth() = (width - padding * 2 - art - gap - 4).coerceAtLeast(0f) // glyph/weight margin
    val fullName = name.trim().replace(Regex("\\s+"), " ")
    // Give back padding, then a little artwork width, before forcing a wrap.
    if (measure(fullName, 14) > textWidth()) padding = 6
    if (width >= 220 && measure(fullName, 14) > textWidth() && art > 28)
        art = maxOf(28, minOf(art, (width - padding * 2 - gap - 4 - measure(fullName, 14)).toInt()))
    val nameLineHeight = ceil(18 * scale).toInt()
    val fontPadding = ceil(4 * scale).toInt()
    fun nameHeight(lines: Int) = if (lines == 0) 0 else lines * nameLineHeight + fontPadding
    var lines = if (a.showDeviceName || !configured) {
        if (measure(fullName, 14) <= textWidth()) 1 else 2
    } else 0
    while (lines > 0 && nameHeight(lines) > available) lines--
    // Explicit balanced word wrapping avoids "OnePlus Buds / 4". Long indivisible words
    // retain normal ellipsis; no speculative shortening of the saved device name.
    val words = fullName.split(' ')
    val balanced = if (lines == 2 && words.size > 1) (1 until words.size).map { split ->
        words.take(split).joinToString(" ") to words.drop(split).joinToString(" ")
    }.filter { (first, second) -> measure(first, 14) <= textWidth() && measure(second, 14) <= textWidth() }
        .minByOrNull { (first, second) -> kotlin.math.abs(measure(first, 14) - measure(second, 14)) }
        ?.let { (first, second) -> "$first\n$second" } ?: fullName else fullName
    val realStatus = state.connection != WidgetConnectionState.UNKNOWN
    val battery = if (a.showBattery && state.connection != WidgetConnectionState.UNAVAILABLE) state.battery?.label() else null
    val status = when {
        !configured -> "Tap to set up"
        state.connection == WidgetConnectionState.UNAVAILABLE -> "Unavailable"
        realStatus -> state.connection.label
        width >= 220 && !slim && a.showStatus -> state.connection.label
        else -> null // Narrow rows never spend name space on a placeholder.
    }
    val secondaryHeight = ceil(18 * scale).toInt()
    val textGap = if (lines == 0) 0 else 2
    val fitsHeight = nameHeight(lines) + textGap + secondaryHeight <= available
    val candidates = when {
        realStatus && status != null -> listOfNotNull(battery?.let { "$status · $it" }, status)
        battery != null -> listOf(battery) // A known battery beats placeholder copy.
        else -> listOfNotNull(status)
    }
    val secondary = if (fitsHeight) candidates.firstOrNull { measure(it, 11) <= textWidth() } else null
    return RowWidgetSpec(if (slim) RowHeightClass.SLIM else RowHeightClass.SPACIOUS,
        art, padding, verticalPadding, gap, balanced, lines,
        nameHeight = nameHeight(lines), statusHeight = secondaryHeight, textGap = textGap,
        secondary = secondary, availableHeight = available)
}
