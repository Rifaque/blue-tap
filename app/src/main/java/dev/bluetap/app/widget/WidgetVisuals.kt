package dev.bluetap.app.widget

import android.content.Context
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import dev.bluetap.app.R
import dev.bluetap.app.bluetooth.DeviceKind

fun deviceIcon(kind: DeviceKind): Int = when (kind) {
    DeviceKind.HEADPHONES -> R.drawable.ic_device_headphones
    DeviceKind.SPEAKER -> R.drawable.ic_device_speaker
    DeviceKind.WATCH -> R.drawable.ic_device_watch
    DeviceKind.CONTROLLER -> R.drawable.ic_device_controller
    DeviceKind.INPUT -> R.drawable.ic_device_input
    DeviceKind.GENERIC -> R.drawable.ic_device_generic
}

data class WidgetPalette(val background: Color, val text: Color, val secondary: Color,
    val accent: Color, val badge: Color, val border: Color, val dark: Boolean)

fun widgetPalette(context: Context, appearance: WidgetAppearance, systemDark: Boolean): WidgetPalette {
    val preset = appearance.preset
    val dark = when {
        presetKeepsBlackSurface(preset) -> true
        appearance.background == WidgetBackground.LIGHT -> false
        appearance.background == WidgetBackground.DARK -> true
        appearance.background == WidgetBackground.CUSTOM -> Color(appearance.customBackground).luminance() < 0.179f
        else -> systemDark
    }
    val system = if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    var bg = when (preset) {
        WidgetPreset.DOT_MONO -> if (dark) Color(0xFF070707) else Color(0xFFFAFAFA)
        WidgetPreset.OLED -> Color.Black
        WidgetPreset.HYPER_GLASS -> if (dark) Color(0xEF253144) else Color(0xF2EDF3FC)
        WidgetPreset.MATERIAL_YOU -> system.secondaryContainer
        WidgetPreset.MINIMAL -> if (dark) Color(0xED111827) else Color(0xF5F8FAFC)
        WidgetPreset.OUTLINE -> if (dark) Color(0xE6111827) else Color(0xEEF8FAFC)
        else -> if (dark) Color(0xFF131E30) else Color(0xFFF8FAFF)
    }
    if (appearance.background == WidgetBackground.CUSTOM && !presetKeepsBlackSurface(preset) && preset != WidgetPreset.OUTLINE)
        bg = Color(appearance.customBackground or 0xFF000000.toInt())
    val text = if (preset == WidgetPreset.DOT_MONO) Color(0xFFF4F4F4) else if (dark) Color(0xFFF5F7FB) else Color(0xFF132036)
    val secondary = if (preset == WidgetPreset.DOT_MONO) Color(0xFFB0B0B0) else if (dark) Color(0xFFB6C2D3) else Color(0xFF526176)
    var accent = when (appearance.accent) {
        WidgetAccent.BLUE -> if (dark) Color(0xFF9BC0FF) else Color(0xFF205BDD)
        WidgetAccent.MINT -> if (dark) Color(0xFF79DDC3) else Color(0xFF087565)
        WidgetAccent.VIOLET -> if (dark) Color(0xFFD1B5FF) else Color(0xFF7440B2)
        WidgetAccent.AMBER -> if (dark) Color(0xFFF9CF87) else Color(0xFF965900)
        WidgetAccent.SYSTEM -> system.primary
    }
    if (preset == WidgetPreset.DOT_MONO) accent = text
    val material = preset == WidgetPreset.MATERIAL_YOU && appearance.background != WidgetBackground.CUSTOM
    return WidgetPalette(bg, if (material) system.onSecondaryContainer else text,
        if (material) system.onSurfaceVariant else secondary, accent,
        if (material) system.primaryContainer else accent.copy(alpha = if (dark) 0.16f else 0.10f),
        if (dark) Color(0xFF7C8DA6) else Color(0xFF94A3B8), dark)
}

fun widgetRadius(preset: WidgetPreset): Int = when (preset) {
    WidgetPreset.HYPER_GLASS -> 32
    WidgetPreset.MATERIAL_YOU -> 28
    WidgetPreset.DOT_MONO, WidgetPreset.MINIMAL, WidgetPreset.COMPACT -> 18
    else -> 24
}

fun deviceArtworkResource(kind: DeviceKind): Int = if (kind == DeviceKind.HEADPHONES) R.drawable.art_earbuds else deviceIcon(kind)
