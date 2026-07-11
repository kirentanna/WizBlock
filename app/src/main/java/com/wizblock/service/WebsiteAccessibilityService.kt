package com.wizblock.service

import android.accessibilityservice.AccessibilityService
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.app.ServiceCompat
import com.wizblock.ServiceLocator
import com.wizblock.browser.ExtractionStatus
import com.wizblock.data.repository.BlockEventRepository
import com.wizblock.data.repository.BlockingStateRepository
import com.wizblock.data.repository.PolicyRepository
import com.wizblock.data.repository.UsageCounterRepository
import com.wizblock.model.BlockReason
import com.wizblock.model.Profile
import com.wizblock.model.StrictModeState
import com.wizblock.model.TargetType
import com.wizblock.overlay.OverlayBlockDetails
import com.wizblock.policy.PolicyDecision
import com.wizblock.policy.PolicyEvaluator
import com.wizblock.policy.PolicySnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.ArrayDeque
import java.time.LocalDate
import java.time.ZoneId

class WebsiteAccessibilityService : AccessibilityService() {
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private lateinit var policyRepository: PolicyRepository
    private lateinit var policyEvaluator: PolicyEvaluator
    private lateinit var blockingStateRepository: BlockingStateRepository
    private lateinit var blockEventRepository: BlockEventRepository
    private lateinit var usageCounterRepository: UsageCounterRepository
    private lateinit var enforcementEngine: EnforcementEngine
    private lateinit var usageSessionTracker: UsageSessionTracker
    private lateinit var overlayDismissController: OverlayDismissController

    @Volatile
    private var policySnapshot = PolicySnapshot(
        blockingEnabled = false,
        quickBlockUntilMs = 0L,
        rules = emptyList(),
        schedules = emptyList(),
        usageLimits = emptyList(),
        profiles = listOf(Profile.Default),
        enabledCategoryIds = emptySet()
    )

    @Volatile
    private var blockNewlyInstalledApps = false

    @Volatile
    private var blockUnsupportedBrowsers = false

    @Volatile
    private var enabledCategoryIds: Set<String> = emptySet()

    @Volatile
    private var strictModeState = StrictModeState.Inactive

    @Volatile
    private var lastHandledKey: String = ""

    @Volatile
    private var lastHandledAtMs: Long = 0L

    @Volatile
    private var lastObservedPackage: String? = null

    @Volatile
    private var lastObservedPackageAtMs: Long = 0L

    private val recentInstallCache = mutableMapOf<String, Boolean>()
    private var heartbeatJob: Job? = null
    private var foregroundStarted = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        ServiceLocator.init(applicationContext)

        policyRepository = ServiceLocator.policyRepository
        policyEvaluator = ServiceLocator.policyEvaluator
        blockingStateRepository = ServiceLocator.blockingStateRepository
        blockEventRepository = ServiceLocator.blockEventRepository
        usageCounterRepository = ServiceLocator.usageCounterRepository
        enforcementEngine = EnforcementEngine(
            policyEvaluator = policyEvaluator,
            isRecentlyInstalledNonSystemApp = ::isRecentlyInstalledNonSystemApp,
            categoryMatcher = ServiceLocator.categoryMatcher
        )
        usageSessionTracker = UsageSessionTracker(scope, usageCounterRepository)
        overlayDismissController = OverlayDismissController(
            scope = scope,
            overlayController = ServiceLocator.overlayController,
            appPackageName = applicationContext.packageName,
            performBack = { performGlobalAction(GLOBAL_ACTION_BACK) },
            performHome = { performGlobalAction(GLOBAL_ACTION_HOME) },
            observedPackage = {
                lastObservedPackage?.let {
                    OverlayDismissController.ObservedPackage(
                        packageName = it,
                        observedAtMs = lastObservedPackageAtMs
                    )
                }
            }
        )

