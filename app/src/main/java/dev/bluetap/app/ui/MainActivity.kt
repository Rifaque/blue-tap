package dev.bluetap.app.ui

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.bluetap.app.widget.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val prefs = remember { getSharedPreferences("app_settings", MODE_PRIVATE) }
            var dynamic by rememberSaveable { mutableStateOf(prefs.safeBoolean("dynamic_color", false)) }
            var refreshed by remember { mutableIntStateOf(0) }
            BlueTapTheme(dynamicColor = dynamic) {
                DeviceSetupScreen(
                    title = "BlueTap", description = "One-tap Bluetooth widgets",
                    onDeviceSelected = null,
                    onDevicesRefreshed = { refreshed++; refreshAllWidgets(this@MainActivity) },
                    afterDevices = {
                        WidgetManagement(refreshed)
                        Row(Modifier.fillMaxWidth().padding(top = 12.dp).heightIn(min = 48.dp)
                            .toggleable(value = dynamic, role = Role.Switch, onValueChange = {
                                dynamic = it; prefs.edit().putBoolean("dynamic_color", it).apply()
                            }), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Use wallpaper colors", style = MaterialTheme.typography.titleSmall)
                                Text("Material You for the app", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(dynamic, onCheckedChange = null)
                        }
                        Text("One-tap connection control will be available on Android 17. Tap a widget to edit it.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                )
            }
        }
    }
}

@Composable
private fun WidgetManagement(refreshed: Int) {
    val context = LocalContext.current
    val manager = AppWidgetManager.getInstance(context)
    val provider = ComponentName(context, BlueTapWidgetReceiver::class.java)
    val ids = remember(refreshed) { manager.getAppWidgetIds(provider).toList() }
    val store = WidgetConfigStore.from(context)
    Column(Modifier.fillMaxWidth().padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Widgets", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
            FilledTonalButton(onClick = {
                val supported = manager.isRequestPinAppWidgetSupported
                val requested = if (supported) try { manager.requestPinAppWidget(provider, null, null) }
                    catch (_: IllegalStateException) { false }
                    catch (_: SecurityException) { false } else false
                Toast.makeText(context, if (requested) "Add the widget, then tap it to choose a device."
                    else "Long-press your home screen, choose Widgets, then BlueTap.", Toast.LENGTH_LONG).show()
            }) { Text("+ Add widget") }
        }
        if (ids.isEmpty()) Text("Add a widget, then choose its device and style.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ids.forEachIndexed { index, id ->
            val device = store.load(id)
            val appearance = store.loadAppearance(id)
            Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth().clickable { context.startActivity(widgetEditorIntent(context, id)) }) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(device?.name ?: "Choose a device", style = MaterialTheme.typography.titleMedium, maxLines = 2)
                        Text("Widget ${index + 1}  /  ${appearance.preset.title}", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("Edit", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
