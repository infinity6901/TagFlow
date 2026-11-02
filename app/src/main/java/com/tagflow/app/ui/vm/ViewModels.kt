package com.tagflow.app.ui.vm

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tagflow.app.action.ActionRunner
import com.tagflow.app.data.AppSettings
import com.tagflow.app.data.ScanLogEntity
import com.tagflow.app.data.SettingsRepository
import com.tagflow.app.data.TagEntity
import com.tagflow.app.data.TagRepository
import com.tagflow.app.model.ActionSpec
import com.tagflow.app.nfc.NfcController
import com.tagflow.app.nfc.NfcOp
import com.tagflow.app.nfc.TagSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject

private val Lazily = SharingStarted.WhileSubscribed(5000)

@HiltViewModel
class NfcViewModel @Inject constructor(
    private val nfc: NfcController,
    private val repo: TagRepository
) : ViewModel() {
    val opState = nfc.opState
    val lastScan = nfc.lastScan
    val outcome = nfc.outcome
    val cloneBuffer = nfc.cloneBuffer
    val dump = nfc.dump

    val tags: StateFlow<List<TagEntity>> = repo.tags.stateIn(viewModelScope, Lazily, emptyList())
    val recentLogs: StateFlow<List<ScanLogEntity>> = repo.recentLogs(30).stateIn(viewModelScope, Lazily, emptyList())
    val logCount: StateFlow<Int> = repo.logCount.stateIn(viewModelScope, Lazily, 0)

    fun startOp(op: NfcOp) = nfc.startOp(op)
    fun cancelOp() = nfc.cancelOp()
    fun clearOp() = nfc.clearOp()
    fun clearOutcome() = nfc.clearOutcome()
    fun clearClone() = nfc.clearClone()

    fun saveToLibrary(s: TagSnapshot, onSaved: (Long) -> Unit) {
        viewModelScope.launch {
            val id = repo.save(TagEntity(uid = s.uid, name = nfc.defaultName(s), ndefBase64 = nfc.encodeNdef(s.ndefBytes)))
            onSaved(id)
        }
    }
}

@HiltViewModel
class LibraryViewModel @Inject constructor(private val repo: TagRepository) : ViewModel() {
    val tags: StateFlow<List<TagEntity>> = repo.tags.stateIn(viewModelScope, Lazily, emptyList())

    fun setEnabled(tag: TagEntity, enabled: Boolean) {
        viewModelScope.launch { repo.update(tag.copy(enabled = enabled)) }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TagDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: TagRepository,
    private val runner: ActionRunner
) : ViewModel() {
    private val id: Long = savedStateHandle.get<Long>("id") ?: -1L
    private val lock = Mutex()

    val tag: StateFlow<TagEntity?> = repo.tag(id).stateIn(viewModelScope, Lazily, null)
    val logs: StateFlow<List<ScanLogEntity>> = tag
        .flatMapLatest { t -> if (t == null) flowOf(emptyList()) else repo.logsFor(t.uid) }
        .stateIn(viewModelScope, Lazily, emptyList())

    fun update(transform: (TagEntity) -> TagEntity) {
        viewModelScope.launch {
            lock.withLock {
                val cur = repo.getById(id) ?: return@withLock
                repo.update(transform(cur))
            }
        }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            repo.delete(id)
            onDone()
        }
    }

    fun runNow(actions: List<ActionSpec>, onResult: (List<ActionRunner.Result>) -> Unit) {
        viewModelScope.launch { onResult(runner.runAll(actions)) }
    }
}

@HiltViewModel
class HistoryViewModel @Inject constructor(private val repo: TagRepository) : ViewModel() {
    val logs: StateFlow<List<ScanLogEntity>> = repo.recentLogs(300).stateIn(viewModelScope, Lazily, emptyList())
    fun clear() { viewModelScope.launch { repo.clearLogs() } }
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val repo: TagRepository
) : ViewModel() {
    val state: StateFlow<AppSettings> = settings.flow.stateIn(viewModelScope, Lazily, AppSettings())

    fun setTheme(v: Int) = viewModelScope.launch { settings.setTheme(v) }
    fun setDynamic(v: Boolean) = viewModelScope.launch { settings.setDynamic(v) }
    fun setAmoled(v: Boolean) = viewModelScope.launch { settings.setAmoled(v) }
    fun setAccent(v: Int) = viewModelScope.launch { settings.setAccent(v) }
    fun setHaptics(v: Boolean) = viewModelScope.launch { settings.setHaptics(v) }
    fun setRunInApp(v: Boolean) = viewModelScope.launch { settings.setRunInApp(v) }

    fun exportBackup(uri: Uri, cr: ContentResolver, onDone: (String) -> Unit) {
        viewModelScope.launch {
            val msg = try {
                val json = repo.exportJson()
                withContext(Dispatchers.IO) {
                    cr.openOutputStream(uri)?.use { it.write(json.toByteArray()) } ?: error("Can't open file")
                }
                "Backup saved"
            } catch (e: Exception) {
                "Export failed: ${e.message}"
            }
            onDone(msg)
        }
    }

    fun importBackup(uri: Uri, cr: ContentResolver, onDone: (String) -> Unit) {
        viewModelScope.launch {
            val msg = try {
                val json = withContext(Dispatchers.IO) {
                    cr.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) } ?: error("Can't open file")
                }
                val n = repo.importJson(json)
                "Imported $n tag${if (n == 1) "" else "s"}"
            } catch (e: Exception) {
                "Import failed: ${e.message}"
            }
            onDone(msg)
        }
    }

    fun clearHistory() { viewModelScope.launch { repo.clearLogs() } }
}
