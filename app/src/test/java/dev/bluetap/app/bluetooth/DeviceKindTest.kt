package dev.bluetap.app.bluetooth

import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceKindTest {
    @Test fun exactClassesMapToIcons() {
        assertEquals(DeviceKind.HEADPHONES, deviceKind(0x0418))
        assertEquals(DeviceKind.SPEAKER, deviceKind(0x0414))
        assertEquals(DeviceKind.WATCH, deviceKind(0x0704))
        assertEquals(DeviceKind.CONTROLLER, deviceKind(0x0508))
        assertEquals(DeviceKind.INPUT, deviceKind(0x0540))
    }
    @Test fun missingOrAmbiguousMetadataStaysGeneric() {
        assertEquals(DeviceKind.GENERIC, deviceKind(null))
        assertEquals(DeviceKind.GENERIC, deviceKind(0x0400))
        assertEquals(DeviceKind.GENERIC, deviceKind(0x7FFF))
    }
    @Test fun obviousProductNamesUseConservativeFallback() {
        listOf("OnePlus Buds 4", "CMF Buds 2 Plus", "EAR BUDS", "Wireless Headset", "My earphones").forEach {
            assertEquals(it, DeviceKind.HEADPHONES, deviceKind(null, it))
        }
        assertEquals(DeviceKind.CONTROLLER, deviceKind(null, "GameSir-G7 Pro"))
        assertEquals(DeviceKind.SPEAKER, deviceKind(0x0400, "Living room soundbar"))
        assertEquals(DeviceKind.WATCH, deviceKind(null, "Fitness band"))
        listOf("RS-7I", "Bandit", "Watchful", "Phone", "Unknown").forEach {
            assertEquals(it, DeviceKind.GENERIC, deviceKind(null, it))
        }
    }
    @Test fun reliableSpecificMetadataWinsOverNames() {
        assertEquals(DeviceKind.SPEAKER, deviceKind(0x0414, "Buds speaker"))
        assertEquals(DeviceKind.HEADPHONES, deviceKind(0x0418, "RS-7I"))
    }
}
