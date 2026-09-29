package dev.bluetap.app.bluetooth

import org.junit.Assert.assertEquals
import org.junit.Test

class AssociationNameTest {

    private val mac = "AA:BB:CC:DD:EE:FF"

    @Test
    fun displayName_takesPriorityWithoutReadingBluetoothName() {
        assertEquals("Chooser Buds", resolveAssociationName("Chooser Buds", mac, "Unknown") {
            error("Bluetooth name should not be read")
        })
    }

    @Test
    fun blankDisplayName_usesBluetoothName() {
        assertEquals("Bluetooth Buds", resolveAssociationName("  ", mac, "Unknown") {
            assertEquals(mac, it)
            "Bluetooth Buds"
        })
    }

    @Test
    fun unavailableBluetoothName_fallsBackToMac() {
        assertEquals(mac, resolveAssociationName(null, mac, "Unknown") { null })
        assertEquals(mac, resolveAssociationName("", mac, "Unknown") { "  " })
    }

    @Test
    fun noAddressOrNames_usesUnknownLabel() {
        assertEquals("Unknown", resolveAssociationName(null, null, "Unknown") {
            error("No address to look up")
        })
    }
}
