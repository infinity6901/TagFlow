package com.tagflow.app.nfc

import android.nfc.Tag
import android.nfc.TagLostException
import android.util.Base64
import com.tagflow.app.action.ActionRunner
import com.tagflow.app.core.toHex
import com.tagflow.app.data.ScanLogEntity
import com.tagflow.app.data.SettingsRepository
import com.tagflow.app.data.TagEntity
import com.tagflow.app.data.TagRepository
import com.tagflow.app.model.ActionCodec
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

data class ActionOutcome(val tagName: String, val results: List<ActionRunner.Result>, val time: Long)

/**
 * Single entry point for every tag that touches the phone. When an operation (write, lock, …) is
 * waiting it consumes the next tag; otherwise the tag is scanned and — if it is saved — its
 * actions run.
 */
@Singleton
class NfcController @Inject constructor(
    private val repo: TagRepository,
    private val settings: SettingsRepository,
    private val runner: ActionRunner
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    private val _opState = MutableStateFlow<OpState>(OpState.Idle)
    val opState: StateFlow<OpState> = _opState.asStateFlow()

    private val _lastScan = MutableStateFlow<TagSnapshot?>(null)
    val lastScan: StateFlow<TagSnapshot?> = _lastScan.asStateFlow()

    private val _outcome = MutableStateFlow<ActionOutcome?>(null)
    val outcome: StateFlow<ActionOutcome?> = _outcome.asStateFlow()

    private val _clone = MutableStateFlow<ByteArray?>(null)
    val cloneBuffer: StateFlow<ByteArray?> = _clone.asStateFlow()

    private val _dump = MutableStateFlow<List<String>>(emptyList())
    val dump: StateFlow<List<String>> = _dump.asStateFlow()

    fun startOp(op: NfcOp) { _opState.value = OpState.Waiting(op) }
    fun cancelOp() { if (_opState.value !is OpState.Working) _opState.value = OpState.Idle }
    fun clearOp() { _opState.value = OpState.Idle }
    fun clearOutcome() { _outcome.value = null }
    fun clearClone() { _clone.value = null }

    fun onTag(tag: Tag) {
        scope.launch { mutex.withLock { process(tag) } }
    }

    /** Called by the invisible TapActivity. Returns true if the tag was a saved tag and its actions ran. */
    suspend fun handleBackground(tag: Tag): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val saved = repo.findByUid(tag.id.toHex(":"))
            if (saved == null || !saved.enabled || ActionCodec.decode(saved.actionsJson).isEmpty()) {
                return@withLock false
            }
            scan(tag, runActions = true)
            true
        }
    }

    fun defaultName(s: TagSnapshot): String {
        val text = s.records.firstOrNull { it.kind == RecordKind.TEXT }?.value?.trim().orEmpty()
        return if (text.isNotEmpty()) text.take(24) else "Tag " + s.uid.replace(":", "").takeLast(5)
    }

    fun encodeNdef(bytes: ByteArray?): String? = bytes?.let { Base64.encodeToString(it, Base64.NO_WRAP) }

    private suspend fun process(tag: Tag) {
        val st = _opState.value
        if (st is OpState.Waiting) runOp(st.op, tag) else scan(tag, settings.flow.first().runInApp)
    }

    private suspend fun runOp(op: NfcOp, tag: Tag) {
        _opState.value = OpState.Working(op)
        _opState.value = try {
            OpState.Done(op, true, execute(op, tag))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            OpState.Done(op, false, friendly(e))
        }
    }

    private fun friendly(e: Exception): String = when (e) {
        is TagLostException -> "Tag moved away — hold it steady and try again"
        is IOException -> "Couldn't talk to the tag (${e.message ?: "I/O error"}). Hold steady and retry"
        else -> e.message ?: "Something went wrong"
    }

    private fun refresh(tag: Tag) {
        runCatching { _lastScan.value = NfcReader.read(tag) }
    }

    private suspend fun execute(op: NfcOp, tag: Tag): String = when (op) {
        is NfcOp.Write -> {
            NfcWriter.write(tag, op.message)
            if (op.lockAfter) NfcWriter.lock(tag)
            refresh(tag)
            if (op.lockAfter) "Written and locked" else "Written · ${op.message.byteArrayLength} bytes"
        }
        NfcOp.Erase -> {
            NfcWriter.erase(tag)
            refresh(tag)
            "Tag erased"
        }
        NfcOp.MakeReadOnly -> {
            NfcWriter.lock(tag)
            refresh(tag)
            "Tag is now permanently read-only"
        }
        NfcOp.ReadForClone -> {
            val snap = NfcReader.read(tag)
            val bytes = snap.ndefBytes ?: error("This tag has no data to clone")
            _lastScan.value = snap
            _clone.value = bytes
            "Copied ${bytes.size} bytes — now write them to another tag"
        }
        NfcOp.DumpMemory -> {
            _dump.value = NtagOps.dump(tag)
            "Memory read · ${_dump.value.size} pages"
        }
        NfcOp.AddTag -> {
            val snap = NfcReader.read(tag)
            _lastScan.value = snap
            if (repo.findByUid(snap.uid) != null) error("This tag is already in your library")
            val name = defaultName(snap)
            repo.save(TagEntity(uid = snap.uid, name = name, ndefBase64 = encodeNdef(snap.ndefBytes)))
            "Saved as “$name”"
        }
        is NfcOp.SetPassword -> {
            NtagOps.setPassword(tag, op.password, op.protectRead)
            refresh(tag)
            if (op.protectRead) "Password set — reading and writing now need it" else "Password set — writing now needs it"
        }
        is NfcOp.RemovePassword -> {
            NtagOps.removePassword(tag, op.password)
            refresh(tag)
            "Password removed"
        }
    }

    private suspend fun scan(tag: Tag, runActions: Boolean) {
        val s = settings.flow.first()
        if (s.haptics) runner.tick()
        val snap = NfcReader.read(tag)
        _lastScan.value = snap
        val saved = repo.findByUid(snap.uid)
        var ran = false
        if (saved != null) {
            repo.markScanned(saved.id)
            val actions = ActionCodec.decode(saved.actionsJson)
            if (saved.enabled && runActions && actions.isNotEmpty()) {
                val results = runner.runAll(actions)
                _outcome.value = ActionOutcome(saved.name, results, System.currentTimeMillis())
                ran = true
            }
        }
        val summary = if (snap.records.isEmpty()) "Empty tag" else snap.records.joinToString(" · ") { it.title }
        repo.log(ScanLogEntity(uid = snap.uid, tagName = saved?.name, summary = summary, ranActions = ran))
    }
}
