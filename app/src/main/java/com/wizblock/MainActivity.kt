package com.wizblock

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wizblock.model.OnboardingStep
import com.wizblock.model.RuleKind
import com.wizblock.ui.HomeActions
import com.wizblock.ui.HomeUiState
import com.wizblock.ui.MainDestination
import com.wizblock.ui.MainViewModel
import com.wizblock.ui.SensitivePermissionDisclosure
import com.wizblock.ui.screens.BlocklistScreen
import com.wizblock.ui.screens.HomeScreen
import com.wizblock.ui.screens.LockScreen
import com.wizblock.ui.screens.OnboardingScreen
import com.wizblock.ui.screens.PermissionStatusScreen
import com.wizblock.ui.screens.RecentHistoryScreen
import com.wizblock.ui.theme.WizBlockTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WizBlockTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WizBlockApp()
                }
            }
        }
    }
}

private enum class MainTab(val label: String) {
    HOME("Home"),
    BLOCKLISTS("Blocklists"),
    RULES("Rules")
}

@Composable
private fun WizBlockApp(
    viewModel: MainViewModel = viewModel()
) {
    val navController = rememberNavController()
    val onboardingStep by viewModel.onboardingStep.collectAsStateWithLifecycle()
    val permissions by viewModel.permissionState.collectAsStateWithLifecycle()
    val blockingEnabled by viewModel.blockingEnabled.collectAsStateWithLifecycle()
    val focusUntilMs by viewModel.focusUntilMs.collectAsStateWithLifecycle()
    val homeDurationPresetMs by viewModel.homeDurationPresetMs.collectAsStateWithLifecycle()
    val lastCustomDurationMs by viewModel.lastCustomDurationMs.collectAsStateWithLifecycle()
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
    val blockedAppPackages by viewModel.blockedAppPackages.collectAsStateWithLifecycle()
    val websiteRules by viewModel.websiteRules.collectAsStateWithLifecycle()
    val keywordRules by viewModel.keywordRules.collectAsStateWithLifecycle()
    val allRules by viewModel.allRules.collectAsStateWithLifecycle()
    val appsBlockedCount by viewModel.appsBlockedCount.collectAsStateWithLifecycle()
    val websitesBlockedCount by viewModel.websitesBlockedCount.collectAsStateWithLifecycle()
    val keywordsBlockedCount by viewModel.keywordsBlockedCount.collectAsStateWithLifecycle()
    val blockNewlyInstalledApps by viewModel.blockNewlyInstalledApps.collectAsStateWithLifecycle()
    val blockUnsupportedBrowsers by viewModel.blockUnsupportedBrowsers.collectAsStateWithLifecycle()
    val enabledCategoryIds by viewModel.enabledCategoryIds.collectAsStateWithLifecycle()
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val strictModeState by viewModel.strictModeState.collectAsStateWithLifecycle()
    val blockedTargets by viewModel.blockedTargets.collectAsStateWithLifecycle()
    val blocklistSummaries by viewModel.blocklistSummaries.collectAsStateWithLifecycle()
    val schedules by viewModel.schedules.collectAsStateWithLifecycle()
    val recentEventsDisplay by viewModel.recentEventsDisplay.collectAsStateWithLifecycle()
    val dailySummary by viewModel.dailySummary.collectAsStateWithLifecycle()
    val topBlockedDisplay by viewModel.topBlockedDisplay.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeviceAdminDisclosure by rememberSaveable { mutableStateOf(false) }
    val deviceAdminLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.handleDeviceAdminEnableResult()
    }
    val requestDeviceAdminEnable = {
        showDeviceAdminDisclosure = true
    }

    val startDestination = remember(onboardingStep) {
        if (onboardingStep == OnboardingStep.Complete) MainDestination.HOME.route else MainDestination.ONBOARDING.route
    }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute == MainDestination.HOME.route ||
        currentRoute?.startsWith(MainDestination.BLOCKLIST_PREFIX) == true ||
        currentRoute == MainDestination.LOCK.route

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissions()
                viewModel.refreshDeviceAdminState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                WizBlockBottomBar(
                    currentRoute = currentRoute,
                    onNavigateHome = {
                        navController.navigate(MainDestination.HOME.route) {
                            launchSingleTop = true
                            popUpTo(MainDestination.HOME.route)
                        }
                    },
                    onNavigateBlocklists = {
                        navController.navigate(MainDestination.BLOCKLISTS.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateRules = {
                        navController.navigate(MainDestination.LOCK.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(MainDestination.ONBOARDING.route) {
                OnboardingScreen(
                    accessibilityGranted = permissions.accessibilityGranted,
                    overlayGranted = permissions.overlayGranted,
                    onOpenAccessibility = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                    onOpenOverlay = {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                        )
                    },
                    onRefresh = viewModel::refreshPermissions,
                    onContinue = {
                        viewModel.refreshPermissions()
                        if (viewModel.permissionState.value.accessibilityGranted &&
                            viewModel.permissionState.value.overlayGranted
                        ) {
                            navController.navigate(MainDestination.HOME.route) {
                                popUpTo(MainDestination.ONBOARDING.route) { inclusive = true }
                            }
                        }
                    }
                )
            }

            composable(MainDestination.HOME.route) {
                HomeScreen(
                    state = HomeUiState(
                        blockingEnabled = blockingEnabled,
                        focusUntilMs = focusUntilMs,
                        permissionState = permissions,
                        appsBlockedCount = appsBlockedCount,
                        websitesBlockedCount = websitesBlockedCount,
                        keywordsBlockedCount = keywordsBlockedCount,
                        blockNewlyInstalledApps = blockNewlyInstalledApps,
                        blockUnsupportedBrowsers = blockUnsupportedBrowsers,
                        categoryPacks = viewModel.categoryPacks,
                        enabledCategoryIds = enabledCategoryIds,
                        profiles = profiles,
                        strictModeState = strictModeState,
                        homeDurationPresetMs = homeDurationPresetMs,
                        lastCustomDurationMs = lastCustomDurationMs,
                        blockedTargets = blockedTargets,
                        schedules = schedules,
                        recentEvents = recentEventsDisplay,
                        dailySummary = dailySummary,
                        topBlocked = topBlockedDisplay
                    ),
                    actions = HomeActions(
                        onToggleBlocking = viewModel::setBlockingEnabled,
                        onOpenBlocklistTab = { tab ->
                            navController.navigate("${MainDestination.BLOCKLIST_PREFIX}/${tab.name}")
                        },
                        onSetBlockNewlyInstalledApps = viewModel::setBlockNewlyInstalledApps,
                        onSetBlockUnsupportedBrowsers = viewModel::setBlockUnsupportedBrowsers,
                        onSetCategoryEnabled = viewModel::setCategoryEnabled,
                        onSetProfileEnabled = viewModel::setProfileEnabled,
                        onSetStrictModeBlockDeviceSettings = viewModel::setStrictModeBlockDeviceSettings,
                        onSetStrictModeUninstallProtection = { enabled ->
                            if (enabled) {
                                requestDeviceAdminEnable()
                            } else {
                                viewModel.disableStrictModeUninstallProtection()
                            }
                        },
                        onStartProtection = viewModel::startProtection,
                        onSwitchRunningProtectionToStrictMode = viewModel::switchRunningProtectionToStrictMode,
                        onInvalidStrictModeNoLimit = viewModel::showStrictModeDurationRequired,
                        onSetHomeDurationPreset = viewModel::setHomeDurationPreset,
                        onSetLastCustomDuration = viewModel::setLastCustomDuration,
                        onOpenLock = { navController.navigate(MainDestination.LOCK.route) },
                        onOpenPermissions = { navController.navigate(MainDestination.PERMISSION_STATUS.route) },
                        onOpenHistory = { navController.navigate(MainDestination.RECENT_HISTORY.route) }
                    )
                )
            }

            composable(MainDestination.RECENT_HISTORY.route) {
                RecentHistoryScreen(
                    dailySummary = dailySummary,
                    recentEvents = recentEventsDisplay,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(MainDestination.PERMISSION_STATUS.route) {
                PermissionStatusScreen(
                    permissionState = permissions,
                    onBack = { navController.popBackStack() },
                    onOpenAccessibility = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                    onOpenOverlay = {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                        )
                    },
                    onRefresh = viewModel::refreshPermissions
                )
            }

            composable(MainDestination.LOCK.route) {
                LockScreen(
                    strictModeState = strictModeState,
                    blockNewlyInstalledApps = blockNewlyInstalledApps,
                    blockUnsupportedBrowsers = blockUnsupportedBrowsers,
                    onBack = { navController.popBackStack() },
                    onSetStrictModeBlockDeviceSettings = viewModel::setStrictModeBlockDeviceSettings,
                    onSetStrictModeUninstallProtection = { enabled ->
                        if (enabled) {
                            requestDeviceAdminEnable()
                        } else {
                            viewModel.disableStrictModeUninstallProtection()
                        }
                    },
                    onSetBlockNewlyInstalledApps = viewModel::setBlockNewlyInstalledApps,
                    onSetBlockUnsupportedBrowsers = viewModel::setBlockUnsupportedBrowsers
                )
            }

            composable(
                route = MainDestination.BLOCKLIST_PATTERN.route,
                arguments = listOf(navArgument("tab") { type = NavType.StringType })
            ) { backStackEntry ->
                val tabArg = backStackEntry.arguments?.getString("tab").orEmpty()
                val initialTab = runCatching { RuleKind.valueOf(tabArg) }.getOrDefault(RuleKind.DOMAIN)
                BlocklistScreen(
                    initialTab = initialTab,
                    installedApps = installedApps,
                    blockedAppPackages = blockedAppPackages,
                    websiteRules = websiteRules,
                    keywordRules = keywordRules,
                    allRules = allRules,
                    blockedTargets = blockedTargets,
                    blocklistSummaries = blocklistSummaries,
                    profiles = profiles,
                    onBack = { navController.popBackStack() },
                    onToggleAppBlocked = viewModel::toggleAppBlocked,
                    onAddWebsite = viewModel::addWebsite,
                    onAddKeyword = viewModel::addKeyword,
                    onToggleRule = viewModel::toggleRule,
                    onCreateBlocklist = viewModel::createBlocklist,
                    onToggleBlocklist = viewModel::setProfileEnabled,
                    onUpdateBlocklist = viewModel::updateBlocklist,
                    onDeleteBlocklist = viewModel::deleteBlocklist,
                    onDeleteRule = viewModel::deleteRule,
                    onAddRuleToBlocklist = viewModel::addRuleToBlocklist,
                    onSaveSchedule = viewModel::saveSchedule,
                    onDeleteSchedule = viewModel::deleteSchedule,
                    onSaveUsageLimit = viewModel::saveUsageLimit,
                    onDeleteUsageLimit = viewModel::deleteUsageLimit
                )
            }
        }
    }

    if (showDeviceAdminDisclosure) {
        AlertDialog(
            onDismissRequest = { showDeviceAdminDisclosure = false },
            title = { Text(SensitivePermissionDisclosure.deviceAdminTitle) },
            text = { Text(SensitivePermissionDisclosure.deviceAdminBody) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeviceAdminDisclosure = false
                        deviceAdminLauncher.launch(viewModel.createDeviceAdminEnableIntent())
                    }
                ) {
                    Text(SensitivePermissionDisclosure.deviceAdminConfirmAction)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeviceAdminDisclosure = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun WizBlockBottomBar(
    currentRoute: String?,
    onNavigateHome: () -> Unit,
    onNavigateBlocklists: () -> Unit,
    onNavigateRules: () -> Unit
) {
    val selectedTab = when {
        currentRoute == MainDestination.HOME.route -> MainTab.HOME
        currentRoute?.startsWith(MainDestination.BLOCKLIST_PREFIX) == true -> MainTab.BLOCKLISTS
        currentRoute == MainDestination.LOCK.route -> MainTab.RULES
        else -> MainTab.HOME
    }
    NavigationBar(containerColor = Color.White) {
        MainTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = selectedTab == tab,
                onClick = {
                    when (tab) {
                        MainTab.HOME -> onNavigateHome()
                        MainTab.BLOCKLISTS -> onNavigateBlocklists()
                        MainTab.RULES -> onNavigateRules()
                    }
                },
                icon = {
                    BottomTabIcon(tab = tab, selected = selectedTab == tab)
                },
                label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF1677FF),
                    selectedTextColor = Color(0xFF1677FF),
                    unselectedIconColor = Color(0xFF667085),
                    unselectedTextColor = Color(0xFF667085),
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}

@Composable
private fun BottomTabIcon(tab: MainTab, selected: Boolean) {
    val color = if (selected) Color(0xFF1677FF) else Color(0xFF667085)
    Canvas(modifier = Modifier.size(20.dp)) {
        val stroke = Stroke(width = 2.dp.toPx())
        when (tab) {
            MainTab.HOME -> {
                val roof = Path().apply {
                    moveTo(size.width * 0.16f, size.height * 0.48f)
                    lineTo(size.width * 0.50f, size.height * 0.18f)
                    lineTo(size.width * 0.84f, size.height * 0.48f)
                }
                drawPath(roof, color = color, style = stroke)
                drawLine(color, Offset(size.width * 0.26f, size.height * 0.46f), Offset(size.width * 0.26f, size.height * 0.84f), strokeWidth = stroke.width)
                drawLine(color, Offset(size.width * 0.74f, size.height * 0.46f), Offset(size.width * 0.74f, size.height * 0.84f), strokeWidth = stroke.width)
                drawLine(color, Offset(size.width * 0.26f, size.height * 0.84f), Offset(size.width * 0.74f, size.height * 0.84f), strokeWidth = stroke.width)
            }
            MainTab.BLOCKLISTS -> {
                listOf(0.28f, 0.50f, 0.72f).forEach { y ->
                    drawCircle(color, radius = 1.8.dp.toPx(), center = Offset(size.width * 0.20f, size.height * y))
                    drawLine(color, Offset(size.width * 0.34f, size.height * y), Offset(size.width * 0.82f, size.height * y), strokeWidth = stroke.width)
                }
            }
            MainTab.RULES -> {
                drawCircle(color, radius = size.minDimension * 0.28f, center = Offset(size.width / 2f, size.height / 2f), style = stroke)
                drawCircle(color, radius = size.minDimension * 0.08f, center = Offset(size.width / 2f, size.height / 2f))
                drawLine(color, Offset(size.width / 2f, size.height * 0.08f), Offset(size.width / 2f, size.height * 0.20f), strokeWidth = stroke.width)
                drawLine(color, Offset(size.width / 2f, size.height * 0.80f), Offset(size.width / 2f, size.height * 0.92f), strokeWidth = stroke.width)
                drawLine(color, Offset(size.width * 0.08f, size.height / 2f), Offset(size.width * 0.20f, size.height / 2f), strokeWidth = stroke.width)
                drawLine(color, Offset(size.width * 0.80f, size.height / 2f), Offset(size.width * 0.92f, size.height / 2f), strokeWidth = stroke.width)
            }
        }
    }
}
