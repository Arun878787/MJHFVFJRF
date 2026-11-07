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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
                val prefs = remember { getSharedPreferences("baccarat_history", Context.MODE_PRIVATE) }
                var history by remember { mutableStateOf(prefs.getString("results", "")?.map { it.toString() } ?: emptyList()) }
                LaunchedEffect(history) { prefs.edit().putString("results", history.joinToString("")).apply() }
                val player = history.count { it == "P" }
                val banker = history.count { it == "B" }
                val tie = history.count { it == "T" }
                val total = history.size.coerceAtLeast(1)
                val percent: (Int) -> Int = { count -> count * 100 / total }

                Column(
                    Modifier.fillMaxSize().background(Color(0xFF0B1220)).verticalScroll(rememberScrollState()).padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("BACCARAT AI", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                    Text("SPEED TABLE ANALYZER", color = Color(0xFF9FB1CA))
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF17243A))) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("LIVE SCREEN READER", color = Color.White, style = MaterialTheme.typography.titleMedium)
                            Text("HONOR 400 Lite • Stake Speed Baccarat A", color = Color(0xFFB7C6D9))
                            Text("OCR: " + AnalyzerState.latestOcrText.take(180), color = Color.White)
                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    if (!Settings.canDrawOverlays(this@MainActivity)) {
                                        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                                    } else {
                                        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                                        captureLauncher.launch(manager.createScreenCaptureIntent())
                                    }
                                }
                            ) { Text("Start automatic screen reading") }
                            OutlinedButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    stopService(Intent(this@MainActivity, CaptureService::class.java))
                                    stopService(Intent(this@MainActivity, FloatingPanelService::class.java))
                                }
                            ) { Text("Stop screen reader") }
                        }
                    }

                    Text("RECORDED RESULTS", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ResultCard("PLAYER", player, percent(player), Color(0xFF2878D0), Modifier.weight(1f))
                        ResultCard("BANKER", banker, percent(banker), Color(0xFFD94B50), Modifier.weight(1f))
                        ResultCard("TIE", tie, percent(tie), Color(0xFF27966B), Modifier.weight(1f))
                    }
                    Text("Add the result after each hand", color = Color(0xFFB7C6D9))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { history = history + "P" }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2878D0))) { Text("PLAYER") }
                        Button(onClick = { history = history + "B" }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD94B50))) { Text("BANKER") }
                        Button(onClick = { history = history + "T" }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27966B))) { Text("TIE") }
                    }
                    Text("RECENT HANDS • ${history.size}", color = Color.White, style = MaterialTheme.typography.titleSmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(history.takeLast(30).reversed()) { result ->
                            Box(
                                Modifier.size(34.dp).background(
                                    when (result) { "P" -> Color(0xFF2878D0); "B" -> Color(0xFFD94B50); else -> Color(0xFF27966B) },
                                    RoundedCornerShape(50)
                                ),
                                contentAlignment = androidx.compose.ui.Alignment.Center
                            ) { Text(result, color = Color.White) }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { if (history.isNotEmpty()) history = history.dropLast(1) }, modifier = Modifier.weight(1f)) { Text("Undo last") }
                        OutlinedButton(onClick = { history = emptyList() }, modifier = Modifier.weight(1f)) { Text("Clear history") }
                    }
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF17243A))) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("ROADMAP ANALYSIS", color = Color.White, style = MaterialTheme.typography.titleMedium)
                            Text("Automatic colored-roadmap recognition still needs calibration against your actual game screen. The app will not guess results from unrelated screen text.", color = Color(0xFFB7C6D9))
                            Text("Percentages above describe recorded past hands only. They do not predict or improve the odds of the next hand.", color = Color(0xFFFFD28A))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultCard(label: String, count: Int, percent: Int, tint: Color, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Color(0xFF17243A))) {
        Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, color = tint, style = MaterialTheme.typography.labelMedium)
            Text(count.toString(), color = Color.White, style = MaterialTheme.typography.headlineSmall)
            Text("$percent%", color = Color(0xFFB7C6D9))
        }
    }
}
