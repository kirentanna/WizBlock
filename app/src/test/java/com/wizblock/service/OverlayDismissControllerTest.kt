package com.wizblock.service

import com.google.common.truth.Truth.assertThat
import com.wizblock.model.BlockReason
import com.wizblock.model.TargetDisplayInfo
import com.wizblock.model.TargetKey
import com.wizblock.model.TargetType
import com.wizblock.overlay.OverlayBlockDetails
import com.wizblock.overlay.OverlayController
import com.wizblock.policy.PolicyDecision
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OverlayDismissControllerTest {
    @Test
    fun appPackageClose_hidesAndSuppressesImmediateSameTargetReshow() = runTest {
        val overlay = FakeOverlayController()
        var backCount = 0
        var homeCount = 0
        val controller = controller(
            scope = this,
            overlay = overlay,
            performBack = {
                backCount++
                true
            },
            performHome = {
                homeCount++
                true
            },
            observedPackage = {
                OverlayDismissController.ObservedPackage(
                    packageName = SETTINGS_PACKAGE,
                    observedAtMs = System.currentTimeMillis()
                )
            }
        )
        controller.attachGoBackHandler()

        assertThat(controller.showBlockedApp(SETTINGS_PACKAGE)).isTrue()
        overlay.close()

        assertThat(overlay.hideReasons).containsExactly("app-dismiss:$SETTINGS_PACKAGE")

        repeat(3) {
            assertThat(controller.showBlockedApp(SETTINGS_PACKAGE)).isFalse()
        }
        assertThat(overlay.showCalls.map { it.target.targetKey.targetValue })
            .containsExactly(SETTINGS_PACKAGE)

        runCurrent()
        assertThat(backCount).isEqualTo(1)
        advanceTimeBy(181)
        runCurrent()
        assertThat(homeCount).isAtLeast(1)
    }

    @Test
    fun appPackageClose_allowsDifferentBlockedTargetDuringSuppressionWindow() = runTest {
        val overlay = FakeOverlayController()
        val controller = controller(
            scope = this,
            overlay = overlay,
            performBack = { true },
            performHome = { true }
        )
        controller.attachGoBackHandler()

        assertThat(controller.showBlockedApp(SETTINGS_PACKAGE)).isTrue()
        overlay.close()

        assertThat(controller.showBlockedApp(PACKAGE_INSTALLER_PACKAGE)).isTrue()
        assertThat(overlay.showCalls.map { it.target.targetKey.targetValue })
            .containsExactly(SETTINGS_PACKAGE, PACKAGE_INSTALLER_PACKAGE)
            .inOrder()
    }

    @Test
    fun staleAppPackageBlockAfterLauncherObservation_doesNotShowOverlay() = runTest {
        val overlay = FakeOverlayController()
        val controller = controller(
            scope = this,
            overlay = overlay,
            observedPackage = {
                OverlayDismissController.ObservedPackage(
                    packageName = LAUNCHER_PACKAGE,
                    observedAtMs = 2_000L
                )
            }
        )
        controller.attachGoBackHandler()

        val shown = controller.showBlockedApp(SETTINGS_PACKAGE, nowMs = 1_000L)

        assertThat(shown).isFalse()
        assertThat(overlay.showCalls).isEmpty()
    }

    @Test
    fun strictModeProtectedSurface_forcesHomeThenShowsDismissOnlyOverlay() = runTest {
        val overlay = FakeOverlayController()
        var backCount = 0
        var homeCount = 0
        val controller = controller(
            scope = this,
            overlay = overlay,
            performBack = {
                backCount++
                true
            },
            performHome = {
                homeCount++
                true
            }
        )
        controller.attachGoBackHandler()

        val handled = controller.handleStrictModeProtectedSurface(
            decision = blockedAppDecision(SETTINGS_PACKAGE),
            displayInfo = displayInfo(TargetType.APP_PACKAGE, SETTINGS_PACKAGE),
            sourcePackage = SETTINGS_PACKAGE,
            details = OverlayBlockDetails(),
            nowMs = 1_000L
        )

        assertThat(handled).isTrue()
        assertThat(homeCount).isEqualTo(1)
        assertThat(overlay.showCalls).isEmpty()
        assertThat(overlay.hideReasons).containsExactly("strict-mode-force-home:$SETTINGS_PACKAGE")
        assertThat(controller.showBlockedApp(SETTINGS_PACKAGE, nowMs = 1_100L)).isFalse()

        advanceTimeBy(451)
        runCurrent()

        assertThat(overlay.showCalls.map { it.target.targetKey.targetValue })
            .containsExactly(SETTINGS_PACKAGE)

        controller.handleNonBlockedDecision(
            decision = PolicyDecision.NotMatched,
            policyActive = true,
            nowMs = 2_000L,
            packageName = SETTINGS_PACKAGE,
            host = null,
            addressText = null
        )
        advanceTimeBy(1_000)
        runCurrent()

        assertThat(overlay.hideReasons).containsExactly("strict-mode-force-home:$SETTINGS_PACKAGE")

        overlay.close()

        assertThat(backCount).isEqualTo(0)
        assertThat(homeCount).isEqualTo(1)
        assertThat(overlay.hideReasons).containsExactly(
            "strict-mode-force-home:$SETTINGS_PACKAGE",
            "strict-home-overlay-dismiss:$SETTINGS_PACKAGE"
        ).inOrder()
    }

    @Test
    fun domainClose_keepsExistingImmediateHideAndBackBehavior() = runTest {
        val overlay = FakeOverlayController()
        var backCount = 0
        var homeCount = 0
        val controller = controller(
            scope = this,
            overlay = overlay,
            performBack = {
                backCount++
                true
            },
            performHome = {
                homeCount++
                true
            }
        )
        controller.attachGoBackHandler()

        assertThat(controller.showBlockedDomain("example.com")).isTrue()
        overlay.close()

        assertThat(overlay.hideReasons).containsExactly("website-dismiss:example.com")
        assertThat(controller.showBlockedDomain("example.com")).isFalse()
        assertThat(backCount).isEqualTo(1)
        assertThat(homeCount).isEqualTo(0)
    }

    private fun controller(
        scope: TestScope,
        overlay: FakeOverlayController,
        performBack: () -> Boolean = { true },
        performHome: () -> Boolean = { true },
        observedPackage: () -> OverlayDismissController.ObservedPackage? = { null }
    ): OverlayDismissController {
        return OverlayDismissController(
            scope = scope,
            overlayController = overlay,
            appPackageName = "com.wizblock",
            performBack = performBack,
            performHome = performHome,
            observedPackage = observedPackage
        )
    }

    private fun OverlayDismissController.showBlockedApp(
        packageName: String,
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        return handleBlockedDecision(
            decision = blockedAppDecision(packageName),
            displayInfo = displayInfo(TargetType.APP_PACKAGE, packageName),
            sourcePackage = packageName,
            reasonText = "Strict Mode",
            details = OverlayBlockDetails(),
            nowMs = nowMs
        )
    }

    private fun blockedAppDecision(packageName: String): PolicyDecision.Blocked {
        return PolicyDecision.Blocked(
            targetType = TargetType.APP_PACKAGE,
            targetValue = packageName,
            reason = BlockReason.RULE_BLOCK,
            matchedRuleId = null
        )
    }

    private fun OverlayDismissController.showBlockedDomain(domain: String): Boolean {
        return handleBlockedDecision(
            decision = PolicyDecision.Blocked(
                targetType = TargetType.DOMAIN,
                targetValue = domain,
                reason = BlockReason.RULE_BLOCK,
                matchedRuleId = null
            ),
            displayInfo = displayInfo(TargetType.DOMAIN, domain),
            sourcePackage = "com.android.chrome",
            reasonText = "Focus mode",
            details = OverlayBlockDetails(),
            nowMs = System.currentTimeMillis()
        )
    }

    private fun displayInfo(targetType: TargetType, targetValue: String): TargetDisplayInfo {
        return TargetDisplayInfo(
            targetKey = TargetKey(targetType, targetValue),
            title = targetValue,
            subtitle = targetType.name
        )
    }

    private class FakeOverlayController : OverlayController {
        val showCalls = mutableListOf<ShowCall>()
        val hideReasons = mutableListOf<String>()
        private var goBackHandler: (() -> Unit)? = null

        override fun setGoBackHandler(handler: (() -> Unit)?) {
            goBackHandler = handler
        }

        override fun showBlocked(target: TargetDisplayInfo, reason: String, details: OverlayBlockDetails) {
            showCalls += ShowCall(target, reason, details)
        }

        override fun hide(reason: String) {
            hideReasons += reason
        }

        fun close() {
            checkNotNull(goBackHandler).invoke()
        }
    }

    private data class ShowCall(
        val target: TargetDisplayInfo,
        val reason: String,
        val details: OverlayBlockDetails
    )

    private companion object {
        const val SETTINGS_PACKAGE = "com.android.settings"
        const val PACKAGE_INSTALLER_PACKAGE = "com.google.android.packageinstaller"
        const val LAUNCHER_PACKAGE = "com.sec.android.app.launcher"
    }
}