        scope.launch {
            policyRepository.snapshot.collectLatest {
                policySnapshot = it
            }
        }
        scope.launch {
            blockingStateRepository.blockNewlyInstalledApps.collectLatest { blockNewlyInstalledApps = it }
        }
        scope.launch {
            blockingStateRepository.blockUnsupportedBrowsers.collectLatest { blockUnsupportedBrowsers = it }
        }
        scope.launch {
            blockingStateRepository.enabledCategoryIds.collectLatest { enabledCategoryIds = it }
        }
        scope.launch {
            blockingStateRepository.strictModeState.collectLatest { strictModeState = it }
        }

        overlayDismissController.attachGoBackHandler()
        scheduleStrictModeReconnectRecovery()

        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (true) {
                blockingStateRepository.setServiceHeartbeat(System.currentTimeMillis())
                delay(5_000)
            }
        }

        startForegroundIfNeeded()
        Log.i(TAG, "Accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val packageName = event.packageName?.toString().orEmpty()
        if (packageName.isBlank()) return
        if (packageName == applicationContext.packageName) return
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) return

        val now = System.currentTimeMillis()
        lastObservedPackage = packageName
        lastObservedPackageAtMs = now
        val rootNode = rootInActiveWindow
        val className = event.className?.toString()

        val strictModeDecision = evaluateStrictModeBypass(packageName, className, rootNode)
        val extraction = ServiceLocator.urlExtractionEngine.extractHost(rootNode, packageName)
        if (extraction is ExtractionStatus.Success && isAddressBarEditing(rootNode, extraction.host)) {
            Log.d(
                TAG,
                "Ignoring typed/autocomplete host=${extraction.host} address=${extraction.addressText} package=$packageName"
            )
            return
        }

        val host = (extraction as? ExtractionStatus.Success)?.host
        val addressText = (extraction as? ExtractionStatus.Success)?.addressText
        val key = "$packageName|${host.orEmpty()}|${addressText.orEmpty()}"
        if (key == lastHandledKey && now - lastHandledAtMs < 450L) return
        lastHandledKey = key
        lastHandledAtMs = now

        usageSessionTracker.ensureObservation(now)
        val input = EnforcementInput(
            packageName = packageName,
            extraction = extraction,
            policySnapshot = policySnapshot,
            usageCountersByTarget = usageSessionTracker.countersByTarget,
            nowMs = now,
            strictModeDecision = strictModeDecision
        )
        val finalDecision = enforcementEngine.evaluate(
            input = input,
            options = EnforcementOptions(
                blockNewlyInstalledApps = blockNewlyInstalledApps,
                blockUnsupportedBrowsers = blockUnsupportedBrowsers,
                enabledCategoryIds = enabledCategoryIds
            )
        )
        val quickBlockActive = policySnapshot.quickBlockUntilMs > now
        val policyActive = policySnapshot.blockingEnabled || quickBlockActive
        scope.launch { blockingStateRepository.setLastPolicyEvaluation(now) }

        usageSessionTracker.update(
            currentTarget = enforcementEngine.resolvedAllowedUsageTarget(input, finalDecision),
            nowMs = now
        )

        Log.d(
            TAG,
            "Decision package=$packageName host=${host.orEmpty()} address=${addressText.orEmpty()} " +
                "final=$finalDecision policyActive=$policyActive"
        )

        when (finalDecision) {
            PolicyDecision.Inactive,
            PolicyDecision.Allowed,
            PolicyDecision.NotMatched -> overlayDismissController.handleNonBlockedDecision(
                decision = finalDecision,
                policyActive = policyActive,
                nowMs = now,
                packageName = packageName,
                host = host,
                addressText = addressText
            )

            is PolicyDecision.Blocked -> {
                lastHandledKey = key
                if (strictModeDecision == finalDecision) {
                    val handled = overlayDismissController.handleStrictModeProtectedSurface(
                        decision = finalDecision,
                        displayInfo = ServiceLocator.targetDisplayResolver.resolve(
                            finalDecision.targetType,
                            finalDecision.targetValue
                        ),
                        sourcePackage = packageName,
                        details = OverlayBlockDetails(),
                        nowMs = now
                    )
                    if (handled) {
                        recordBlockedDecision(finalDecision, packageName)
                    }
                    return
                }
                val reasonText = when (finalDecision.reason) {
                    BlockReason.RULE_BLOCK -> "Focus mode"
                    BlockReason.CATEGORY_BLOCK -> "Category block"
                    BlockReason.USAGE_LIMIT_EXCEEDED -> "Usage limit reached"
                }
                val displayInfo = ServiceLocator.targetDisplayResolver.resolve(
                    finalDecision.targetType,
                    finalDecision.targetValue
                )
                scope.launch {
                    val todayRange = currentDayRangeMillis(now)
                    val existingAttempts = blockEventRepository.countTargetBlocks(
                        targetType = finalDecision.targetType,
                        targetValue = finalDecision.targetValue,
                        startMs = todayRange.first,
                        endMs = todayRange.second
                    )
                    val shown = overlayDismissController.handleBlockedDecision(
                        decision = finalDecision,
                        displayInfo = displayInfo,
                        sourcePackage = packageName,
                        reasonText = reasonText,
                        details = OverlayBlockDetails(todayAttemptCount = existingAttempts + 1),
                        nowMs = now
                    )
                    if (shown) {
                        recordBlockedDecision(finalDecision, packageName)
                    }
                }
            }
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        heartbeatJob?.cancel()
        if (::usageSessionTracker.isInitialized) {
            usageSessionTracker.stop(System.currentTimeMillis())
        }
        if (::overlayDismissController.isInitialized) {
            overlayDismissController.detach()
        }
        if (foregroundStarted) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            foregroundStarted = false
        }
        scope.cancel()
    }

    private fun recordBlockedDecision(decision: PolicyDecision.Blocked, sourcePackage: String) {
        scope.launch {
            blockingStateRepository.setLastBlockedTarget(decision.targetValue)
            blockEventRepository.record(
                targetType = decision.targetType,
                targetValue = decision.targetValue,
                browserPackage = sourcePackage,
                reason = decision.reason,
                matchedRuleId = decision.matchedRuleId,
                sessionId = null
            )
        }
    }

    private fun currentDayRangeMillis(nowMs: Long): Pair<Long, Long> {
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.ofInstant(java.time.Instant.ofEpochMilli(nowMs), zoneId)
        val start = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val end = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        return start to end
    }

    private fun evaluateStrictModeBypass(
        packageName: String,
        className: String?,
        rootNode: AccessibilityNodeInfo?
    ): PolicyDecision.Blocked? {
        val state = strictModeState
        if (rootNode == null) return null
        return StrictModeBypassEvaluator.evaluate(
            packageName = packageName,
            className = className ?: rootNode.className?.toString(),
            visibleText = collectVisibleText(rootNode),
            state = state
        )
    }

    private fun scheduleStrictModeReconnectRecovery() {
        scope.launch {
            repeat(STRICT_MODE_RECONNECT_RECOVERY_ATTEMPTS) { attempt ->
                delay(STRICT_MODE_RECONNECT_RECOVERY_STEP_MS)
                evaluateCurrentWindowAfterReconnect(attempt + 1)
            }
        }
    }

    private fun evaluateCurrentWindowAfterReconnect(attempt: Int) {
        val rootNode = rootInActiveWindow ?: return
        val packageName = rootNode.packageName?.toString().orEmpty()
        if (packageName.isBlank() || packageName == applicationContext.packageName) return

        val now = System.currentTimeMillis()
        val decision = StrictModeBypassEvaluator.evaluate(
            packageName = packageName,
            className = rootNode.className?.toString(),
            visibleText = collectVisibleText(rootNode),
            state = strictModeState
        ) ?: return

        Log.d(TAG, "Reconnect recovery blocked package=$packageName attempt=$attempt decision=$decision")
        val handled = overlayDismissController.handleStrictModeProtectedSurface(
            decision = decision,
            displayInfo = ServiceLocator.targetDisplayResolver.resolve(
                decision.targetType,
                decision.targetValue
            ),
            sourcePackage = packageName,
            details = OverlayBlockDetails(),
            nowMs = now
        )
        if (handled) {
            recordBlockedDecision(decision, packageName)
        }
    }

    private fun collectVisibleText(rootNode: AccessibilityNodeInfo): String {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(rootNode)
        val builder = StringBuilder()
        var visited = 0
        while (queue.isNotEmpty() && visited < 180) {
            val node = queue.removeFirst()
            visited++
            node.text?.toString()?.takeIf { it.isNotBlank() }?.let { builder.append(' ').append(it) }
            node.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let { builder.append(' ').append(it) }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return builder.toString()
    }

    private fun isRecentlyInstalledNonSystemApp(packageName: String): Boolean {
        recentInstallCache[packageName]?.let { return it }
        val result = runCatching {
            val pm = packageManager
            val packageInfo = getPackageInfoCompat(pm, packageName)
            val appInfo = packageInfo.applicationInfo
            val isSystem = ((appInfo?.flags ?: 0) and ApplicationInfo.FLAG_SYSTEM) != 0
            val launchable = pm.getLaunchIntentForPackage(packageName) != null
            val installedRecently = System.currentTimeMillis() - packageInfo.firstInstallTime < RECENT_INSTALL_THRESHOLD_MS
            !isSystem && launchable && installedRecently
        }.getOrDefault(false)
        recentInstallCache[packageName] = result
        return result
    }

    private fun getPackageInfoCompat(pm: PackageManager, packageName: String): PackageInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(packageName, 0)
        }
    }

    private fun isAddressBarEditing(rootNode: AccessibilityNodeInfo?, extractedHost: String): Boolean {
        if (rootNode == null) return false
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(rootNode)
        var visited = 0
        while (queue.isNotEmpty() && visited < 220) {
            val node = queue.removeFirst()
            visited++
            val viewId = node.viewIdResourceName.orEmpty().lowercase()
            val text = node.text?.toString().orEmpty().lowercase().trim()
            val hint = node.hintText?.toString().orEmpty().lowercase().trim()
            val candidate = if (text.isNotBlank()) text else hint

            if (node.isFocused && node.isEditable) {
                if (
                    viewId.contains("url") ||
                    viewId.contains("address") ||
                    viewId.contains("omnibox") ||
                    viewId.contains("location")
                ) {
                    return true
                }
                if (candidate.isNotBlank()) {
                    val cleaned = candidate
                        .removePrefix("https://")
                        .removePrefix("http://")
                        .removePrefix("www.")
                        .substringBefore('/')
                        .trim()
                    if (
                        cleaned.contains('.') &&
                        (extractedHost.startsWith(cleaned) ||
                            cleaned.startsWith(extractedHost) ||
                            cleaned.contains(extractedHost))
                    ) {
                        return true
                    }
                }
            }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return false
    }

    private fun startForegroundIfNeeded() {
        if (foregroundStarted) return
        val channelId = NOTIFICATION_CHANNEL_ID
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                channelId,
                "WizBlock protection",
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(channel)
        }

        val notification = Notification.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle("WizBlock active")
            .setContentText("App and website blocking is running")
            .setOngoing(true)
            .build()

        ServiceCompat.startForeground(
            this,
            FOREGROUND_NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
        foregroundStarted = true
    }

    private companion object {
        const val TAG = "WebsiteAccessibility"
        const val FOREGROUND_NOTIFICATION_ID = 1102
        const val NOTIFICATION_CHANNEL_ID = "wizblock_protection"
        const val RECENT_INSTALL_THRESHOLD_MS = 24L * 60L * 60L * 1000L
        const val STRICT_MODE_RECONNECT_RECOVERY_ATTEMPTS = 6
        const val STRICT_MODE_RECONNECT_RECOVERY_STEP_MS = 350L
    }
}
