package dev.bluetap.app.bluetooth

import android.bluetooth.BluetoothProfile

/** Connection state of a [PairedDevice], independent of any specific Android API. */
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

    companion object {
        /** Maps a `BluetoothProfile.STATE_*` value to a [ConnectionState]. */
        fun fromProfileState(state: Int): ConnectionState = when (state) {
            BluetoothProfile.STATE_DISCONNECTED -> Disconnected
            BluetoothProfile.STATE_CONNECTING -> Connecting
            BluetoothProfile.STATE_CONNECTED -> Connected
            BluetoothProfile.STATE_DISCONNECTING -> Disconnecting
            else -> Unknown
        }
    }
}

enum class ToggleAction {
    Connect,
    Disconnect,
}
