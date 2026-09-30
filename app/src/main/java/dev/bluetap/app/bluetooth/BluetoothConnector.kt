package dev.bluetap.app.bluetooth

/**
 * Connects and disconnects associated Bluetooth devices.
 *
 * Dormant future boundary; no current UI or widget action calls it. Its legacy
 * AssociatedDevice parameter must be revisited for bonded MAC identity when API 37
 * integration resumes (see DEVELOPMENT_STATUS.md). Do not create a CDM association
 * merely to adapt today's bonded-device model to this placeholder interface.
 *
 * No implementation exists yet.
 */
interface BluetoothConnector {
    suspend fun connect(device: AssociatedDevice): Result<Unit>

    suspend fun disconnect(device: AssociatedDevice): Result<Unit>

    suspend fun getConnectionState(device: AssociatedDevice): ConnectionState
}
