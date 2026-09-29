package dev.bluetap.app.bluetooth

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager

/** An empty list means no paired devices; inability to read the list is a separate state. */
sealed interface BondedDeviceResult {
    data class Available(val devices: List<BondedDevice>) : BondedDeviceResult
    data object PermissionRequired : BondedDeviceResult
    data object BluetoothOff : BondedDeviceResult
    data object Unsupported : BondedDeviceResult
    data object Unavailable : BondedDeviceResult
}

/** Reads paired devices only. Does not scan, pair, associate, or connect to devices. */
class BondedDeviceProvider(private val context: Context) {
    fun currentDevices(): BondedDeviceResult {
        if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) !=
            PackageManager.PERMISSION_GRANTED
        ) return BondedDeviceResult.PermissionRequired

        return try {
            val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
                ?: return BondedDeviceResult.Unsupported
            if (!adapter.isEnabled) return BondedDeviceResult.BluetoothOff
            val bonded = adapter.bondedDevices ?: return BondedDeviceResult.Unavailable
            val devices = bonded.map { device ->
                val name = device.name
                val alias = if (name.isNullOrBlank()) device.alias else null
                mapBondedDevice(device.address, name, alias)
            }
            // Bluetooth can turn off during the read, making an empty list misleading.
            if (!adapter.isEnabled) return BondedDeviceResult.BluetoothOff
            BondedDeviceResult.Available(prepareBondedDevices(devices))
        } catch (e: SecurityException) {
            // Permission may have been revoked after the initial check.
            BondedDeviceResult.PermissionRequired
        } catch (e: RuntimeException) {
            BondedDeviceResult.Unavailable
        }
    }
}
