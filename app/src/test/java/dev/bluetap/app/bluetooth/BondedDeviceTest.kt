package dev.bluetap.app.bluetooth

import org.junit.Assert.assertEquals
import org.junit.Test

class BondedDeviceTest {
    private val address = "88:92:CC:EF:B9:56"

    @Test
    fun mapsBondedDeviceWithFriendlyNameBeforeAlias() {
        assertEquals(
            BondedDevice(address, "OnePlus Buds 4"),
            mapBondedDevice("88:92:cc:ef:b9:56", "OnePlus Buds 4", "My earbuds"),
        )
    }

    @Test
    fun missingOrBlankNameFallsBackToAlias() {
        for (name in listOf(null, "", "  ")) {
            assertEquals("My earbuds", mapBondedDevice(address, name, " My earbuds ").name)
        }
    }

    @Test
    fun missingOrBlankNameAndAliasFallBackToAddress() {
        assertEquals(address, mapBondedDevice(address, null, null).name)
        assertEquals(address, mapBondedDevice(address.lowercase(), "  ", "").name)
    }

    @Test
    fun sortsByNameIgnoringCaseAndBreaksTiesByAddress() {
        val devices = listOf(
            BondedDevice(address, "OnePlus Buds 4"),
            BondedDevice("33:33:33:33:33:33", "echo Dot"),
            BondedDevice("22:22:22:22:22:22", "CMF Buds 2 Plus"),
            BondedDevice("11:11:11:11:11:11", "Echo Dot"),
        )
        assertEquals(listOf(devices[2], devices[3], devices[1], devices[0]), prepareBondedDevices(devices))
    }

    @Test
    fun deduplicatesAddressIgnoringCaseAndKeepsFriendlyName() {
        val devices = listOf(
            mapBondedDevice(address, null, null),
            BondedDevice(address.lowercase(), "OnePlus Buds 4"),
            mapBondedDevice(address, "OnePlus Buds 4", null),
        )
        val expected = listOf(BondedDevice(address, "OnePlus Buds 4"))
        assertEquals(expected, prepareBondedDevices(devices))
        assertEquals(expected, prepareBondedDevices(devices.reversed()))
    }

    @Test
    fun sameNameWithDifferentAddressesRemainsTwoDevices() {
        val devices = listOf(
            BondedDevice("11:11:11:11:11:11", "Buds"),
            BondedDevice("22:22:22:22:22:22", "Buds"),
        )
        assertEquals(devices, prepareBondedDevices(devices))
    }

    @Test
    fun emptyBondedListStaysEmpty() {
        assertEquals(emptyList<BondedDevice>(), prepareBondedDevices(emptyList()))
    }
}
