package com.wizblock.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wizblock.model.StrictModeState
import com.wizblock.ui.SensitivePermissionDisclosure

private val CardShape = RoundedCornerShape(10.dp)
private val ScreenBackground = Color(0xFFF4F6FB)
private val TextPrimary = Color(0xFF1A202C)
private val TextMuted = Color(0xFF667085)

@Composable
fun LockScreen(
    strictModeState: StrictModeState,
    blockNewlyInstalledApps: Boolean,
    blockUnsupportedBrowsers: Boolean,
    onBack: () -> Unit,
    onSetStrictModeBlockDeviceSettings: (Boolean) -> Unit,
    onSetStrictModeUninstallProtection: (Boolean) -> Unit,
    onSetBlockNewlyInstalledApps: (Boolean) -> Unit,
    onSetBlockUnsupportedBrowsers: (Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            TextButton(onClick = onBack) { Text("Back") }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Lock protection", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text(
                    "Choose how hard it should be to change your setup.",
                    color = TextMuted,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        item {
            Surface(modifier = Modifier.fillMaxWidth(), color = Color.White, shape = CardShape) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Advanced lock options", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    LockSwitchRow(
                        title = "Block Settings screens",
                        subtitle = "Stop device settings that can weaken protection.",
                        checked = strictModeState.blockDeviceSettings,
                        onCheckedChange = onSetStrictModeBlockDeviceSettings
                    )
                    LockSwitchRow(
                        title = "Uninstall protection",
                        subtitle = if (strictModeState.uninstallProtectionEnabled) {
                            "Device Admin is active. Normal uninstall is blocked until this is disabled."
                        } else {
                            SensitivePermissionDisclosure.deviceAdminBody
                        },
                        checked = strictModeState.uninstallProtectionEnabled,
                        onCheckedChange = onSetStrictModeUninstallProtection
                    )
                    LockSwitchRow(
                        title = "Block newly installed apps",
                        subtitle = "New apps start blocked until you review them.",
                        checked = blockNewlyInstalledApps,
                        onCheckedChange = onSetBlockNewlyInstalledApps
                    )
                    LockSwitchRow(
                        title = "Block unsupported browsers",
                        subtitle = "Close browser paths that cannot be inspected.",
                        checked = blockUnsupportedBrowsers,
                        onCheckedChange = onSetBlockUnsupportedBrowsers
                    )
                }
            }
        }
    }
}

@Composable
private fun LockSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, color = TextMuted, style = MaterialTheme.typography.bodyMedium)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
