@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.tagflow.app.ui.screens

import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ClearAll
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tagflow.app.ui.components.ScreenHeader
import com.tagflow.app.ui.components.SectionTitle
import com.tagflow.app.ui.theme.AccentNames
import com.tagflow.app.ui.theme.accentPreview
import com.tagflow.app.ui.vm.SettingsViewModel

@Composable
fun SettingsScreen(vm: SettingsViewModel, onBack: () -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val toast = { m: String -> Toast.makeText(ctx, m, Toast.LENGTH_SHORT).show() }

    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) vm.exportBackup(uri, ctx.contentResolver, toast)
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importBackup(uri, ctx.contentResolver, toast)
    }
    val dynamicAvailable = Build.VERSION.SDK_INT >= 31

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ScreenHeader("Settings", onBack = onBack) }

        item { SectionTitle("Appearance") }
        item {
            Card(shape = MaterialTheme.shapes.extraLarge, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    val labels = listOf("System", "Light", "Dark")
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        labels.forEachIndexed { i, l ->
                            SegmentedButton(
                                selected = s.theme == i, onClick = { vm.setTheme(i) },
                                shape = SegmentedButtonDefaults.itemShape(i, labels.size),
                                label = { Text(l) }
                            )
                        }
                    }
                    if (dynamicAvailable) {
                        ToggleRow("Material You colors", "Use your wallpaper's palette", s.dynamic) { vm.setDynamic(it) }
                    }
                    if (!dynamicAvailable || !s.dynamic) {
                        Text("Accent", style = MaterialTheme.typography.titleSmall)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            AccentNames.forEachIndexed { i, n ->
                                Box(
                                    Modifier.size(40.dp).clip(CircleShape).background(accentPreview(i))
                                        .then(if (i == s.accent) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                                        .clickable { vm.setAccent(i) },
                                    contentAlignment = Alignment.Center
                                ) { if (i == s.accent) Icon(Icons.Rounded.Check, n, tint = Color.White, modifier = Modifier.size(20.dp)) }
                            }
                        }
                    }
                    ToggleRow("AMOLED black", "Pure black background in dark mode", s.amoled) { vm.setAmoled(it) }
                }
            }
        }

        item { SectionTitle("Behavior") }
        item {
            Card(shape = MaterialTheme.shapes.extraLarge, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ToggleRow("Haptic feedback", "Buzz when a tag is detected", s.haptics) { vm.setHaptics(it) }
                    ToggleRow("Run actions while app is open", "Turn off to only inspect tags in the app", s.runInApp) { vm.setRunInApp(it) }
                }
            }
        }

        item { SectionTitle("Data") }
        item {
            Card(shape = MaterialTheme.shapes.extraLarge, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column {
                    ListItem(
                        headlineContent = { Text("Export backup") }, supportingContent = { Text("Save your tags and actions as JSON") },
                        leadingContent = { Icon(Icons.Rounded.FileUpload, null) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { exporter.launch("tagflow-backup.json") }
                    )
                    ListItem(
                        headlineContent = { Text("Import backup") }, supportingContent = { Text("Restore tags from a JSON backup") },
                        leadingContent = { Icon(Icons.Rounded.FileDownload, null) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { importer.launch(arrayOf("application/json", "text/plain", "*/*")) }
                    )
                    ListItem(
                        headlineContent = { Text("Clear activity history") },
                        leadingContent = { Icon(Icons.Rounded.ClearAll, null) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { vm.clearHistory(); toast("History cleared") }
                    )
                }
            }
        }

        item { SectionTitle("About") }
        item {
            Card(shape = MaterialTheme.shapes.extraLarge, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("TagFlow 1.0.0", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Tip: an NTAG213 holds 144 bytes. Short links, a TagFlow link and small Wi‑Fi records fit comfortably; vCards with many fields may not.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun ToggleRow(title: String, body: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
