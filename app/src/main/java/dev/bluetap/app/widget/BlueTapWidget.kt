package dev.bluetap.app.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.*
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.color.ColorProvider
import androidx.glance.layout.*
import androidx.glance.text.*
import dev.bluetap.app.R
import dev.bluetap.app.bluetooth.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BlueTapWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val action = actionStartActivity(widgetEditorIntent(context, appWidgetId))
        val fontScale = context.resources.configuration.fontScale
        provideContent {
            val token = currentState<Preferences>()
            val snapshot by produceState<WidgetSnapshot?>(null, token) {
                value = withContext(Dispatchers.IO) { loadWidgetSnapshot(context, appWidgetId) }
            }
            GlanceTheme {
                val loaded = snapshot
                if (loaded != null) WidgetContent(loaded.state, loaded.appearance, action, loaded.bitmap, loaded.artwork, fontScale)
                else Box(GlanceModifier.fillMaxSize().appWidgetBackground().background(GlanceTheme.colors.background)
                    .clickable(action), contentAlignment = Alignment.Center) {
                    Text(context.getString(R.string.app_name), style = TextStyle(color = GlanceTheme.colors.onBackground))
                }
            }
        }
    }
}

private data class WidgetSnapshot(val state: WidgetState, val appearance: WidgetAppearance,
    val artwork: DeviceArtwork, val bitmap: Bitmap?)

/** Glance composition runs on main; preferences, Bluetooth IPC and disk decoding do not. */
private fun loadWidgetSnapshot(context: Context, id: Int): WidgetSnapshot {
    val store = WidgetConfigStore.from(context)
    val state = resolveWidgetState(store.load(id),
        (BondedDeviceProvider(context).currentDevices() as? BondedDeviceResult.Available)?.devices)
    val appearance = store.loadAppearance(id)
    val device = when (state) {
        is WidgetState.Ready -> state.device
        is WidgetState.Unavailable -> state.device
        WidgetState.NotConfigured -> null
    }
    val artworkStore = DeviceArtworkStore.from(context)
    val artwork = artworkStore.resolve(device, appearance)
    return WidgetSnapshot(state, appearance, artwork, artworkStore.bitmap(artwork))
}

private val RefreshKey = longPreferencesKey("refresh")
suspend fun refreshWidget(context: Context, appWidgetId: Int): Boolean = try {
    val id = GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)
    updateAppWidgetState(context, id) { it[RefreshKey] = (it[RefreshKey] ?: 0L) + 1L }
    BlueTapWidget().update(context, id)
    true
} catch (e: CancellationException) { throw e }
catch (_: Exception) { false } // Deleted ID / launcher failure: callers can offer a refresh retry.

suspend fun refreshAllWidgets(context: Context): Boolean = try {
    val manager = GlanceAppWidgetManager(context)
    var success = true
    manager.getGlanceIds(BlueTapWidget::class.java).forEach {
        if (!refreshWidget(context, manager.getAppWidgetId(it))) success = false
    }
    success
} catch (e: CancellationException) { throw e }
catch (_: Exception) { false }
fun widgetEditorIntent(context: Context, appWidgetId: Int) =
    Intent(context, WidgetConfigActivity::class.java)
        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        .setIdentifier("widget-$appWidgetId")

