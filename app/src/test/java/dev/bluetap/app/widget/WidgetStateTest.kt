package dev.bluetap.app.widget

import dev.bluetap.app.bluetooth.AssociatedDevice
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetStateTest {

    private val buds = AssociatedDevice(1, "AA:BB:CC:DD:EE:FF", "Buds")
    private val headphones = AssociatedDevice(2, "11:22:33:44:55:66", "Headphones")

    @Test
    fun noSavedDevice_isNotConfigured() {
        assertEquals(WidgetState.NotConfigured, resolveWidgetState(null, listOf(buds)))
    }

    @Test
    fun savedDeviceStillAssociated_isReady() {
        assertEquals(WidgetState.Ready(buds), resolveWidgetState(buds, listOf(headphones, buds)))
    }

    @Test
    fun savedAssociationRemoved_isUnavailable() {
        assertEquals(WidgetState.Unavailable(buds), resolveWidgetState(buds, listOf(headphones)))
        assertEquals(WidgetState.Unavailable(buds), resolveWidgetState(buds, emptyList()))
    }

    @Test
    fun associationsUnknown_isUnavailable() {
        assertEquals(WidgetState.Unavailable(buds), resolveWidgetState(buds, null))
    }
}
