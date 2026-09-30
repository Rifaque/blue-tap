package dev.bluetap.app.ui

import android.graphics.Bitmap
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.bluetap.app.bluetooth.BondedDevice
import dev.bluetap.app.bluetooth.DeviceKind
import dev.bluetap.app.bluetooth.artworkKind
import dev.bluetap.app.widget.*

/** Separate renderer, shared palette and exact-dimension layout rules with Glance. */
@Composable
fun WidgetPreview(device: BondedDevice?, appearance: WidgetAppearance, width: Float, height: Float,
    dark: Boolean, unavailable: Boolean = false, modifier: Modifier = Modifier,
    artwork: Bitmap? = null, previewStatus: WidgetDeviceStatus? = null, builtInArtwork: Int? = null) {
    val state = previewStatus ?: WidgetDeviceStatus(if (unavailable) WidgetConnectionState.UNAVAILABLE else WidgetConnectionState.UNKNOWN)
    val density = LocalDensity.current
    val measure = remember(density.density, density.fontScale, appearance.preset) {
        rowTextMeasurer(density.density, density.fontScale, appearance.preset)
    }
    val r = widgetLayoutRules(width, height, appearance, state, device != null, density.fontScale,
        device?.name ?: "Choose a device", measure)
    val palette = widgetPalette(LocalContext.current, appearance, dark)
    val mono = appearance.preset == WidgetPreset.DOT_MONO
    val family = if (mono) FontFamily.Monospace else FontFamily.SansSerif
    val align = when (appearance.alignment) {
        WidgetAlignment.START -> Alignment.Start
        WidgetAlignment.CENTER -> Alignment.CenterHorizontally
        WidgetAlignment.END -> Alignment.End
    }
    val textAlign = when (appearance.alignment) {
        WidgetAlignment.START -> TextAlign.Start
        WidgetAlignment.CENTER -> TextAlign.Center
        WidgetAlignment.END -> TextAlign.End
    }
    val moving = state.connection in setOf(WidgetConnectionState.CONNECTING, WidgetConnectionState.DISCONNECTING)
    var surface = modifier.size(width.dp, height.dp).clip(RoundedCornerShape(widgetRadius(appearance.preset).dp)).background(palette.background)
    if (appearance.preset == WidgetPreset.OUTLINE)
        surface = surface.border(1.dp, palette.border.copy(alpha = .5f), RoundedCornerShape(widgetRadius(appearance.preset).dp))
    @Composable fun Artwork() {
        if (appearance.showArtwork && r.artworkSize > 0) {
            val alpha = state.artworkAlpha()
            Box(Modifier.size(r.artworkSize.dp).then(if (appearance.preset == WidgetPreset.MATERIAL_YOU)
                Modifier.background(palette.badge, CircleShape).padding(6.dp) else Modifier), contentAlignment = Alignment.Center) {
                if (artwork != null && appearance.artworkSource == ArtworkSource.DEVICE) {
                    Image(artwork.asImageBitmap(), if (r.showName) null else device?.name, Modifier.fillMaxSize().alpha(alpha), contentScale = ContentScale.Fit)
                } else if (builtInArtwork != null && appearance.artworkSource == ArtworkSource.DEVICE) {
                    Image(painterResource(builtInArtwork), if (r.showName) null else device?.name, Modifier.fillMaxSize().alpha(alpha), contentScale = ContentScale.Fit)
                } else {
                    Icon(painterResource(deviceArtworkResource(artworkKind(device))), if (r.showName) null else device?.name,
                        Modifier.fillMaxSize().alpha(alpha), tint = palette.accent)
                }
            }
        }
    }
    @Composable fun Status() {
        Column(horizontalAlignment = align, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (moving && r.status != null) {
                ConnectingDots(palette.accent)
                Text(r.status, color = palette.secondary, fontSize = 11.sp, lineHeight = 13.sp, maxLines = 2, textAlign = textAlign)
            } else r.status?.let {
                Text(it, color = palette.secondary, fontSize = 11.sp, lineHeight = 13.sp, maxLines = 2,
                    textAlign = textAlign, overflow = TextOverflow.Ellipsis)
            }
            r.battery?.let { Text(it, color = palette.text, fontSize = 11.sp, lineHeight = 13.sp, fontFamily = family) }
        }
    }
    @Composable fun Identity(modifier: Modifier = Modifier) {
        val row = r.row
        if (row != null) {
            Column(modifier, horizontalAlignment = align) {
                if (row.showName) Box(Modifier.fillMaxWidth().height(row.nameHeight.dp), contentAlignment = Alignment.Center) {
                    Text(row.name, color = palette.text, fontSize = row.nameTextSize.sp,
                        fontWeight = if (mono) FontWeight.Bold else FontWeight.Medium, fontFamily = family,
                        maxLines = row.nameMaxLines, overflow = TextOverflow.Ellipsis, textAlign = textAlign,
                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = true)),
                        modifier = Modifier.fillMaxWidth())
                }
                row.secondary?.let {
                    if (row.showName) Spacer(Modifier.height(row.textGap.dp))
                    Box(Modifier.fillMaxWidth().height(row.statusHeight.dp), contentAlignment = Alignment.Center) {
                        Text(it, color = palette.secondary, fontSize = row.statusTextSize.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = textAlign,
                            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = true)),
                            modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            return
        }
        Column(modifier, horizontalAlignment = align, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            if (r.showName) Text(device?.name ?: "Choose a device", color = palette.text, fontSize = r.nameSize.sp,
                lineHeight = (r.nameSize + 2).sp, fontWeight = if (mono) FontWeight.Bold else FontWeight.Medium, fontFamily = family,
                maxLines = r.nameLines, overflow = TextOverflow.Ellipsis, textAlign = textAlign, modifier = Modifier.fillMaxWidth())
            Status()
        }
    }
    val contentPadding = r.row?.let { Modifier.padding(horizontal = it.horizontalPadding.dp, vertical = it.verticalPadding.dp) }
        ?: Modifier.padding(r.padding.dp)
    Column(surface.then(contentPadding), horizontalAlignment = align, verticalArrangement = Arrangement.Center) {
        if (r.showLabel) {
            Text("BlueTap", color = palette.accent, fontSize = 10.sp, lineHeight = 12.sp, fontFamily = family)
            Spacer(Modifier.height(6.dp))
        }
        if (r.horizontal) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Artwork()
                if (appearance.showArtwork && r.artworkSize > 0) Spacer(Modifier.width(r.gap.dp))
                Identity(Modifier.weight(1f))
            }
        } else {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Artwork() }
            if (appearance.showArtwork && r.artworkSize > 0) Spacer(Modifier.height(r.gap.dp))
            Identity(Modifier.fillMaxWidth())
        }
    }
}

/** Animation exists only inside Compose. Real AppWidgets render a static three-dot state. */
@Composable
private fun ConnectingDots(color: androidx.compose.ui.graphics.Color) {
    val transition = rememberInfiniteTransition(label = "Connection preview")
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(3) { index ->
            val alpha by transition.animateFloat(.25f, 1f,
                infiniteRepeatable(tween(600, easing = FastOutSlowInEasing), RepeatMode.Reverse,
                    StartOffset(index * 180)), label = "Dot $index")
            Box(Modifier.size(4.dp).alpha(alpha).background(color, CircleShape))
        }
    }
}
