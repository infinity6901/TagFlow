package com.tagflow.app.nfc

import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import com.tagflow.app.model.FieldSpec
import com.tagflow.app.model.KeyKind

const val APP_PACKAGE = "com.tagflow.app"

enum class WriteType(val label: String, val blurb: String, val fields: List<FieldSpec>) {
    TEXT("Text", "Store a plain note", listOf(FieldSpec("text", "Text", multiline = true))),
    URL("Link", "Open a website or deep link when tapped", listOf(FieldSpec("url", "URL", "https://example.com", KeyKind.URI))),
    PHONE("Call", "Dial a phone number", listOf(FieldSpec("number", "Phone number", "+1 555 0100", KeyKind.PHONE))),
    EMAIL(
        "Email", "Start an email draft",
        listOf(
            FieldSpec("to", "To", "name@example.com", KeyKind.EMAIL),
            FieldSpec("subject", "Subject", required = false),
            FieldSpec("body", "Body", required = false, multiline = true)
        )
    ),
    SMS("SMS", "Start a text message", listOf(FieldSpec("number", "Phone number", kind = KeyKind.PHONE), FieldSpec("body", "Message", required = false))),
    LOCATION(
        "Location", "Open a map position",
        listOf(
            FieldSpec("lat", "Latitude", "48.8584"),
            FieldSpec("lng", "Longitude", "2.2945"),
            FieldSpec("label", "Label", required = false)
        )
    ),
    WIFI(
        "Wi‑Fi", "Share network credentials",
        listOf(
            FieldSpec("ssid", "Network name (SSID)"),
            FieldSpec("password", "Password", required = false, secret = true),
            FieldSpec("security", "Security", options = listOf("WPA2", "Open"), default = "WPA2")
        )
    ),
    CONTACT(
        "Contact", "Share a vCard contact",
        listOf(
            FieldSpec("name", "Full name"),
            FieldSpec("phone", "Phone", required = false, kind = KeyKind.PHONE),
            FieldSpec("email", "Email", required = false, kind = KeyKind.EMAIL)
        )
    ),
    APP("Launch app", "Open an installed app", listOf(FieldSpec("pkg", "Package name", "com.spotify.music", picker = true))),
    TAGFLOW("TagFlow link", "Opens TagFlow instantly so this tag can run its actions", listOf(FieldSpec("label", "Label", required = false)))
}

sealed interface BuildResult {
    class Ok(val message: NdefMessage) : BuildResult
    class Err(val reason: String) : BuildResult
}

object WriteBuilder {

    fun build(type: WriteType, f: Map<String, String>, addLauncher: Boolean): BuildResult {
        fun v(k: String): String = (f[k] ?: type.fields.firstOrNull { it.key == k }?.default ?: "").trim()

        for (spec in type.fields) {
            if (spec.required && v(spec.key).isEmpty()) return BuildResult.Err("Fill in ${spec.label.lowercase()}")
        }

        val records = mutableListOf<NdefRecord>()
        try {
            when (type) {
                WriteType.TEXT -> records += NdefRecord.createTextRecord("en", v("text"))
                WriteType.URL -> {
                    val u = v("url")
                    records += NdefRecord.createUri(if ("://" in u || u.contains(':')) u else "https://$u")
                }
                WriteType.PHONE -> records += NdefRecord.createUri("tel:" + v("number").filter { it.isDigit() || it == '+' })
                WriteType.EMAIL -> {
                    val q = buildList {
                        if (v("subject").isNotEmpty()) add("subject=" + Uri.encode(v("subject")))
                        if (v("body").isNotEmpty()) add("body=" + Uri.encode(v("body")))
                    }
                    records += NdefRecord.createUri("mailto:" + v("to") + if (q.isEmpty()) "" else "?" + q.joinToString("&"))
                }
                WriteType.SMS -> {
                    val body = if (v("body").isEmpty()) "" else "?body=" + Uri.encode(v("body"))
                    records += NdefRecord.createUri("sms:" + v("number").filter { it.isDigit() || it == '+' } + body)
                }
                WriteType.LOCATION -> {
                    val lat = v("lat").toDoubleOrNull() ?: return BuildResult.Err("Latitude must be a number")
                    val lng = v("lng").toDoubleOrNull() ?: return BuildResult.Err("Longitude must be a number")
                    if (lat !in -90.0..90.0 || lng !in -180.0..180.0) return BuildResult.Err("Coordinates out of range")
                    val uri = if (v("label").isEmpty()) "geo:$lat,$lng" else "geo:0,0?q=$lat,$lng(" + Uri.encode(v("label")) + ")"
                    records += NdefRecord.createUri(uri)
                }
                WriteType.WIFI -> records += wifiRecord(v("ssid"), v("password"), v("security") == "Open")
                WriteType.CONTACT -> {
                    val vcard = buildString {
                        append("BEGIN:VCARD\nVERSION:3.0\n")
                        append("N:;").append(v("name")).append(";;;\n")
                        append("FN:").append(v("name")).append('\n')
                        if (v("phone").isNotEmpty()) append("TEL;TYPE=CELL:").append(v("phone")).append('\n')
                        if (v("email").isNotEmpty()) append("EMAIL:").append(v("email")).append('\n')
                        append("END:VCARD")
                    }
                    records += NdefRecord.createMime("text/vcard", vcard.toByteArray(Charsets.UTF_8))
                }
                WriteType.APP -> records += NdefRecord.createApplicationRecord(v("pkg"))
                WriteType.TAGFLOW -> {
                    if (v("label").isNotEmpty()) records += NdefRecord.createTextRecord("en", v("label"))
                    records += NdefRecord.createApplicationRecord(APP_PACKAGE)
                }
            }
            if (addLauncher && type != WriteType.TAGFLOW && type != WriteType.APP) {
                records += NdefRecord.createApplicationRecord(APP_PACKAGE)
            }
            return BuildResult.Ok(NdefMessage(records.toTypedArray()))
        } catch (e: Exception) {
            return BuildResult.Err(e.message ?: "Invalid input")
        }
    }

    private fun tlv(type: Int, value: ByteArray): ByteArray =
        byteArrayOf((type shr 8).toByte(), type.toByte(), (value.size shr 8).toByte(), value.size.toByte()) + value

    private fun short(v: Int) = byteArrayOf((v shr 8).toByte(), v.toByte())

    private fun wifiRecord(ssid: String, password: String, open: Boolean): NdefRecord {
        val auth = if (open) 0x0001 else 0x0020
        val enc = if (open) 0x0001 else 0x0008
        val cred = tlv(0x1026, byteArrayOf(1)) +
            tlv(0x1045, ssid.toByteArray(Charsets.UTF_8)) +
            tlv(0x1003, short(auth)) +
            tlv(0x100F, short(enc)) +
            tlv(0x1027, if (open) ByteArray(0) else password.toByteArray(Charsets.UTF_8)) +
            tlv(0x1020, ByteArray(6) { 0xFF.toByte() })
        val payload = tlv(0x104A, byteArrayOf(0x10)) + tlv(0x100E, cred)
        return NdefRecord.createMime("application/vnd.wfa.wsc", payload)
    }
}
