package dev.bluetap.app.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.bluetap.app.R
import dev.bluetap.app.bluetooth.BondedDevice
import dev.bluetap.app.bluetooth.BondedDeviceProvider
import dev.bluetap.app.bluetooth.BondedDeviceResult
import kotlinx.coroutines.launch

/** Lists paired Bluetooth devices. Widget configuration supplies the selection callback. */
@Composable
fun DeviceSetupScreen(
    title: String,
    description: String,
    onDeviceSelected: (suspend (BondedDevice) -> Unit)?,
    onDevicesRefreshed: suspend () -> Unit,
    footer: String? = null,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val provider = remember(context) { BondedDeviceProvider(context) }
    var result by remember { mutableStateOf(provider.currentDevices()) }
    var permissionRequested by rememberSaveable { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val latestOnRefreshed by rememberUpdatedState(onDevicesRefreshed)
    val refresh = {
        result = provider.currentDevices()
        scope.launch { latestOnRefreshed() }
        Unit
    }
    val latestRefresh by rememberUpdatedState(refresh)
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { latestRefresh() }

    LaunchedEffect(Unit) {
        if (result == BondedDeviceResult.PermissionRequired && !permissionRequested) {
            permissionRequested = true
            permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        }
    }
    // Reload after pairing/unpairing, toggling Bluetooth, or changing permission in Settings.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) latestRefresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val openSettings: (Intent) -> Unit = { intent ->
        try {
            context.startActivity(intent)
            message = null
        } catch (e: ActivityNotFoundException) {
            message = context.getString(R.string.settings_unavailable)
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.headlineMedium)
            Text(text = description, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = stringResource(R.string.devices_heading),
                style = MaterialTheme.typography.titleMedium,
            )

            when (val current = result) {
                is BondedDeviceResult.Available -> {
                    if (current.devices.isEmpty()) {
                        Text(
                            text = stringResource(R.string.devices_empty),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    current.devices.forEach { device ->
                        ListItem(
                            headlineContent = { Text(device.name) },
                            supportingContent = { Text(device.macAddress) },
                            modifier = if (onDeviceSelected != null) {
                                Modifier.clickable { scope.launch { onDeviceSelected(device) } }
                            } else Modifier,
                        )
                    }
                }
                BondedDeviceResult.PermissionRequired -> {
                    Text(stringResource(R.string.bluetooth_permission_required))
                    Button(onClick = {
                        permissionRequested = true
                        permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                    }) {
                        Text(stringResource(R.string.grant_bluetooth_permission))
                    }
                    Button(onClick = {
                        openSettings(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:${context.packageName}")))
                    }) {
                        Text(stringResource(R.string.app_settings))
                    }
                }
                BondedDeviceResult.BluetoothOff -> Text(stringResource(R.string.bluetooth_off))
                BondedDeviceResult.Unsupported -> Text(stringResource(R.string.bluetooth_unsupported))
                BondedDeviceResult.Unavailable -> Text(stringResource(R.string.devices_unavailable))
            }

            if (result != BondedDeviceResult.PermissionRequired &&
                result != BondedDeviceResult.Unsupported
            ) {
                Button(onClick = refresh) { Text(stringResource(R.string.refresh_devices)) }
                Button(onClick = { openSettings(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }) {
                    Text(stringResource(R.string.bluetooth_settings))
                }
            }
            message?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }
            footer?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
