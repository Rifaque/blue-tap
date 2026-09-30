package dev.bluetap.app.widget

import dev.bluetap.app.bluetooth.BondedDevice
import dev.bluetap.app.bluetooth.DeviceKind
import org.junit.Assert.*
import org.junit.Test

class WidgetAppearanceTest {
    @Test fun malformedPreferenceTypesUseDefaultsWithoutLosingValidFields() {
        val bad = FakeSharedPreferences()
        bad.edit().putInt("widget_7_preset", 5).putString("widget_7_show_battery", "true")
            .putBoolean("widget_7_custom_background", true).putInt("widget_7_artwork_source", 2)
            .putString("widget_7_accent", "MINT").apply()
        assertEquals(WidgetAppearance(accent = WidgetAccent.MINT), WidgetConfigStore(bad).loadAppearance(7))
    }
    private val prefs = FakeSharedPreferences()
    private val store = WidgetConfigStore(prefs)
    private val buds = BondedDevice("88:92:CC:EF:B9:56", "OnePlus Buds 4", DeviceKind.HEADPHONES)
    private val custom = WidgetAppearance(WidgetPreset.HYPER_GLASS, WidgetAccent.MINT,
        WidgetBackground.CUSTOM, 0xFF123E36.toInt(), false, false, false, WidgetAlignment.END, true,
        showBattery = false, showArtwork = false, artworkSource = ArtworkSource.GENERIC)

    @Test fun everyAppearanceFieldSurvivesReload() {
        store.saveAppearance(7, custom)
        assertEquals(custom, WidgetConfigStore(prefs).loadAppearance(7))
    }
    @Test fun legacyWidgetGetsDefaultAppearanceAndKeepsDevice() {
        prefs.edit().putString("widget_7_mac_address", buds.macAddress)
            .putString("widget_7_name", buds.name).putInt("widget_7_association_id", 99).apply()
        assertEquals(WidgetAppearance(), store.loadAppearance(7))
        assertEquals(buds.macAddress, store.load(7)?.macAddress)
        assertEquals(buds.name, store.load(7)?.name)
    }
    @Test fun newWidgetDefaultsToBlueTap() {
        assertEquals(WidgetAppearance(), store.loadAppearance(9))
    }
    @Test fun differentWidgetsRetainIndependentAppearanceAndDevice() {
        val other = BondedDevice("3C:B0:ED:F7:32:25", "CMF Buds 2 Plus")
        store.save(1, buds); store.save(2, other)
        store.saveAppearance(1, custom)
        store.saveAppearance(2, appearanceForPreset(WidgetPreset.DOT_MONO))
        assertEquals(custom, store.loadAppearance(1))
        assertEquals(WidgetPreset.DOT_MONO, store.loadAppearance(2).preset)
        assertEquals(buds, store.load(1)); assertEquals(other, store.load(2))
    }
    @Test fun everyPresetUsesStableNameSerialization() {
        for (preset in WidgetPreset.entries) {
            val appearance = appearanceForPreset(preset)
            store.saveAppearance(7, appearance)
            assertEquals(preset.name, prefs.getString("widget_7_preset", null))
            assertEquals(appearance, store.loadAppearance(7))
        }
    }
    @Test fun invalidStoredPresetFallsBackWithoutErasingOtherOptions() {
        store.save(7, buds)
        store.saveAppearance(7, custom)
        prefs.edit().putString("widget_7_preset", "REMOVED_PRESET").apply()
        assertEquals(custom.copy(preset = WidgetPreset.BLUE_TAP), store.loadAppearance(7))
        assertEquals(buds, store.load(7))
    }
    @Test fun invalidEnumOptionsFallBackSafely() {
        prefs.edit().putString("widget_7_accent", "?").putString("widget_7_background", "?")
            .putString("widget_7_alignment", "?").apply()
        assertEquals(WidgetAppearance(), store.loadAppearance(7))
    }
    @Test fun appearanceChangesDoNotChangeSelectedDevice() {
        store.save(7, buds)
        store.saveAppearance(7, custom)
        assertEquals(buds, store.load(7))
    }
    @Test fun changingDeviceDoesNotResetAppearance() {
        store.saveAppearance(7, custom)
        store.save(7, buds)
        assertEquals(custom, store.loadAppearance(7))
    }
    @Test fun deletingWidgetRemovesStyleWithoutAffectingOtherWidget() {
        store.save(1, buds); store.saveAppearance(1, custom)
        store.save(10, buds); store.saveAppearance(10, custom)
        store.delete(1)
        assertNull(store.load(1)); assertEquals(WidgetAppearance(), store.loadAppearance(1))
        assertEquals(buds, store.load(10)); assertEquals(custom, store.loadAppearance(10))
        assertTrue(prefs.all.keys.none { it.startsWith("widget_1_") })
    }
    @Test fun upgradedVisualPassRetainsPresetAndAddsSafeDefaults() {
        prefs.edit().putString("widget_7_preset", "HYPER_GLASS").putBoolean("widget_7_show_name", false)
            .putString("widget_7_accent", "MINT").apply()
        val a = store.loadAppearance(7)
        assertEquals(WidgetPreset.HYPER_GLASS, a.preset)
        assertEquals(WidgetAccent.MINT, a.accent)
        assertFalse(a.showDeviceName)
        assertTrue(a.showBattery); assertTrue(a.showArtwork)
        assertEquals(ArtworkSource.DEVICE, a.artworkSource)
    }
    @Test fun unknownArtworkSourceFallsBackToSharedDeviceImage() {
        prefs.edit().putString("widget_7_artwork_source", "obsolete").apply()
        assertEquals(ArtworkSource.DEVICE, store.loadAppearance(7).artworkSource)
    }
}
