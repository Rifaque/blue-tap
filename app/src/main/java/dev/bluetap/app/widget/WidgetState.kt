package dev.bluetap.app.widget

import dev.bluetap.app.bluetooth.AssociatedDevice

/** What a BlueTap widget should show. */
sealed interface WidgetState {
    /** No device has been assigned to the widget. */
    data object NotConfigured : WidgetState

    /** The assigned device's association still exists. */
    data class Ready(val device: AssociatedDevice) : WidgetState

    /** A device was assigned, but its association no longer exists or cannot be checked. */
    data class Unavailable(val device: AssociatedDevice) : WidgetState
}

/**
 * @param saved The device saved for the widget, if any.
 * @param currentAssociations BlueTap's current associations, or `null` if they
 *  cannot be determined (Companion Device Manager unavailable).
 */
fun resolveWidgetState(
    saved: AssociatedDevice?,
    currentAssociations: List<AssociatedDevice>?,
): WidgetState = when {
    saved == null -> WidgetState.NotConfigured
    currentAssociations.orEmpty().any { it.isSameAssociationAs(saved) } -> WidgetState.Ready(saved)
    else -> WidgetState.Unavailable(saved)
}
