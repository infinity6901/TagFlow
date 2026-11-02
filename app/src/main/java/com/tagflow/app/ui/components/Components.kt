@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.tagflow.app.ui.components

import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Nfc
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tagflow.app.model.ActionType
import com.tagflow.app.model.FieldSpec
import com.tagflow.app.model.KeyKind
import com.tagflow.app.nfc.OpState
import com.tagflow.app.nfc.RecordKind
import com.tagflow.app.ui.theme.TagColors
import kotlinx.coroutines.delay

// ───────────────────────── icon maps ─────────────────────────

val TagIcons: List<Pair<String, ImageVector>> = listOf(
    "tag" to Icons.Rounded.Sell,
    "home" to Icons.Rounded.Home,
    "work" to Icons.Rounded.Work,
    "car" to Icons.Rounded.DirectionsCar,
    "star" to Icons.Rounded.Star,
    "heart" to Icons.Rounded.Favorite,
    "bolt" to Icons.Rounded.Bolt,
    "alarm" to Icons.Rounded.Alarm,
    "music" to Icons.Rounded.MusicNote,
    "wifi" to Icons.Rounded.Wifi,
    "lock" to Icons.Rounded.Lock,
    "person" to Icons.Rounded.Person
)

fun tagIcon(key: String): ImageVector = TagIcons.firstOrNull { it.first == key }?.second ?: Icons.Rounded.Sell

fun ActionType.icon(): ImageVector = when (this) {
    ActionType.OPEN_URL -> Icons.Rounded.Link
    ActionType.LAUNCH_APP -> Icons.Rounded.Apps
    ActionType.TORCH -> Icons.Rounded.FlashlightOn
    ActionType.ALARM -> Icons.Rounded.Alarm
    ActionType.TIMER -> Icons.Rounded.Timer
    ActionType.DIAL -> Icons.Rounded.Phone
    ActionType.SMS -> Icons.Rounded.Sms
    ActionType.NOTIFY -> Icons.Rounded.Notifications
    ActionType.MEDIA -> Icons.Rounded.MusicNote
    ActionType.RINGER -> Icons.Rounded.VolumeUp
    ActionType.DND -> Icons.Rounded.DoNotDisturbOn
    ActionType.VOLUME -> Icons.Rounded.VolumeUp
    ActionType.CLIPBOARD -> Icons.Rounded.ContentPaste
    ActionType.PANEL -> Icons.Rounded.Wifi
    ActionType.MAPS -> Icons.Rounded.Place
    ActionType.VIBRATE -> Icons.Rounded.Vibration
    ActionType.WEBHOOK -> Icons.Rounded.Public
    ActionType.DELAY -> Icons.Rounded.HourglassEmpty
}

fun RecordKind.icon(): ImageVector = when (this) {
    RecordKind.TEXT -> Icons.Rounded.TextFields
    RecordKind.URL -> Icons.Rounded.Link
    RecordKind.PHONE -> Icons.Rounded.Phone
    RecordKind.EMAIL -> Icons.Rounded.Email
    RecordKind.SMS -> Icons.Rounded.Sms
    RecordKind.GEO -> Icons.Rounded.Place
    RecordKind.WIFI -> Icons.Rounded.Wifi
    RecordKind.CONTACT -> Icons.Rounded.Person
    RecordKind.APP -> Icons.Rounded.Apps
    RecordKind.MIME -> Icons.Rounded.Language
    RecordKind.EMPTY -> Icons.Rounded.Nfc
    RecordKind.OTHER -> Icons.Rounded.Nfc
}

// ───────────────────────── small building blocks ─────────────────────────

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
            Spacer(Modifier.width(4.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        actions()
    }
}

@Composable
fun SectionTitle(text: String, action: Pair<String, () -> Unit>? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text, style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f)
        )
        if (action != null) TextButton(onClick = action.second) { Text(action.first) }
    }
}

