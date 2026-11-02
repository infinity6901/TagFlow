package com.tagflow.app

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tagflow.app.core.parcelable
import com.tagflow.app.data.AppSettings
import com.tagflow.app.data.SettingsRepository
import com.tagflow.app.nfc.NfcController
import com.tagflow.app.nfc.NfcStatus
import com.tagflow.app.ui.TagFlowRoot
import com.tagflow.app.ui.theme.TagFlowTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var nfc: NfcController
    @Inject lateinit var settings: SettingsRepository

    private var adapter: NfcAdapter? = null
    private var nfcStatus by mutableStateOf(NfcStatus.Ready)

    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* result not needed */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handleIntent(intent)
        if (android.os.Build.VERSION.SDK_INT >= 33) notifPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)

        setContent {
            val s by settings.flow.collectAsStateWithLifecycle(initialValue = AppSettings())
            TagFlowTheme(s) {
                TagFlowRoot(
                    nfcStatus = nfcStatus,
                    onOpenNfcSettings = { startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        adapter = NfcAdapter.getDefaultAdapter(this)
        nfcStatus = when {
            adapter == null -> NfcStatus.Unsupported
            adapter?.isEnabled == true -> NfcStatus.Ready
            else -> NfcStatus.Disabled
        }
        adapter?.enableReaderMode(
            this,
            { tag: Tag -> nfc.onTag(tag) },
            NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or NfcAdapter.FLAG_READER_NFC_V,
            null
        )
    }

    override fun onPause() {
        super.onPause()
        runCatching { adapter?.disableReaderMode(this) }
        nfc.cancelOp()
    }

    private fun handleIntent(i: Intent?) {
        val tag = i?.parcelable<Tag>(NfcAdapter.EXTRA_TAG) ?: return
        i.removeExtra(NfcAdapter.EXTRA_TAG)
        nfc.onTag(tag)
    }
}
