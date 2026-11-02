package com.tagflow.app.action

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.AlarmClock
import android.provider.Settings
import android.view.KeyEvent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.tagflow.app.R
import com.tagflow.app.model.ActionSpec
import com.tagflow.app.model.ActionType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActionRunner @Inject constructor(@ApplicationContext private val context: Context) {

    data class Result(val spec: ActionSpec, val ok: Boolean, val message: String)

    @Volatile
    private var torchOn = false

    init {
        runCatching {
            val cm = context.getSystemService(CameraManager::class.java)
            cm.registerTorchCallback(object : CameraManager.TorchCallback() {
                override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                    torchOn = enabled
                }
            }, Handler(Looper.getMainLooper()))
        }
    }

    suspend fun runAll(actions: List<ActionSpec>): List<Result> {
        val out = mutableListOf<Result>()
        for (a in actions) out += run(a)
        return out
    }

    suspend fun run(a: ActionSpec): Result = try {
        val msg = when (a.type) {
            ActionType.WEBHOOK -> withContext(Dispatchers.IO) { webhook(a) }
            ActionType.DELAY -> {
                val secs = ((a.arg1.toDoubleOrNull() ?: 1.0)).coerceIn(0.0, 60.0)
                delay((secs * 1000).toLong())
                "Waited ${a.arg1}s"
            }
            else -> withContext(Dispatchers.Main) { runSync(a) }
        }
        Result(a, true, msg)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result(a, false, e.message ?: e.javaClass.simpleName)
    }

    /** A short haptic tick used when a tag is detected. */
    fun tick() {
        runCatching {
            val v = context.getSystemService(Vibrator::class.java)
            v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        }
    }

    private fun startIntent(i: Intent) {
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(i)
    }

    private fun normalizeUrl(raw: String): String {
        val u = raw.trim()
        return if ("://" in u) u else "https://$u"
    }

    private fun runSync(a: ActionSpec): String {
        return when (a.type) {
            ActionType.OPEN_URL -> {
                val url = normalizeUrl(a.arg1)
                startIntent(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                return "Opened $url"
            }
            ActionType.LAUNCH_APP -> {
                val i = context.packageManager.getLaunchIntentForPackage(a.arg1.trim())
                    ?: error("App not installed: ${a.arg1}")
                startIntent(i)
                return "Launched ${a.arg1}"
            }
            ActionType.TORCH -> {
                val cm = context.getSystemService(CameraManager::class.java)
                val id = cm.cameraIdList.firstOrNull {
                    cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                } ?: error("This phone has no flashlight")
                val target = when (a.arg1) {
                    "on" -> true
                    "off" -> false
                    else -> !torchOn
                }
                cm.setTorchMode(id, target)
                torchOn = target
                return if (target) "Flashlight on" else "Flashlight off"
            }
            ActionType.ALARM -> {
                val parts = a.arg1.trim().split(":")
                val h = parts.getOrNull(0)?.trim()?.toIntOrNull()
                val m = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
                if (h == null || h !in 0..23 || m !in 0..59) error("Time must look like 07:30")
                startIntent(
                    Intent(AlarmClock.ACTION_SET_ALARM)
                        .putExtra(AlarmClock.EXTRA_HOUR, h)
                        .putExtra(AlarmClock.EXTRA_MINUTES, m)
                        .putExtra(AlarmClock.EXTRA_MESSAGE, a.arg2)
                        .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                )
                return "Alarm set for %02d:%02d".format(h, m)
            }
            ActionType.TIMER -> {
                val mins = a.arg1.trim().toDoubleOrNull() ?: error("Minutes must be a number")
                startIntent(
                    Intent(AlarmClock.ACTION_SET_TIMER)
                        .putExtra(AlarmClock.EXTRA_LENGTH, (mins * 60).toInt().coerceAtLeast(1))
                        .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                )
                return "Timer started (${a.arg1} min)"
            }
            ActionType.DIAL -> {
                startIntent(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(a.arg1.trim()))))
                return "Dialer opened"
            }
            ActionType.SMS -> {
                startIntent(
                    Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + Uri.encode(a.arg1.trim())))
                        .putExtra("sms_body", a.arg2)
                )
                return "SMS draft opened"
            }
            ActionType.NOTIFY -> {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= 33) {
                    error("Notification permission not granted (Settings → Apps → TagFlow)")
                }
                val nm = context.getSystemService(NotificationManager::class.java)
                nm.createNotificationChannel(NotificationChannel(CHANNEL, "Tag actions", NotificationManager.IMPORTANCE_DEFAULT))
                val n = NotificationCompat.Builder(context, CHANNEL)
                    .setSmallIcon(R.drawable.ic_stat_nfc)
                    .setContentTitle(a.arg1.ifBlank { "TagFlow" })
                    .setContentText(a.arg2)
                    .setAutoCancel(true)
                    .build()
                nm.notify((System.nanoTime() and 0x7FFFFFF).toInt(), n)
                return "Notification posted"
            }
            ActionType.MEDIA -> {
                val am = context.getSystemService(AudioManager::class.java)
                val code = when (a.arg1) {
                    "next" -> KeyEvent.KEYCODE_MEDIA_NEXT
                    "previous" -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
                    else -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                }
                am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
                am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
                return "Media: ${a.arg1.replace('_', ' ')}"
            }
            ActionType.RINGER -> {
                val am = context.getSystemService(AudioManager::class.java)
                am.ringerMode = if (a.arg1 == "vibrate") AudioManager.RINGER_MODE_VIBRATE else AudioManager.RINGER_MODE_NORMAL
                return "Ringer: ${a.arg1}"
            }
            ActionType.DND -> {
                val nm = context.getSystemService(NotificationManager::class.java)
                if (!nm.isNotificationPolicyAccessGranted) {
                    startIntent(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                    error("Grant Do Not Disturb access to TagFlow, then tap again")
                }
                nm.setInterruptionFilter(
                    if (a.arg1 == "off") NotificationManager.INTERRUPTION_FILTER_ALL else NotificationManager.INTERRUPTION_FILTER_PRIORITY
                )
                return "Do Not Disturb ${a.arg1}"
            }
            ActionType.VOLUME -> {
                val am = context.getSystemService(AudioManager::class.java)
                val pct = (a.arg1.trim().toIntOrNull() ?: error("Volume must be a number")).coerceIn(0, 100)
                val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                am.setStreamVolume(AudioManager.STREAM_MUSIC, pct * max / 100, AudioManager.FLAG_SHOW_UI)
                return "Volume $pct%"
            }
            ActionType.CLIPBOARD -> {
                val cm = context.getSystemService(ClipboardManager::class.java)
                cm.setPrimaryClip(ClipData.newPlainText("TagFlow", a.arg1))
                return "Copied to clipboard"
            }
            ActionType.PANEL -> {
                val action = when (a.arg1) {
                    "wifi" -> Settings.Panel.ACTION_WIFI
                    "volume" -> Settings.Panel.ACTION_VOLUME
                    "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
                    else -> Settings.Panel.ACTION_INTERNET_CONNECTIVITY
                }
                startIntent(Intent(action))
                return "Opened ${a.arg1} panel"
            }
            ActionType.MAPS -> {
                startIntent(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(a.arg1))))
                return "Searching maps"
            }
            ActionType.VIBRATE -> {
                val ms = (a.arg1.trim().toLongOrNull() ?: 300L).coerceIn(10L, 3000L)
                val v = context.getSystemService(Vibrator::class.java)
                v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
                return "Vibrated ${ms}ms"
            }
            ActionType.WEBHOOK, ActionType.DELAY -> return "" // handled in run()
        }
    }

    private fun webhook(a: ActionSpec): String {
        val conn = URL(normalizeUrl(a.arg1)).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("User-Agent", "TagFlow/1.0")
            if (a.arg2.isNotBlank()) {
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.outputStream.use { it.write(a.arg2.toByteArray()) }
            } else {
                conn.requestMethod = "GET"
            }
            val code = conn.responseCode
            if (code !in 200..299) error("HTTP $code")
            return "HTTP $code"
        } finally {
            conn.disconnect()
        }
    }

    private companion object {
        const val CHANNEL = "tag_actions"
    }
}
