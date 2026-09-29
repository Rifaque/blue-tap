package dev.bluetap.app.bluetooth.android17

import androidx.annotation.RequiresApi
import dev.bluetap.app.bluetooth.BluetoothConnector
import dev.bluetap.app.bluetooth.ConnectionState
import dev.bluetap.app.bluetooth.PairedDevice

/**
 * Placeholder for the Android 17 (API 37) implementation of [BluetoothConnector].
 *
 * NOT IMPLEMENTED. It is not used anywhere yet and performs no Bluetooth operations.
 *
 * TODO(api37): Implement with the public Android 17 connect/disconnect APIs once
 *  the project compiles against SDK 37 and it can be tested on a real Android 17
 *  device. Do not use hidden APIs, reflection or OEM-specific protocols.
 */
@RequiresApi(37)
class Android17BluetoothConnector : BluetoothConnector {

    override suspend fun connect(device: PairedDevice): Result<Unit> =
        Result.failure(NotImplementedError(NOT_IMPLEMENTED))

    override suspend fun disconnect(device: PairedDevice): Result<Unit> =
        Result.failure(NotImplementedError(NOT_IMPLEMENTED))

    override suspend fun getConnectionState(device: PairedDevice): ConnectionState =
        ConnectionState.Unknown

    private companion object {
        const val NOT_IMPLEMENTED = "Android 17 Bluetooth support is not implemented yet"
    }
}
