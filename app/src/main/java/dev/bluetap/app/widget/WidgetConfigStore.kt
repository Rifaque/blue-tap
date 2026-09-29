package dev.bluetap.app.widget

import android.content.Context
import android.content.SharedPreferences
import dev.bluetap.app.bluetooth.AssociatedDevice

/** Stores which [AssociatedDevice] each BlueTap widget (by `appWidgetId`) is assigned to. */
class WidgetConfigStore(private val prefs: SharedPreferences) {

    fun save(appWidgetId: Int, device: AssociatedDevice) {
        val editor = prefs.edit()
        val idKey = key(appWidgetId, ASSOCIATION_ID)
        if (device.associationId != null) {
            editor.putInt(idKey, device.associationId)
        } else {
            editor.remove(idKey)
        }
        editor
            .putString(key(appWidgetId, MAC_ADDRESS), device.macAddress)
            .putString(key(appWidgetId, NAME), device.name)
            .apply()
    }

    /** The device assigned to [appWidgetId], or `null` if the widget is not configured. */
    fun load(appWidgetId: Int): AssociatedDevice? {
        val name = prefs.getString(key(appWidgetId, NAME), null) ?: return null
        val idKey = key(appWidgetId, ASSOCIATION_ID)
        val associationId = if (prefs.contains(idKey)) prefs.getInt(idKey, 0) else null
        val macAddress = prefs.getString(key(appWidgetId, MAC_ADDRESS), null)
        if (associationId == null && macAddress == null) return null
        return AssociatedDevice(associationId, macAddress, name)
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
