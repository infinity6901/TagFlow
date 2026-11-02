package com.tagflow.app.model

import org.json.JSONArray
import org.json.JSONObject

enum class KeyKind { TEXT, NUMBER, PHONE, URI, EMAIL }

/** Describes one input field in a form (used by both the action editor and the tag writer). */
data class FieldSpec(
    val key: String,
    val label: String,
    val hint: String = "",
    val kind: KeyKind = KeyKind.TEXT,
    val required: Boolean = true,
    val multiline: Boolean = false,
    val options: List<String> = emptyList(),
    val secret: Boolean = false,
    val default: String = "",
    val picker: Boolean = false
)

enum class ActionType(val label: String, val blurb: String, val fields: List<FieldSpec>) {
    OPEN_URL("Open link", "Open a website in your browser", listOf(FieldSpec("arg1", "URL", "https://example.com", KeyKind.URI))),
    LAUNCH_APP("Launch app", "Open any installed app", listOf(FieldSpec("arg1", "Package name", "com.spotify.music", picker = true))),
    TORCH("Flashlight", "Turn the torch on, off or toggle it", listOf(FieldSpec("arg1", "Mode", options = listOf("toggle", "on", "off"), default = "toggle"))),
    ALARM("Set alarm", "Create an alarm in your clock app", listOf(FieldSpec("arg1", "Time (24 h)", "07:30"), FieldSpec("arg2", "Label", required = false))),
    TIMER("Start timer", "Start a countdown timer", listOf(FieldSpec("arg1", "Minutes", "5", KeyKind.NUMBER))),
    DIAL("Dial number", "Open the dialer with a number", listOf(FieldSpec("arg1", "Phone number", "+1 555 0100", KeyKind.PHONE))),
    SMS("Compose SMS", "Open a text message draft", listOf(FieldSpec("arg1", "Phone number", kind = KeyKind.PHONE), FieldSpec("arg2", "Message", required = false))),
    NOTIFY("Show notification", "Post a notification", listOf(FieldSpec("arg1", "Title"), FieldSpec("arg2", "Message", required = false))),
    MEDIA("Media control", "Play, pause or skip music", listOf(FieldSpec("arg1", "Command", options = listOf("play_pause", "next", "previous"), default = "play_pause"))),
    RINGER("Ringer mode", "Switch between normal and vibrate", listOf(FieldSpec("arg1", "Mode", options = listOf("normal", "vibrate"), default = "vibrate"))),
    DND("Do Not Disturb", "Turn Do Not Disturb on or off", listOf(FieldSpec("arg1", "State", options = listOf("on", "off"), default = "on"))),
    VOLUME("Media volume", "Set the media volume", listOf(FieldSpec("arg1", "Volume %", "50", KeyKind.NUMBER))),
    CLIPBOARD("Copy text", "Copy text to the clipboard", listOf(FieldSpec("arg1", "Text", multiline = true))),
    PANEL("Quick panel", "Open a settings panel", listOf(FieldSpec("arg1", "Panel", options = listOf("wifi", "internet", "volume", "bluetooth"), default = "wifi"))),
    MAPS("Open maps", "Search a place or address", listOf(FieldSpec("arg1", "Place or address", "Coffee near me"))),
    VIBRATE("Vibrate", "Give a haptic buzz", listOf(FieldSpec("arg1", "Duration (ms)", "300", KeyKind.NUMBER, default = "300"))),
    WEBHOOK("Webhook", "Call a URL — Home Assistant, n8n, IFTTT…", listOf(FieldSpec("arg1", "URL", "http://192.168.1.10:5678/webhook/…", KeyKind.URI), FieldSpec("arg2", "JSON body (optional, sends POST)", required = false, multiline = true))),
    DELAY("Wait", "Pause the chain for a few seconds", listOf(FieldSpec("arg1", "Seconds", "2", KeyKind.NUMBER, default = "2")))
}

data class ActionSpec(val type: ActionType, val arg1: String = "", val arg2: String = "") {
    fun summary(): String = listOf(arg1, arg2).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { type.blurb }
}

object ActionCodec {
    fun encode(list: List<ActionSpec>): String {
        val arr = JSONArray()
        list.forEach { arr.put(JSONObject().put("t", it.type.name).put("a1", it.arg1).put("a2", it.arg2)) }
        return arr.toString()
    }

    fun decode(json: String): List<ActionSpec> = runCatching {
        val arr = JSONArray(json)
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.getJSONObject(i)
            val t = runCatching { ActionType.valueOf(o.getString("t")) }.getOrNull() ?: return@mapNotNull null
            ActionSpec(t, o.optString("a1", ""), o.optString("a2", ""))
        }
    }.getOrDefault(emptyList())
}
