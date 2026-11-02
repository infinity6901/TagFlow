@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.tagflow.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Nfc
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tagflow.app.nfc.BuildResult
import com.tagflow.app.nfc.NfcOp
import com.tagflow.app.nfc.WriteBuilder
import com.tagflow.app.nfc.WriteType
import com.tagflow.app.ui.components.AppPickerDialog
import com.tagflow.app.ui.components.CapacityBar
import com.tagflow.app.ui.components.FormField
import com.tagflow.app.ui.components.ScreenHeader
import com.tagflow.app.ui.vm.NfcViewModel

@Composable
fun WriteScreen(vm: NfcViewModel) {
    val snap by vm.lastScan.collectAsStateWithLifecycle()
    var type by rememberSaveable { mutableStateOf(WriteType.TEXT) }
    val fields = remember { mutableStateMapOf<String, String>() }
    var addLauncher by rememberSaveable { mutableStateOf(false) }
    var lock by rememberSaveable { mutableStateOf(false) }
    var picking by remember { mutableStateOf(false) }

    val capacity = snap?.capacity?.takeIf { it > 0 } ?: 144 // NTAG213 by default
    val values = type.fields.associate { it.key to (fields["${type.name}.${it.key}"] ?: it.default) }
    val built = remember(type, values, addLauncher) { WriteBuilder.build(type, values, addLauncher) }
    val size = (built as? BuildResult.Ok)?.message?.byteArrayLength ?: 0
    val tooBig = size > capacity

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { ScreenHeader("Write", "Put something on a tag") }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(WriteType.entries.toList()) { wt ->
                    FilterChip(selected = wt == type, onClick = { type = wt }, label = { Text(wt.label) })
                }
            }
        }
        item {
            Card(shape = MaterialTheme.shapes.extraLarge, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(type.blurb, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    type.fields.forEach { f ->
                        FormField(
                            spec = f,
                            value = fields["${type.name}.${f.key}"] ?: f.default,
                            onChange = { fields["${type.name}.${f.key}"] = it },
                            onPick = if (f.picker) ({ picking = true }) else null
                        )
                    }
                }
            }
        }
        item {
            Card(shape = MaterialTheme.shapes.extraLarge, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CapacityBar(size, capacity)
                    if (built is BuildResult.Err) {
                        Text(built.reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else if (tooBig) {
                        Text("Too big for this tag — shorten the content.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                    if (type != WriteType.TAGFLOW && type != WriteType.APP) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Also open TagFlow on tap", style = MaterialTheme.typography.titleSmall)
                                Text("Adds an app-launch record (+33 bytes)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = addLauncher, onCheckedChange = { addLauncher = it })
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Lock after writing", style = MaterialTheme.typography.titleSmall)
                            Text("Makes the tag permanently read-only. Can't be undone.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                        Switch(checked = lock, onCheckedChange = { lock = it })
                    }
                }
            }
        }
        item {
            Button(
                onClick = { (built as? BuildResult.Ok)?.let { vm.startOp(NfcOp.Write(it.message, "Write ${type.label}", lock)) } },
                enabled = built is BuildResult.Ok && !tooBig,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(Icons.Rounded.Nfc, null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text("Write to tag")
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }

    if (picking) {
        AppPickerDialog(
            onPick = { fields["${type.name}.pkg"] = it; picking = false },
            onDismiss = { picking = false }
        )
    }
}
