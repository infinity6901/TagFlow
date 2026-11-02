@file:OptIn(ExperimentalMaterial3Api::class)

package com.tagflow.app.ui.screens

import android.nfc.NdefMessage
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Nfc
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tagflow.app.nfc.NfcOp
import com.tagflow.app.ui.components.ScreenHeader
import com.tagflow.app.ui.components.SectionTitle
import com.tagflow.app.ui.vm.NfcViewModel

private enum class ToolDialog { ERASE, LOCK, SET_PASSWORD, REMOVE_PASSWORD }

@Composable
fun ToolsScreen(vm: NfcViewModel) {
    val clone by vm.cloneBuffer.collectAsStateWithLifecycle()
    val dump by vm.dump.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<ToolDialog?>(null) }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ScreenHeader("Tools", "Power features for your tags") }

        item { SectionTitle("Inspect & copy") }
        item {
            ToolCard(Icons.Rounded.Memory, "Memory viewer", "Read every page of an NTAG as hex + ASCII") {
                vm.startOp(NfcOp.DumpMemory)
            }
        }
        if (dump.isNotEmpty()) {
            item {
                Card(shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                    Column(Modifier.padding(16.dp).horizontalScroll(rememberScrollState())) {
                        Text("PAGE  HEX          ASCII", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        dump.forEach { Text(it, fontFamily = FontFamily.Monospace, fontSize = 12.sp) }
                    }
                }
            }
        }
        item {
            Card(shape = MaterialTheme.shapes.extraLarge, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Nfc, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Clone a tag", style = MaterialTheme.typography.titleMedium)
                            Text(
                                clone?.let { "Source captured · ${it.size} bytes" } ?: "Step 1: read the source tag. Step 2: write it to another.",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = { vm.startOp(NfcOp.ReadForClone) }) { Text("1 · Read source") }
                        Button(
                            enabled = clone != null,
                            onClick = {
                                clone?.let { bytes ->
                                    runCatching { NdefMessage(bytes) }.getOrNull()?.let { vm.startOp(NfcOp.Write(it, "Write cloned data")) }
                                }
                            }
                        ) { Text("2 · Write copy") }
                        if (clone != null) TextButton(onClick = vm::clearClone) { Text("Clear") }
                    }
                }
            }
        }

        item { SectionTitle("Security") }
        item { ToolCard(Icons.Rounded.VpnKey, "Set password", "NTAG213/215/216 — require a 4-character password to write") { dialog = ToolDialog.SET_PASSWORD } }
        item { ToolCard(Icons.Rounded.Shield, "Remove password", "Unlock a tag you protected with TagFlow") { dialog = ToolDialog.REMOVE_PASSWORD } }

        item { SectionTitle("Danger zone") }
        item { ToolCard(Icons.Rounded.DeleteForever, "Erase tag", "Wipe all stored data", danger = true) { dialog = ToolDialog.ERASE } }
        item { ToolCard(Icons.Rounded.Lock, "Make read-only", "Permanently lock the tag — irreversible", danger = true) { dialog = ToolDialog.LOCK } }
        item { Spacer(Modifier.height(16.dp)) }
    }

    when (dialog) {
        ToolDialog.ERASE -> ConfirmDialog(
            "Erase tag?", "Everything stored on the next tag you tap will be wiped.", "Erase",
            onConfirm = { dialog = null; vm.startOp(NfcOp.Erase) }, onDismiss = { dialog = null }
        )
        ToolDialog.LOCK -> ConfirmDialog(
            "Lock tag forever?", "A read-only tag can never be written or erased again — by any app. Only do this on a tag you're finished with.", "Lock forever",
            onConfirm = { dialog = null; vm.startOp(NfcOp.MakeReadOnly) }, onDismiss = { dialog = null }
        )
        ToolDialog.SET_PASSWORD -> PasswordDialog(
            title = "Set tag password", confirmLabel = "Continue", askReadToo = true,
            onConfirm = { pw, readToo -> dialog = null; vm.startOp(NfcOp.SetPassword(pw, readToo)) },
            onDismiss = { dialog = null }
        )
        ToolDialog.REMOVE_PASSWORD -> PasswordDialog(
            title = "Remove tag password", confirmLabel = "Continue", askReadToo = false,
            onConfirm = { pw, _ -> dialog = null; vm.startOp(NfcOp.RemovePassword(pw)) },
            onDismiss = { dialog = null }
        )
        null -> {}
    }
}

@Composable
private fun ToolCard(icon: ImageVector, title: String, body: String, danger: Boolean = false, onClick: () -> Unit) {
    Card(
        onClick = onClick, shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = if (danger) MaterialTheme.colorScheme.errorContainer.copy(alpha = .45f) else MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ConfirmDialog(title: String, body: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirm, color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun PasswordDialog(
    title: String,
    confirmLabel: String,
    askReadToo: Boolean,
    onConfirm: (String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var pw by remember { mutableStateOf("") }
    var readToo by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = pw, onValueChange = { if (it.length <= 4) pw = it },
                    label = { Text("4-character password") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    shape = MaterialTheme.shapes.medium
                )
                if (askReadToo) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Also require it to read", style = MaterialTheme.typography.titleSmall)
                            Text("Other phones (and TagFlow) won't see the content. Remember the password!", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = readToo, onCheckedChange = { readToo = it })
                    }
                }
            }
        },
        confirmButton = { TextButton(enabled = pw.length == 4, onClick = { onConfirm(pw, readToo) }) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
