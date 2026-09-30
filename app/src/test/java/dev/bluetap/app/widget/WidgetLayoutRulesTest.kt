package dev.bluetap.app.widget

import org.junit.Assert.*
import org.junit.Test

class WidgetLayoutRulesTest {
    private val a = WidgetAppearance()
    @Test fun actualDimensionsSelectAllSixCompositions() {
        assertEquals(WidgetLayoutClass.TINY, widgetLayoutClass(90f, 80f))
        assertEquals(WidgetLayoutClass.TINY, widgetLayoutClass(180f, 48f))
        assertEquals(WidgetLayoutClass.SLEEK, widgetLayoutClass(190f, 80f))
        assertEquals(WidgetLayoutClass.SLEEK_WIDE, widgetLayoutClass(300f, 80f))
        assertEquals(WidgetLayoutClass.CARD, widgetLayoutClass(170f, 170f))
        assertEquals(WidgetLayoutClass.WIDE, widgetLayoutClass(290f, 170f))
        assertEquals(WidgetLayoutClass.SHOWCASE, widgetLayoutClass(260f, 240f))
        assertEquals(WidgetLayoutClass.CARD, widgetLayoutClass(160f, 210f)) // Tall doesn't imply wide.
    }
    @Test fun oneRowAndSquareKeepReadableNamesAndStatusBeforeBranding() {
        for ((w, h) in listOf(150f to 150f, 170f to 170f)) {
            val r = widgetLayoutRules(w, h, a)
            assertEquals(2, r.nameLines)
            assertTrue(r.showName); assertFalse(r.showLabel)
            assertEquals("Android 17 required", r.status)
            assertTrue(r.artworkSize >= 40)
        }
        assertTrue(widgetLayoutRules(190f, 80f, a).horizontal)
        assertFalse(widgetLayoutRules(170f, 170f, a).horizontal)
        assertNotNull(widgetLayoutRules(350f, 80f, a).row)
    }
    @Test fun batteryAndActualStateTakePriorityOverOptionalUnknownCopy() {
        val unknown = widgetLayoutRules(90f, 90f, a)
        assertNull(unknown.status); assertNull(unknown.battery); assertFalse(unknown.showLabel)
        val connected = WidgetDeviceStatus(WidgetConnectionState.CONNECTED, DeviceBattery(percent = 92))
        val tiny = widgetLayoutRules(90f, 90f, a.copy(showStatus = false), connected)
        assertEquals("Connected · 92%", tiny.status); assertNull(tiny.battery)
        assertNull(widgetLayoutRules(190f, 80f, a.copy(showBattery = false), connected).battery)
        val unavailable = widgetLayoutRules(170f, 170f, a.copy(showStatus = false), connected.copy(connection = WidgetConnectionState.UNAVAILABLE))
        assertEquals("Unavailable", unavailable.status); assertNull(unavailable.battery)
    }
    @Test fun presetsKeepTheirArtDirectionAndSurfaceConstraints() {
        val normal = widgetLayoutRules(260f, 240f, a)
        val minimal = widgetLayoutRules(260f, 240f, appearanceForPreset(WidgetPreset.MINIMAL))
        val glass = widgetLayoutRules(260f, 280f, appearanceForPreset(WidgetPreset.HYPER_GLASS))
        assertFalse(normal.horizontal); assertTrue(minimal.horizontal)
        assertTrue(glass.artworkSize > normal.artworkSize)
        assertFalse(widgetLayoutRules(260f, 240f, appearanceForPreset(WidgetPreset.DOT_MONO)).showLabel)
        assertFalse(widgetLayoutRules(260f, 240f, appearanceForPreset(WidgetPreset.OLED)).showLabel)
        assertTrue(presetKeepsBlackSurface(WidgetPreset.DOT_MONO))
        assertTrue(presetKeepsBlackSurface(WidgetPreset.OLED))
        assertFalse(presetKeepsBlackSurface(WidgetPreset.MATERIAL_YOU))
    }
    @Test fun batteryDoesNotInventUnknownSidesOrValues() {
        assertNull(DeviceBattery().label())
        assertEquals("L 95%", DeviceBattery(left = 95).label())
        assertEquals("L 95%  R 100%", DeviceBattery(left = 95, right = 100).label())
        assertEquals(EarbudArtworkState.FULL, WidgetDeviceStatus().earbudArtwork)
    }
    @Test fun largerFontGivesSpaceToInformationBeforeArtwork() {
        val regular = widgetLayoutRules(170f, 170f, a)
        val scaled = widgetLayoutRules(170f, 170f, a, fontScale = 1.5f)
        assertEquals(regular.status, scaled.status)
        assertEquals(2, scaled.nameLines)
        assertTrue(scaled.artworkSize < regular.artworkSize)
        assertFalse(scaled.showLabel)
    }
    @Test(expected = IllegalArgumentException::class) fun invalidBatteryIsRejected() { DeviceBattery(percent = 101) }
}
