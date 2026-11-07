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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay

private val Navy = Color(0xFF0A1220)
private val Panel = Color(0xFF142238)
private val Muted = Color(0xFF9FB1C8)
private val PlayerBlue = Color(0xFF2878E8)
private val BankerRed = Color(0xFFD9434B)
private val TieGreen = Color(0xFF159B68)
private val Gold = Color(0xFFFFC857)

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
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = PlayerBlue, secondary = TieGreen, background = Navy,
                    surface = Panel, onPrimary = Color.White, onSurface = Color.White
                )
            ) {
                val prefs = remember { getSharedPreferences("baccarat_history", Context.MODE_PRIVATE) }
                var history by remember { mutableStateOf(prefs.getString("results", "").orEmpty().map { it.toString() }) }
                var analysisMode by remember { mutableStateOf("Historical patterns") }
                var modeMenu by remember { mutableStateOf(false) }
                var liveStatus by remember { mutableStateOf("Automatic reader is stopped") }
                var sessionName by remember { mutableStateOf("Current shoe") }
                LaunchedEffect(history) {
                    prefs.edit().putString("results", history.joinToString("")).apply()
                    AnalyzerState.setHistory(history)
                }
                LaunchedEffect(Unit) {
                    while (true) {
                        liveStatus = AnalyzerState.roadmapStatus
                        delay(900)
                    }
                }

                val p = history.count { it == "P" }
                val b = history.count { it == "B" }
                val t = history.count { it == "T" }
                val total = history.size
                val resolved = (p + b).coerceAtLeast(1)
                val estimate = remember(history) { AnalyzerState.estimateNext() }

                Column(
                    Modifier.fillMaxSize().background(Navy).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("BACCARAT COACH", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
                            Text("SHOE TRACKER  •  LIVE ANALYSIS", color = Muted, fontSize = 10.sp, letterSpacing = 1.2.sp)
                        }
                        Surface(color = Color(0xFF20334D), shape = RoundedCornerShape(20.dp)) {
                            Text("ANDROID", Modifier.padding(horizontal = 12.dp, vertical = 7.dp), color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("BUILD SHOE HISTORY", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("$sessionName  •  $total hands recorded", color = Muted, fontSize = 11.sp)
                                }
                                Text("SHOE", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ResultEntryButton("PLAYER", "P", PlayerBlue, Modifier.weight(1f)) { history = history + "P" }
                                ResultEntryButton("BANKER", "B", BankerRed, Modifier.weight(1f)) { history = history + "B" }
                                ResultEntryButton("TIE", "T", TieGreen, Modifier.weight(1f)) { history = history + "T" }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { if (history.isNotEmpty()) history = history.dropLast(1) }, modifier = Modifier.weight(1f), enabled = history.isNotEmpty()) { Text("↶  UNDO") }
                                OutlinedButton(onClick = { history = emptyList(); sessionName = "New shoe" }, modifier = Modifier.weight(1f), enabled = history.isNotEmpty()) { Text("CLEAR SHOE") }
                            }
                            if (history.isNotEmpty()) {
                                Text("RECENT RESULTS", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    itemsIndexed(history.takeLast(50)) { index, result ->
                                        val color = resultColor(result)
                                        Box(Modifier.size(29.dp).background(color, CircleShape), contentAlignment = Alignment.Center) {
                                            Text(result, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            } else {
                                Text("Tap P, B or T after each completed hand.", color = Muted, fontSize = 12.sp)
                            }
                        }
                    }

                    Text("SHOE STATISTICS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatCard("PLAYER", p, if (total == 0) 0 else p * 100 / total, PlayerBlue, Modifier.weight(1f))
                        StatCard("BANKER", b, if (total == 0) 0 else b * 100 / total, BankerRed, Modifier.weight(1f))
                        StatCard("TIE", t, if (total == 0) 0 else t * 100 / total, TieGreen, Modifier.weight(1f))
                    }
                    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("SYSTEM SUGGESTION", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Box {
                                OutlinedButton(onClick = { modeMenu = true }) {
                                    Text(analysisMode, color = Color.White)
                                    Text("  ▾", color = Gold)
                                }
                                DropdownMenu(expanded = modeMenu, onDismissRequest = { modeMenu = false }) {
                                    listOf("Historical patterns", "Statistics only", "Manual tracking").forEach { mode ->
                                        DropdownMenuItem(text = { Text(mode) }, onClick = { analysisMode = mode; modeMenu = false })
                                    }
                                }
                            }
                            when (analysisMode) {
                                "Statistics only" -> Text("No next-hand call. This panel reports recorded shoe statistics only.", color = Muted, fontSize = 12.sp)
                                "Manual tracking" -> Text("Manual entry mode is active. Record the actual result after each hand.", color = Muted, fontSize = 12.sp)
                                else -> {
                                    Text(
                                        if (estimate.side == "WAITING" || estimate.side == "NO CLEAR EDGE") "NO CLEAR HISTORICAL PATTERN" else "HISTORICAL OBSERVATION: ${estimate.side}",
                                        color = if (estimate.side == "PLAYER") PlayerBlue else if (estimate.side == "BANKER") BankerRed else Gold,
                                        fontSize = 16.sp, fontWeight = FontWeight.Bold
                                    )
                                    Text("${estimate.pattern}  •  matched examples: ${estimate.sampleSize}", color = Muted, fontSize = 12.sp)
                                    if (estimate.confidence > 0) Text("Observed frequency: ${estimate.confidence}% in matching past examples", color = Muted, fontSize = 12.sp)
                                }
                            }
                            Text("Historical records are descriptive only. Baccarat outcomes are random; this is not a prediction or betting instruction.", color = Gold, fontSize = 11.sp)
                        }
                    }

                    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                            Text("LIVE TABLE READER", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Read the visible baccarat roadmap from another app using Android screen capture. Always verify detected results before relying on the saved history.", color = Muted, fontSize = 12.sp)
                            Text(liveStatus, color = Gold, fontSize = 11.sp)
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
                            ) { Text("START AUTOMATIC SCREEN READING") }
                            OutlinedButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    stopService(Intent(this@MainActivity, CaptureService::class.java))
                                    stopService(Intent(this@MainActivity, FloatingPanelService::class.java))
                                    liveStatus = "Automatic reader stopped"
                                }
                            ) { Text("STOP SCREEN READER") }
                        }
                    }
                    Text("Baccarat Coach style • Manual + automatic workflow", color = Muted, fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
                }
            }
        }
    }
}

@Composable
private fun ResultEntryButton(label: String, letter: String, tint: Color, modifier: Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick, modifier = modifier.height(62.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = tint),
        contentPadding = PaddingValues(4.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(letter, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StatCard(label: String, count: Int, percent: Int, tint: Color, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = tint, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(count.toString(), color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
            Text("$percent% of shoe", color = Muted, fontSize = 10.sp)
        }
    }
}

private fun resultColor(result: String): Color = when (result) {
    "P" -> PlayerBlue
    "B" -> BankerRed
    else -> TieGreen
}
