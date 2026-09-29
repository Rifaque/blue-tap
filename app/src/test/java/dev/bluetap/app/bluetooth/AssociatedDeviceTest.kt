package dev.bluetap.app.bluetooth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AssociatedDeviceTest {

    @Test(expected = IllegalArgumentException::class)
    fun requiresAnAssociationIdOrMacAddress() {
        AssociatedDevice(associationId = null, macAddress = null, name = "Buds")
    }

    @Test
    fun sameAssociationId_matches() {
        val saved = AssociatedDevice(7, "AA:BB:CC:DD:EE:FF", "Buds")
        assertTrue(saved.isSameAssociationAs(AssociatedDevice(7, "AA:BB:CC:DD:EE:FF", "Renamed")))
    }

    @Test
    fun differentAssociationIds_doNotMatchEvenForTheSameDevice() {
        // The device was removed and associated again: the saved association is stale.
        val saved = AssociatedDevice(7, "AA:BB:CC:DD:EE:FF", "Buds")
        assertFalse(saved.isSameAssociationAs(AssociatedDevice(8, "AA:BB:CC:DD:EE:FF", "Buds")))
    }

    @Test
    fun withoutAssociationId_matchesByMacAddressIgnoringCase() {
        // Saved on Android 12/12L, checked after an upgrade to Android 13+.
        val saved = AssociatedDevice(null, "AA:BB:CC:DD:EE:FF", "Buds")
        assertTrue(saved.isSameAssociationAs(AssociatedDevice(3, "aa:bb:cc:dd:ee:ff", "Buds")))
        assertFalse(saved.isSameAssociationAs(AssociatedDevice(3, "11:22:33:44:55:66", "Buds")))
    }

    @Test
    fun withoutAssociationIdOrMacAddressOnOtherSide_doesNotMatch() {
        val saved = AssociatedDevice(null, "AA:BB:CC:DD:EE:FF", "Buds")
        assertFalse(saved.isSameAssociationAs(AssociatedDevice(3, null, "Buds")))
    }
}
