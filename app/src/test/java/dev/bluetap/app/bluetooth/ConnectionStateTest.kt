package dev.bluetap.app.bluetooth

import android.bluetooth.BluetoothProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConnectionStateTest {

    @Test
    fun fromProfileState_mapsEveryProfileState() {
        assertEquals(
            ConnectionState.Disconnected,
            ConnectionState.fromProfileState(BluetoothProfile.STATE_DISCONNECTED),
        )
        assertEquals(
            ConnectionState.Connecting,
            ConnectionState.fromProfileState(BluetoothProfile.STATE_CONNECTING),
        )
        assertEquals(
            ConnectionState.Connected,
            ConnectionState.fromProfileState(BluetoothProfile.STATE_CONNECTED),
        )
        assertEquals(
            ConnectionState.Disconnecting,
            ConnectionState.fromProfileState(BluetoothProfile.STATE_DISCONNECTING),
        )
    }

    @Test
    fun fromProfileState_unrecognisedValueIsUnknown() {
        assertEquals(ConnectionState.Unknown, ConnectionState.fromProfileState(-1))
        assertEquals(ConnectionState.Unknown, ConnectionState.fromProfileState(42))
    }

    @Test
    fun toggleAction_connectsWhenDisconnectedAndDisconnectsWhenConnected() {
        assertEquals(ToggleAction.Connect, ConnectionState.Disconnected.toggleAction())
        assertEquals(ToggleAction.Disconnect, ConnectionState.Connected.toggleAction())
    }

    @Test
    fun toggleAction_doesNothingWhileTransitioningOrUnknown() {
        assertNull(ConnectionState.Connecting.toggleAction())
        assertNull(ConnectionState.Disconnecting.toggleAction())
        assertNull(ConnectionState.Unknown.toggleAction())
    }
}
