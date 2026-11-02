package com.tagflow.app.nfc

import android.nfc.NdefMessage

enum class RecordKind { TEXT, URL, PHONE, EMAIL, SMS, GEO, WIFI, CONTACT, APP, MIME, EMPTY, OTHER }

data class RecordInfo(
    val kind: RecordKind,
    val title: String,
    val value: String,
    val size: Int,
    val openUri: String? = null
)

enum class Protection { NONE, WRITE, READ_WRITE, UNKNOWN }

data class TagSnapshot(
    val uid: String,
    val techs: List<String>,
    val chip: String,
    val type: String,
    val capacity: Int,
    val used: Int,
    val writable: Boolean,
    val canLock: Boolean,
    val protection: Protection,
    val records: List<RecordInfo>,
    val ndefBytes: ByteArray?,
    val time: Long = System.currentTimeMillis()
)

enum class NfcStatus { Ready, Disabled, Unsupported }

sealed class NfcOp(val title: String) {
    class Write(val message: NdefMessage, title: String, val lockAfter: Boolean = false) : NfcOp(title)
    object Erase : NfcOp("Erase tag")
    object MakeReadOnly : NfcOp("Lock tag permanently")
    object ReadForClone : NfcOp("Read source tag")
    object DumpMemory : NfcOp("Read tag memory")
    object AddTag : NfcOp("Add tag to library")
    class SetPassword(val password: String, val protectRead: Boolean) : NfcOp("Set tag password")
    class RemovePassword(val password: String) : NfcOp("Remove tag password")
}

sealed interface OpState {
    object Idle : OpState
    class Waiting(val op: NfcOp) : OpState
    class Working(val op: NfcOp) : OpState
    class Done(val op: NfcOp, val success: Boolean, val message: String) : OpState
}
