package dev.bluetap.app.widget

import dev.bluetap.app.bluetooth.BondedDevice
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetStateTest {
    private val buds = BondedDevice("88:92:CC:EF:B9:56", "OnePlus Buds 4")
    private val headphones = BondedDevice("3C:B0:ED:F7:32:25", "CMF Buds 2 Plus")

    @Test
    fun noSavedDeviceIsNotConfigured() {
        assertEquals(WidgetState.NotConfigured, resolveWidgetState(null, listOf(buds)))
    }

    @Test
    fun savedDeviceStillBondedIsReady() {
        assertEquals(WidgetState.Ready(buds), resolveWidgetState(buds, listOf(headphones, buds)))
    }

    @Test
    fun matchesAddressIgnoringCaseAndUsesCurrentName() {
        val current = BondedDevice(buds.macAddress.lowercase(), "Renamed Buds")
        assertEquals(WidgetState.Ready(current), resolveWidgetState(buds, listOf(current)))
    }

    @Test
    fun unpairedOrMissingDeviceIsUnavailable() {
        assertEquals(WidgetState.Unavailable(buds), resolveWidgetState(buds, listOf(headphones)))
        assertEquals(WidgetState.Unavailable(buds), resolveWidgetState(buds, emptyList()))
    }

    @Test
    fun unreadableBondedListIsUnavailableAndRetainsSavedDevice() {
        assertEquals(WidgetState.Unavailable(buds), resolveWidgetState(buds, null))
    }

    @Test
    fun sameNameAtDifferentAddressDoesNotMatch() {
        assertEquals(WidgetState.Unavailable(buds),
            resolveWidgetState(buds, listOf(headphones.copy(name = buds.name))))
    }
}
