package dev.bluetap.app.widget

import android.content.SharedPreferences

/** Minimal in-memory [SharedPreferences] so storage code can be tested on the JVM. */
class FakeSharedPreferences : SharedPreferences {

    private val values = mutableMapOf<String, Any?>()

    override fun getAll(): Map<String, *> = values.toMap()
    override fun getString(key: String, defValue: String?) = values[key] as String? ?: defValue

    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String, defValues: Set<String>?) =
        values[key] as Set<String>? ?: defValues

    override fun getInt(key: String, defValue: Int) = values[key] as Int? ?: defValue
    override fun getLong(key: String, defValue: Long) = values[key] as Long? ?: defValue
    override fun getFloat(key: String, defValue: Float) = values[key] as Float? ?: defValue
    override fun getBoolean(key: String, defValue: Boolean) = values[key] as Boolean? ?: defValue
    override fun contains(key: String) = key in values
    override fun edit(): SharedPreferences.Editor = Editor()

    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener,
    ) = Unit

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener,
    ) = Unit

    private inner class Editor : SharedPreferences.Editor {
        // Pending changes; a null value means "remove". Later calls win, as on Android.
        private val changes = mutableMapOf<String, Any?>()
        private var clear = false

        // Like the real implementation, putting null is the same as removing the key.
        private fun put(key: String, value: Any?) = apply { changes[key] = value }

        override fun putString(key: String, value: String?) = put(key, value)
        override fun putStringSet(key: String, values: Set<String>?) = put(key, values)
        override fun putInt(key: String, value: Int) = put(key, value)
        override fun putLong(key: String, value: Long) = put(key, value)
        override fun putFloat(key: String, value: Float) = put(key, value)
        override fun putBoolean(key: String, value: Boolean) = put(key, value)
        override fun remove(key: String) = put(key, null)
        override fun clear() = apply { clear = true }

        override fun commit(): Boolean {
            if (clear) values.clear()
            changes.forEach { (key, value) ->
                if (value == null) values.remove(key) else values[key] = value
            }
            return true
        }

        override fun apply() {
            commit()
        }
    }
}