@Composable
private fun WidgetContent(state: WidgetState, appearance: WidgetAppearance, action: Action,
    artwork: Bitmap?, source: DeviceArtwork, fontScale: Float,
    status: WidgetDeviceStatus = WidgetDeviceStatus(if (state is WidgetState.Unavailable)
        WidgetConnectionState.UNAVAILABLE else WidgetConnectionState.UNKNOWN)) {
    val context = LocalContext.current
    val size = LocalSize.current
    val device = when (state) {
        is WidgetState.Ready -> state.device
        is WidgetState.Unavailable -> state.device
        WidgetState.NotConfigured -> null
    }
    val title = device?.name ?: "Choose a device"
    val density = context.resources.displayMetrics.density
    val measure = remember(density, fontScale, appearance.preset) { rowTextMeasurer(density, fontScale, appearance.preset) }
    // There is deliberately no connection/battery source yet. Bonding is not connection state.
    // Future integration point: supply WidgetDeviceStatus from verified public state/battery
    // sources here; leave battery null until a real source exists (see DEVELOPMENT_STATUS.md).
    val r = widgetLayoutRules(size.width.value, size.height.value, appearance, status, state != WidgetState.NotConfigured,
        fontScale, title, measure)
    val day = widgetPalette(context, appearance, false)
    val night = widgetPalette(context, appearance, true)
    fun color(select: (WidgetPalette) -> Color) = ColorProvider(day = select(day), night = select(night))
    val dynamic = appearance.preset == WidgetPreset.MATERIAL_YOU && appearance.background == WidgetBackground.AUTO
    val backgroundColor = if (dynamic) GlanceTheme.colors.secondaryContainer else color { it.background }
    val textColor = if (dynamic) GlanceTheme.colors.onSecondaryContainer else color { it.text }
    val secondaryColor = if (dynamic) GlanceTheme.colors.onSurfaceVariant else color { it.secondary }
    val accentColor = if (dynamic && appearance.accent == WidgetAccent.SYSTEM) GlanceTheme.colors.primary else color { it.accent }
    val alignment = when (appearance.alignment) {
        WidgetAlignment.START -> Alignment.Start
        WidgetAlignment.CENTER -> Alignment.CenterHorizontally
        WidgetAlignment.END -> Alignment.End
    }
    val textAlign = when (appearance.alignment) {
        WidgetAlignment.START -> TextAlign.Start
        WidgetAlignment.CENTER -> TextAlign.Center
        WidgetAlignment.END -> TextAlign.End
    }
    val family = if (appearance.preset == WidgetPreset.DOT_MONO) FontFamily.Monospace else FontFamily.SansSerif
    val artworkAlpha = status.artworkAlpha()
    val displayedBitmap = remember(artwork, artworkAlpha) {
        if (artwork == null || artworkAlpha == 1f) artwork else {
            Bitmap.createBitmap(artwork.width, artwork.height, Bitmap.Config.ARGB_8888).also { faded ->
                android.graphics.Canvas(faded).drawBitmap(artwork, 0f, 0f,
                    android.graphics.Paint().apply { alpha = (artworkAlpha * 255).toInt() })
            }
        }
    }
    var surface = GlanceModifier.fillMaxSize().appWidgetBackground()
        .background(backgroundColor).cornerRadius(widgetRadius(appearance.preset).dp)
    if (appearance.preset == WidgetPreset.OUTLINE) {
        val outline = when (appearance.background) {
            WidgetBackground.LIGHT -> R.drawable.widget_outline_light
            WidgetBackground.DARK -> R.drawable.widget_outline_dark
            else -> R.drawable.widget_outline
        }
        surface = surface.background(ImageProvider(outline))
    }
    @Composable fun Artwork() {
        if (appearance.showArtwork && r.artworkSize > 0) {
            val material = appearance.preset == WidgetPreset.MATERIAL_YOU
            Box(GlanceModifier.size(r.artworkSize.dp).then(if (material) GlanceModifier
                .background(color { it.badge }).cornerRadius(100.dp).padding(6.dp) else GlanceModifier), contentAlignment = Alignment.Center) {
            if (displayedBitmap != null) {
                Image(ImageProvider(displayedBitmap), if (r.showName) null else title, GlanceModifier.fillMaxSize(), contentScale = ContentScale.Fit)
            } else {
                val resource = (source as? DeviceArtwork.BuiltIn)?.resource ?: deviceArtworkResource(artworkKind(device))
                Image(ImageProvider(resource), if (r.showName) null else title, GlanceModifier.fillMaxSize(),
                    contentScale = ContentScale.Fit, colorFilter = if (source is DeviceArtwork.BuiltIn) null
                    else ColorFilter.tint(color { it.accent.copy(alpha = artworkAlpha) }))
            }
            }
        }
    }
    @Composable fun Status() {
        Column(horizontalAlignment = alignment) {
            if (status.connection in setOf(WidgetConnectionState.CONNECTING, WidgetConnectionState.DISCONNECTING)) {
                // Static in RemoteViews. No animation timers or background update loops.
                Row {
                    repeat(3) {
                        Box(GlanceModifier.size(4.dp).background(accentColor).cornerRadius(2.dp)) {}
                        if (it < 2) Spacer(GlanceModifier.width(5.dp))
                    }
                }
                Spacer(GlanceModifier.height(3.dp))
            }
            r.status?.let {
                Text(it, maxLines = 2, style = TextStyle(color = secondaryColor, fontSize = 11.sp, textAlign = textAlign))
            }
            r.battery?.let {
                Spacer(GlanceModifier.height(2.dp))
                Text(it, maxLines = 1, style = TextStyle(color = textColor, fontSize = 11.sp, fontFamily = family))
            }
        }
    }
    @Composable fun Identity(modifier: GlanceModifier = GlanceModifier) {
        val row = r.row
        if (row != null) {
            Column(modifier, horizontalAlignment = alignment) {
                if (row.showName) Box(GlanceModifier.fillMaxWidth().height(row.nameHeight.dp), contentAlignment = Alignment.Center) {
                    Text(row.name, maxLines = row.nameMaxLines, modifier = GlanceModifier.fillMaxWidth(),
                        style = TextStyle(color = textColor, fontSize = row.nameTextSize.sp, fontFamily = family,
                            fontWeight = if (appearance.preset == WidgetPreset.DOT_MONO) FontWeight.Bold else FontWeight.Medium,
                            textAlign = textAlign))
                }
                row.secondary?.let {
                    if (row.showName) Spacer(GlanceModifier.height(row.textGap.dp))
                    Box(GlanceModifier.fillMaxWidth().height(row.statusHeight.dp), contentAlignment = Alignment.Center) {
                        Text(it, maxLines = 1, modifier = GlanceModifier.fillMaxWidth(),
                            style = TextStyle(color = secondaryColor, fontSize = row.statusTextSize.sp, textAlign = textAlign))
                    }
                }
            }
            return
        }
        Column(modifier, horizontalAlignment = alignment) {
            if (r.showName) {
                Text(title, maxLines = r.nameLines, modifier = GlanceModifier.fillMaxWidth(),
                    style = TextStyle(color = textColor, fontSize = r.nameSize.sp,
                        fontWeight = if (appearance.preset == WidgetPreset.DOT_MONO) FontWeight.Bold else FontWeight.Medium,
                        fontFamily = family, textAlign = textAlign))
                Spacer(GlanceModifier.height(3.dp))
            }
            Status()
        }
    }
    val contentPadding = r.row?.let { GlanceModifier.padding(horizontal = it.horizontalPadding.dp, vertical = it.verticalPadding.dp) }
        ?: GlanceModifier.padding(r.padding.dp)
    Column(surface.clickable(action).then(contentPadding),
        horizontalAlignment = alignment, verticalAlignment = Alignment.CenterVertically) {
        if (r.showLabel) {
            Text("BlueTap", style = TextStyle(color = accentColor, fontSize = 10.sp, fontFamily = family))
            Spacer(GlanceModifier.height(6.dp))
        }
        if (r.horizontal) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Artwork()
                if (appearance.showArtwork && r.artworkSize > 0) Spacer(GlanceModifier.width(r.gap.dp))
                Identity(GlanceModifier.defaultWeight())
            }
        } else {
            Box(GlanceModifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Artwork() }
            if (appearance.showArtwork && r.artworkSize > 0) Spacer(GlanceModifier.height(r.gap.dp))
            Identity(GlanceModifier.fillMaxWidth())
        }
    }
}
