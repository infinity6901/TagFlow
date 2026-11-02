@file:OptIn(ExperimentalMaterial3Api::class)

package com.tagflow.app.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ClearAll
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tagflow.app.ui.components.EmptyState
import com.tagflow.app.ui.components.ScreenHeader
import com.tagflow.app.ui.vm.HistoryViewModel

@Composable
fun HistoryScreen(vm: HistoryViewModel, onBack: () -> Unit) {
    val logs by vm.logs.collectAsStateWithLifecycle()
    LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)) {
        item {
            ScreenHeader("Activity", "${logs.size} recent taps", onBack = onBack) {
                IconButton(onClick = vm::clear, enabled = logs.isNotEmpty()) { Icon(Icons.Rounded.ClearAll, "Clear history") }
            }
        }
        if (logs.isEmpty()) {
            item { EmptyState(Icons.Rounded.History, "Nothing yet", "Every tag you tap shows up here.", Modifier.padding(top = 24.dp)) }
        }
        items(logs, key = { it.id }) { LogRow(it) }
    }
}
