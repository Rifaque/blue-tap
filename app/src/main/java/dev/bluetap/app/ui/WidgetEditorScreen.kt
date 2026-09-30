package dev.bluetap.app.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.bluetap.app.bluetooth.*
import dev.bluetap.app.widget.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.res.stringResource
import dev.bluetap.app.R

@Composable
fun WidgetEditorScreen(initialDevice: BondedDevice?, initialAppearance: WidgetAppearance,
    onSave: suspend (BondedDevice, WidgetAppearance) -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    var device by rememberSaveable { mutableStateOf(initialDevice) }
    var appearance by rememberSaveable { mutableStateOf(initialAppearance) }
    var choosing by rememberSaveable { mutableStateOf(initialDevice == null) }
    var previewSize by rememberSaveable { mutableIntStateOf(2) }
    val systemDark = isSystemInDarkTheme()
    var previewDark by remember { mutableStateOf(systemDark) }
    var details by remember { mutableStateOf(false) }
    var previewConnection by remember { mutableStateOf(WidgetConnectionState.UNKNOWN) }
    var importing by remember { mutableStateOf(false) }
    var pendingArtworkAddress by rememberSaveable { mutableStateOf<String?>(null) }
    var artworkVersion by remember { mutableIntStateOf(0) }
    val artworkStore = remember { DeviceArtworkStore.from(context) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var available by remember { mutableStateOf(BondedDeviceProvider(context).currentDevices()) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) available = BondedDeviceProvider(context).currentDevices()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val scope = rememberCoroutineScope()
    val artworkPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val address = pendingArtworkAddress
        pendingArtworkAddress = null
        if (uri != null && address != null) {
            importing = true
            scope.launch {
                try {
                    withContext(Dispatchers.IO) { artworkStore.import(context, address, uri) }
                    if (device?.macAddress == address) appearance = appearance.copy(showArtwork = true, artworkSource = ArtworkSource.DEVICE)
                    artworkVersion++
                    error = if (refreshAllWidgets(context)) null else context.getString(R.string.artwork_refresh_failed)
                } catch (e: CancellationException) { throw e }
                catch (_: Exception) { error = context.getString(R.string.artwork_import_failed) }
                finally { importing = false }
            }
        }
    }
    val artworkSource = remember(device, artworkVersion, appearance.artworkSource, appearance.showArtwork) {
        artworkStore.resolve(device, appearance)
    }
    val artwork by produceState<android.graphics.Bitmap?>(null, artworkSource, artworkVersion) {
        value = null
        value = withContext(Dispatchers.IO) { artworkStore.bitmap(artworkSource) }
    }
    BackHandler(choosing && device != null) { choosing = false }
    BackHandler(saving || importing) { /* Finish the short storage operation before leaving. */ }
    if (choosing) {
        DeviceSetupScreen("Select device", "Choose the device for this widget", onDeviceSelected = {
            device = it; choosing = false; available = BondedDeviceProvider(context).currentDevices()
        }, onDevicesRefreshed = { refreshAllWidgets(context) }, afterDevices = {
            TextButton(onClick = { if (device == null) onCancel() else choosing = false }) { Text(stringResource(R.string.cancel)) }
        })
        return
    }
    val unavailable = (available as? BondedDeviceResult.Available)?.devices
        ?.none { it.macAddress.equals(device?.macAddress, true) } ?: true
    Scaffold(bottomBar = {
        Surface(shadowElevation = 6.dp) {
            Column(Modifier.navigationBarsPadding().padding(horizontal = 24.dp, vertical = 12.dp)) {
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(onClick = {
                    device?.let { selected ->
                        saving = true
                        scope.launch {
                            try { onSave(selected, appearance) }
                            catch (e: CancellationException) { throw e }
                            catch (_: Exception) { error = context.getString(R.string.widget_save_failed); saving = false }
                        }
                    }
                }, enabled = device != null && !saving && !importing, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)) {
                    Text(stringResource(if (saving) R.string.saving else R.string.save_widget))
                }
            }
        }
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Your widget", style = MaterialTheme.typography.headlineSmall)
                        Text("Make a little space for your device.", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = onCancel, enabled = !saving && !importing) { Text(stringResource(R.string.cancel)) }
                }
            }
            item {
                device?.let { DeviceRow(it, onClick = { choosing = true }) }
                TextButton(onClick = { choosing = true }) { Text(stringResource(R.string.change_device)) }
                if (unavailable) Text("This device is unavailable. Keep its assignment or choose another paired device.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                if (previewConnection != WidgetConnectionState.UNKNOWN) Text("Visual demo · ${previewConnection.label}",
                    style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 8.dp))
                Surface(shape = RoundedCornerShape(28.dp), color = if (previewDark) Color(0xFF303A4B) else Color(0xFFDDE5EF)) {
                    BoxWithConstraints(Modifier.fillMaxWidth().height(264.dp).padding(12.dp), contentAlignment = Alignment.Center) {
                        val width = minOf(listOf(160f, 260f, 170f, 290f, 260f)[previewSize], maxWidth.value)
                        val height = listOf(56f, 64f, 170f, 170f, 232f)[previewSize]
                        WidgetPreview(device, appearance, width, height, previewDark, unavailable, artwork = artwork,
                            previewStatus = if (previewConnection == WidgetConnectionState.UNKNOWN) null else WidgetDeviceStatus(previewConnection),
                            builtInArtwork = (artworkSource as? DeviceArtwork.BuiltIn)?.resource)
                    }
                }
                ChoiceRow(listOf(0, 1, 2, 3, 4), previewSize, { listOf("2×1", "3×1", "2×2", "Wide", "Large")[it] }) { previewSize = it }
                Text("Preview shapes are examples; launcher sizes vary.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Preview background", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
                SingleChoiceSegmentedButtonRow {
                    listOf(false, true).forEachIndexed { index, value ->
                        SegmentedButton(selected = previewDark == value, onClick = { previewDark = value },
                            shape = SegmentedButtonDefaults.itemShape(index, 2)) { Text(if (value) "Dark" else "Light") }
                    }
                }
            }
            item {
                Text("Choose a style", style = MaterialTheme.typography.titleLarge)
                Text(appearance.preset.description, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(end = 16.dp)) {
                    items(WidgetPreset.entries) { preset ->
                        val selected = appearance.preset == preset
                        OutlinedCard(modifier = Modifier.semantics { this.selected = selected }, onClick = { appearance = appearanceForPreset(preset).copy(
                            showBattery = appearance.showBattery, showArtwork = appearance.showArtwork, artworkSource = appearance.artworkSource) },
                            border = androidx.compose.foundation.BorderStroke(if (selected) 2.dp else 1.dp,
                                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) {
                            Column(Modifier.padding(8.dp)) {
                                WidgetPreview(device, appearanceForPreset(preset), 150f, 180f, previewDark, unavailable, artwork = artwork)
                                Text(preset.title, Modifier.padding(8.dp), style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
            if (appearance.preset != WidgetPreset.DOT_MONO) item {
                Text("Accent", style = MaterialTheme.typography.titleMedium)
                ChoiceRow(WidgetAccent.entries, appearance.accent, { it.title }) { appearance = appearance.copy(accent = it) }
            }
            item {
                Text("Surface", style = MaterialTheme.typography.titleMedium)
                if (presetKeepsBlackSurface(appearance.preset)) {
                    Text(if (appearance.preset == WidgetPreset.OLED) "OLED keeps a pure black surface." else "Dot Mono keeps a monochrome dark surface.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    val modes = if (appearance.preset in setOf(WidgetPreset.OUTLINE, WidgetPreset.DOT_MONO))
                        WidgetBackground.entries.filter { it != WidgetBackground.CUSTOM } else WidgetBackground.entries
                    ChoiceRow(modes, appearance.background, { it.title }) { appearance = appearance.copy(background = it) }
                    if (appearance.background == WidgetBackground.CUSTOM) {
                        val swatches = listOf(0xFF172554.toInt(), 0xFF123E36.toInt(), 0xFF3E214D.toInt(), 0xFFE7EDF5.toInt())
                        ChoiceRow(swatches, appearance.customBackground,
                            { listOf("Midnight", "Pine", "Plum", "Mist")[swatches.indexOf(it)] }) {
                            appearance = appearance.copy(customBackground = it)
                        }
                    }
                }
            }
            item {
                Text("Text alignment", style = MaterialTheme.typography.titleMedium)
                ChoiceRow(WidgetAlignment.entries, appearance.alignment, { it.title }) { appearance = appearance.copy(alignment = it) }
            }
            item {
                TextButton(onClick = { details = !details }, contentPadding = PaddingValues(0.dp)) {
                    Text(if (details) "Details −" else "Details +", style = MaterialTheme.typography.titleMedium)
                }
            }
            if (details) item {
                ToggleRow("BlueTap label", appearance.showLabel) { appearance = appearance.copy(showLabel = it) }
                ToggleRow("Device name", appearance.showDeviceName) { appearance = appearance.copy(showDeviceName = it) }
                ToggleRow("Secondary status", appearance.showStatus) { appearance = appearance.copy(showStatus = it) }
                ToggleRow("Compact layout", appearance.compact) { appearance = appearance.copy(compact = it) }
                ToggleRow("Show battery", appearance.showBattery) { appearance = appearance.copy(showBattery = it) }
                Text("Battery appears only when a supported source supplies a real reading. No reading is available in this build.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                HorizontalDivider(Modifier.padding(vertical = 16.dp))
                Text("Device artwork", style = MaterialTheme.typography.titleMedium)
                val custom = remember(device, artworkVersion) { device?.let { artworkStore.custom(it.macAddress) } != null }
                Text(if (custom) "Custom · shared by widgets for this device" else "Automatic · built-in artwork or a generic silhouette",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row {
                    TextButton(onClick = {
                        pendingArtworkAddress = device?.macAddress
                        try { artworkPicker.launch(arrayOf("image/*")) }
                        catch (_: android.content.ActivityNotFoundException) { error = context.getString(R.string.no_image_picker) }
                    }, enabled = !importing && !saving) { Text(if (importing) "Updating image…" else "Change image") }
                    if (custom) TextButton(onClick = {
                        device?.let { selected ->
                            importing = true
                            scope.launch {
                            try {
                                withContext(Dispatchers.IO) { artworkStore.remove(selected.macAddress) }
                                artworkVersion++
                                error = if (refreshAllWidgets(context)) null else context.getString(R.string.artwork_refresh_failed)
                            } catch (e: CancellationException) { throw e }
                            catch (_: Exception) { error = context.getString(R.string.artwork_remove_failed) }
                            finally { importing = false }
                        } }
                    }, enabled = !importing && !saving) { Text("Remove image") }
                }
                Text("Image changes apply immediately to all widgets using this device image. Transparent PNG or WebP works best.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ToggleRow("Show artwork", appearance.showArtwork) { appearance = appearance.copy(showArtwork = it) }
                ToggleRow("Use device image", appearance.artworkSource == ArtworkSource.DEVICE) {
                    appearance = appearance.copy(artworkSource = if (it) ArtworkSource.DEVICE else ArtworkSource.GENERIC)
                }
                HorizontalDivider(Modifier.padding(vertical = 16.dp))
                Text("State preview", style = MaterialTheme.typography.titleMedium)
                Text("Visual demo only. This does not read or change your device's connection.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ChoiceRow(WidgetConnectionState.entries, previewConnection,
                    { if (it == WidgetConnectionState.UNKNOWN) "Current" else it.label }) { previewConnection = it }
                Text("Labels give way to artwork and useful status at smaller sizes. Device names can use two lines.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun <T> ChoiceRow(values: List<T>, selected: T, label: (T) -> String, choose: (T) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(end = 16.dp)) {
        items(values) { value -> FilterChip(selected == value, { choose(value) }, label = { Text(label(value)) }) }
    }
}
@Composable
private fun ToggleRow(label: String, value: Boolean, change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(value = value, role = Role.Switch, onValueChange = change),
        verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(value, onCheckedChange = null)
    }
}
