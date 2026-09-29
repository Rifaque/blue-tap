package dev.bluetap.app.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.res.stringResource
import dev.bluetap.app.R
import dev.bluetap.app.bluetooth.AssociatedDevice
import dev.bluetap.app.ui.BlueTapTheme
import dev.bluetap.app.ui.DeviceSetupScreen

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
        setContent {
            BlueTapTheme {
                DeviceSetupScreen(
                    title = stringResource(R.string.config_title),
                    description = stringResource(R.string.config_description),
                    onDeviceSelected = ::assignDevice,
                    onDeviceAssociated = ::assignDevice,
                )
            }
        }
    }

    private suspend fun assignDevice(device: AssociatedDevice) {
        WidgetConfigStore.from(this).save(appWidgetId, device)
        refreshWidget(this, appWidgetId)
        configured = true
        setResult(RESULT_OK, resultIntent())
        finish()
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
