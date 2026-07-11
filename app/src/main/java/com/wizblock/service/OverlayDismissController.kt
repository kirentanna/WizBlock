package com.wizblock.service

import android.util.Log
import com.wizblock.model.TargetDisplayInfo
import com.wizblock.model.TargetType
import com.wizblock.overlay.OverlayBlockDetails
import com.wizblock.overlay.OverlayController
import com.wizblock.policy.PolicyDecision
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class OverlayDismissController(
    private val scope: CoroutineScope,
    private val overlayController: OverlayController,
    private val appPackageName: String,
    private val performBack: () -> Boolean,
    private val performHome: () -> Boolean,
    private val observedPackage: () -> ObservedPackage?
) {
    @Volatile
    private var activeBlockedTarget: String? = null

    @Volatile
    private var activeBlockedTargetType: TargetType? = null

    @Volatile
    private var activeBlockedSourcePackage: String? = null

    @Volatile
    private var suppressedTarget: String? = null

    @Volatile
    private var suppressedUntilMs: Long = 0L

    @Volatile
    private var activeDismissOnlyOverlay = false

    private var hideDebounceJob: Job? = null
    private var dismissAfterBackJob: Job? = null
    private var postHomeOverlayJob: Job? = null

    fun attachGoBackHandler() {
        overlayController.setGoBackHandler {
            val active = activeBlockedTarget
            val activeType = activeBlockedTargetType
            val activeSourcePackage = activeBlockedSourcePackage
            if (active.isNullOrBlank() || activeSourcePackage.isNullOrBlank()) {
                overlayController.hide("dismiss-without-active-target")
                return@setGoBackHandler
            }
            hideDebounceJob?.cancel()
            hideDebounceJob = null
            dismissAfterBackJob?.cancel()
            dismissAfterBackJob = null
            postHomeOverlayJob?.cancel()
            postHomeOverlayJob = null

            if (activeDismissOnlyOverlay) {
                clearActive()
                overlayController.hide("strict-home-overlay-dismiss:$active")
                Log.d(TAG, "Strict Mode post-Home overlay dismissed target=$active")
                return@setGoBackHandler
            }

            if (activeType == TargetType.DOMAIN || activeType == TargetType.KEYWORD) {
                suppressedTarget = active
                suppressedUntilMs = System.currentTimeMillis() + SUPPRESS_AFTER_GO_BACK_MS
                Log.d(TAG, "Close tapped; suppressing website target=$active until=$suppressedUntilMs")
                val firstBack = performBack()
                Log.d(TAG, "performBack first result=$firstBack target=$active")
                scope.launch {
                    delay(180)
                    val secondBack = performBack()
                    Log.d(TAG, "performBack second result=$secondBack target=$active")
                }
                clearActive()
                overlayController.hide("website-dismiss:$active")
                return@setGoBackHandler
            }

            suppressedTarget = active
            suppressedUntilMs = System.currentTimeMillis() + SUPPRESS_AFTER_GO_BACK_MS
            clearActive()
            overlayController.hide("app-dismiss:$active")
            Log.d(
                TAG,
                "Close tapped; suppressing app target=$active sourcePackage=$activeSourcePackage " +
                    "until=$suppressedUntilMs"
            )

            val dismissStartedAtMs = System.currentTimeMillis()
            dismissAfterBackJob = scope.launch {
                val backResult = performBack()
                Log.d(
                    TAG,
                    "Close tapped; performBack result=$backResult target=$active sourcePackage=$activeSourcePackage"
                )
                delay(BACK_BEFORE_HOME_MS)

                repeat(HOME_DISMISS_ATTEMPTS) { attempt ->
                    val homeResult = performHome()
                    Log.d(
                        TAG,
                        "Dismiss fallback performHome result=$homeResult target=$active " +
                            "sourcePackage=$activeSourcePackage attempt=${attempt + 1}"
                    )
                    delay(HOME_DISMISS_STEP_MS)

                    val observed = observedPackage()
                    if (
                        isStableDismissDestinationPackage(
                            packageName = observed?.packageName,
                            sourcePackage = activeSourcePackage
                        ) &&
                        (observed?.observedAtMs ?: 0L) >= dismissStartedAtMs
                    ) {
                        Log.d(
                            TAG,
                            "Dismiss navigation reached destination target=$active sourcePackage=$activeSourcePackage " +
                                "destinationPackage=${observed?.packageName} attempt=${attempt + 1}"
                        )
                        return@launch
                    }
                }

                Log.w(
                    TAG,
                    "Close action did not move away from blocked target=$active sourcePackage=$activeSourcePackage " +
                        "lastObservedPackage=${observedPackage()?.packageName}"
                )
            }
        }
    }

    fun detach() {
        hideDebounceJob?.cancel()
        dismissAfterBackJob?.cancel()
        postHomeOverlayJob?.cancel()
        overlayController.setGoBackHandler(null)
        overlayController.hide("service-destroy")
    }

    fun handleBlockedDecision(
        decision: PolicyDecision.Blocked,
        displayInfo: TargetDisplayInfo,
        sourcePackage: String,
        reasonText: String,
        details: OverlayBlockDetails,
        nowMs: Long
    ): Boolean {
        if (suppressedTarget == decision.targetValue && nowMs < suppressedUntilMs) return false
        val observed = observedPackage()
        if (
            decision.targetType == TargetType.APP_PACKAGE &&
            isStableDismissDestinationPackage(observed?.packageName, sourcePackage) &&
            (observed?.observedAtMs ?: 0L) > nowMs
        ) {
            Log.d(
                TAG,
                "Ignoring stale blocked app decision target=${decision.targetValue} " +
                    "sourcePackage=$sourcePackage observedPackage=${observed?.packageName} " +
                    "observedAt=${observed?.observedAtMs} eventAt=$nowMs"
            )
            return false
        }
        suppressedTarget = null
        suppressedUntilMs = 0L
        hideDebounceJob?.cancel()
        hideDebounceJob = null
        dismissAfterBackJob?.cancel()
        dismissAfterBackJob = null
        postHomeOverlayJob?.cancel()
        postHomeOverlayJob = null

        if (activeBlockedTarget != decision.targetValue) {
            Log.d(TAG, "Showing overlay for target=${decision.targetValue} reason=$reasonText")
        } else {
            Log.d(TAG, "Refreshing overlay for target=${decision.targetValue} reason=$reasonText")
        }
        overlayController.showBlocked(displayInfo, reasonText, details)
        activeBlockedTarget = decision.targetValue
        activeBlockedTargetType = decision.targetType
        activeBlockedSourcePackage = sourcePackage
        return true
    }

    fun handleStrictModeProtectedSurface(
        decision: PolicyDecision.Blocked,
        displayInfo: TargetDisplayInfo,
        sourcePackage: String,
        details: OverlayBlockDetails,
        nowMs: Long
    ): Boolean {
        if (suppressedTarget == decision.targetValue && nowMs < suppressedUntilMs) return false

        suppressedTarget = decision.targetValue
        suppressedUntilMs = nowMs + SUPPRESS_AFTER_GO_BACK_MS
        hideDebounceJob?.cancel()
        hideDebounceJob = null
        dismissAfterBackJob?.cancel()
        dismissAfterBackJob = null
        postHomeOverlayJob?.cancel()
        postHomeOverlayJob = null
        clearActive()
        overlayController.hide("strict-mode-force-home:${decision.targetValue}")

        val homeResult = performHome()
        Log.d(
            TAG,
            "Strict Mode protected surface forced Home result=$homeResult " +
                "target=${decision.targetValue} sourcePackage=$sourcePackage suppressUntil=$suppressedUntilMs"
        )
        if (homeResult) {
            postHomeOverlayJob = scope.launch {
                delay(POST_HOME_OVERLAY_DELAY_MS)
                activeBlockedTarget = decision.targetValue
                activeBlockedTargetType = decision.targetType
                activeBlockedSourcePackage = sourcePackage
                activeDismissOnlyOverlay = true
                overlayController.showBlocked(displayInfo, "Strict Mode", details)
                Log.d(TAG, "Strict Mode post-Home overlay shown target=${decision.targetValue}")
            }
        }
        return homeResult
    }

    fun handleNonBlockedDecision(
        decision: PolicyDecision,
        policyActive: Boolean,
        nowMs: Long,
        packageName: String,
        host: String?,
        addressText: String?
    ) {
        val activeTarget = activeBlockedTarget
        if (activeTarget.isNullOrBlank()) {
            if (!policyActive) {
                overlayController.hide("inactive-no-target:$decision")
            }
            return
        }

        if (!policyActive || decision == PolicyDecision.Inactive) {
            hideDebounceJob?.cancel()
            hideDebounceJob = null
            dismissAfterBackJob?.cancel()
            dismissAfterBackJob = null
            clearActive()
            Log.d(TAG, "Hiding overlay immediately decision=$decision activeTarget=$activeTarget")
            overlayController.hide("inactive-or-disabled:$decision")
            return
        }

        val activeType = activeBlockedTargetType
        val activeSourcePackage = activeBlockedSourcePackage
        if (activeDismissOnlyOverlay) {
            Log.d(
                TAG,
                "Keeping strict dismiss-only overlay despite non-blocked decision package=$packageName " +
                    "activeTarget=$activeTarget activeSource=$activeSourcePackage"
            )
            return
        }

        if (shouldIgnoreNonBlockedDecision(packageName, host, addressText)) {
            Log.d(
                TAG,
                "Ignoring non-blocked decision package=$packageName host=${host.orEmpty()} " +
                    "address=${addressText.orEmpty()} activeTarget=$activeTarget activeType=$activeType " +
                    "activeSource=$activeSourcePackage"
            )
            return
        }

        val scheduledTarget = activeTarget
        val scheduledSourcePackage = activeSourcePackage
        hideDebounceJob?.cancel()
        Log.d(
            TAG,
            "Scheduling overlay hide decision=$decision target=$scheduledTarget sourcePackage=$scheduledSourcePackage " +
                "eventPackage=$packageName host=${host.orEmpty()} address=${addressText.orEmpty()} " +
                "debounce=$OVERLAY_HIDE_DEBOUNCE_MS"
        )
        hideDebounceJob = scope.launch {
            val startMs = nowMs
            delay(OVERLAY_HIDE_DEBOUNCE_MS)
            val stillSameTarget = activeBlockedTarget == scheduledTarget
            val stillSameSourcePackage = activeBlockedSourcePackage == scheduledSourcePackage
            val stableEnough = System.currentTimeMillis() - startMs >= OVERLAY_HIDE_DEBOUNCE_MS
            if (stillSameTarget && stillSameSourcePackage && stableEnough) {
                Log.d(
                    TAG,
                    "Hiding overlay after debounce decision=$decision target=$scheduledTarget " +
                        "sourcePackage=$scheduledSourcePackage"
                )
                clearActive()
                overlayController.hide("debounced:$decision")
            } else {
                Log.d(
                    TAG,
                    "Skipped hide decision=$decision target=$scheduledTarget currentTarget=$activeBlockedTarget " +
                        "sourcePackage=$scheduledSourcePackage currentSourcePackage=$activeBlockedSourcePackage"
                )
            }
        }
    }

    private fun shouldIgnoreNonBlockedDecision(
        packageName: String,
        host: String?,
        addressText: String?
    ): Boolean {
        val activeType = activeBlockedTargetType ?: return false
        val activeSourcePackage = activeBlockedSourcePackage ?: return false

        if (packageName != activeSourcePackage) {
            return true
        }

        if (activeType == TargetType.APP_PACKAGE) {
            return false
        }

        val normalizedHost = host.orEmpty().trim()
        val normalizedAddress = addressText.orEmpty().trim()
        if (normalizedHost.isBlank() && normalizedAddress.isBlank()) {
            return true
        }

        return false
    }

    private fun clearActive() {
        activeBlockedTarget = null
        activeBlockedTargetType = null
        activeBlockedSourcePackage = null
        activeDismissOnlyOverlay = false
    }

    private fun isStableDismissDestinationPackage(
        packageName: String?,
        sourcePackage: String
    ): Boolean {
        val value = packageName.orEmpty()
        if (value.isBlank()) return false
        if (value == sourcePackage) return false
        if (value == appPackageName) return false
        if (value == "android") return false
        if (value == "com.android.systemui") return false
        return true
    }

    data class ObservedPackage(
        val packageName: String,
        val observedAtMs: Long
    )

    private companion object {
        const val TAG = "OverlayDismiss"
        const val SUPPRESS_AFTER_GO_BACK_MS = 2_500L
        const val OVERLAY_HIDE_DEBOUNCE_MS = 650L
        const val BACK_BEFORE_HOME_MS = 180L
        const val HOME_DISMISS_ATTEMPTS = 6
        const val HOME_DISMISS_STEP_MS = 140L
        const val POST_HOME_OVERLAY_DELAY_MS = 450L
    }
}
