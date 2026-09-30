package dev.bluetap.app.widget

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import dev.bluetap.app.bluetooth.BondedDevice
import dev.bluetap.app.bluetooth.DeviceKind
import dev.bluetap.app.bluetooth.artworkKind
import java.io.File
import java.util.Locale
import java.util.UUID

sealed interface DeviceArtwork {
    data object None : DeviceArtwork
    data class BuiltIn(val resource: Int) : DeviceArtwork
    data class Custom(val fileName: String) : DeviceArtwork
    data class Generic(val kind: DeviceKind) : DeviceArtwork
}

/** Only app-owned/licensed assets belong here. No vendor artwork is bundled yet. */
object BuiltInArtworkRegistry {
    private val models: Map<String, Int> = emptyMap()
    fun find(name: String): DeviceArtwork.BuiltIn? = models[name.trim().lowercase(Locale.ROOT)]?.let(DeviceArtwork::BuiltIn)
}

/** MAC-scoped mapping is independent from widget preferences and widget deletion. */
class DeviceArtworkStore(private val prefs: SharedPreferences, private val directory: File) {
    private fun key(address: String) = address.trim().uppercase(Locale.ROOT)
    private fun safeFile(name: String): File? = if (Regex("[a-f0-9-]{36}\\.png").matches(name)) File(directory, name) else null
    fun custom(address: String): DeviceArtwork.Custom? = prefs.safeString(key(address), null)?.let { name ->
        safeFile(name)?.takeIf { it.isFile && it.length() > 0 }?.let { DeviceArtwork.Custom(name) }
    }
    fun set(address: String, name: String) = synchronized(fileLock) {
        require(safeFile(name)?.isFile == true)
        val previous = prefs.safeString(key(address), null)
        check(prefs.edit().putString(key(address), name).commit()) { "Could not save artwork" }
        if (previous != name) previous?.let(::deleteUnreferenced)
    }
    fun remove(address: String) = synchronized(fileLock) {
        val previous = prefs.safeString(key(address), null)
        check(prefs.edit().remove(key(address)).commit()) { "Could not remove artwork" }
        previous?.let(::deleteUnreferenced)
        cleanupOrphans()
    }
    private fun deleteUnreferenced(name: String) {
        if (name !in prefs.all.values) safeFile(name)?.delete()
    }

    /** Only owned UUID PNGs absent from every mapping and older than a day are collectible.
     * The shared lock excludes active imports; mapped artwork survives widget deletion/unpairing.
     * Called on artwork edits (IO), so crashes cannot accumulate files across repeated imports.
     */
    fun cleanupOrphans(now: Long = System.currentTimeMillis()) = synchronized(fileLock) {
        val referenced = prefs.all.values.filterIsInstance<String>().toSet()
        directory.listFiles()?.filter { file ->
            safeFile(file.name) != null && file.isFile && file.name !in referenced &&
                now - file.lastModified() >= 24 * 60 * 60 * 1000L
        }?.forEach { it.delete() }
        Unit
    }
    fun resolve(device: BondedDevice?, appearance: WidgetAppearance): DeviceArtwork = when {
        !appearance.showArtwork -> DeviceArtwork.None
        device == null -> DeviceArtwork.Generic(DeviceKind.GENERIC)
        appearance.artworkSource == ArtworkSource.GENERIC -> DeviceArtwork.Generic(artworkKind(device))
        else -> custom(device.macAddress) ?: BuiltInArtworkRegistry.find(device.name) ?: DeviceArtwork.Generic(artworkKind(device))
    }
    fun bitmap(artwork: DeviceArtwork): Bitmap? = if (artwork is DeviceArtwork.Custom) {
        try { safeFile(artwork.fileName)?.takeIf { it.length() <= 4 * 1024 * 1024 }?.let { file ->
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, options)
            if (options.outWidth <= 0 || options.outHeight <= 0) null else {
                options.inJustDecodeBounds = false
                options.inSampleSize = 1
                while (options.outWidth / options.inSampleSize > 384 || options.outHeight / options.inSampleSize > 384)
                    options.inSampleSize *= 2
                options.inPreferredConfig = Bitmap.Config.ARGB_8888
                BitmapFactory.decodeFile(file.path, options)
            }
        } }
        catch (_: RuntimeException) { null }
    } else null

    /** Copy and normalize during the temporary picker grant; no lasting external URI dependency.
     * ImageDecoder applies orientation and caps decoded allocation to 384px, preserving alpha.
     * Call on Dispatchers.IO. Decode/write failures leave the previous mapping intact;
     * a preference commit failure is surfaced to the editor for retry.
     */
    fun import(context: Context, address: String, uri: Uri) {
        val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
            val ratio = minOf(1f, 384f / maxOf(info.size.width, info.size.height))
            decoder.setTargetSize(maxOf(1, (info.size.width * ratio).toInt()), maxOf(1, (info.size.height * ratio).toInt()))
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.setTargetColorSpace(android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.SRGB))
        }
        try {
            synchronized(fileLock) {
                check(directory.exists() || directory.mkdirs())
                val file = File(directory, "${UUID.randomUUID()}.png")
                try {
                    file.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
                    set(address, file.name)
                    cleanupOrphans()
                } catch (e: Exception) { if (file.name !in prefs.all.values) file.delete(); throw e }
            }
        }
        finally { bitmap.recycle() }
    }
    companion object {
        private val fileLock = Any()
        fun from(context: Context) = DeviceArtworkStore(context.getSharedPreferences("device_artwork", Context.MODE_PRIVATE),
            File(context.filesDir, "device_artwork"))
    }
}
