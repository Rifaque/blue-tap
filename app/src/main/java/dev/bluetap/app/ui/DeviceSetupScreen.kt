package dev.bluetap.app.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.bluetap.app.R
import dev.bluetap.app.bluetooth.AssociatedDevice
import dev.bluetap.app.bluetooth.AssociationOutcome
import dev.bluetap.app.bluetooth.CompanionDeviceAssociator
import kotlinx.coroutines.launch

/**
 * Lists BlueTap's associated devices and lets the user associate a new one through
 * the system Companion Device Manager chooser.
 *
 * @param onDeviceSelected Called when an existing device is tapped; `null` makes the
 *  list read-only.
 * @param onDeviceAssociated Called after a new association has been created.
 */
@Composable
fun DeviceSetupScreen(
    title: String,
    description: String,
    onDeviceSelected: (suspend (AssociatedDevice) -> Unit)?,
    onDeviceAssociated: suspend (AssociatedDevice) -> Unit,
    footer: String? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val associator = remember { CompanionDeviceAssociator(context) }
    var devices by remember { mutableStateOf(associator.currentAssociations().orEmpty()) }
    var message by remember { mutableStateOf<String?>(null) }

    val startAssociation = rememberAssociationLauncher(associator) { outcome ->
        message = when (outcome) {
            is AssociationOutcome.Associated -> {
                devices = associator.currentAssociations().orEmpty()
                scope.launch { onDeviceAssociated(outcome.device) }
                null
            }
            AssociationOutcome.Cancelled -> context.getString(R.string.setup_cancelled)
            is AssociationOutcome.Failed -> outcome.message
                ?.let { context.getString(R.string.setup_failed_with_reason, it) }
                ?: context.getString(R.string.setup_failed)
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

            if (!associator.isSupported) {
                Text(
                    text = stringResource(R.string.setup_not_supported),
                    color = MaterialTheme.colorScheme.error,
                )
            } else {
                Text(
                    text = stringResource(R.string.devices_heading),
                    style = MaterialTheme.typography.titleMedium,
                )
                if (devices.isEmpty()) {
                    Text(
                        text = stringResource(R.string.devices_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                devices.forEach { device ->
                    ListItem(
                        headlineContent = { Text(device.name) },
                        supportingContent = device.macAddress?.let { { Text(it) } },
                        modifier = if (onDeviceSelected != null) {
                            Modifier.clickable { scope.launch { onDeviceSelected(device) } }
                        } else {
                            Modifier
                        },
                    )
                }
                Button(onClick = startAssociation) {
                    Text(stringResource(R.string.add_device))
                }
            }

            message?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }
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

/**
 * Returns a function that starts Companion Device Manager association and reports
 * the result to [onOutcome]. On Android 12/12L it first asks for `BLUETOOTH_CONNECT`
 * (to read the device name); association continues whether or not it is granted.
 */
@Composable
private fun rememberAssociationLauncher(
    associator: CompanionDeviceAssociator,
    onOutcome: (AssociationOutcome) -> Unit,
): () -> Unit {
    val latestOnOutcome by rememberUpdatedState(onOutcome)
    val chooserLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        latestOnOutcome(associator.parseResult(result.resultCode, result.data))
    }
    val startAssociation = {
        associator.startAssociation(
            onChooserReady = { chooserLauncher.launch(IntentSenderRequest.Builder(it).build()) },
            onError = { latestOnOutcome(AssociationOutcome.Failed(it)) },
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { startAssociation() }

    return {
        if (associator.shouldRequestConnectPermission()) {
            permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            startAssociation()
        }
    }
}
