package com.wizblock.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wizblock.model.DailyBlockSummary
import com.wizblock.ui.RecentBlockDisplayItem
import java.text.DateFormat
import java.util.Date

private val CardShape = RoundedCornerShape(10.dp)
private val ScreenBackground = Color(0xFFF4F6FB)
private val TextPrimary = Color(0xFF1A202C)
private val TextMuted = Color(0xFF667085)
private val RowSurface = Color(0xFFF8FAFF)

@Composable
fun RecentHistoryScreen(
    dailySummary: DailyBlockSummary,
    recentEvents: List<RecentBlockDisplayItem>,
    onBack: () -> Unit
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
                Text("Recent blocks", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
                Text("What WizBlock stopped today and most recently.", style = MaterialTheme.typography.bodyLarge, color = TextMuted)
            }
        }
        item {
            Surface(modifier = Modifier.fillMaxWidth(), color = Color.White, shape = CardShape) {
                Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HistoryMetric("Blocks", dailySummary.total.toString(), Modifier.weight(1f))
                    HistoryMetric("Apps", dailySummary.appBlocks.toString(), Modifier.weight(1f))
                    HistoryMetric("Sites", dailySummary.domainBlocks.toString(), Modifier.weight(1f))
                }
            }
        }
        if (recentEvents.isEmpty()) {
            item {
                Surface(modifier = Modifier.fillMaxWidth(), color = Color.White, shape = CardShape) {
                    Text(
                        "No blocks recorded yet.",
                        modifier = Modifier.padding(16.dp),
                        color = TextMuted,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } else {
            items(recentEvents, key = { it.id }) { event ->
                RecentEventRow(event)
            }
        }
    }
}

@Composable
private fun HistoryMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = RowSurface, shape = CardShape) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(label, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
        }
    }
}

@Composable
private fun RecentEventRow(event: RecentBlockDisplayItem) {
    Surface(modifier = Modifier.fillMaxWidth(), color = Color.White, shape = CardShape) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(event.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text("${event.subtitle} - ${event.reason}", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
            Text(DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(event.blockedAt)), style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
    }
}
