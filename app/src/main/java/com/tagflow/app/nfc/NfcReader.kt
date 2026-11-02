package com.tagflow.app.nfc

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.nfc.tech.NfcA
import com.tagflow.app.core.toHex

object NfcReader {

    fun read(tag: Tag): TagSnapshot {
        val uid = tag.id.toHex(":")
        val techs = tag.techList.map { it.substringAfterLast('.') }
        var chip = when {
            "MifareClassic" in techs -> "MIFARE Classic"
            "IsoDep" in techs -> "ISO-DEP card"
            "NfcV" in techs -> "ISO 15693"
            "NfcF" in techs -> "FeliCa"
            else -> "NFC tag"
        }
        var chipCapacity = 0
        var protection = Protection.UNKNOWN

        if ("MifareClassic" !in techs) {
            NfcA.get(tag)?.let { a ->
                try {
                    a.connect()
                    val cc = a.transceive(byteArrayOf(0x30, 0x03))
                    if (cc.size >= 4 && (cc[0].toInt() and 0xFF) == 0xE1) {
                        val lay = NtagOps.layoutOf(cc[2].toInt() and 0xFF)
                        if (lay != null) {
                            chip = lay.name
                            chipCapacity = lay.userBytes
                            protection = try {
                                val cfg = a.transceive(byteArrayOf(0x30, lay.cfg.toByte()))
                                val auth0 = cfg[3].toInt() and 0xFF
                                when {
                                    auth0 > lay.cfg + 3 -> Protection.NONE
                                    (cfg[4].toInt() and 0x80) != 0 -> Protection.READ_WRITE
                                    else -> Protection.WRITE
                                }
                            } catch (e: Exception) {
                                Protection.READ_WRITE // config page unreadable → password-protected
                            }
                        } else {
                            chip = "NFC Forum Type 2"
                        }
                    } else {
                        chip = "NfcA tag (SAK 0x%02X)".format(a.sak)
                    }
                } catch (_: Exception) {
                } finally {
                    runCatching { a.close() }
                }
            }
        }

        var type = "Unknown"
        var capacity = chipCapacity
        var used = 0
        var writable = false
        var canLock = false
        var records = emptyList<RecordInfo>()
        var bytes: ByteArray? = null

        val ndef = Ndef.get(tag)
        if (ndef != null) {
            try {
                ndef.connect()
                val msg: NdefMessage? = ndef.ndefMessage
                type = prettyType(ndef.type)
                capacity = ndef.maxSize
                writable = ndef.isWritable
                canLock = ndef.canMakeReadOnly()
                if (msg != null) {
                    val empty = msg.records.size == 1 && msg.records[0].tnf == NdefRecord.TNF_EMPTY
                    if (!empty) {
                        used = msg.byteArrayLength
                        records = msg.records.map { NdefParser.parse(it) }
                        bytes = msg.toByteArray()
                    }
                }
            } catch (_: Exception) {
            } finally {
                runCatching { ndef.close() }
            }
        } else if (NdefFormatable.get(tag) != null) {
            type = "Blank · NDEF formattable"
            writable = true
        }

        return TagSnapshot(
            uid = uid, techs = techs, chip = chip, type = type, capacity = capacity, used = used,
            writable = writable, canLock = canLock, protection = protection,
            records = records, ndefBytes = bytes
        )
    }

    private fun prettyType(t: String): String = when {
        t.contains("type1") -> "NFC Forum Type 1"
        t.contains("type2") -> "NFC Forum Type 2"
        t.contains("type3") -> "NFC Forum Type 3"
        t.contains("type4") -> "NFC Forum Type 4"
        t.contains("mifare") -> "MIFARE Classic"
        else -> t
    }
}
