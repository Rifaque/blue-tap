package dev.bluetap.app.bluetooth

/**
 * A reference to an already-paired Bluetooth device.
 *
 * Used instead of [android.bluetooth.BluetoothDevice] so the UI and widget code
 * (and saved widget configuration) do not depend on Android Bluetooth classes.
 * A [BluetoothConnector] implementation resolves it to a real device.
 *
 * @property address The device's hardware address, e.g. `00:11:22:AA:BB:CC`.
 * @property name A display name for the device.
 */
data class PairedDevice(
    val address: String,
    val name: String,
)
