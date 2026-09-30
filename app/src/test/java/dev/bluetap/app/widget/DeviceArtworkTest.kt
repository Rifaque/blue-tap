package dev.bluetap.app.widget

import dev.bluetap.app.bluetooth.BondedDevice
import dev.bluetap.app.bluetooth.DeviceKind
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.UUID

class DeviceArtworkTest {
    @Test fun malformedMappingFallsBackWithoutReadingExternalPaths() {
        prefs.edit().putBoolean(buds.macAddress, true).apply()
        assertEquals(DeviceArtwork.Generic(DeviceKind.HEADPHONES),
            DeviceArtworkStore(prefs, temp.root).resolve(buds, WidgetAppearance()))
    }
    @Test fun cleanupOnlyDeletesOldUnreferencedOwnedFiles() {
        val store = DeviceArtworkStore(prefs, temp.root)
        val referenced = image(); val orphan = image(); val fresh = image()
        val unowned = File(temp.root, "keep.txt").apply { writeText("not owned artwork") }
        val now = System.currentTimeMillis()
        listOf(referenced, orphan, unowned).forEach { assertTrue(it.setLastModified(now - 25 * 60 * 60 * 1000L)) }
        store.set(buds.macAddress, referenced.name)
        store.cleanupOrphans(now)
        assertTrue(referenced.exists()); assertTrue(fresh.exists()); assertTrue(unowned.exists())
        assertFalse(orphan.exists())
    }
    @Test fun removingOneMappingNeverDeletesAFileStillReferencedByAnotherDevice() {
        val store = DeviceArtworkStore(prefs, temp.root)
        val shared = image()
        store.set(buds.macAddress, shared.name); store.set("OTHER", shared.name)
        store.remove(buds.macAddress)
        assertTrue(shared.exists()); assertEquals(DeviceArtwork.Custom(shared.name), store.custom("OTHER"))
    }
    @get:Rule val temp = TemporaryFolder()
    private val prefs = FakeSharedPreferences()
    private val buds = BondedDevice("88:92:CC:EF:B9:56", "OnePlus Buds 4", DeviceKind.HEADPHONES)
    private fun image(): File = File(temp.root, "${UUID.randomUUID()}.png").apply { writeBytes(byteArrayOf(1, 2, 3)) }
    @Test fun customMappingIsAddressScopedAndSurvivesStoreRecreation() {
        val store = DeviceArtworkStore(prefs, temp.root)
        val file = image()
        store.set(buds.macAddress.lowercase(), file.name)
        assertEquals(DeviceArtwork.Custom(file.name), DeviceArtworkStore(prefs, temp.root).resolve(buds, WidgetAppearance()))
        assertEquals(DeviceArtwork.Generic(DeviceKind.HEADPHONES), store.resolve(buds.copy(macAddress = "AA:BB:CC:DD:EE:FF"), WidgetAppearance()))
    }
    @Test fun replacementAndRemovalRestoreAutomaticWithoutAffectingOtherDevice() {
        val store = DeviceArtworkStore(prefs, temp.root)
        val first = image(); val second = image(); val other = image()
        store.set(buds.macAddress, first.name); store.set("OTHER", other.name)
        store.set(buds.macAddress, second.name)
        assertFalse(first.exists()); assertTrue(second.exists())
        store.remove(buds.macAddress)
        assertFalse(second.exists()); assertTrue(other.exists())
        assertEquals(DeviceArtwork.Generic(DeviceKind.HEADPHONES), store.resolve(buds, WidgetAppearance()))
    }
    @Test fun missingFileAndStaleExternalUriFallBackSafely() {
        val store = DeviceArtworkStore(prefs, temp.root)
        val file = image(); store.set(buds.macAddress, file.name); file.delete()
        assertEquals(DeviceArtwork.Generic(DeviceKind.HEADPHONES), store.resolve(buds, WidgetAppearance()))
        prefs.edit().putString(buds.macAddress, "content://stale-gallery/image/1").apply()
        assertNull(store.custom(buds.macAddress))
        prefs.edit().putString(buds.macAddress, "../private.png").apply()
        assertNull(store.custom(buds.macAddress))
    }
    @Test fun widgetArtworkOptionsDoNotDeleteSharedImage() {
        val store = DeviceArtworkStore(prefs, temp.root)
        val file = image(); store.set(buds.macAddress, file.name)
        assertEquals(DeviceArtwork.None, store.resolve(buds, WidgetAppearance(showArtwork = false)))
        assertEquals(DeviceArtwork.Generic(DeviceKind.HEADPHONES), store.resolve(buds, WidgetAppearance(artworkSource = ArtworkSource.GENERIC)))
        WidgetConfigStore(FakeSharedPreferences()).apply { save(1, buds); save(2, buds); delete(1) }
        assertEquals(DeviceArtwork.Custom(file.name), store.resolve(buds, WidgetAppearance()))
        assertTrue(file.exists())
    }
    @Test fun automaticHasNoUnlicensedProductAsset() {
        assertNull(BuiltInArtworkRegistry.find(buds.name))
        assertEquals(DeviceArtwork.Generic(DeviceKind.GENERIC), DeviceArtworkStore(prefs, temp.root).resolve(null, WidgetAppearance()))
    }
}
