package dev.bluetap.app.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Light = lightColorScheme(
    primary = Color(0xFF2563EB), onPrimary = Color.White,
    primaryContainer = Color(0xFFE2ECFF), onPrimaryContainer = Color(0xFF123573),
    secondary = Color(0xFF526176), onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3ECF8), onSecondaryContainer = Color(0xFF203753),
    surfaceContainerLow = Color(0xFFF0F4FA), surfaceContainer = Color(0xFFEAF0F7),
    surfaceContainerHigh = Color(0xFFE2EAF4),
    background = Color(0xFFF8FAFC), surface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFFE9EEF5), onSurface = Color(0xFF111827),
    onSurfaceVariant = Color(0xFF526176), outline = Color(0xFF778599),
)
private val Dark = darkColorScheme(
    primary = Color(0xFF93B8FF), onPrimary = Color(0xFF002E79),
    primaryContainer = Color(0xFF193C73), onPrimaryContainer = Color(0xFFDCE8FF),
    secondary = Color(0xFFB5C6DE), onSecondary = Color(0xFF1C2C42),
    secondaryContainer = Color(0xFF26374F), onSecondaryContainer = Color(0xFFDCE8FA),
    surfaceContainerLow = Color(0xFF151E2C), surfaceContainer = Color(0xFF192536),
    surfaceContainerHigh = Color(0xFF233247),
    background = Color(0xFF0C1018), surface = Color(0xFF101722),
    surfaceVariant = Color(0xFF202B3A), onSurface = Color(0xFFF1F5FB),
    onSurfaceVariant = Color(0xFFA8B5C7), outline = Color(0xFF708099),
)
private val Type = Typography(
    headlineSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 26.sp, letterSpacing = (-0.6).sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 21.sp, letterSpacing = (-0.3).sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun BlueTapTheme(dynamicColor: Boolean = false, content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val colors = if (dynamicColor && Build.VERSION.SDK_INT >= 31) {
        if (dark) dynamicDarkColorScheme(LocalContext.current) else dynamicLightColorScheme(LocalContext.current)
    } else if (dark) Dark else Light
    MaterialTheme(colorScheme = colors, typography = Type, content = content)
}
