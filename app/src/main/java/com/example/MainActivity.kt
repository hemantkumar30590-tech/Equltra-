package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.EqViewModel
import com.example.ui.EqultraScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: EqViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingPresetIntent(intent)
        setContent {
            MyApplicationTheme {
                EqultraScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingPresetIntent(intent)
    }

    private fun handleIncomingPresetIntent(intent: Intent?) {
        if (intent == null) return
        when (intent.action) {
            Intent.ACTION_SEND -> {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                val sharedStream: Uri? = if (android.os.Build.VERSION.SDK_INT >= 33) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
                if (!sharedText.isNullOrBlank()) {
                    viewModel.importPresetFromContent(sharedText, "Shared Preset") { saved ->
                        if (saved != null) {
                            viewModel.selectTab(3)
                            Toast.makeText(this, "Imported '${saved.name}'!", Toast.LENGTH_LONG).show()
                        }
                    }
                } else if (sharedStream != null) {
                    importFromUri(sharedStream)
                }
            }
            Intent.ACTION_VIEW -> {
                intent.data?.let { uri ->
                    importFromUri(uri)
                }
            }
        }
    }

    private fun importFromUri(uri: Uri) {
        runCatching {
            val content = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            if (!content.isNullOrBlank()) {
                val name = uri.lastPathSegment?.substringAfterLast("/") ?: "Shared Preset"
                viewModel.importPresetFromContent(content, name) { saved ->
                    if (saved != null) {
                        viewModel.selectTab(3)
                        Toast.makeText(this, "Imported '${saved.name}' into My Imports!", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}
