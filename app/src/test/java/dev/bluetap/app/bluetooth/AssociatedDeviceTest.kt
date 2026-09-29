package dev.bluetap.app.bluetooth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
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
    fun differentAssociationIds_matchForTheSameDevice() {
        val saved = AssociatedDevice(7, "AA:BB:CC:DD:EE:FF", "Buds")
        assertTrue(saved.isSameAssociationAs(AssociatedDevice(8, "aa:bb:cc:dd:ee:ff", "Buds")))
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

    @Test
    fun duplicateMacs_keepOneRowAndTheOldestIdWithFriendlyName() {
        val duplicates = listOf(
            AssociatedDevice(12, "AA:BB:CC:DD:EE:FF", "AA:BB:CC:DD:EE:FF"),
            AssociatedDevice(7, "aa:bb:cc:dd:ee:ff", "Buds"),
            AssociatedDevice(15, "AA:BB:CC:DD:EE:FF", "AA:BB:CC:DD:EE:FF"),
            AssociatedDevice(9, "11:22:33:44:55:66", "Other"),
        )

        assertEquals(
            listOf(AssociatedDevice(7, "aa:bb:cc:dd:ee:ff", "Buds"), duplicates[3]),
            deduplicateAssociations(duplicates),
        )
    }

    @Test
    fun associationsWithoutMac_keepDistinctIds() {
        assertEquals(2, deduplicateAssociations(listOf(
            AssociatedDevice(1, null, "One"), AssociatedDevice(2, null, "Two"),
        )).size)
    }
}
