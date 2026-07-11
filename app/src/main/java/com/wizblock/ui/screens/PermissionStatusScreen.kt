package com.wizblock.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wizblock.ui.PermissionState
import com.wizblock.ui.PermissionStatusUiFormatter

private val CardShape = RoundedCornerShape(10.dp)
private val ScreenBackground = Color(0xFFF4F6FB)
private val AccentBlue = Color(0xFF2E90FF)
private val TextPrimary = Color(0xFF1A202C)
private val TextMuted = Color(0xFF667085)

@Composable
fun PermissionStatusScreen(
    permissionState: PermissionState,
    onBack: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onOpenOverlay: () -> Unit,
    onRefresh: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            TextButton(onClick = onBack) { Text("Back") }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("System readiness", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    "Review the permissions WizBlock needs to keep protection reliable.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextMuted
                )
            }
        }
        PermissionStatusUiFormatter.permissionRows(permissionState).forEach { row ->
            item(key = row.title) {
                val action: () -> Unit = when (row.title) {
                    "Accessibility" -> onOpenAccessibility
                    "Overlay permission" -> onOpenOverlay
                    else -> ({})
                }
                PermissionStatusRow(
                    title = row.title,
                    status = row.status,
                    description = row.description,
                    disclosure = row.disclosure,
                    healthy = row.healthy,
                    actionLabel = row.actionLabel,
                    onAction = action
                )
            }
        }
        item {
            Button(
                onClick = onRefresh,
                modifier = Modifier.fillMaxWidth(),
                shape = CardShape,
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White)
            ) {
                Text("Refresh status", modifier = Modifier.padding(vertical = 4.dp))
            }
        }
    }
}

@Composable
private fun PermissionStatusRow(
    title: String,
    status: String,
    description: String,
    disclosure: String?,
    healthy: Boolean,
    actionLabel: String?,
    onAction: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxWidth(), color = Color.White, shape = CardShape) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(if (healthy) Color(0xFF16A34A) else Color(0xFFF59E0B), CircleShape)
                )
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    status,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (healthy) Color(0xFF219653) else AccentBlue,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(description, style = MaterialTheme.typography.bodyLarge, color = TextMuted)
            if (disclosure != null) {
                Text(disclosure, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
            }
            if (actionLabel != null) {
                Text(
                    text = actionLabel,
                    modifier = Modifier.clickable(onClick = onAction),
                    color = AccentBlue,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
