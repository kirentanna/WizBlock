package com.wizblock.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.wizblock.R
import com.wizblock.model.TargetDisplayInfo
import com.wizblock.ui.theme.WizBlockTheme

class SystemOverlayController(
    private val context: Context
) : OverlayController {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    @Volatile
    private var overlayView: ComposeView? = null
    private var owner: OverlayViewOwner? = null
    private var shownTarget: TargetDisplayInfo? = null
    private var shownReason: String = ""
    private var shownDetails: OverlayBlockDetails = OverlayBlockDetails()
    private var onGoBackHandler: (() -> Unit)? = null

    override fun setGoBackHandler(handler: (() -> Unit)?) {
        onGoBackHandler = handler
    }

    override fun showBlocked(target: TargetDisplayInfo, reason: String, details: OverlayBlockDetails) {
        if (!Settings.canDrawOverlays(context)) {
            Log.w(TAG, "Overlay permission missing. Cannot show block overlay.")
            return
        }

        mainHandler.post {
            val isUpdating = overlayView != null
            val sameContent = shownTarget == target && shownReason == reason && shownDetails == details
            shownTarget = target
            shownReason = reason
            shownDetails = details
            if (overlayView == null) {
                val overlayOwner = OverlayViewOwner()
                owner = overlayOwner

                val view = ComposeView(context).apply {
                    setViewTreeLifecycleOwner(overlayOwner)
                    setViewTreeViewModelStoreOwner(overlayOwner)
                    setViewTreeSavedStateRegistryOwner(overlayOwner)
                    setContent {
                        WizBlockTheme {
                            OverlayScreen(
                                target = target,
                                reason = reason,
                                details = details,
                                context = context,
                                onGoBack = {
                                    val handler = onGoBackHandler
                                    if (handler != null) {
                                        handler.invoke()
                                    } else {
                                        hide("user-dismiss-no-handler")
                                    }
                                }
                            )
                        }
                    }
                }

                val params = WindowManager.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    } else {
                        @Suppress("DEPRECATION")
                        WindowManager.LayoutParams.TYPE_PHONE
                    },
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.START
                }

                runCatching {
                    windowManager.addView(view, params)
                    overlayView = view
                    Log.d(TAG, "Overlay shown for target=${target.targetKey.id} reason=$reason")
                }
                return@post
            }

            if (sameContent) {
                Log.d(TAG, "Overlay already visible for target=${target.targetKey.id}; skipping update")
                return@post
            }

            overlayView?.setContent {
                WizBlockTheme {
                    OverlayScreen(
                        target = target,
                        reason = reason,
                        details = details,
                        context = context,
                        onGoBack = {
                            val handler = onGoBackHandler
                            if (handler != null) {
                                handler.invoke()
                            } else {
                                hide("user-dismiss-no-handler")
                            }
                        }
                    )
                }
            }
            Log.d(TAG, "Overlay updated for target=${target.targetKey.id} reason=$reason updating=$isUpdating")
        }
    }

    override fun hide(reason: String) {
        mainHandler.post {
            val view = overlayView ?: return@post
            Log.d(TAG, "Overlay hide requested reason=$reason target=${shownTarget?.targetKey?.id.orEmpty()}")
            runCatching {
                windowManager.removeViewImmediate(view)
                Log.d(TAG, "Overlay hidden")
            }
            overlayView = null
            shownTarget = null
            shownReason = ""
            shownDetails = OverlayBlockDetails()
            owner?.destroy()
            owner = null
        }
    }

    private companion object {
        const val TAG = "WizBlockOverlay"
    }
}

@Composable
private fun OverlayScreen(
    target: TargetDisplayInfo,
    reason: String,
    details: OverlayBlockDetails,
    context: Context,
    onGoBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF071941),
                        Color(0xFF07112D),
                        Color(0xFF030A1C)
                    )
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.Start
        ) {
            OverlayTopBar()
            Spacer(modifier = Modifier.height(78.dp))
            OverlayHero(context = context, target = target)
            Spacer(modifier = Modifier.weight(1f))
            OverlayCommandSheet(
                target = target,
                reason = reason,
                details = details,
                onGoBack = onGoBack
            )
        }
    }
}

@Composable
private fun OverlayTopBar() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.wizblockicon),
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(7.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "WizBlock",
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFFDCE7FF)
            )
        }
        Surface(
            color = Color(0x66111F3E),
            shape = RoundedCornerShape(999.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3DB9D0FF))
        ) {
            Text(
                text = "Focus active",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFB9D0FF)
            )
        }
    }
}

@Composable
private fun OverlayHero(
    context: Context,
    target: TargetDisplayInfo
) {
    Column {
        OverlayIcon(
            context = context,
            iconPackageName = target.iconPackageName,
            size = 58.dp
        )
        Spacer(modifier = Modifier.height(26.dp))
        Text(
            text = "Access to",
            style = MaterialTheme.typography.headlineLarge,
            color = Color(0xFFF2F6FF),
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = target.title,
            style = MaterialTheme.typography.headlineLarge,
            color = Color(0xFFFF6C63),
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "is paused.",
            style = MaterialTheme.typography.headlineLarge,
            color = Color(0xFFF2F6FF),
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun OverlayCommandSheet(
    target: TargetDisplayInfo,
    reason: String,
    details: OverlayBlockDetails,
    onGoBack: () -> Unit
) {
    val rows = remember(target, reason, details) {
        OverlayBlockDetailsFormatter.rows(target, reason, details)
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xE6111F3E),
        contentColor = Color(0xFFE8F0FF),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 12.dp, bottomEnd = 12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x29B9D0FF))
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 20.dp)) {
            Text(
                text = "Blocked by WizBlock",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFB9D0FF),
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(10.dp))
            rows.forEachIndexed { index, row ->
                OverlayDetailRow(row = row)
                if (index != rows.lastIndex) {
                    HorizontalDivider(color = Color(0x21B9D0FF))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(
                onClick = onGoBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.textButtonColors(
                    containerColor = Color(0xFF1E6BFF),
                    contentColor = Color(0xFFF5FAFF)
                )
            ) {
                Text("Close", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Private blocking. Rules and counts stay on this device.",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF93A8D6),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun OverlayDetailRow(row: OverlayDetailRow) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 11.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = row.label,
            modifier = Modifier.weight(0.9f),
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF8297C8)
        )
        Text(
            text = row.value,
            modifier = Modifier.weight(1.4f),
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFE8F0FF),
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun OverlayIcon(
    context: Context,
    iconPackageName: String?,
    size: Dp = 86.dp
) {
    val appIcon = remember(iconPackageName) {
        iconPackageName?.let { packageName ->
            runCatching { context.packageManager.getApplicationIcon(packageName) }.getOrNull()
        }
    }
    if (appIcon != null) {
        Image(
            bitmap = appIcon.toBitmap(96, 96).asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.size(size)
        )
    } else {
        Image(
            painter = painterResource(id = R.drawable.wizblockicon),
            contentDescription = null,
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(16.dp))
        )
    }
}
