package dev.bluetap.app.widget

import android.content.Context
import android.content.SharedPreferences
import dev.bluetap.app.bluetooth.DeviceKind
import dev.bluetap.app.bluetooth.BondedDevice
import dev.bluetap.app.bluetooth.mapBondedDevice

/** Stores the bonded device address and display name separately for each widget ID. */
class WidgetConfigStore(private val prefs: SharedPreferences) {

    fun save(appWidgetId: Int, device: BondedDevice) {
        prefs.edit().writeDevice(appWidgetId, device).apply()
    }

    private fun SharedPreferences.Editor.writeDevice(appWidgetId: Int, device: BondedDevice): SharedPreferences.Editor {
        val normalized = mapBondedDevice(device.macAddress, device.name, null)
        val editor = this
        if (device.kind == DeviceKind.GENERIC) editor.remove(key(appWidgetId, "kind"))
        else editor.putString(key(appWidgetId, "kind"), device.kind.name)
        return editor
            .remove(key(appWidgetId, ASSOCIATION_ID))
            .putString(key(appWidgetId, MAC_ADDRESS), normalized.macAddress)
            .putString(key(appWidgetId, NAME), normalized.name)
    }

    /** The device assigned to [appWidgetId], or `null` if the widget is not configured. */
    fun load(appWidgetId: Int): BondedDevice? {
        // Reuse legacy MAC/name fields. Never infer a bonded address from a CDM ID or name.
        val macAddress = prefs.safeString(key(appWidgetId, MAC_ADDRESS), null)
            ?.takeIf { it.isNotBlank() } ?: return null
        val name = prefs.safeString(key(appWidgetId, NAME), null)
        return mapBondedDevice(macAddress, name, null).copy(
            kind = storedEnum(prefs.safeString(key(appWidgetId, "kind"), null), DeviceKind.GENERIC),
        )
    }

    fun loadAppearance(appWidgetId: Int): WidgetAppearance = WidgetAppearance(
        preset = storedEnum(prefs.safeString(key(appWidgetId, "preset"), null), WidgetPreset.BLUE_TAP),
        accent = storedEnum(prefs.safeString(key(appWidgetId, "accent"), null), WidgetAccent.BLUE),
        background = storedEnum(prefs.safeString(key(appWidgetId, "background"), null), WidgetBackground.AUTO),
        customBackground = prefs.safeInt(key(appWidgetId, "custom_background"), 0xFF172554.toInt()) or 0xFF000000.toInt(),
        showLabel = prefs.safeBoolean(key(appWidgetId, "show_label"), true),
        showDeviceName = prefs.safeBoolean(key(appWidgetId, "show_name"), true),
        showStatus = prefs.safeBoolean(key(appWidgetId, "show_status"), true),
        alignment = storedEnum(prefs.safeString(key(appWidgetId, "alignment"), null), WidgetAlignment.START),
        compact = prefs.safeBoolean(key(appWidgetId, "compact"), false),
        showBattery = prefs.safeBoolean(key(appWidgetId, "show_battery"), true),
        showArtwork = prefs.safeBoolean(key(appWidgetId, "show_artwork"), true),
        artworkSource = storedEnum(prefs.safeString(key(appWidgetId, "artwork_source"), null), ArtworkSource.DEVICE),
    )

    /** Appearance writes never alter the assigned Bluetooth device. */
    fun saveAppearance(appWidgetId: Int, appearance: WidgetAppearance) {
        prefs.edit().writeAppearance(appWidgetId, appearance).apply()
    }

    private fun SharedPreferences.Editor.writeAppearance(appWidgetId: Int, appearance: WidgetAppearance): SharedPreferences.Editor =
        this
            .putString(key(appWidgetId, "preset"), appearance.preset.name)
            .putString(key(appWidgetId, "accent"), appearance.accent.name)
            .putString(key(appWidgetId, "background"), appearance.background.name)
            .putInt(key(appWidgetId, "custom_background"), appearance.customBackground)
            .putBoolean(key(appWidgetId, "show_label"), appearance.showLabel)
            .putBoolean(key(appWidgetId, "show_name"), appearance.showDeviceName)
            .putBoolean(key(appWidgetId, "show_status"), appearance.showStatus)
            .putString(key(appWidgetId, "alignment"), appearance.alignment.name)
            .putBoolean(key(appWidgetId, "compact"), appearance.compact)
            .putBoolean(key(appWidgetId, "show_battery"), appearance.showBattery)
            .putBoolean(key(appWidgetId, "show_artwork"), appearance.showArtwork)
            .putString(key(appWidgetId, "artwork_source"), appearance.artworkSource.name)

    /** One disk transaction for a completed editor save. Call off the main thread. */
    fun saveConfiguration(appWidgetId: Int, device: BondedDevice, appearance: WidgetAppearance) {
        check(prefs.edit().writeDevice(appWidgetId, device).writeAppearance(appWidgetId, appearance).commit()) {
            "Could not persist widget configuration"
        }
    }

    /** Launcher restore can renumber IDs. Snapshot first so overlapping ID pairs are safe. */
    fun remapWidgets(oldIds: IntArray, newIds: IntArray) {
        if (oldIds.size != newIds.size) return
        val snapshot = prefs.all
        val editor = prefs.edit()
        val pairs = oldIds.zip(newIds).filter { (old, new) -> old > 0 && new > 0 }
        pairs.flatMap { (old, new) -> listOf(old, new) }.distinct().forEach { id ->
            snapshot.keys.filter { it.startsWith("widget_${id}_") }.forEach(editor::remove)
        }
        pairs.forEach { (old, new) ->
            val prefix = "widget_${old}_"
            snapshot.filterKeys { it.startsWith(prefix) }.forEach { (key, value) ->
                val target = "widget_${new}_${key.removePrefix(prefix)}"
                when (value) {
                    is String -> editor.putString(target, value)
                    is Int -> editor.putInt(target, value)
                    is Boolean -> editor.putBoolean(target, value)
                    is Long -> editor.putLong(target, value)
                    is Float -> editor.putFloat(target, value)
                }
            }
        }
        editor.apply()
    }

    fun delete(appWidgetId: Int) {
        val editor = prefs.edit()
        val prefix = "widget_${appWidgetId}_"
        prefs.all.keys.filter { it.startsWith(prefix) }.forEach(editor::remove)
        editor.apply()
    }

    companion object {
        private const val PREFS_NAME = "widget_config"
        private const val ASSOCIATION_ID = "association_id"
        private const val MAC_ADDRESS = "mac_address"
        private const val NAME = "name"

        private fun key(appWidgetId: Int, field: String) = "widget_${appWidgetId}_$field"

        fun from(context: Context) =
            WidgetConfigStore(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE))
    }
}
