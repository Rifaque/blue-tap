package dev.bluetap.app.widget

import dev.bluetap.app.bluetooth.AssociatedDevice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WidgetConfigStoreTest {

    private val prefs = FakeSharedPreferences()
    private val store = WidgetConfigStore(prefs)

    private val buds = AssociatedDevice(1, "AA:BB:CC:DD:EE:FF", "Buds")
    private val headphones = AssociatedDevice(2, "11:22:33:44:55:66", "Headphones")

    @Test
    fun unconfiguredWidget_loadsNull() {
        assertNull(store.load(42))
    }

    @Test
    fun savedDevice_roundTrips() {
        store.save(42, buds)
        assertEquals(buds, store.load(42))
    }

    @Test
    fun deviceWithoutAssociationId_roundTrips() {
        val android12Device = AssociatedDevice(null, "AA:BB:CC:DD:EE:FF", "Buds")
        store.save(42, android12Device)
        assertEquals(android12Device, store.load(42))
    }

    @Test
    fun deviceWithoutMacAddress_roundTrips() {
        val device = AssociatedDevice(5, null, "Buds")
        store.save(42, device)
        assertEquals(device, store.load(42))
    }

    @Test
    fun widgetsAreConfiguredIndependently() {
        store.save(1, buds)
        store.save(2, headphones)
        assertEquals(buds, store.load(1))
        assertEquals(headphones, store.load(2))
    }

    @Test
    fun savingAgain_replacesPreviousDevice() {
        store.save(42, buds)
        store.save(42, AssociatedDevice(null, "11:22:33:44:55:66", "Headphones"))
        // The old association ID must not survive.
        assertEquals(AssociatedDevice(null, "11:22:33:44:55:66", "Headphones"), store.load(42))
    }

    @Test
    fun delete_removesOnlyThatWidget() {
        store.save(1, buds)
        store.save(2, headphones)
        store.delete(1)
        assertNull(store.load(1))
        assertEquals(headphones, store.load(2))
    }

    @Test
    fun delete_leavesNoKeysBehind() {
        store.save(1, buds)
        store.delete(1)
        assertEquals(emptyMap<String, Any?>(), prefs.all)
    }

    @Test
    fun deletingUnconfiguredWidget_isHarmless() {
        store.delete(99)
        assertNull(store.load(99))
    }
}
