package dev.bluetap.app.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Alignment
import androidx.compose.material3.Button
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Lists paired Bluetooth devices. Widget configuration supplies the selection callback. */
@Composable
@OptIn(ExperimentalLayoutApi::class)
fun DeviceSetupScreen(
    title: String,
    description: String,
    onDeviceSelected: (suspend (BondedDevice) -> Unit)?,
    onDevicesRefreshed: suspend () -> Boolean,
    footer: String? = null,
    afterDevices: @Composable () -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val provider = remember(context) { BondedDeviceProvider(context) }
    var result by remember { mutableStateOf<BondedDeviceResult>(BondedDeviceResult.Unavailable) }
    var loading by remember { mutableStateOf(true) }
    var permissionRequested by rememberSaveable { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val latestOnRefreshed by rememberUpdatedState(onDevicesRefreshed)
    val refresh = {
        scope.launch {
            result = withContext(Dispatchers.IO) { provider.currentDevices() }
            loading = false
            message = if (latestOnRefreshed()) null else context.getString(R.string.widget_refresh_failed)
        }
        Unit
    }
    val latestRefresh by rememberUpdatedState(refresh)
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { latestRefresh() }

    LaunchedEffect(result) {
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
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = RoundedCornerShape(15.dp), color = MaterialTheme.colorScheme.primary) {
                    Icon(painterResource(R.drawable.ic_bluetap), null, Modifier.padding(10.dp).size(28.dp),
                        tint = MaterialTheme.colorScheme.onPrimary)
                }
                Column(Modifier.weight(1f)) {
                    Text(text = title, style = MaterialTheme.typography.headlineSmall)
                    Text(text = description, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(
                text = stringResource(R.string.devices_heading),
                style = MaterialTheme.typography.titleMedium,
            )

            if (loading) Text(stringResource(R.string.loading_devices), style = MaterialTheme.typography.bodyMedium)
            else when (val current = result) {
                is BondedDeviceResult.Available -> {
                    if (current.devices.isEmpty()) {
                        DeviceNotice(stringResource(R.string.no_paired_devices), stringResource(R.string.devices_empty))
                    }
                    current.devices.forEach { device ->
                        DeviceRow(device, onClick = onDeviceSelected?.let { select ->
                            { scope.launch { select(device) }; Unit }
                        })
                    }
                }
                BondedDeviceResult.PermissionRequired -> {
                    DeviceNotice(stringResource(R.string.nearby_permission_title), stringResource(R.string.bluetooth_permission_required))
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
                BondedDeviceResult.BluetoothOff -> DeviceNotice(stringResource(R.string.bluetooth_off_title), stringResource(R.string.bluetooth_off))
                BondedDeviceResult.Unsupported -> DeviceNotice(stringResource(R.string.bluetooth_unavailable_title), stringResource(R.string.bluetooth_unsupported))
                BondedDeviceResult.Unavailable -> DeviceNotice(stringResource(R.string.devices_unavailable_title), stringResource(R.string.devices_unavailable))
            }

            if (result != BondedDeviceResult.PermissionRequired &&
                result != BondedDeviceResult.Unsupported
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = refresh) { Text(stringResource(R.string.refresh)) }
                    TextButton(onClick = { openSettings(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }) {
                        Text(stringResource(R.string.bluetooth_settings))
                    }
                }
            }
            afterDevices()
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

@Composable
private fun DeviceNotice(title: String, body: String) {
    Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
