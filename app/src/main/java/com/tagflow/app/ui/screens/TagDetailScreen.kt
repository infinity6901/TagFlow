@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.tagflow.app.ui.screens

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
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Nfc
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tagflow.app.action.ActionRunner
import com.tagflow.app.core.relativeTime
import com.tagflow.app.model.ActionCodec
import com.tagflow.app.model.ActionSpec
import com.tagflow.app.model.ActionType
import com.tagflow.app.nfc.BuildResult
import com.tagflow.app.nfc.NfcOp
import com.tagflow.app.nfc.WriteBuilder
import com.tagflow.app.nfc.WriteType
import com.tagflow.app.ui.components.AppPickerDialog
import com.tagflow.app.ui.components.EmptyState
import com.tagflow.app.ui.components.FormField
import com.tagflow.app.ui.components.ScreenHeader
import com.tagflow.app.ui.components.SectionTitle
import com.tagflow.app.ui.components.TagAvatar
import com.tagflow.app.ui.components.TagIcons
import com.tagflow.app.ui.components.icon
import com.tagflow.app.ui.theme.TagColors
import com.tagflow.app.ui.vm.NfcViewModel
import com.tagflow.app.ui.vm.TagDetailViewModel

private data class EditTarget(val index: Int, val spec: ActionSpec?)

