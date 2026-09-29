package dev.bluetap.app.bluetooth

/**
 * A Bluetooth device the user has associated with BlueTap through Android's
 * Companion Device Manager.
 *
 * This is BlueTap's own model, so the UI, widget and saved configuration do not
 * depend on Android Bluetooth or companion-device classes.
 *
 * @property associationId The Companion Device Manager association ID. Android 13+
 *  always provides one; on Android 12/12L the platform exposes no IDs, so it is `null`.
 * @property macAddress The device's Bluetooth address, if the association exposes it.
 *  Only used as the identity when there is no [associationId].
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
     * Whether [other] refers to the same association.
     *
     * Association IDs are compared when both sides have one, so a device that was
     * removed and associated again counts as a different association. Otherwise
     * (associations created on Android 12/12L) the MAC address is compared.
     */
    fun isSameAssociationAs(other: AssociatedDevice): Boolean =
        if (associationId != null && other.associationId != null) {
            associationId == other.associationId
        } else {
            macAddress != null && macAddress.equals(other.macAddress, ignoreCase = true)
        }
}
