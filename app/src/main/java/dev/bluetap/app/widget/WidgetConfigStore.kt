package dev.bluetap.app.widget

import android.content.Context
import android.content.SharedPreferences
import dev.bluetap.app.bluetooth.BondedDevice
import dev.bluetap.app.bluetooth.mapBondedDevice

/** Stores the bonded device address and display name separately for each widget ID. */
class WidgetConfigStore(private val prefs: SharedPreferences) {

    fun save(appWidgetId: Int, device: BondedDevice) {
        val normalized = mapBondedDevice(device.macAddress, device.name, null)
        prefs.edit()
            .remove(key(appWidgetId, ASSOCIATION_ID))
            .putString(key(appWidgetId, MAC_ADDRESS), normalized.macAddress)
            .putString(key(appWidgetId, NAME), normalized.name)
            .apply()
    }

    /** The device assigned to [appWidgetId], or `null` if the widget is not configured. */
    fun load(appWidgetId: Int): BondedDevice? {
        // Reuse legacy MAC/name fields. Never infer a bonded address from a CDM ID or name.
        val macAddress = prefs.getString(key(appWidgetId, MAC_ADDRESS), null)
            ?.takeIf { it.isNotBlank() } ?: return null
        val name = prefs.getString(key(appWidgetId, NAME), null)
        return mapBondedDevice(macAddress, name, null)
    }

    fun delete(appWidgetId: Int) {
        prefs.edit()
            .remove(key(appWidgetId, ASSOCIATION_ID))
            .remove(key(appWidgetId, MAC_ADDRESS))
            .remove(key(appWidgetId, NAME))
            .apply()
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