@Composable
fun Pill(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.secondaryContainer, onColor: Color = MaterialTheme.colorScheme.onSecondaryContainer) {
    Surface(shape = CircleShape, color = color, modifier = modifier) {
        Text(
            text, style = MaterialTheme.typography.labelMedium, color = onColor,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun TagAvatar(colorIndex: Int, iconKey: String, size: Dp = 48.dp) {
    val c = TagColors[colorIndex.coerceIn(0, TagColors.lastIndex)]
    Box(
        Modifier.size(size).clip(CircleShape).background(Brush.linearGradient(listOf(c, c.copy(alpha = .65f)))),
        contentAlignment = Alignment.Center
    ) {
        Icon(tagIcon(iconKey), null, tint = Color.White, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
fun CapacityBar(used: Int, capacity: Int, modifier: Modifier = Modifier) {
    if (capacity <= 0) return
    val frac = (used.toFloat() / capacity).coerceIn(0f, 1f)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        LinearProgressIndicator(
            progress = { frac },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
            color = if (frac > .95f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Text(
            "$used / $capacity bytes used",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, body: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(72.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(34.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

// ───────────────────────── scan orb ─────────────────────────

@Composable
fun ScanOrb(modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "orb")
    val p by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(2800, easing = LinearEasing), RepeatMode.Restart),
        label = "p"
    )
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val maxR = size.minDimension / 2f
            for (i in 0..2) {
                val ph = (p + i / 3f) % 1f
                drawCircle(
                    color = primary.copy(alpha = (1f - ph) * 0.35f),
                    radius = maxR * (0.4f + 0.6f * ph),
                    center = Offset(size.width / 2f, size.height / 2f),
                    style = Stroke(width = 3.dp.toPx())
                )
            }
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(primary.copy(alpha = .30f), Color.Transparent),
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = maxR * 0.7f
                ),
                radius = maxR * 0.7f,
                center = Offset(size.width / 2f, size.height / 2f)
            )
        }
        Surface(shape = CircleShape, color = primary, shadowElevation = 6.dp, modifier = Modifier.size(88.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Nfc, null, tint = onPrimary, modifier = Modifier.size(42.dp))
            }
        }
    }
}

// ───────────────────────── forms ─────────────────────────

private fun optionLabel(o: String) = o.replace('_', ' ').replaceFirstChar { it.uppercase() }

@Composable
fun FormField(
    spec: FieldSpec,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onPick: (() -> Unit)? = null
) {
    if (spec.options.isNotEmpty()) {
        Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(spec.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                spec.options.forEach { opt ->
                    FilterChip(selected = value == opt, onClick = { onChange(opt) }, label = { Text(optionLabel(opt)) })
                }
            }
        }
    } else {
        val trailing: (@Composable () -> Unit)? = if (spec.picker && onPick != null) {
            { IconButton(onClick = onPick) { Icon(Icons.Rounded.Apps, "Pick app") } }
        } else null
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = modifier.fillMaxWidth(),
            label = { Text(spec.label + if (!spec.required) " (optional)" else "") },
            placeholder = { if (spec.hint.isNotEmpty()) Text(spec.hint) },
            singleLine = !spec.multiline,
            minLines = if (spec.multiline) 3 else 1,
            visualTransformation = if (spec.secret) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                keyboardType = when (spec.kind) {
                    KeyKind.NUMBER -> KeyboardType.Number
                    KeyKind.PHONE -> KeyboardType.Phone
                    KeyKind.URI -> KeyboardType.Uri
                    KeyKind.EMAIL -> KeyboardType.Email
                    KeyKind.TEXT -> KeyboardType.Text
                }
            ),
            trailingIcon = trailing,
            shape = MaterialTheme.shapes.medium
        )
    }
}

private data class AppEntry(val label: String, val pkg: String)

@Suppress("DEPRECATION")
@Composable
fun AppPickerDialog(onPick: (String) -> Unit, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var query by remember { mutableStateOf("") }
    val apps = remember {
        val pm = ctx.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .map { AppEntry(it.loadLabel(pm).toString(), it.activityInfo.packageName) }
            .distinctBy { it.pkg }
            .sortedBy { it.label.lowercase() }
    }
    val filtered = apps.filter { query.isBlank() || it.label.contains(query, true) || it.pkg.contains(query, true) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pick an app") },
        text = {
            Column {
                OutlinedTextField(
                    value = query, onValueChange = { query = it }, singleLine = true,
                    label = { Text("Search") }, modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.height(320.dp)) {
                    items(filtered, key = { it.pkg }) { app ->
                        ListItem(
                            headlineContent = { Text(app.label) },
                            supportingContent = { Text(app.pkg, style = MaterialTheme.typography.labelSmall) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable { onPick(app.pkg) }
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

// ───────────────────────── NFC operation sheet ─────────────────────────

@Composable
fun OpSheet(state: OpState, onCancel: () -> Unit, onClose: () -> Unit) {
    if (state is OpState.Idle) return
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(state) {
        if (state is OpState.Done) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            if (state.success) {
                delay(1700)
                onClose()
            }
        }
    }
    ModalBottomSheet(
        onDismissRequest = { if (state is OpState.Done) onClose() else onCancel() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (state) {
                is OpState.Waiting -> {
                    ScanOrb(Modifier.size(190.dp))
                    Text(state.op.title, style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Hold the back of your phone against the tag and keep it steady.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    OutlinedButton(onClick = onCancel) { Text("Cancel") }
                }
                is OpState.Working -> {
                    Spacer(Modifier.height(24.dp))
                    CircularProgressIndicator(Modifier.size(56.dp))
                    Text("Working…", style = MaterialTheme.typography.titleLarge)
                    Text("Don't move the tag.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(24.dp))
                }
                is OpState.Done -> {
                    Spacer(Modifier.height(16.dp))
                    Icon(
                        if (state.success) Icons.Rounded.CheckCircle else Icons.Rounded.Error, null,
                        tint = if (state.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(72.dp)
                    )
                    Text(if (state.success) "Done" else "Didn't work", style = MaterialTheme.typography.titleLarge)
                    Text(state.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    Button(onClick = onClose) { Text(if (state.success) "Close" else "OK") }
                }
                else -> {}
            }
        }
    }
}
