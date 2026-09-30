package dev.bluetap.app.widget

/** Dimensions are the host's usable dp, never an assumed launcher cell count. */
enum class WidgetLayoutClass { TINY, SLEEK, SLEEK_WIDE, CARD, WIDE, SHOWCASE }
fun widgetLayoutClass(width: Float, height: Float): WidgetLayoutClass = when {
    width < 130 || height < 58 -> WidgetLayoutClass.TINY
    height < 125 -> if (width >= 260) WidgetLayoutClass.SLEEK_WIDE else WidgetLayoutClass.SLEEK
    width >= 240 && height >= 230 -> WidgetLayoutClass.SHOWCASE
    width >= 260 && width / height >= 1.4f -> WidgetLayoutClass.WIDE
    else -> WidgetLayoutClass.CARD
}

enum class WidgetConnectionState(val label: String) {
    UNAVAILABLE("Device unavailable"), DISCONNECTED("Disconnected"), CONNECTING("Connecting"),
    CONNECTED("Connected"), DISCONNECTING("Disconnecting"), UNKNOWN("Android 17 required")
}

/** Only populate with explicitly known readings. Null means unknown, never zero/full. */
data class DeviceBattery(val percent: Int? = null, val left: Int? = null, val right: Int? = null) {
    init { require(listOfNotNull(percent, left, right).all { it in 0..100 }) }
    fun label(): String? = if (left != null || right != null)
        listOfNotNull(left?.let { "L $it%" }, right?.let { "R $it%" }).joinToString("  ")
    else percent?.let { "$it%" }
}

/** Reserved for a future reliable public source. Current callers always use FULL.
 * Never infer single-earbud state from audio channel configuration.
 */
enum class EarbudArtworkState { FULL, LEFT_ONLY, RIGHT_ONLY }
data class WidgetDeviceStatus(
    val connection: WidgetConnectionState = WidgetConnectionState.UNKNOWN,
    val battery: DeviceBattery? = null,
    val earbudArtwork: EarbudArtworkState = EarbudArtworkState.FULL,
)
fun WidgetDeviceStatus.artworkAlpha(): Float = when (connection) {
    WidgetConnectionState.UNAVAILABLE, WidgetConnectionState.DISCONNECTED,
    WidgetConnectionState.CONNECTING, WidgetConnectionState.DISCONNECTING -> .55f
    else -> 1f
}

data class WidgetLayoutRules(
    val layout: WidgetLayoutClass, val horizontal: Boolean, val padding: Int,
    val artworkSize: Int, val nameSize: Int, val nameLines: Int,
    val showLabel: Boolean, val showName: Boolean, val status: String?, val battery: String?,
    val gap: Int, val row: RowWidgetSpec? = null,
)

fun widgetLayoutRules(width: Float, height: Float, a: WidgetAppearance,
    status: WidgetDeviceStatus = WidgetDeviceStatus(), configured: Boolean = true, fontScale: Float = 1f,
    name: String = "", measure: (String, Int) -> Float = { text, sp -> text.length * sp * .6f * fontScale }): WidgetLayoutRules {
    val layout = widgetLayoutClass(width, height)
    if (isOneRowWidget(width, height)) {
        val row = rowWidgetSpec(width, height, name, a, status, configured, fontScale, measure)
        return WidgetLayoutRules(layout, true, row.horizontalPadding, row.artworkSize, row.nameTextSize,
            row.nameMaxLines, false, row.showName, row.secondary, null, row.artworkTextGap, row)
    }
    val tiny = layout == WidgetLayoutClass.TINY
    val showcase = layout == WidgetLayoutClass.SHOWCASE
    val horizontal = (tiny && width >= 110 && height < 100) || layout in setOf(WidgetLayoutClass.SLEEK, WidgetLayoutClass.SLEEK_WIDE, WidgetLayoutClass.WIDE) ||
        (layout == WidgetLayoutClass.CARD && a.preset == WidgetPreset.COMPACT && width >= 160) ||
        (showcase && (a.compact || a.preset in setOf(WidgetPreset.COMPACT, WidgetPreset.MINIMAL, WidgetPreset.OUTLINE)))
    val pad = if (tiny) 6 else if (height < 90 || a.compact) 8 else if (showcase) 16 else 10
    var battery = if (a.showBattery && status.connection != WidgetConnectionState.UNAVAILABLE) status.battery?.label() else null
    var statusText = when {
        !configured -> "Tap to set up"
        status.connection == WidgetConnectionState.UNAVAILABLE -> if (width < 180) "Unavailable" else status.connection.label
        status.connection != WidgetConnectionState.UNKNOWN -> status.connection.label // Real state beats decorative preferences.
        a.showStatus && !tiny -> status.connection.label
        else -> null
    }
    if (tiny && statusText != null && battery != null) {
        statusText = "$statusText · $battery"
        battery = null // One compact information block rather than three stacked text rows.
    }
    val label = a.showLabel && showcase && height >= 230 * fontScale && !a.compact &&
        a.preset !in setOf(WidgetPreset.DOT_MONO, WidgetPreset.OLED)
    val moving = status.connection in setOf(WidgetConnectionState.CONNECTING, WidgetConnectionState.DISCONNECTING)
    val nameLines = if (tiny && height < 90 && (battery != null || statusText != null)) 1 else 2
    val statusLines = if ((statusText?.length ?: 0) * 6 * fontScale > width - pad * 2) 2 else 1
    val textHeight = ((if (a.showDeviceName || !configured) nameLines * (if (tiny) 14 else if (showcase) 20 else 16) + 3 else 0) +
        (if (statusText != null) statusLines * 15 else 0) + (if (moving) 8 else 0) +
        (if (battery != null) 17 else 0) + (if (label) 20 else 0)) * fontScale
    val preferredArt = when {
        tiny -> 26
        a.preset == WidgetPreset.MINIMAL -> if (showcase) 64 else 40
        a.preset == WidgetPreset.COMPACT -> if (showcase) 64 else 36
        horizontal -> if (layout == WidgetLayoutClass.WIDE || showcase) 86 else if (layout == WidgetLayoutClass.SLEEK_WIDE) 54 else 40
        showcase -> if (a.preset == WidgetPreset.HYPER_GLASS) 136 else 120
        a.preset in setOf(WidgetPreset.DOT_MONO, WidgetPreset.OLED, WidgetPreset.HYPER_GLASS) -> 98
        else -> 84
    }
    val maxArt = if (horizontal) height - pad * 2 else height - pad * 2 - textHeight - 8
    val artSize = if (!horizontal && maxArt < 16) 0 else minOf(preferredArt, maxArt.toInt().coerceAtLeast(16))
    return WidgetLayoutRules(layout, horizontal, pad, artSize,
        if (showcase) 18 else if (tiny) 12 else 14, nameLines, label, a.showDeviceName || !configured,
        statusText, battery, if (a.compact || tiny) 4 else 8)
}

fun presetKeepsBlackSurface(preset: WidgetPreset) = preset in setOf(WidgetPreset.DOT_MONO, WidgetPreset.OLED)
