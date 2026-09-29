package dev.bluetap.app.widget

import dev.bluetap.app.bluetooth.BondedDevice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class WidgetConfigStoreTest {
    private val prefs = FakeSharedPreferences()
    private val store = WidgetConfigStore(prefs)
    private val buds = BondedDevice("88:92:CC:EF:B9:56", "OnePlus Buds 4")
    private val headphones = BondedDevice("3C:B0:ED:F7:32:25", "CMF Buds 2 Plus")

    @Test
    fun unconfiguredWidgetLoadsNull() {
        assertNull(store.load(42))
    }

    @Test
    fun savedBondedDeviceRoundTripsWithOnlyAddressAndName() {
        store.save(42, buds)
        assertEquals(buds, store.load(42))
        assertEquals(mapOf(
            "widget_42_mac_address" to buds.macAddress,
            "widget_42_name" to buds.name,
        ), prefs.all)
    }

    @Test
    fun savedAddressIsNormalized() {
        store.save(42, buds.copy(macAddress = buds.macAddress.lowercase()))
        assertEquals(buds, store.load(42))
    }

    @Test
    fun differentWidgetsSelectDifferentBondedDevices() {
        store.save(1, buds)
        store.save(2, headphones)
        assertEquals(buds, store.load(1))
        assertEquals(headphones, store.load(2))
        store.save(1, headphones)
        assertEquals(headphones, store.load(1))
        assertEquals(headphones, store.load(2))
    }

    @Test
    fun legacyMacAndNameRemainUsableWithoutAssociationId() {
        prefs.edit().putInt("widget_42_association_id", 7)
            .putString("widget_42_mac_address", buds.macAddress)
            .putString("widget_42_name", buds.name).apply()
        assertEquals(buds, store.load(42))
        store.save(42, headphones)
        assertEquals(headphones, store.load(42))
        assertFalse(prefs.contains("widget_42_association_id"))
    }

    @Test
    fun legacyAssociationWithoutAddressRequiresSetupAgain() {
        prefs.edit().putInt("widget_42_association_id", 7)
            .putString("widget_42_name", buds.name).apply()
        assertNull(store.load(42))
    }

    @Test
    fun legacyEndpointWithDifferentAddressIsNotGuessedFromName() {
        val old = BondedDevice("52:C6:B9:ED:0C:F8", buds.name)
        prefs.edit().putInt("widget_42_association_id", 7)
            .putString("widget_42_mac_address", old.macAddress)
            .putString("widget_42_name", old.name).apply()
        assertEquals(old, store.load(42))
        assertEquals(WidgetState.Unavailable(old), resolveWidgetState(store.load(42), listOf(buds)))
    }

    @Test
    fun missingNameUsesStoredAddress() {
        prefs.edit().putString("widget_42_mac_address", buds.macAddress).apply()
        assertEquals(BondedDevice(buds.macAddress, buds.macAddress), store.load(42))
    }

    @Test
    fun blankAddressIsNotAConfiguredDevice() {
        prefs.edit().putString("widget_42_mac_address", " ")
            .putString("widget_42_name", buds.name).apply()
        assertNull(store.load(42))
    }

    @Test
    fun deletingWidgetRemovesOnlyItsConfiguration() {
        store.save(1, buds)
        store.save(2, headphones)
        store.delete(1)
        assertNull(store.load(1))
        assertEquals(headphones, store.load(2))
    }

    @Test
    fun deleteLeavesNoKeysIncludingLegacyId() {
        store.save(1, buds)
        prefs.edit().putInt("widget_1_association_id", 7).apply()
        store.delete(1)
        assertEquals(emptyMap<String, Any?>(), prefs.all)
    }

    @Test
    fun deletingUnconfiguredWidgetIsHarmless() {
        store.delete(99)
        assertNull(store.load(99))
    }
}
