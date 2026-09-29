package dev.bluetap.app.bluetooth

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.companion.AssociationInfo
import android.companion.AssociationRequest
import android.companion.BluetoothDeviceFilter
import android.companion.CompanionDeviceManager
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import dev.bluetap.app.R

/** Result of the system device chooser. */
sealed interface AssociationOutcome {
    data class Associated(val device: AssociatedDevice) : AssociationOutcome
    data object Cancelled : AssociationOutcome
    data class Failed(val message: String?) : AssociationOutcome
}

/**
 * Wraps Android's [CompanionDeviceManager]: starts the system device chooser,
 * reads its result and lists BlueTap's existing associations.
 *
 * Only association is handled here. Nothing in this class connects to,
 * disconnects from or bonds with a device.
 *
 * API differences:
 * - Android 13+ (API 33): associations have an ID and are described by [AssociationInfo].
 * - Android 12/12L (API 31-32): associations are only identified by MAC address, and
 *   reading the chosen device's name needs the `BLUETOOTH_CONNECT` runtime permission.
 */
class CompanionDeviceAssociator(private val context: Context) {

    private val manager: CompanionDeviceManager? =
        context.getSystemService(CompanionDeviceManager::class.java)

    val isSupported: Boolean
        get() = manager != null &&
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_COMPANION_DEVICE_SETUP)

    /**
     * True on Android 12/12L when `BLUETOOTH_CONNECT` has not been granted yet. It is
     * only used to read the chosen device's name; association works without it.
     */
    fun shouldRequestConnectPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU && !hasConnectPermission()

    /**
     * Asks the system to prepare its device chooser. [onChooserReady] receives an
     * [IntentSender] that must be launched for a result, which is then passed to
     * [parseResult]. [onError] is called if the chooser cannot be shown.
     */
    fun startAssociation(onChooserReady: (IntentSender) -> Unit, onError: (String?) -> Unit) {
        val manager = manager
        if (manager == null || !isSupported) {
            onError(null)
            return
        }

        // A filter without constraints limits the chooser to classic Bluetooth devices.
        val request = AssociationRequest.Builder()
            .addDeviceFilter(BluetoothDeviceFilter.Builder().build())
            .setSingleDevice(false)
            .build()

        // The association itself is read from the chooser's activity result on every
        // API level, so onAssociationCreated is intentionally not overridden.
        val callback = object : CompanionDeviceManager.Callback() {
            // Android 13+.
            override fun onAssociationPending(intentSender: IntentSender) =
                onChooserReady(intentSender)

            // Android 12/12L.
            @Suppress("OVERRIDE_DEPRECATION")
            override fun onDeviceFound(intentSender: IntentSender) = onChooserReady(intentSender)

            override fun onFailure(error: CharSequence?) = onError(error?.toString())
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                manager.associate(request, context.mainExecutor, callback)
            } else {
                @Suppress("DEPRECATION")
                manager.associate(request, callback, null)
            }
        } catch (e: RuntimeException) {
            onError(e.message)
        }
    }

    /** Converts the device chooser's activity result into an [AssociationOutcome]. */
    fun parseResult(resultCode: Int, data: Intent?): AssociationOutcome {
        if (resultCode != Activity.RESULT_OK) return AssociationOutcome.Cancelled

        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            @Suppress("DEPRECATION")
            data?.getParcelableExtra<AssociationInfo>(CompanionDeviceManager.EXTRA_ASSOCIATION)
                ?.toAssociatedDevice()
        } else {
            @Suppress("DEPRECATION")
            data?.getParcelableExtra<BluetoothDevice>(CompanionDeviceManager.EXTRA_DEVICE)
                ?.let { AssociatedDevice(null, it.address, nameOf(it) ?: it.address) }
        }
        return device?.let { AssociationOutcome.Associated(it) } ?: AssociationOutcome.Failed(null)
    }

    /**
     * BlueTap's current associations, or `null` if Companion Device Manager is
     * unavailable and they cannot be determined.
     */
    fun currentAssociations(): List<AssociatedDevice>? {
        val manager = manager ?: return null
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                manager.myAssociations.map { it.toAssociatedDevice() }
            } else {
                @Suppress("DEPRECATION")
                val addresses = manager.associations
                addresses.map { address ->
                    AssociatedDevice(null, address, remoteDeviceName(address) ?: address)
                }
            }
        } catch (e: RuntimeException) {
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun AssociationInfo.toAssociatedDevice(): AssociatedDevice {
        val address = deviceMacAddress?.toString()?.uppercase()
        val name = displayName?.toString()?.takeIf { it.isNotBlank() }
            ?: address
            ?: context.getString(R.string.unknown_device)
        return AssociatedDevice(associationId = id, macAddress = address, name = name)
    }

    private fun remoteDeviceName(address: String): String? = try {
        context.getSystemService(BluetoothManager::class.java)?.adapter
            ?.getRemoteDevice(address)
            ?.let(::nameOf)
    } catch (e: IllegalArgumentException) {
        null
    }

    private fun nameOf(device: BluetoothDevice): String? {
        if (!hasConnectPermission()) return null
        return try {
            device.name?.takeIf { it.isNotBlank() }
        } catch (e: SecurityException) {
            null
        }
    }

    private fun hasConnectPermission(): Boolean =
        context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED
}
