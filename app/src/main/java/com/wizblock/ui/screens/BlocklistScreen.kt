package com.wizblock.ui.screens

import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.wizblock.data.repository.BlocklistTargetDraft
import com.wizblock.model.Profile
import com.wizblock.model.Rule
import com.wizblock.model.RuleKind
import com.wizblock.model.TargetType
import com.wizblock.ui.BlocklistSummaryItem
import com.wizblock.ui.BlocklistUiModel
import com.wizblock.ui.BlockedTargetSettingsItem
import com.wizblock.ui.BlockedTargetUiFormatter
import com.wizblock.ui.InstalledAppItem
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val CardShape = RoundedCornerShape(10.dp)
private val SheetShape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
private val ScreenBackground = Color(0xFFF4F6FB)
private val AccentBlue = Color(0xFF2E90FF)
private val SoftBlue = Color(0xFFE8F1FF)
private val RowSurface = Color(0xFFF8FAFF)
private val TextPrimary = Color(0xFF1A202C)
private val TextMuted = Color(0xFF667085)

private enum class TargetFilter(val label: String) {
    APPS("Apps"),
    WEBSITES("Websites"),
    KEYWORDS("Keywords"),
    BLOCKLISTS("Blocklists")
}

private enum class DetailEditor {
    NONE,
    SCHEDULE,
    LIMIT
}

private enum class BlocklistLimitMode {
    TIME,
    OPENS
}

