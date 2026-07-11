package com.wizblock.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wizblock.R
import com.wizblock.ui.SensitivePermissionDisclosure

private val CardShape = RoundedCornerShape(8.dp)

@Composable
fun OnboardingScreen(
    accessibilityGranted: Boolean,
    overlayGranted: Boolean,
    onOpenAccessibility: () -> Unit,
    onOpenOverlay: () -> Unit,
    onRefresh: () -> Unit,
    onContinue: () -> Unit
) {
    val allGranted = accessibilityGranted && overlayGranted

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = CardShape,
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.wizblockicon),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp)
                    )
                    Text("Local blocking setup", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Enable the two required permissions to block distracting apps and websites on this device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            PermissionCard(
                title = "Accessibility",
                granted = accessibilityGranted,
                description = "Lets WizBlock detect active apps and browser URLs.",
                disclosure = SensitivePermissionDisclosure.accessibilityBody,
                actionLabel = SensitivePermissionDisclosure.accessibilityAction,
                onAction = onOpenAccessibility
            )

            PermissionCard(
                title = "Overlay permission",
                granted = overlayGranted,
                description = "Lets WizBlock show the block screen over distracting apps and browsers.",
                disclosure = SensitivePermissionDisclosure.overlayBody,
                actionLabel = SensitivePermissionDisclosure.overlayAction,
                onAction = onOpenOverlay
            )

            TextButton(
                onClick = onRefresh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Refresh permission status")
            }

            Button(
                onClick = onContinue,
                enabled = allGranted,
                shape = CardShape,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Continue", modifier = Modifier.padding(vertical = 4.dp), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    granted: Boolean,
    description: String,
    disclosure: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Surface(
        shape = CardShape,
        tonalElevation = 3.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            if (granted) Color(0xFF34C759) else MaterialTheme.colorScheme.primary
                        )
                )
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(
                    if (granted) "Enabled" else "Required",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (granted) Color(0xFF219653) else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!granted) {
                Text(
                    text = disclosure,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onAction,
                    shape = CardShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(actionLabel, modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp))
                }
            }
        }
    }
}
