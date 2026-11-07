package com.baccarat.ai

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private val captureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val serviceIntent = Intent(this, CaptureService::class.java).apply {
                putExtra(CaptureService.EXTRA_CODE, result.resultCode)
                putExtra(CaptureService.EXTRA_DATA, result.data)
            }
            ContextCompat.startForegroundService(this, serviceIntent)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Column(Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Baccarat AI • Speed", style = MaterialTheme.typography.headlineMedium)
                    Text("HONOR 400 Lite • Stake Speed Baccarat A")
                    Text("Screen reading starts only after Android permissions and screen-capture consent.")
                    Button(modifier = Modifier.fillMaxWidth(), onClick = {
                        if (!Settings.canDrawOverlays(this@MainActivity)) {
                            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                        } else {
                            val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                            captureLauncher.launch(manager.createScreenCaptureIntent())
                        }
                    }) { Text("Start automatic screen reading") }
                    OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
                        stopService(Intent(this@MainActivity, CaptureService::class.java))
                        stopService(Intent(this@MainActivity, FloatingPanelService::class.java))
                    }) { Text("Stop analyzer") }
                    Text("OCR prototype: displays visible screen text. It does not yet reliably decode roadmap circles or predict outcomes.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}