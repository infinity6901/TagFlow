package com.tagflow.app.nfc

import android.net.Uri
import android.nfc.NdefRecord
import java.nio.charset.Charset

object NdefParser {

    fun parse(r: NdefRecord): RecordInfo {
        val size = runCatching { r.toByteArray().size }.getOrDefault(0)
        return try {
            when {
                r.tnf == NdefRecord.TNF_EMPTY -> RecordInfo(RecordKind.EMPTY, "Empty", "", size)

                r.tnf == NdefRecord.TNF_WELL_KNOWN && r.type.contentEquals(NdefRecord.RTD_TEXT) -> {
                    val p = r.payload
                    val langLen = p[0].toInt() and 0x3F
                    val cs = if ((p[0].toInt() and 0x80) != 0) Charsets.UTF_16 else Charsets.UTF_8
                    val text = String(p, 1 + langLen, p.size - 1 - langLen, cs)
                    RecordInfo(RecordKind.TEXT, "Text", text, size)
                }

                (r.tnf == NdefRecord.TNF_WELL_KNOWN && r.type.contentEquals(NdefRecord.RTD_URI)) ||
                    r.tnf == NdefRecord.TNF_ABSOLUTE_URI -> uriInfo(r.toUri(), size)

                r.tnf == NdefRecord.TNF_MIME_MEDIA -> mimeInfo(String(r.type, Charsets.US_ASCII), r.payload, size)

                r.tnf == NdefRecord.TNF_EXTERNAL_TYPE -> {
                    val t = String(r.type, Charsets.US_ASCII)
                    if (t == "android.com:pkg") {
                        RecordInfo(RecordKind.APP, "App launch", String(r.payload, Charsets.UTF_8), size)
                    } else {
                        RecordInfo(RecordKind.OTHER, "External", "$t · ${r.payload.size} bytes", size)
                    }
                }

                else -> RecordInfo(RecordKind.OTHER, "Record", "${r.payload.size} bytes", size)
            }
        } catch (e: Exception) {
            RecordInfo(RecordKind.OTHER, "Unreadable record", e.message ?: "", size)
        }
    }

    private fun uriInfo(uri: Uri?, size: Int): RecordInfo {
        if (uri == null) return RecordInfo(RecordKind.OTHER, "Link", "", size)
        val s = uri.toString()
        return when (uri.scheme?.lowercase()) {
            "tel" -> RecordInfo(RecordKind.PHONE, "Phone", s.removePrefix("tel:"), size, s)
            "mailto" -> RecordInfo(RecordKind.EMAIL, "Email", s.removePrefix("mailto:"), size, s)
            "sms", "smsto" -> RecordInfo(RecordKind.SMS, "SMS", s.substringAfter(':'), size, s)
            "geo" -> RecordInfo(RecordKind.GEO, "Location", s.removePrefix("geo:"), size, s)
            else -> RecordInfo(RecordKind.URL, "Link", s, size, s)
        }
    }

    private fun mimeInfo(mime: String, payload: ByteArray, size: Int): RecordInfo = when (mime) {
        "application/vnd.wfa.wsc" -> {
            val (ssid, key) = parseWifi(payload)
            val secured = if (key.isEmpty()) "open network" else "password saved"
            RecordInfo(RecordKind.WIFI, "Wi‑Fi", "$ssid · $secured", size)
        }
        "text/vcard", "text/x-vcard" -> {
            val text = String(payload, Charsets.UTF_8)
            val name = Regex("(?m)^FN[^:]*:(.*)$").find(text)?.groupValues?.get(1)?.trim().orEmpty()
            val tel = Regex("(?m)^TEL[^:]*:(.*)$").find(text)?.groupValues?.get(1)?.trim().orEmpty()
            RecordInfo(RecordKind.CONTACT, "Contact", listOf(name, tel).filter { it.isNotEmpty() }.joinToString(" · "), size)
        }
        else -> {
            val textual = mime.startsWith("text/") || mime.contains("json")
            val v = if (textual) String(payload, Charset.forName("UTF-8")) else "${payload.size} bytes"
            RecordInfo(RecordKind.MIME, mime, v, size)
        }
    }

    /** Returns (ssid, networkKey) from a WSC payload. */
    fun parseWifi(p: ByteArray): Pair<String, String> {
        var ssid = ""
        var key = ""
        fun walk(from: Int, to: Int) {
            var i = from
            while (i + 4 <= to) {
                val t = ((p[i].toInt() and 0xFF) shl 8) or (p[i + 1].toInt() and 0xFF)
                val l = ((p[i + 2].toInt() and 0xFF) shl 8) or (p[i + 3].toInt() and 0xFF)
                val s = i + 4
                if (s + l > to) break
                when (t) {
                    0x100E -> walk(s, s + l)
                    0x1045 -> ssid = String(p, s, l, Charsets.UTF_8)
                    0x1027 -> key = String(p, s, l, Charsets.UTF_8)
                }
                i = s + l
            }
        }
        walk(0, p.size)
        return ssid to key
    }
}
