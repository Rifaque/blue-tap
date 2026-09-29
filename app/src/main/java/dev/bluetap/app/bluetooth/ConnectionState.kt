package dev.bluetap.app.bluetooth

/**
 * Connection state of an [AssociatedDevice].
 *
 * Deliberately free of Android framework types; mapping from platform values
 * belongs in the Android-specific [BluetoothConnector] implementation.
 */
enum class ConnectionState {
    Disconnected,
    Connecting,
    Connected,
    Disconnecting,

    /** The state could not be determined, e.g. Bluetooth is off or permission is missing. */
    Unknown;

    /** What a one-tap toggle should do from this state, or `null` if nothing should happen. */
    fun toggleAction(): ToggleAction? = when (this) {
        Disconnected -> ToggleAction.Connect
        Connected -> ToggleAction.Disconnect
        Connecting, Disconnecting, Unknown -> null
    }
}

enum class ToggleAction {
    Connect,
    Disconnect,
}
