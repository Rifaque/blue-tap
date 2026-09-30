package dev.bluetap.app.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.bluetap.app.bluetooth.BondedDevice
import dev.bluetap.app.ui.BlueTapTheme
import dev.bluetap.app.ui.WidgetEditorScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * App widget configuration activity. Opened by the launcher when a BlueTap widget
 * is added, and by tapping a widget to change its device.
 *
 * Returns `RESULT_OK` only after a device has been saved for the widget.
 */
class WidgetConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var wasConfigured = false
    private var configured = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appWidgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        // If the user backs out, the launcher discards the new widget.
        setResult(RESULT_CANCELED, resultIntent())
        if (!isBlueTapWidget(appWidgetId)) {
            appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
            finish()
            return
        }
        wasConfigured = WidgetConfigStore.from(this).load(appWidgetId) != null

        enableEdgeToEdge()
        val store = WidgetConfigStore.from(this)
        setContent {
            BlueTapTheme(dynamicColor = getSharedPreferences("app_settings", MODE_PRIVATE).safeBoolean("dynamic_color", false)) {
                WidgetEditorScreen(
                    initialDevice = store.load(appWidgetId),
                    initialAppearance = store.loadAppearance(appWidgetId),
                    onSave = ::assignDevice,
                    onCancel = { finish() },
                )
            }
        }
    }

    private suspend fun assignDevice(device: BondedDevice, appearance: WidgetAppearance) {
        check(isBlueTapWidget(appWidgetId)) { "Widget was removed while editing" }
        withContext(Dispatchers.IO) { WidgetConfigStore.from(this@WidgetConfigActivity).saveConfiguration(appWidgetId, device, appearance) }
        configured = true
        setResult(RESULT_OK, resultIntent())
        // A launcher can remove the widget between saving and rendering. The committed save
        // is still successful; never report it as an unsaved editor change.
        try { refreshWidget(this, appWidgetId) } finally { finish() }
    }

    override fun onDestroy() {
        // Cancelled while adding a new widget: make sure nothing is left behind.
        // Cancelling a reconfiguration keeps the existing device.
        if (isFinishing && !configured && !wasConfigured &&
            appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID
        ) {
            WidgetConfigStore.from(this).delete(appWidgetId)
        }
        super.onDestroy()
    }

    private fun resultIntent() =
        Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)

    /** Rejects IDs that do not belong to a BlueTap widget, since this activity is exported. */
    private fun isBlueTapWidget(appWidgetId: Int): Boolean {
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return false
        val info = AppWidgetManager.getInstance(this).getAppWidgetInfo(appWidgetId)
        return info?.provider == ComponentName(this, BlueTapWidgetReceiver::class.java)
    }
}
