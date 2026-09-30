package dev.bluetap.app.widget

import org.junit.Assert.*
import org.junit.Test

class RowWidgetSpecTest {
    private val a = WidgetAppearance()
    // Deterministic measured-width fixture. Production uses the same Android Paint in both UIs.
    private val measure: (String, Int) -> Float = { text, sp -> text.length * sp * .55f }
    private fun spec(w: Float, h: Float, name: String = "OnePlus Buds 4", state: WidgetDeviceStatus = WidgetDeviceStatus(),
        appearance: WidgetAppearance = a, scale: Float = 1f) = rowWidgetSpec(w, h, name, appearance, state,
        fontScale = scale, measure = { text, size -> measure(text, size) * scale })

    @Test fun narrowRowKeepsArtworkAndBalancedTwoLineNameWithoutPlaceholder() {
        val r = spec(150f, 56f)
        assertTrue(r.showName); assertEquals(2, r.nameMaxLines)
        assertEquals("OnePlus\nBuds 4", r.name)
        assertNull(r.secondary); assertTrue(r.artworkSize >= 28)
        assertTrue(r.textHeight <= r.availableHeight)
        assertNull(spec(190f, 100f).secondary) // Width-constrained family never shows placeholder.
    }
    @Test fun shortRowsDropSecondaryBeforeReducingNameLines() {
        val tight = spec(150f, 56f)
        assertEquals(2, tight.nameMaxLines)
        assertNull(tight.secondary)
        assertEquals(2, spec(150f, 48f).nameMaxLines)
        val veryTight = spec(150f, 44f)
        assertEquals(1, veryTight.nameMaxLines)
        assertNull(veryTight.secondary)
        assertTrue(veryTight.textHeight <= veryTight.availableHeight)
        assertNull(spec(270f, 64f).secondary)
        assertEquals(1, spec(270f, 64f).nameMaxLines)
    }
    @Test fun exactWidthAndHeightBoundariesControlPlaceholder() {
        assertNull(spec(219f, 80f).secondary)
        assertNull(spec(220f, 71f).secondary)
        assertEquals("Android 17 required", spec(220f, 72f).secondary)
        assertEquals(RowHeightClass.SLIM, spec(270f, 71f).heightClass)
        assertEquals(RowHeightClass.SPACIOUS, spec(270f, 72f).heightClass)
        assertTrue(isOneRowWidget(110f, 124f))
        assertFalse(isOneRowWidget(110f, 125f))
        assertFalse(isOneRowWidget(109f, 80f))
    }
    @Test fun threeFourAndFiveCellLikeWidthsKeepOneLineNameAndGroupedStatus() {
        for (width in listOf(260f, 340f, 430f, 540f)) {
            val r = spec(width, 80f)
            assertEquals(1, r.nameMaxLines)
            assertEquals("OnePlus Buds 4", r.name)
            assertEquals("Android 17 required", r.secondary)
            assertTrue(r.textHeight <= r.availableHeight)
            val layout = widgetLayoutRules(width, 80f, a, name = "OnePlus Buds 4", measure = measure)
            assertTrue(layout.horizontal); assertNotNull(layout.row)
            assertFalse(layout.showLabel)
            assertEquals(layout.row?.secondary, layout.status)
            assertNull(layout.battery) // Never a separate third column or third text row.
        }
    }
    @Test fun futureKnownStateAndBatteryBeatPlaceholderButMustFit() {
        val connected = WidgetDeviceStatus(WidgetConnectionState.CONNECTED, DeviceBattery(percent = 92))
        assertEquals("Connected · 92%", spec(270f, 64f, state = connected).secondary)
        assertNull(spec(150f, 56f, state = connected).secondary) // Do not sacrifice the two-line name.
        assertEquals("Connected", spec(270f, 64f, state = connected, appearance = a.copy(showStatus = false, showBattery = false)).secondary)
        assertEquals("92%", spec(190f, 64f, state = WidgetDeviceStatus(battery = DeviceBattery(percent = 92))).secondary)
        assertEquals("Connecting", spec(270f, 64f, state = WidgetDeviceStatus(WidgetConnectionState.CONNECTING)).secondary)
    }
    @Test fun allRowContentFitsBudgetAcrossTightHeightsPresetsAndFontScales() {
        for (preset in WidgetPreset.entries) for (w in listOf(110f, 150f, 190f, 220f, 270f, 340f, 440f))
            for (h in listOf(40f, 48f, 56f, 64f, 72f, 80f, 124f)) for (scale in listOf(1f, 1.3f, 2f)) {
                val r = spec(w, h, "My unusually long Bluetooth headphones", appearance = appearanceForPreset(preset), scale = scale)
                assertTrue("$preset $w x $h scale=$scale", r.textHeight <= r.availableHeight)
                assertTrue(r.artworkSize <= r.availableHeight)
                assertTrue(r.artworkSize <= 52)
            }
    }
    @Test fun paddingIsReturnedBeforeAnUnnecessaryWrap() {
        val r = spec(220f, 72f, "OnePlus Buds 4 Ultra")
        assertEquals(1, r.nameMaxLines)
        assertEquals(6, r.horizontalPadding)
    }
    @Test fun longNameKeepsTwoLinesEvenWhenThatRemovesPlaceholder() {
        val r = spec(240f, 72f, "OnePlus very long Buds 4 headphones", scale = 1.3f)
        assertEquals(2, r.nameMaxLines)
        assertNull(r.secondary)
        assertTrue(r.textHeight <= r.availableHeight)
    }
}
