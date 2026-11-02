package com.tagflow.app.nfc

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable

object NfcWriter {

    fun write(tag: Tag, msg: NdefMessage) {
        val ndef = Ndef.get(tag)
        if (ndef != null) {
            ndef.connect()
            try {
                if (!ndef.isWritable) error("Tag is read-only (locked or password-protected)")
                val size = msg.byteArrayLength
                if (size > ndef.maxSize) error("Message is $size bytes but this tag holds ${ndef.maxSize}")
                ndef.writeNdefMessage(msg)
            } finally {
                runCatching { ndef.close() }
            }
            return
        }
        val fmt = NdefFormatable.get(tag) ?: error("This tag doesn't support NDEF")
        fmt.connect()
        try {
            fmt.format(msg)
        } finally {
            runCatching { fmt.close() }
        }
    }

    @Suppress("DEPRECATION")
    fun erase(tag: Tag) {
        write(tag, NdefMessage(arrayOf(NdefRecord(NdefRecord.TNF_EMPTY, null, null, null))))
    }

    fun lock(tag: Tag) {
        val ndef = Ndef.get(tag) ?: error("Only NDEF tags can be locked")
        ndef.connect()
        try {
            if (!ndef.canMakeReadOnly()) error("This tag can't be made read-only")
            if (!ndef.makeReadOnly()) error("Locking failed")
        } finally {
            runCatching { ndef.close() }
        }
    }
}