@Composable
fun TagDetailScreen(vm: TagDetailViewModel, nfc: NfcViewModel, onBack: () -> Unit) {
    val tag by vm.tag.collectAsStateWithLifecycle()
    val logs by vm.logs.collectAsStateWithLifecycle()
    val t = tag
    if (t == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    var name by remember(t.id) { mutableStateOf(t.name) }
    var notes by remember(t.id) { mutableStateOf(t.notes) }
    var editing by remember { mutableStateOf<EditTarget?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<ActionRunner.Result>>(emptyList()) }
    val actions = remember(t.actionsJson) { ActionCodec.decode(t.actionsJson) }

    fun setActions(list: List<ActionSpec>) {
        val json = ActionCodec.encode(list)
        vm.update { it.copy(actionsJson = json) }
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ScreenHeader("Tag", t.uid, onBack = onBack) {
                IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Rounded.Delete, "Delete") }
            }
        }

        // identity
        item {
            Card(shape = MaterialTheme.shapes.extraLarge, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TagAvatar(t.colorIndex, t.iconKey, 56.dp)
                        Spacer(Modifier.width(14.dp))
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it; vm.update { e -> e.copy(name = it) } },
                            label = { Text("Name") }, singleLine = true,
                            modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.medium
                        )
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        TagColors.forEachIndexed { i, c ->
                            Box(
                                Modifier.size(34.dp).clip(CircleShape).background(c)
                                    .then(if (i == t.colorIndex) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                                    .clickable { vm.update { e -> e.copy(colorIndex = i) } },
                                contentAlignment = Alignment.Center
                            ) { if (i == t.colorIndex) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        TagIcons.forEach { (key, icon) ->
                            val sel = key == t.iconKey
                            Box(
                                Modifier.size(42.dp).clip(CircleShape)
                                    .background(if (sel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .clickable { vm.update { e -> e.copy(iconKey = key) } },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    icon, null, modifier = Modifier.size(22.dp),
                                    tint = if (sel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it; vm.update { e -> e.copy(notes = it) } },
                        label = { Text("Notes") }, minLines = 2,
                        modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Run actions on tap", style = MaterialTheme.typography.titleSmall)
                            Text("${t.scanCount} scans so far", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = t.enabled, onCheckedChange = { v -> vm.update { e -> e.copy(enabled = v) } })
                    }
                }
            }
        }

        // actions
        item { SectionTitle("Actions") }
        if (actions.isEmpty()) {
            item {
                EmptyState(Icons.Rounded.Bolt, "No actions yet", "Add actions to build a chain that runs in order each time this tag is tapped.")
            }
        }
        items(actions.size, key = { "a$it-${actions[it].type}-${actions[it].arg1}" }) { i ->
            val a = actions[i]
            Card(shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                Row(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(a.type.icon(), null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f).clickable { editing = EditTarget(i, a) }) {
                        Text("${i + 1}. ${a.type.label}", style = MaterialTheme.typography.titleSmall)
                        Text(a.summary(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                    }
                    IconButton(enabled = i > 0, onClick = {
                        val l = actions.toMutableList(); val x = l.removeAt(i); l.add(i - 1, x); setActions(l)
                    }) { Icon(Icons.Rounded.KeyboardArrowUp, "Move up") }
                    IconButton(enabled = i < actions.lastIndex, onClick = {
                        val l = actions.toMutableList(); val x = l.removeAt(i); l.add(i + 1, x); setActions(l)
                    }) { Icon(Icons.Rounded.KeyboardArrowDown, "Move down") }
                    IconButton(onClick = {
                        val l = actions.toMutableList(); l.removeAt(i); setActions(l)
                    }) { Icon(Icons.Rounded.Delete, "Remove") }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { editing = EditTarget(-1, null) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(8.dp)); Text("Add action")
                }
                FilledTonalButton(
                    onClick = { vm.runNow(actions) { results = it } },
                    enabled = actions.isNotEmpty(), modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.PlayArrow, null); Spacer(Modifier.width(8.dp)); Text("Test run")
                }
            }
        }
        if (results.isNotEmpty()) {
            item {
                Card(shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { ResultRows(results) }
                }
            }
        }

        // make tag open instantly
        item {
            Card(shape = MaterialTheme.shapes.extraLarge, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Run even when the app is closed", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Text(
                        "Write a tiny TagFlow link onto this tag. Tapping it will then launch TagFlow in the background and run your actions instantly (it keeps the same UID).",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    OutlinedButton(onClick = {
                        val built = WriteBuilder.build(WriteType.TAGFLOW, mapOf("label" to t.name), false)
                        if (built is BuildResult.Ok) nfc.startOp(NfcOp.Write(built.message, "Write TagFlow link"))
                    }) {
                        Icon(Icons.Rounded.Nfc, null); Spacer(Modifier.width(8.dp)); Text("Write TagFlow link")
                    }
                }
            }
        }

        // history
        if (logs.isNotEmpty()) {
            item { SectionTitle("Recent taps") }
            items(logs.take(8), key = { "l${it.id}" }) { l ->
                ListItem(
                    headlineContent = { Text(l.summary) },
                    supportingContent = { Text(relativeTime(l.timestamp) + if (l.ranActions) " · actions ran" else "") },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    editing?.let { target ->
        ActionEditorSheet(
            initial = target.spec,
            onDismiss = { editing = null },
            onSave = { spec ->
                val l = actions.toMutableList()
                if (target.index < 0) l.add(spec) else l[target.index] = spec
                setActions(l)
                editing = null
            }
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this tag?") },
            text = { Text("It will be removed from your library. The physical tag isn't changed.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; vm.delete(onBack) }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun ActionEditorSheet(initial: ActionSpec?, onSave: (ActionSpec) -> Unit, onDismiss: () -> Unit) {
    var type by remember { mutableStateOf(initial?.type) }
    var a1 by remember { mutableStateOf(initial?.arg1 ?: "") }
    var a2 by remember { mutableStateOf(initial?.arg2 ?: "") }
    var picking by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        val chosen = type
        if (chosen == null) {
            Text("Choose an action", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            LazyColumn(Modifier.fillMaxWidth().height(460.dp)) {
                items(ActionType.entries.toList()) { at ->
                    ListItem(
                        headlineContent = { Text(at.label) },
                        supportingContent = { Text(at.blurb) },
                        leadingContent = { Icon(at.icon(), null, tint = MaterialTheme.colorScheme.primary) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable {
                            type = at
                            a1 = at.fields.getOrNull(0)?.default ?: ""
                            a2 = at.fields.getOrNull(1)?.default ?: ""
                        }
                    )
                }
            }
        } else {
            val valid = chosen.fields.withIndex().all { (i, f) -> !f.required || (if (i == 0) a1 else a2).isNotBlank() }
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(chosen.icon(), null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(chosen.label, style = MaterialTheme.typography.titleLarge)
                        Text(chosen.blurb, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                chosen.fields.forEachIndexed { i, f ->
                    FormField(
                        spec = f, value = if (i == 0) a1 else a2,
                        onChange = { v -> if (i == 0) a1 = v else a2 = v },
                        onPick = if (f.picker) ({ picking = true }) else null
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (initial == null) OutlinedButton(onClick = { type = null }) { Text("Back") }
                    Button(onClick = { onSave(ActionSpec(chosen, a1.trim(), a2.trim())) }, enabled = valid, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Check, null); Spacer(Modifier.width(8.dp)); Text("Save action")
                    }
                }
            }
        }
    }
    if (picking) AppPickerDialog(onPick = { a1 = it; picking = false }, onDismiss = { picking = false })
}
