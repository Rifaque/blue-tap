package dev.bluetap.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.bluetap.app.bluetooth.BondedDevice
import dev.bluetap.app.bluetooth.DeviceKind
import dev.bluetap.app.widget.deviceIcon

@Composable
fun DeviceRow(device: BondedDevice, onClick: (() -> Unit)? = null) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(
            role = Role.Button, onClickLabel = stringResource(dev.bluetap.app.R.string.choose_device), onClick = onClick) else Modifier)) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = RoundedCornerShape(15.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(painterResource(deviceIcon(device.kind)), null, Modifier.padding(9.dp).size(22.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(device.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(device.macAddress, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (device.kind != DeviceKind.GENERIC) Text(device.kind.label,
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            if (onClick != null) Icon(painterResource(dev.bluetap.app.R.drawable.ic_chevron), null,
                Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
