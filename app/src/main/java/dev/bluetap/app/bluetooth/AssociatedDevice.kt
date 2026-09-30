package dev.bluetap.app.bluetooth

import java.util.Locale

/**
 * A Bluetooth device the user has associated with BlueTap through Android's
 * Companion Device Manager.
 *
 * Retained only for dormant CDM experiments and the future connector placeholder.
 * Current UI and widget persistence use BondedDevice, not this association model.
 *
 * @property associationId The Companion Device Manager association ID. Android 13+
 *  always provides one; on Android 12/12L the platform exposes no IDs, so it is `null`.
 * @property macAddress The device's Bluetooth address, if the association exposes it.
 * @property name Display name for the device.
 */
data class AssociatedDevice(
    val associationId: Int?,
    val macAddress: String?,
    val name: String,
) {
    init {
        require(associationId != null || macAddress != null) {
            "An associated device needs an association ID or a MAC address"
        }
    }

    /**
     * Whether two records expose the same address or ID. This cannot identify
     * separate classic/BLE endpoints or resolve randomized addresses to a product.
     */
    fun isSameAssociationAs(other: AssociatedDevice): Boolean =
        if (macAddress != null && other.macAddress != null) {
            macAddress.equals(other.macAddress, ignoreCase = true)
        } else {
            associationId != null && associationId == other.associationId
        }
}

/** One UI entry per Bluetooth address, keeping the earliest association ID. */
internal fun deduplicateAssociations(devices: List<AssociatedDevice>): List<AssociatedDevice> {
    val unique = linkedMapOf<String, AssociatedDevice>()
    for (device in devices) {
        val key = device.macAddress?.let { "mac:${it.uppercase(Locale.ROOT)}" }
            ?: "id:${device.associationId}"
        val previous = unique[key]
        if (previous == null) {
            unique[key] = device
        } else {
            val kept = if ((device.associationId ?: Int.MAX_VALUE) <
                (previous.associationId ?: Int.MAX_VALUE)
            ) device else previous
            val friendlyName = listOf(previous, device).firstOrNull {
                it.name.isNotBlank() && !it.name.equals(it.macAddress, ignoreCase = true)
            }?.name
            unique[key] = if (friendlyName != null) kept.copy(name = friendlyName) else kept
        }
    }
    return unique.values.toList()
}
