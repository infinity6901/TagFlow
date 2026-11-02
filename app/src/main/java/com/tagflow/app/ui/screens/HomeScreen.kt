@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.tagflow.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Nfc
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tagflow.app.action.ActionRunner
import com.tagflow.app.core.copyToClipboard
import com.tagflow.app.core.relativeTime
import com.tagflow.app.data.ScanLogEntity
import com.tagflow.app.data.TagEntity
import com.tagflow.app.nfc.ActionOutcome
import com.tagflow.app.nfc.NfcStatus
import com.tagflow.app.nfc.Protection
import com.tagflow.app.nfc.RecordInfo
import com.tagflow.app.nfc.TagSnapshot
import com.tagflow.app.ui.components.CapacityBar
import com.tagflow.app.ui.components.Pill
import com.tagflow.app.ui.components.ScanOrb
import com.tagflow.app.ui.components.ScreenHeader
import com.tagflow.app.ui.components.SectionTitle
import com.tagflow.app.ui.components.TagAvatar
import com.tagflow.app.ui.components.icon
import com.tagflow.app.ui.vm.NfcViewModel

@Composable
fun HomeScreen(
    vm: NfcViewModel,
    nfcStatus: NfcStatus,
    onOpenNfcSettings: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenTag: (Long) -> Unit
) {
    val snap by vm.lastScan.collectAsStateWithLifecycle()
    val outcome by vm.outcome.collectAsStateWithLifecycle()
    val tags by vm.tags.collectAsStateWithLifecycle()
    val logs by vm.recentLogs.collectAsStateWithLifecycle()
    val logCount by vm.logCount.collectAsStateWithLifecycle()
    val current = snap
    val saved = current?.let { s -> tags.firstOrNull { it.uid == s.uid } }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ScreenHeader("TagFlow", "Tap. Trigger. Done.") {
                IconButton(onClick = onOpenSettings) { Icon(Icons.Rounded.Settings, "Settings") }
            }
        }
        if (nfcStatus != NfcStatus.Ready) item { NfcBanner(nfcStatus, onOpenNfcSettings) }
        item { HeroCard() }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(tags.size.toString(), "Saved tags", Modifier.weight(1f))
                StatCard(logCount.toString(), "Total scans", Modifier.weight(1f))
                StatCard(tags.count { it.enabled }.toString(), "Active", Modifier.weight(1f))
            }
        }
        if (current != null) {
            item(key = current.time) {
                ScanDetailCard(
                    s = current, saved = saved,
                    onSave = { vm.saveToLibrary(current, onOpenTag) },
                    onOpen = { saved?.let { onOpenTag(it.id) } }
                )
            }
        }
        outcome?.let { o ->
            item(key = "outcome-${o.time}") { OutcomeCard(o, onDismiss = vm::clearOutcome) }
        }
        if (logs.isNotEmpty()) {
            item { SectionTitle("Recent activity", "See all" to onOpenHistory) }
            items(logs.take(4), key = { it.id }) { LogRow(it) }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun HeroCard() {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ScanOrb(Modifier.size(210.dp))
            Text("Ready to scan", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(
                "Hold a tag against the back of your phone",
                style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .75f)
            )
        }
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier, shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun NfcBanner(status: NfcStatus, onOpenSettings: () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Warning, null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.width(12.dp))
            Text(
                if (status == NfcStatus.Disabled) "NFC is turned off." else "This device has no NFC.",
                color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f)
            )
            if (status == NfcStatus.Disabled) TextButton(onClick = onOpenSettings) { Text("Turn on") }
        }
    }
}

@Composable
private fun ScanDetailCard(s: TagSnapshot, saved: TagEntity?, onSave: () -> Unit, onOpen: () -> Unit) {
    val ctx = LocalContext.current
    ElevatedCard(shape = MaterialTheme.shapes.extraLarge) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (saved != null) {
                    TagAvatar(saved.colorIndex, saved.iconKey, 46.dp)
                } else {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.tertiaryContainer, modifier = Modifier.size(46.dp)) {
                        Icon(Icons.Rounded.Nfc, null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.padding(11.dp))
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(saved?.name ?: "New tag detected", style = MaterialTheme.typography.titleLarge)
                    Text("${s.chip} · ${relativeTime(s.time)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(
                s.uid, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.clickable { ctx.copyToClipboard("UID", s.uid) }
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill(
                    when (s.protection) {
                        Protection.NONE -> "No password"
                        Protection.WRITE -> "Write-protected"
                        Protection.READ_WRITE -> "Password-locked"
                        Protection.UNKNOWN -> "Protection unknown"
                    }
                )
                Pill(if (s.writable) "Writable" else "Read-only")
                if (s.type != "Unknown") Pill(s.type)
            }
            CapacityBar(s.used, s.capacity)
            if (s.records.isEmpty()) {
                Text("This tag has no data stored on it.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                s.records.forEach { RecordCard(it) }
            }
            if (saved == null) {
                Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Save to library")
                }
            } else {
                FilledTonalButton(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Sell, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Open in library")
                }
            }
        }
    }
}

@Composable
fun RecordCard(r: RecordInfo) {
    val ctx = LocalContext.current
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(r.kind.icon(), null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(r.title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (r.value.isNotEmpty()) Text(r.value, style = MaterialTheme.typography.bodyLarge, maxLines = 4)
            }
            if (r.value.isNotEmpty()) {
                IconButton(onClick = { ctx.copyToClipboard(r.title, r.value) }) { Icon(Icons.Rounded.ContentCopy, "Copy") }
            }
            if (r.openUri != null) {
                IconButton(onClick = {
                    runCatching {
                        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(r.openUri)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                }) { Icon(Icons.Rounded.OpenInNew, "Open") }
            }
        }
    }
}

@Composable
fun OutcomeCard(o: ActionOutcome, onDismiss: () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(Modifier.padding(start = 20.dp, top = 12.dp, bottom = 16.dp, end = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Actions ran · ${o.tagName}", style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, "Dismiss", tint = MaterialTheme.colorScheme.onTertiaryContainer) }
            }
            ResultRows(o.results)
        }
    }
}

@Composable
fun ResultRows(results: List<ActionRunner.Result>) {
    results.forEach { r ->
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                if (r.ok) Icons.Rounded.CheckCircle else Icons.Rounded.Error, null,
                tint = if (r.ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text("${r.spec.type.label} — ${r.message}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun LogRow(l: ScanLogEntity) {
    ListItem(
        headlineContent = { Text(l.tagName ?: l.uid, maxLines = 1) },
        supportingContent = { Text("${l.summary} · ${relativeTime(l.timestamp)}", maxLines = 1) },
        leadingContent = {
            Icon(
                if (l.ranActions) Icons.Rounded.CheckCircle else Icons.Rounded.History, null,
                tint = if (l.ranActions) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}
