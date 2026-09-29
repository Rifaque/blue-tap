package dev.bluetap.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import dev.bluetap.app.R
import dev.bluetap.app.widget.refreshAllWidgets

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            BlueTapTheme {
                DeviceSetupScreen(
                    title = stringResource(R.string.app_name),
                    description = stringResource(R.string.app_description),
                    onDeviceSelected = null,
                    onDevicesRefreshed = { refreshAllWidgets(context) },
                    footer = stringResource(R.string.main_widget_hint),
                )
            }
        }
    }
}
