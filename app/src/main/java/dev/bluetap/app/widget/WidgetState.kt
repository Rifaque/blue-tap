package dev.bluetap.app.widget

import dev.bluetap.app.bluetooth.BondedDevice

/** What a BlueTap widget should show. */
sealed interface WidgetState {
    /** No device has been assigned to the widget. */
    data object NotConfigured : WidgetState

    /** The assigned address is still in Android's bonded device list. */
    data class Ready(val device: BondedDevice) : WidgetState

    /** The device is no longer bonded, or the bonded list cannot currently be read. */
    data class Unavailable(val device: BondedDevice) : WidgetState
}

/**
 * @param saved The device saved for the widget, if any.
 * @param bondedDevices Current paired devices, or `null` if permission, Bluetooth
 *  state, or service availability prevents reading them. Saved configuration is retained.
 */
fun resolveWidgetState(
    saved: BondedDevice?,
    bondedDevices: List<BondedDevice>?,
): WidgetState {
    if (saved == null) return WidgetState.NotConfigured
    val current = bondedDevices.orEmpty().firstOrNull {
        it.macAddress.equals(saved.macAddress, ignoreCase = true)
    }
    return if (current != null) WidgetState.Ready(current) else WidgetState.Unavailable(saved)
}
