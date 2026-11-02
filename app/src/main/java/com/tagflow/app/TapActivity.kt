package com.tagflow.app

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.tagflow.app.core.parcelable
import com.tagflow.app.nfc.NfcController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Invisible entry point for tags tapped while TagFlow isn't in the foreground. If the tag is saved
 * and has actions, they run immediately and this activity disappears. Otherwise the tag is handed to
 * the main UI.
 */
@AndroidEntryPoint
class TapActivity : ComponentActivity() {

    @Inject lateinit var nfc: NfcController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handle(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(i: Intent?) {
        val tag = i?.parcelable<Tag>(NfcAdapter.EXTRA_TAG)
        if (tag == null) {
            finish()
            return
        }
        lifecycleScope.launch {
            val handled = runCatching { nfc.handleBackground(tag) }.getOrDefault(false)
            if (!handled) {
                startActivity(
                    Intent(this@TapActivity, MainActivity::class.java)
                        .putExtra(NfcAdapter.EXTRA_TAG, tag)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                )
            }
            finish()
        }
    }
}
