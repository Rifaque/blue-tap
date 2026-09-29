package dev.bluetap.app.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import dev.bluetap.app.R
import dev.bluetap.app.bluetooth.BondedDeviceProvider
import dev.bluetap.app.bluetooth.BondedDeviceResult

/**
 * Home-screen widget showing the device assigned to it. It does not connect to or
 * disconnect from the device yet; tapping it opens the widget's device setup.
 */
class BlueTapWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val setupAction = actionStartActivity(configureIntent(context, appWidgetId))

        provideContent {
            // Changes whenever refreshWidget() runs, so saved configuration is re-read
            // even if this composition session is still running.
            val refreshToken = currentState<Preferences>()
            val state = remember(refreshToken) { loadWidgetState(context, appWidgetId) }
            GlanceTheme {
                WidgetContent(state, setupAction)
            }
        }
    }
}

private val RefreshKey = longPreferencesKey("refresh")

/** Re-reads the widget's configuration and bonded device availability and redraws it. */
suspend fun refreshWidget(context: Context, appWidgetId: Int) {
    val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)
    refreshWidget(context, glanceId)
}

/** Redraws every BlueTap widget, e.g. after a device is unpaired in system settings. */
suspend fun refreshAllWidgets(context: Context) {
    GlanceAppWidgetManager(context).getGlanceIds(BlueTapWidget::class.java)
        .forEach { refreshWidget(context, it) }
}

private suspend fun refreshWidget(context: Context, glanceId: GlanceId) {
    updateAppWidgetState(context, glanceId) { it[RefreshKey] = System.currentTimeMillis() }
    BlueTapWidget().update(context, glanceId)
}

private fun loadWidgetState(context: Context, appWidgetId: Int): WidgetState =
    resolveWidgetState(
        saved = WidgetConfigStore.from(context).load(appWidgetId),
        bondedDevices = (BondedDeviceProvider(context).currentDevices() as? BondedDeviceResult.Available)
            ?.devices,
    )

private fun configureIntent(context: Context, appWidgetId: Int): Intent =
    Intent(context, WidgetConfigActivity::class.java)
        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        // Keeps each widget's PendingIntent distinct.
        .setIdentifier("widget-$appWidgetId")

@Composable
private fun WidgetContent(state: WidgetState, setupAction: Action) {
    val context = LocalContext.current
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(16.dp)
            .padding(12.dp)
            .clickable(setupAction),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = context.getString(R.string.app_name),
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        when (state) {
            WidgetState.NotConfigured -> {
                SecondaryText(context.getString(R.string.widget_no_device))
                Spacer(modifier = GlanceModifier.height(8.dp))
                Button(text = context.getString(R.string.widget_set_up_device), onClick = setupAction)
            }

            is WidgetState.Ready -> {
                PrimaryText(state.device.name)
                // Connection status is not implemented yet, so none is claimed.
                SecondaryText(context.getString(R.string.widget_status_not_available))
            }

            is WidgetState.Unavailable -> {
                PrimaryText(context.getString(R.string.widget_device_unavailable))
                SecondaryText(state.device.name)
                Spacer(modifier = GlanceModifier.height(8.dp))
                Button(text = context.getString(R.string.widget_set_up_again), onClick = setupAction)
            }
        }
    }
}

@Composable
private fun PrimaryText(text: String) {
    Text(
        text = text,
        maxLines = 1,
        style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp),
    )
}

@Composable
private fun SecondaryText(text: String) {
    Text(
        text = text,
        maxLines = 1,
        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp),
    )
}
