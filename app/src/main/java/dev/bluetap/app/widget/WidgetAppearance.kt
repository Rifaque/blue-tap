package dev.bluetap.app.widget

enum class WidgetPreset(val title: String, val description: String) {
    BLUE_TAP("Blue Tap", "The original. Clear and balanced."),
    DOT_MONO("Dot Mono", "Graphic dots. Monochrome contrast."),
    HYPER_GLASS("Hyper Glass", "Soft surfaces. Space to breathe."),
    MATERIAL_YOU("Material You", "Your wallpaper, your palette."),
    OLED("OLED", "Pure black. Just the essentials."),
    MINIMAL("Minimal", "A quiet device companion."),
    COMPACT("Compact", "Small footprint. Clear identity."),
    OUTLINE("Outline", "A fine frame. A lighter presence.");
}

enum class WidgetAccent(val title: String) {
    BLUE("Blue"), MINT("Mint"), VIOLET("Violet"), AMBER("Amber"), SYSTEM("System")
}
enum class WidgetBackground(val title: String) {
    AUTO("Automatic"), LIGHT("Light"), DARK("Dark"), CUSTOM("Custom")
}
enum class WidgetAlignment(val title: String) { START("Start"), CENTER("Center"), END("End") }
enum class ArtworkSource { DEVICE, GENERIC }

data class WidgetAppearance(
    val preset: WidgetPreset = WidgetPreset.BLUE_TAP,
    val accent: WidgetAccent = WidgetAccent.BLUE,
    val background: WidgetBackground = WidgetBackground.AUTO,
    val customBackground: Int = 0xFF172554.toInt(),
    val showLabel: Boolean = true,
    val showDeviceName: Boolean = true,
    val showStatus: Boolean = true,
    val alignment: WidgetAlignment = WidgetAlignment.START,
    val compact: Boolean = false,
    val showBattery: Boolean = true,
    val showArtwork: Boolean = true,
    val artworkSource: ArtworkSource = ArtworkSource.DEVICE,
) : java.io.Serializable

fun appearanceForPreset(preset: WidgetPreset) = WidgetAppearance(
    preset = preset,
    background = if (preset == WidgetPreset.DOT_MONO) WidgetBackground.DARK else WidgetBackground.AUTO,
    accent = if (preset == WidgetPreset.MATERIAL_YOU) WidgetAccent.SYSTEM else WidgetAccent.BLUE,
    showLabel = preset !in setOf(WidgetPreset.MINIMAL, WidgetPreset.COMPACT, WidgetPreset.OLED, WidgetPreset.DOT_MONO),
    alignment = if (preset in setOf(WidgetPreset.HYPER_GLASS, WidgetPreset.MATERIAL_YOU, WidgetPreset.DOT_MONO, WidgetPreset.OLED))
        WidgetAlignment.CENTER else WidgetAlignment.START,
    compact = preset == WidgetPreset.COMPACT,
)

internal inline fun <reified T : Enum<T>> storedEnum(value: String?, fallback: T): T =
    enumValues<T>().firstOrNull { it.name == value } ?: fallback

