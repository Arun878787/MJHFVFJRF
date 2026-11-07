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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private val captureLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
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
                Surface(Modifier.fillMaxSize(), color = Color(0xFF101827)) {
                    AnalyzerHome(
                        onStart = {
                            if (!Settings.canDrawOverlays(this)) {
                                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                            } else {
                                val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                                captureLauncher.launch(manager.createScreenCaptureIntent())
                            }
                        },
                        onStop = {
                            stopService(Intent(this, CaptureService::class.java))
                            stopService(Intent(this, FloatingPanelService::class.java))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AnalyzerHome(onStart: () -> Unit, onStop: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        Text("BACCARAT AI", color = Color(0xFF69B7FF), style = MaterialTheme.typography.labelLarge)
        Text("Speed Analyzer", color = Color.White, style = MaterialTheme.typography.headlineLarge)
        Text("HONOR 400 Lite  •  Android 16", color = Color(0xFFB9C7D8))

        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2A3D)), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("SCREEN READER", color = Color.White, style = MaterialTheme.typography.titleMedium)
                Text("1. Allow display over other apps.\n2. Return to this app.\n3. Tap Start and approve Android screen capture.", color = Color(0xFFD4DFEA))
            }
        }

        Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
            Text("Start automatic screen reading")
        }
        OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth()) {
            Text("Stop analyzer", color = Color.White)
        }

        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF192235)), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("WHAT THIS VERSION DOES", color = Color.White, style = MaterialTheme.typography.titleSmall)
                Text("• Reads visible screen text on-device using OCR\n• Shows recognized text in a floating panel\n• Attempts to extract visible Player / Banker / Tie totals when legible", color = Color(0xFFD4DFEA))
                Text("LIMITATION", color = Color(0xFFFFD27A), style = MaterialTheme.typography.titleSmall)
                Text("This prototype does not reliably recognize roadmap circles or predict the next hand. Baccarat outcomes are random; past results do not guarantee future results. Use the displayed percentages only as historical counts, not betting advice.", color = Color(0xFFD4DFEA), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
