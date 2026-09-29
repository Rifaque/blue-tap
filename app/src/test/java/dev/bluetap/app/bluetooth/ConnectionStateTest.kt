package dev.bluetap.app.bluetooth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConnectionStateTest {

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