@Composable
fun BlocklistScreen(
    initialTab: RuleKind,
    installedApps: List<InstalledAppItem>,
    blockedAppPackages: Set<String>,
    websiteRules: List<Rule>,
    keywordRules: List<Rule>,
    allRules: List<Rule>,
    blockedTargets: List<BlockedTargetSettingsItem>,
    blocklistSummaries: List<BlocklistSummaryItem>,
    profiles: List<Profile>,
    onBack: () -> Unit,
    onToggleAppBlocked: (String, Boolean) -> Unit,
    onAddWebsite: (String) -> Unit,
    onAddKeyword: (String) -> Unit,
    onToggleRule: (String, Boolean) -> Unit,
    onCreateBlocklist: (String, List<BlocklistTargetDraft>) -> Unit,
    onToggleBlocklist: (String, Boolean) -> Unit,
    onUpdateBlocklist: (Profile, String, Boolean) -> Unit,
    onDeleteBlocklist: (String) -> Unit,
    onDeleteRule: (String) -> Unit,
    onAddRuleToBlocklist: (String, RuleKind, String) -> Unit,
    onSaveSchedule: (TargetType, String, Int, Int, Int) -> Unit,
    onDeleteSchedule: (String) -> Unit,
    onSaveUsageLimit: (TargetType, String, Int, Int) -> Unit,
    onDeleteUsageLimit: (String) -> Unit
) {
    var selectedFilter by rememberSaveable(initialTab) { mutableStateOf(initialTab.toTargetFilter()) }
    var showAddPanel by rememberSaveable { mutableStateOf(false) }
    var showSaveBlocklistSheet by rememberSaveable { mutableStateOf(false) }
    var websiteInput by rememberSaveable { mutableStateOf("") }
    var keywordInput by rememberSaveable { mutableStateOf("") }
    var showAppPicker by remember { mutableStateOf(false) }
    var appPickerProfileId by remember { mutableStateOf<String?>(null) }
    var pickerQuery by remember { mutableStateOf("") }
    var pickerSelection by remember { mutableStateOf(blockedAppPackages) }
    var selectedTarget by remember { mutableStateOf<BlockedTargetSettingsItem?>(null) }
    var editingBlocklistId by rememberSaveable { mutableStateOf<String?>(null) }

    fun openPicker(profileId: String? = null) {
        appPickerProfileId = profileId
        pickerSelection = if (profileId == null) {
            blockedAppPackages
        } else {
            allRules
                .filter { it.profileId == profileId && it.kind == RuleKind.APP_PACKAGE && it.enabled }
                .map { it.value }
                .toSet()
        }
        pickerQuery = ""
        showAppPicker = true
    }

    fun removeTarget(target: BlockedTargetSettingsItem) {
        when (target.targetType) {
            TargetType.APP_PACKAGE -> onToggleAppBlocked(target.targetValue, false)
            TargetType.DOMAIN -> websiteRules.filter { it.value.equals(target.targetValue, ignoreCase = true) }
                .forEach { onToggleRule(it.id, false) }
            TargetType.KEYWORD -> keywordRules.filter { it.value.equals(target.targetValue, ignoreCase = true) }
                .forEach { onToggleRule(it.id, false) }
        }
        selectedTarget = null
    }

    val filteredTargets = remember(blockedTargets, selectedFilter) {
        blockedTargets.filter { target ->
            when (selectedFilter) {
                TargetFilter.APPS -> target.targetType == TargetType.APP_PACKAGE
                TargetFilter.WEBSITES -> target.targetType == TargetType.DOMAIN
                TargetFilter.KEYWORDS -> target.targetType == TargetType.KEYWORD
                TargetFilter.BLOCKLISTS -> false
            }
        }
    }
    val saveTargets = remember(filteredTargets) {
        BlocklistUiModel.saveableAdHocTargets(filteredTargets)
    }
    val editingProfile = profiles.firstOrNull { it.id == editingBlocklistId }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onBack) { Text("Back") }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (selectedFilter != TargetFilter.BLOCKLISTS && saveTargets.isNotEmpty()) {
                            TextButton(onClick = { showSaveBlocklistSheet = true }) {
                                Text("Save as Blocklist")
                            }
                        }
                        Button(
                            onClick = {
                                if (selectedFilter == TargetFilter.BLOCKLISTS) {
                                    showSaveBlocklistSheet = true
                                } else {
                                    showAddPanel = !showAddPanel
                                }
                            },
                            shape = CardShape
                        ) {
                            Text(if (selectedFilter == TargetFilter.BLOCKLISTS) "New" else if (showAddPanel) "Close" else "Add")
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Blocklists",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Block apps, websites, and keywords.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextMuted
                    )
                }
            }

            item {
                FilterRow(selectedFilter = selectedFilter, onSelected = { selectedFilter = it })
            }

            if (showAddPanel) {
                item {
                    AddTargetPanel(
                        selectedFilter = selectedFilter,
                        websiteInput = websiteInput,
                        keywordInput = keywordInput,
                        onWebsiteInputChange = { websiteInput = it },
                        onKeywordInputChange = { keywordInput = it },
                        onOpenPicker = { openPicker(null) },
                        onAddWebsite = {
                            val value = websiteInput.trim()
                            if (value.isNotBlank()) {
                                onAddWebsite(value)
                                websiteInput = ""
                                showAddPanel = false
                            }
                        },
                        onAddKeyword = {
                            val value = keywordInput.trim()
                            if (value.isNotBlank()) {
                                onAddKeyword(value)
                                keywordInput = ""
                                showAddPanel = false
                            }
                        }
                    )
                }
            }

            if (selectedFilter == TargetFilter.BLOCKLISTS) {
                if (blocklistSummaries.isEmpty()) {
                    item {
                        EmptyTargetState(selectedFilter = selectedFilter)
                    }
                } else {
                    items(blocklistSummaries, key = { it.id }) { summary ->
                        BlocklistCard(
                            summary = summary,
                            onOpen = { editingBlocklistId = summary.id },
                            onToggle = { enabled -> onToggleBlocklist(summary.id, enabled) }
                        )
                    }
                    item {
                        Text(
                            "Blocklists run locally on your device. No internet required.",
                            color = TextMuted,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)
                        )
                    }
                }
            } else if (filteredTargets.isEmpty()) {
                item {
                    EmptyTargetState(selectedFilter = selectedFilter)
                }
            } else {
                item {
                    TargetListCard(
                        targets = filteredTargets,
                        onOpenTarget = { selectedTarget = it },
                        onToggleTarget = { target, enabled ->
                            when (target.targetType) {
                                TargetType.APP_PACKAGE -> onToggleAppBlocked(target.targetValue, enabled)
                                TargetType.DOMAIN, TargetType.KEYWORD -> target.ruleId?.let { onToggleRule(it, enabled) }
                            }
                        }
                    )
                }
            }
        }

        if (showAppPicker) {
            val pickerProfileId = appPickerProfileId
            AppPickerOverlay(
                installedApps = installedApps,
                searchQuery = pickerQuery,
                selectedPackages = pickerSelection,
                onSearchQueryChange = { pickerQuery = it },
                onTogglePackage = { packageName, selected ->
                    pickerSelection = if (selected) {
                        pickerSelection + packageName
                    } else {
                        pickerSelection.filterNot { it.equals(packageName, ignoreCase = true) }.toSet()
                    }
                },
                onDismiss = {
                    showAppPicker = false
                    appPickerProfileId = null
                },
                onConfirm = {
                    val currentPackages = if (pickerProfileId == null) {
                        blockedAppPackages
                    } else {
                        allRules
                            .filter { it.profileId == pickerProfileId && it.kind == RuleKind.APP_PACKAGE && it.enabled }
                            .map { it.value }
                            .toSet()
                    }
                    val normalizedCurrent = currentPackages.map { it.lowercase() }.toSet()
                    val normalizedSelected = pickerSelection.map { it.lowercase() }.toSet()
                    val selectedByLowercase = pickerSelection.associateBy { it.lowercase() }
                    val toAdd = normalizedSelected - normalizedCurrent
                    val toRemove = normalizedCurrent - normalizedSelected
                    if (pickerProfileId == null) {
                        toAdd.forEach { onToggleAppBlocked(selectedByLowercase[it] ?: it, true) }
                        toRemove.forEach { onToggleAppBlocked(it, false) }
                    } else {
                        toAdd.forEach { onAddRuleToBlocklist(pickerProfileId, RuleKind.APP_PACKAGE, selectedByLowercase[it] ?: it) }
                        toRemove.forEach { packageName ->
                            allRules
                                .filter {
                                    it.profileId == pickerProfileId &&
                                        it.kind == RuleKind.APP_PACKAGE &&
                                        it.value.equals(packageName, ignoreCase = true)
                                }
                                .forEach { onDeleteRule(it.id) }
                        }
                    }
                    showAppPicker = false
                    appPickerProfileId = null
                    showAddPanel = false
                }
            )
        }

        if (showSaveBlocklistSheet) {
            SaveBlocklistSheet(
                selectedFilter = selectedFilter,
                selectedTargets = saveTargets,
                onDismiss = { showSaveBlocklistSheet = false },
                onCreate = { name ->
                    onCreateBlocklist(name, saveTargets)
                    showSaveBlocklistSheet = false
                }
            )
        }

        editingProfile?.let { profile ->
            BlocklistEditorOverlay(
                profile = profile,
                rules = allRules.filter { it.profileId == profile.id && it.enabled },
                installedApps = installedApps,
                onDismiss = { editingBlocklistId = null },
                onSave = { name, enabled ->
                    onUpdateBlocklist(profile, name, enabled)
                    editingBlocklistId = null
                },
                onDelete = {
                    onDeleteBlocklist(profile.id)
                    editingBlocklistId = null
                },
                onDeleteRule = onDeleteRule,
                onAddApps = { openPicker(profile.id) },
                onAddWebsite = { onAddRuleToBlocklist(profile.id, RuleKind.DOMAIN, it) },
                onAddKeyword = { onAddRuleToBlocklist(profile.id, RuleKind.KEYWORD, it) }
            )
        }

        selectedTarget?.let { target ->
            TargetDetailSheet(
                target = target,
                onDismiss = { selectedTarget = null },
                onRemove = { removeTarget(target) },
                onSaveSchedule = { startMinute, endMinute, daysMask ->
                    onSaveSchedule(target.targetType, target.targetValue, startMinute, endMinute, daysMask)
                    selectedTarget = null
                },
                onClearSchedule = {
                    target.schedule?.let { onDeleteSchedule(it.id) }
                    selectedTarget = null
                },
                onSaveUsageLimit = { minutesPerDay, opensPerDay ->
                    onSaveUsageLimit(target.targetType, target.targetValue, minutesPerDay, opensPerDay)
                    selectedTarget = null
                },
                onClearLimit = {
                    target.usageLimit?.let { onDeleteUsageLimit(it.id) }
                    selectedTarget = null
                }
            )
        }
    }
}

