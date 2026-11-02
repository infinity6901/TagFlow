@file:OptIn(ExperimentalMaterial3Api::class)

package com.tagflow.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tagflow.app.core.relativeTime
import com.tagflow.app.data.TagEntity
import com.tagflow.app.model.ActionCodec
import com.tagflow.app.ui.components.EmptyState
import com.tagflow.app.ui.components.ScreenHeader
import com.tagflow.app.ui.components.TagAvatar
import com.tagflow.app.ui.vm.LibraryViewModel

@Composable
fun LibraryScreen(vm: LibraryViewModel, onOpen: (Long) -> Unit, onAdd: () -> Unit) {
    val tags by vm.tags.collectAsStateWithLifecycle()
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ScreenHeader("Library", if (tags.isEmpty()) "Your tags live here" else "${tags.size} saved tag${if (tags.size == 1) "" else "s"}")
            }
            if (tags.isEmpty()) {
                item {
                    EmptyState(
                        Icons.Rounded.Sell, "No tags yet",
                        "Tap “Add tag”, hold a tag to your phone and give it actions — it’ll run them every time you tap it."
                    )
                }
            }
            items(tags, key = { it.id }) { t -> TagCard(t, onClick = { onOpen(t.id) }, onToggle = { vm.setEnabled(t, it) }) }
        }
        ExtendedFloatingActionButton(
            onClick = onAdd,
            icon = { Icon(Icons.Rounded.Add, null) },
            text = { Text("Add tag") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)
        )
    }
}

@Composable
private fun TagCard(t: TagEntity, onClick: () -> Unit, onToggle: (Boolean) -> Unit) {
    val n = ActionCodec.decode(t.actionsJson).size
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            TagAvatar(t.colorIndex, t.iconKey, 52.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(t.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(
                    "$n action${if (n == 1) "" else "s"} · ${t.scanCount} scan${if (t.scanCount == 1) "" else "s"}" +
                        if (t.lastScanAt > 0) " · ${relativeTime(t.lastScanAt)}" else "",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1
                )
            }
            Switch(checked = t.enabled, onCheckedChange = onToggle)
        }
    }
}
