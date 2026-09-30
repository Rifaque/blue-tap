package dev.bluetap.app.widget

import android.content.SharedPreferences

/** Badly typed legacy/restored entries must not crash the editor or widget renderer. */
internal fun SharedPreferences.safeString(key: String, fallback: String? = null): String? =
    try { getString(key, fallback) } catch (_: ClassCastException) { fallback }
internal fun SharedPreferences.safeBoolean(key: String, fallback: Boolean): Boolean =
    try { getBoolean(key, fallback) } catch (_: ClassCastException) { fallback }
internal fun SharedPreferences.safeInt(key: String, fallback: Int): Int =
    try { getInt(key, fallback) } catch (_: ClassCastException) { fallback }