@Composable
private fun FilterRow(
    selectedFilter: TargetFilter,
    onSelected: (TargetFilter) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        TargetFilter.entries.forEach { filter ->
            Surface(
                modifier = Modifier
                    .widthIn(min = 0.dp)
                    .clickable { onSelected(filter) },
                color = if (selectedFilter == filter) AccentBlue else Color.White,
                shape = CardShape
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        filter.label,
                        color = if (selectedFilter == filter) Color.White else Color(0xFF475467),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun AddTargetPanel(
    selectedFilter: TargetFilter,
    websiteInput: String,
    keywordInput: String,
    onWebsiteInputChange: (String) -> Unit,
    onKeywordInputChange: (String) -> Unit,
    onOpenPicker: () -> Unit,
    onAddWebsite: () -> Unit,
    onAddKeyword: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxWidth(), color = Color.White, shape = CardShape) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Add blocked item", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            if (selectedFilter == TargetFilter.APPS) {
                Button(onClick = onOpenPicker, modifier = Modifier.fillMaxWidth(), shape = CardShape) {
                    Text("Choose apps")
                }
            }
            if (selectedFilter == TargetFilter.WEBSITES) {
                AddTextRow(
                    value = websiteInput,
                    onValueChange = onWebsiteInputChange,
                    label = "Add website",
                    onAction = onAddWebsite
                )
            }
            if (selectedFilter == TargetFilter.KEYWORDS) {
                AddTextRow(
                    value = keywordInput,
                    onValueChange = onKeywordInputChange,
                    label = "Add keyword",
                    onAction = onAddKeyword
                )
            }
            if (selectedFilter == TargetFilter.BLOCKLISTS) {
                Text(
                    "Use New to create a named Blocklist.",
                    color = TextMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun AddTextRow(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.weight(1f),
            singleLine = true
        )
        Button(
            onClick = onAction,
            enabled = value.isNotBlank(),
            shape = CardShape
        ) {
            Text("Add")
        }
    }
}

@Composable
private fun TargetListCard(
    targets: List<BlockedTargetSettingsItem>,
    onOpenTarget: (BlockedTargetSettingsItem) -> Unit,
    onToggleTarget: (BlockedTargetSettingsItem, Boolean) -> Unit
) {
    Surface(modifier = Modifier.fillMaxWidth(), color = Color.White, shape = CardShape) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            targets.forEachIndexed { index, target ->
                TargetRow(
                    target = target,
                    onClick = { onOpenTarget(target) },
                    onToggle = { enabled -> onToggleTarget(target, enabled) }
                )
                if (index < targets.lastIndex) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp),
                        color = Color(0xFFE5EAF3)
                    ) {
                        Box(modifier = Modifier.size(1.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TargetRow(
    target: BlockedTargetSettingsItem,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit
) {
    val schedule = BlockedTargetUiFormatter.scheduleSummary(target)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TargetAvatar(target = target)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(target.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "${BlockedTargetUiFormatter.targetTypeLabel(target.targetType)} - ${schedule.value}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (target.blocklistNames.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    target.blocklistNames.take(3).forEach { name ->
                        SmallChip(name)
                    }
                }
            }
        }
        Switch(
            checked = target.enabled,
            onCheckedChange = onToggle
        )
    }
}

@Composable
private fun TargetAvatar(target: BlockedTargetSettingsItem) {
    val context = LocalContext.current
    if (target.targetType == TargetType.APP_PACKAGE) {
        AppIcon(context = context, packageName = target.targetValue)
    } else {
        Surface(
            modifier = Modifier.size(38.dp),
            shape = CircleShape,
            color = SoftBlue
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    target.title.take(1).uppercase(),
                    color = AccentBlue,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun BlocklistCard(
    summary: BlocklistSummaryItem,
    onOpen: () -> Unit,
    onToggle: (Boolean) -> Unit
) {
    Surface(modifier = Modifier.fillMaxWidth(), color = Color.White, shape = CardShape) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(modifier = Modifier.size(44.dp), color = SoftBlue, shape = CircleShape) {
                Box(contentAlignment = Alignment.Center) {
                    Text(summary.name.take(1).uppercase(), color = AccentBlue, fontWeight = FontWeight.Bold)
                }
            }
            Column(modifier = Modifier.weight(1f).padding(end = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(summary.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    if (summary.appCount > 0) SmallChip("${summary.appCount} apps")
                    if (summary.websiteCount > 0) SmallChip("${summary.websiteCount} sites")
                    if (summary.keywordCount > 0) SmallChip("${summary.keywordCount} keywords")
                    if (summary.appCount + summary.websiteCount + summary.keywordCount == 0) SmallChip("Empty")
                }
                Text("Always blocked", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
            }
            Switch(
                checked = summary.enabled,
                onCheckedChange = onToggle
            )
        }
    }
}

@Composable
private fun SmallChip(label: String) {
    Surface(color = SoftBlue, shape = RoundedCornerShape(6.dp)) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            color = AccentBlue,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SaveBlocklistSheet(
    selectedFilter: TargetFilter,
    selectedTargets: List<BlocklistTargetDraft>,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.44f))
                .clickable(
                    onClick = onDismiss,
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                )
        )
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            shape = SheetShape,
            color = Color.White
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Surface(modifier = Modifier.size(width = 38.dp, height = 4.dp), color = Color(0xFFD0D5DD), shape = CircleShape) {}
                }
                Text("Save as Blocklist", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Blocklist name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Surface(color = RowSurface, shape = CardShape) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Will save ${selectedTargets.size} ${selectedFilter.label.lowercase()}", fontWeight = FontWeight.SemiBold)
                        Text("You can edit apps, websites, and keywords after creating it.", color = TextMuted)
                    }
                }
                Button(
                    onClick = { onCreate(name) },
                    enabled = name.isNotBlank() && selectedTargets.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = CardShape
                ) {
                    Text("Create Blocklist")
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
private fun BlocklistEditorOverlay(
    profile: Profile,
    rules: List<Rule>,
    installedApps: List<InstalledAppItem>,
    onDismiss: () -> Unit,
    onSave: (String, Boolean) -> Unit,
    onDelete: () -> Unit,
    onDeleteRule: (String) -> Unit,
    onAddApps: () -> Unit,
    onAddWebsite: (String) -> Unit,
    onAddKeyword: (String) -> Unit
) {
    var name by rememberSaveable(profile.id) { mutableStateOf(profile.name) }
    var enabled by rememberSaveable(profile.id) { mutableStateOf(profile.enabled) }
    var tab by rememberSaveable(profile.id) { mutableStateOf(TargetFilter.APPS) }
    var websiteInput by rememberSaveable(profile.id) { mutableStateOf("") }
    var keywordInput by rememberSaveable(profile.id) { mutableStateOf("") }
    val filteredRules = rules.filter { it.kind == tab.toRuleKindOrNull() }

    Surface(modifier = Modifier.fillMaxSize(), color = ScreenBackground) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss) { Text("<") }
                    Text(profile.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text("...", style = MaterialTheme.typography.titleLarge)
                }
            }
            item {
                Surface(color = Color.White, shape = CardShape) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Blocklist name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        SettingToggleRow("Enabled", "Blocks in this list are active.", enabled, { enabled = it })
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Schedule", fontWeight = FontWeight.SemiBold)
                                Text("Always blocked", color = TextMuted)
                            }
                            Text("Edit", color = AccentBlue, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(TargetFilter.APPS, TargetFilter.WEBSITES, TargetFilter.KEYWORDS).forEach { option ->
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { tab = option },
                            color = if (tab == option) AccentBlue else Color.White,
                            shape = CardShape
                        ) {
                            Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                Text(option.label, color = if (tab == option) Color.White else TextPrimary, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
            items(filteredRules, key = { it.id }) { rule ->
                BlocklistRuleRow(
                    rule = rule,
                    installedApps = installedApps,
                    onDelete = { onDeleteRule(rule.id) }
                )
            }
            item {
                when (tab) {
                    TargetFilter.APPS -> Button(onClick = onAddApps, modifier = Modifier.fillMaxWidth(), shape = CardShape) { Text("+ Add apps") }
                    TargetFilter.WEBSITES -> AddTextRow(
                        value = websiteInput,
                        onValueChange = { websiteInput = it },
                        label = "Add website",
                        onAction = {
                            if (websiteInput.isNotBlank()) {
                                onAddWebsite(websiteInput)
                                websiteInput = ""
                            }
                        }
                    )
                    TargetFilter.KEYWORDS -> AddTextRow(
                        value = keywordInput,
                        onValueChange = { keywordInput = it },
                        label = "Add keyword",
                        onAction = {
                            if (keywordInput.isNotBlank()) {
                                onAddKeyword(keywordInput)
                                keywordInput = ""
                            }
                        }
                    )
                    TargetFilter.BLOCKLISTS -> Unit
                }
            }
            item {
                Surface(color = Color.White, shape = CardShape) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDelete, modifier = Modifier.weight(1f)) {
                            Text("Delete Blocklist", color = Color.Red)
                        }
                        Button(onClick = { onSave(name, enabled) }, modifier = Modifier.weight(1f), shape = CardShape) {
                            Text("Save changes")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BlocklistRuleRow(
    rule: Rule,
    installedApps: List<InstalledAppItem>,
    onDelete: () -> Unit
) {
    val app = installedApps.firstOrNull { it.packageName.equals(rule.value, ignoreCase = true) }
    Surface(modifier = Modifier.fillMaxWidth(), color = Color.White, shape = CardShape) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (rule.kind == RuleKind.APP_PACKAGE) {
                AppIcon(context = LocalContext.current, packageName = rule.value)
            } else {
                Surface(modifier = Modifier.size(38.dp), color = SoftBlue, shape = CircleShape) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(rule.value.take(1).uppercase(), color = AccentBlue, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(app?.label ?: rule.value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(if (rule.kind == RuleKind.APP_PACKAGE) rule.value else rule.kind.name.lowercase(), color = TextMuted)
            }
            Surface(modifier = Modifier.size(34.dp).clickable(onClick = onDelete), color = RowSurface, shape = CircleShape) {
                Box(contentAlignment = Alignment.Center) { Text("x", color = TextMuted, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun EmptyTargetState(selectedFilter: TargetFilter) {
    Surface(modifier = Modifier.fillMaxWidth(), color = Color.White, shape = CardShape) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("No ${selectedFilter.label.lowercase()} yet", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleLarge)
            Text("Tap Add to choose what WizBlock should stop.", color = TextMuted, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun TargetDetailSheet(
    target: BlockedTargetSettingsItem,
    onDismiss: () -> Unit,
    onRemove: () -> Unit,
    onSaveSchedule: (Int, Int, Int) -> Unit,
    onClearSchedule: () -> Unit,
    onSaveUsageLimit: (Int, Int) -> Unit,
    onClearLimit: () -> Unit
) {
    var editor by rememberSaveable(target.targetType.name, target.targetValue) { mutableStateOf(DetailEditor.SCHEDULE) }
    val schedule = BlockedTargetUiFormatter.scheduleSummary(target)
    val usageLimit = BlockedTargetUiFormatter.usageLimitSummary(target)
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.44f))
                .clickable(
                    onClick = onDismiss,
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                )
        )
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.95f),
            shape = SheetShape,
            color = Color.White
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentPadding = PaddingValues(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Surface(
                            modifier = Modifier.size(width = 46.dp, height = 5.dp),
                            color = Color(0xFFD0D7E2),
                            shape = CircleShape
                        ) {}
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            TargetAvatar(target = target)
                            Column {
                                Text(target.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                                Text(
                                    BlockedTargetUiFormatter.targetTypeLabel(target.targetType),
                                    color = TextMuted,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        TextButton(onClick = onDismiss) { Text("Done") }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SummaryTile(
                            label = schedule.label,
                            value = schedule.value,
                            selected = editor == DetailEditor.SCHEDULE,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                editor = if (editor == DetailEditor.SCHEDULE) DetailEditor.NONE else DetailEditor.SCHEDULE
                            }
                        )
                        SummaryTile(
                            label = usageLimit.label,
                            value = usageLimit.value,
                            selected = editor == DetailEditor.LIMIT,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                editor = if (editor == DetailEditor.LIMIT) DetailEditor.NONE else DetailEditor.LIMIT
                            }
                        )
                    }
                }
                if (editor == DetailEditor.SCHEDULE) {
                    item {
                        ScheduleEditor(
                            target = target,
                            onSaveSchedule = onSaveSchedule,
                            onClearSchedule = if (target.schedule == null) null else onClearSchedule
                        )
                    }
                }

                if (editor == DetailEditor.LIMIT) {
                    item {
                        UsageLimitEditor(
                            target = target,
                            onSaveUsageLimit = onSaveUsageLimit,
                            onClearLimit = if (target.usageLimit == null) null else onClearLimit
                        )
                    }
                }

                item {
                    TextButton(onClick = onRemove, modifier = Modifier.fillMaxWidth()) {
                        Text("Remove from blocklist")
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryTile(
    label: String,
    value: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(78.dp)
            .clickable(onClick = onClick),
        color = if (selected) SoftBlue else RowSurface,
        shape = CardShape
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(label, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            Text(value, color = AccentBlue, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ScheduleEditor(
    target: BlockedTargetSettingsItem,
    onSaveSchedule: (Int, Int, Int) -> Unit,
    onClearSchedule: (() -> Unit)?
) {
    val existing = target.schedule
    var allDay by remember(existing?.id, target.targetValue) { mutableStateOf(existing?.startMinute == existing?.endMinute) }
    var startMinute by remember(existing?.id, target.targetValue) { mutableStateOf(existing?.startMinute ?: (8 * 60)) }
    var endMinute by remember(existing?.id, target.targetValue) { mutableStateOf(existing?.endMinute ?: (18 * 60)) }
    var daysMask by remember(existing?.id, target.targetValue) { mutableStateOf(existing?.daysMask ?: ALL_DAYS_MASK) }
    val selectedPreset = when {
        allDay && daysMask == ALL_DAYS_MASK -> "Always"
        !allDay && startMinute == 9 * 60 && endMinute == 17 * 60 && daysMask == WEEKDAYS_MASK -> "Work hours"
        !allDay && startMinute == 17 * 60 && endMinute == 22 * 60 && daysMask == ALL_DAYS_MASK -> "Evening"
        else -> "Custom"
    }

    Surface(modifier = Modifier.fillMaxWidth(), color = Color.White, shape = CardShape) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PresetRow(
                values = listOf(
                    SchedulePreset("Always", 0, 0, ALL_DAYS_MASK),
                    SchedulePreset("Work hours", 9 * 60, 17 * 60, WEEKDAYS_MASK),
                    SchedulePreset("Evening", 17 * 60, 22 * 60, ALL_DAYS_MASK),
                    SchedulePreset("Custom", startMinute, endMinute, daysMask)
                ),
                selectedLabel = selectedPreset,
                label = { it.label },
                onSelected = { preset ->
                    if (preset.label == "Custom") {
                        allDay = false
                    } else {
                        allDay = preset.startMinute == preset.endMinute
                        startMinute = preset.startMinute
                        endMinute = preset.endMinute
                        daysMask = preset.daysMask
                    }
                }
            )
            SettingToggleRow(
                title = "All day",
                subtitle = if (allDay) "Block on selected days for the whole day" else "Use a specific time window",
                checked = allDay,
                onCheckedChange = { checked ->
                    allDay = checked
                    if (checked) {
                        startMinute = 0
                        endMinute = 0
                    } else if (startMinute == endMinute) {
                        startMinute = 8 * 60
                        endMinute = 18 * 60
                    }
                }
            )
            if (!allDay) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TimePickerChip(
                        label = "Start",
                        minuteOfDay = startMinute,
                        modifier = Modifier.weight(1f),
                        onMinuteSelected = { startMinute = it }
                    )
                    TimePickerChip(
                        label = "End",
                        minuteOfDay = endMinute,
                        modifier = Modifier.weight(1f),
                        onMinuteSelected = { endMinute = it }
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Days", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                DaySelector(daysMask = daysMask, onDaysMaskChanged = { daysMask = it })
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val safeStart = if (allDay) 0 else startMinute
                        val safeEnd = if (allDay) 0 else endMinute
                        onSaveSchedule(safeStart, safeEnd, daysMask)
                    },
                    modifier = Modifier.weight(1f),
                    shape = CardShape,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White)
                ) {
                    Text("Save schedule")
                }
                if (onClearSchedule != null) {
                    TextButton(onClick = onClearSchedule, modifier = Modifier.weight(1f)) {
                        Text("Clear")
                    }
                }
            }
        }
    }
}

@Composable
private fun UsageLimitEditor(
    target: BlockedTargetSettingsItem,
    onSaveUsageLimit: (Int, Int) -> Unit,
    onClearLimit: (() -> Unit)?
) {
    val existing = target.usageLimit
    var mode by remember(existing?.id, target.targetValue) {
        mutableStateOf(if ((existing?.opensPerDay ?: 0) > 0) BlocklistLimitMode.OPENS else BlocklistLimitMode.TIME)
    }
    var value by remember(existing?.id, target.targetValue) {
        mutableStateOf(
            when {
                (existing?.opensPerDay ?: 0) > 0 -> existing?.opensPerDay ?: 3
                (existing?.minutesPerDay ?: 0) > 0 -> existing?.minutesPerDay ?: 15
                else -> 30
            }
        )
    }
    val presets = if (mode == BlocklistLimitMode.TIME) listOf(15, 30, 60, 120) else listOf(1, 3, 5, 10)

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ModeSelector(mode = mode, onModeSelected = { selected ->
            mode = selected
            value = if (selected == BlocklistLimitMode.TIME) maxOf(value, 15) else maxOf(1, value)
        })
        ValueStepper(mode = mode, value = value, onValueChange = { value = it.coerceAtLeast(1) })
        PresetRow(
            values = presets,
            selectedLabel = value.toString(),
            label = { preset -> if (mode == BlocklistLimitMode.TIME) "${preset}m" else "$preset" },
            onSelected = { value = it }
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    val minutesPerDay = if (mode == BlocklistLimitMode.TIME) value else 0
                    val opensPerDay = if (mode == BlocklistLimitMode.OPENS) value else 0
                    onSaveUsageLimit(minutesPerDay, opensPerDay)
                },
                modifier = Modifier.weight(1f),
                shape = CardShape,
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White)
            ) {
                Text("Save limit")
            }
            if (onClearLimit != null) {
                TextButton(onClick = onClearLimit, modifier = Modifier.weight(1f)) {
                    Text("Clear")
                }
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(modifier = Modifier.fillMaxWidth(), color = RowSurface, shape = CardShape) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleLarge)
                Text(subtitle, color = TextMuted, style = MaterialTheme.typography.bodyMedium)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun TimePickerChip(
    label: String,
    minuteOfDay: Int,
    modifier: Modifier = Modifier,
    onMinuteSelected: (Int) -> Unit
) {
    val context = LocalContext.current
    Surface(
        modifier = modifier.clickable {
            TimePickerDialog(
                context,
                { _, hourOfDay, minute -> onMinuteSelected(hourOfDay * 60 + minute) },
                minuteOfDay / 60,
                minuteOfDay % 60,
                false
            ).show()
        },
        color = RowSurface,
        shape = CardShape
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(label, color = TextMuted, style = MaterialTheme.typography.bodyMedium)
            Text(formatMinuteOfDay(minuteOfDay), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun DaySelector(daysMask: Int, onDaysMaskChanged: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DAY_OPTIONS.forEach { option ->
            val selected = daysMask and option.mask != 0
            Surface(
                modifier = Modifier
                    .size(42.dp)
                    .clickable {
                        val updated = if (selected) daysMask and option.mask.inv() else daysMask or option.mask
                        if (updated != 0) {
                            onDaysMaskChanged(updated)
                        }
                    },
                color = if (selected) AccentBlue else Color(0xFFF1F5F9),
                shape = CircleShape
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = option.label,
                        color = if (selected) Color.White else TextMuted,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeSelector(mode: BlocklistLimitMode, onModeSelected: (BlocklistLimitMode) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        BlocklistLimitMode.entries.forEach { option ->
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onModeSelected(option) },
                color = if (mode == option) AccentBlue else Color(0xFFF1F5F9),
                shape = CardShape
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (option == BlocklistLimitMode.TIME) "Time per day" else "Launches per day",
                        color = if (mode == option) Color.White else TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun ValueStepper(
    mode: BlocklistLimitMode,
    value: Int,
    onValueChange: (Int) -> Unit
) {
    val step = if (mode == BlocklistLimitMode.TIME) 15 else 1
    Surface(modifier = Modifier.fillMaxWidth(), color = RowSurface, shape = CardShape) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StepButton(label = "-", onClick = { onValueChange(maxOf(1, value - step)) })
            Text(
                text = if (mode == BlocklistLimitMode.TIME) "$value min" else "$value opens",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            StepButton(label = "+", onClick = { onValueChange(value + step) })
        }
    }
}

@Composable
private fun StepButton(label: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(44.dp)
            .clickable(onClick = onClick),
        color = Color.White,
        shape = CircleShape,
        tonalElevation = 1.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun <T> PresetRow(
    values: List<T>,
    selectedLabel: String,
    label: (T) -> String,
    onSelected: (T) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        values.forEach { value ->
            val text = label(value)
            Surface(
                modifier = Modifier
                    .widthIn(min = 0.dp)
                    .clickable { onSelected(value) },
                color = if (text == selectedLabel) SoftBlue else RowSurface,
                shape = CardShape
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = text,
                        color = if (text == selectedLabel) Color(0xFF175CD3) else Color(0xFF475467),
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
private fun AppPickerOverlay(
    installedApps: List<InstalledAppItem>,
    searchQuery: String,
    selectedPackages: Set<String>,
    onSearchQueryChange: (String) -> Unit,
    onTogglePackage: (String, Boolean) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val context = LocalContext.current
    val filteredApps = remember(installedApps, searchQuery) {
        val query = searchQuery.trim().lowercase()
        installedApps.filter { app ->
            query.isBlank() ||
                app.label.lowercase().contains(query) ||
                app.packageName.lowercase().contains(query)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.44f))
                .clickable(
                    onClick = onDismiss,
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                )
        )
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.84f),
            shape = SheetShape,
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Add apps", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Search installed apps and select the ones you want blocked.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Search apps") }
                )

                Text(
                    text = "${selectedPackages.size} selected",
                    style = MaterialTheme.typography.labelLarge,
                    color = AccentBlue
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        val selected = selectedPackages.any { it.equals(app.packageName, ignoreCase = true) }
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = RowSurface,
                            shape = CardShape
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    AppIcon(context = context, packageName = app.packageName)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(app.label, style = MaterialTheme.typography.titleLarge)
                                        Text(
                                            app.packageName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF7C8799),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Checkbox(
                                    checked = selected,
                                    onCheckedChange = { onTogglePackage(app.packageName, it) }
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Close")
                    }
                    Button(onClick = onConfirm, modifier = Modifier.weight(1f), shape = CardShape) {
                        Text("Apply")
                    }
                }
            }
        }
    }
}

@Composable
private fun AppIcon(
    context: Context,
    packageName: String
) {
    val iconDrawable = remember(packageName) {
        runCatching { context.packageManager.getApplicationIcon(packageName) }.getOrNull()
    }
    if (iconDrawable != null) {
        Image(
            bitmap = iconDrawable.toBitmap(64, 64).asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.size(34.dp)
        )
    } else {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(SoftBlue, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("A", color = AccentBlue, fontWeight = FontWeight.Bold)
        }
    }
}

private fun formatMinuteOfDay(minuteOfDay: Int): String {
    val safe = minuteOfDay.coerceIn(0, 1439)
    val localTime = LocalTime.of(safe / 60, safe % 60)
    return localTime.format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
}

private fun RuleKind.toTargetFilter(): TargetFilter {
    return when (this) {
        RuleKind.APP_PACKAGE -> TargetFilter.APPS
        RuleKind.DOMAIN -> TargetFilter.WEBSITES
        RuleKind.KEYWORD -> TargetFilter.KEYWORDS
    }
}

private fun TargetType.toRuleKind(): RuleKind {
    return when (this) {
        TargetType.APP_PACKAGE -> RuleKind.APP_PACKAGE
        TargetType.DOMAIN -> RuleKind.DOMAIN
        TargetType.KEYWORD -> RuleKind.KEYWORD
    }
}

private fun TargetFilter.toRuleKindOrNull(): RuleKind? {
    return when (this) {
        TargetFilter.APPS -> RuleKind.APP_PACKAGE
        TargetFilter.WEBSITES -> RuleKind.DOMAIN
        TargetFilter.KEYWORDS -> RuleKind.KEYWORD
        TargetFilter.BLOCKLISTS -> null
    }
}

private data class BlocklistDayOption(val label: String, val longLabel: String, val mask: Int)

private data class SchedulePreset(
    val label: String,
    val startMinute: Int,
    val endMinute: Int,
    val daysMask: Int
)

private val DAY_OPTIONS = listOf(
    BlocklistDayOption("S", "Sun", 1 shl 6),
    BlocklistDayOption("M", "Mon", 1 shl 0),
    BlocklistDayOption("T", "Tue", 1 shl 1),
    BlocklistDayOption("W", "Wed", 1 shl 2),
    BlocklistDayOption("T", "Thu", 1 shl 3),
    BlocklistDayOption("F", "Fri", 1 shl 4),
    BlocklistDayOption("S", "Sat", 1 shl 5)
)

private const val ALL_DAYS_MASK = 0b1111111
private const val WEEKDAYS_MASK = (1 shl 0) or (1 shl 1) or (1 shl 2) or (1 shl 3) or (1 shl 4)
