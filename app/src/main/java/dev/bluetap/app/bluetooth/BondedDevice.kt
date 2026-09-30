package dev.bluetap.app.bluetooth

import java.util.Locale

/** A paired Bluetooth device, identified by its Bluetooth address. */
data class BondedDevice(val macAddress: String, val name: String, val kind: DeviceKind = DeviceKind.GENERIC) : java.io.Serializable {
    init {
        require(macAddress.isNotBlank()) { "A bonded device needs a Bluetooth address" }
        require(name.isNotBlank()) { "A bonded device needs a display name" }
    }
}

/** Maps public BluetoothDevice values without retaining Android objects. */
internal fun mapBondedDevice(address: String, name: String?, alias: String?): BondedDevice {
    val normalizedAddress = address.trim().uppercase(Locale.ROOT)
    val displayName = name?.trim()?.takeIf { it.isNotEmpty() }
        ?: alias?.trim()?.takeIf { it.isNotEmpty() }
        ?: normalizedAddress
    return BondedDevice(normalizedAddress, displayName)
}

/** Prefer named entries when duplicates exist, then display devices alphabetically. */
internal fun prepareBondedDevices(devices: List<BondedDevice>): List<BondedDevice> = devices
    .map { it.copy(macAddress = it.macAddress.trim().uppercase(Locale.ROOT)) }
    .sortedWith(compareBy<BondedDevice> { it.name.equals(it.macAddress, ignoreCase = true) }
        .thenBy { it.name.lowercase(Locale.ROOT) })
    .distinctBy { it.macAddress }
    .sortedWith(compareBy<BondedDevice> { it.name.lowercase(Locale.ROOT) }.thenBy { it.macAddress })
