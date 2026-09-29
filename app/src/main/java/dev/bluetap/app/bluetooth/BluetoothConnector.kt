package dev.bluetap.app.bluetooth

/**
 * Connects and disconnects paired Bluetooth devices.
 *
 * The UI and widget layers depend only on this interface, never on Android
 * Bluetooth APIs directly, so the platform-specific implementation can be
 * swapped (e.g. for [dev.bluetap.app.bluetooth.android17.Android17BluetoothConnector]).
 *
 * No implementation exists yet.
 */
interface BluetoothConnector {
    suspend fun connect(device: PairedDevice): Result<Unit>

    suspend fun disconnect(device: PairedDevice): Result<Unit>

    suspend fun getConnectionState(device: PairedDevice): ConnectionState
}
