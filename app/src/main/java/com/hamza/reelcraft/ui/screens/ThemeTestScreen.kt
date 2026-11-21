package com.hamza.reelcraft.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hamza.reelcraft.data.AiGenerator
import com.hamza.reelcraft.data.SecurePreferences
import com.hamza.reelcraft.model.SceneGenerator
import com.hamza.reelcraft.model.SlideData
import com.hamza.reelcraft.render.SceneRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.system.measureTimeMillis

data class ThemePreviewItem(
    val topic: String,
    val seed: Long,
    val bitmap: Bitmap,
    val renderTimeMs: Long
)

data class ScriptDebugItem(
    val topic: String,
    val niche: String,
    val slides: List<SlideData>,
    val modelUsed: String?,
    val pass1Ms: Long,
    val pass2Ms: Long,
    val pass2Used: Boolean,
    val totalMs: Long,
    val usedOffline: Boolean,
    val statusMsg: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeTestScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0: Theme Grid, 1: Script Debug (6 New Topics)

    val topics = listOf(
        "horror", "space", "motivation", "Islamic quote",
        "shayari", "cricket", "history", "ocean",
        "riddle", "technology", "love", "funny"
    )

    val newTopics = listOf(
        "Bhoot wali purani haveli",
        "Rickshaw chalane wala jo doctor bana",
        "Aisi cheez jo jitni dhoyo utni gandi hoti hai",
        "Insaan ke dil ke baare mein hairan kar dene wali haqiqat",
        "Barish ki raat ka intezar",
        "Hazrat Ali (RA) ka ek waqia"
    )

    val previews = remember { mutableStateListOf<ThemePreviewItem>() }
    val scriptDebugItems = remember { mutableStateListOf<ScriptDebugItem>() }
    var apiKeyStatus by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        // Load theme previews
        val list = mutableListOf<ThemePreviewItem>()
        for (topic in topics) {
            for (seedOffset in listOf(111L, 999L)) {
                val seed = topic.hashCode().toLong() + seedOffset
                val bmp = Bitmap.createBitmap(360, 640, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)

                val renderMs = measureTimeMillis {
                    val spec = SceneGenerator.getContextualSpec(topic, "General", topic, seed, 0, 1)
                    SceneRenderer.render(canvas, spec, 1.5f)
                }
                list.add(ThemePreviewItem(topic, seed, bmp, renderMs))
            }
        }
        previews.clear()
        previews.addAll(list)

        // Load 6 new script debug items via AiGenerator
        val scriptList = mutableListOf<ScriptDebugItem>()
        withContext(Dispatchers.IO) {
            val activeKey = SecurePreferences(context).getActiveApiKey()
            if (activeKey.isBlank()) {
                withContext(Dispatchers.Main) {
                    apiKeyStatus = "Settings mein key daalo (Using offline fallback scripts)"
                }
            }

            for (t in newTopics) {
                try {
                    val niche = AiGenerator.detectNicheForTopic(t)
                    val res = AiGenerator.generateSlidesForTopic(
                        context = context,
                        nicheInput = niche,
                        topic = t,
                        apiKey = activeKey,
                        allowOfflineFallback = true
                    )
                    val statusStr = if (res.usedOffline) {
                        "Offline script: ${res.errorMessage ?: "API Key missing"}"
                    } else {
                        "Model: ${res.modelUsed} | Pass1: ${res.pass1Ms}ms | Pass2: ${if (res.pass2Used) "${res.pass2Ms}ms" else "Off"} | Total: ${res.latencyMs}ms"
                    }
                    scriptList.add(
                        ScriptDebugItem(
                            topic = t,
                            niche = niche,
                            slides = res.slides,
                            modelUsed = res.modelUsed,
                            pass1Ms = res.pass1Ms,
                            pass2Ms = res.pass2Ms,
                            pass2Used = res.pass2Used,
                            totalMs = res.latencyMs,
                            usedOffline = res.usedOffline,
                            statusMsg = statusStr
                        )
                    )
                } catch (e: Exception) {
                    scriptList.add(
                        ScriptDebugItem(
                            topic = t,
                            niche = AiGenerator.detectNicheForTopic(t),
                            slides = emptyList(),
                            modelUsed = null,
                            pass1Ms = 0,
                            pass2Ms = 0,
                            pass2Used = false,
                            totalMs = 0,
                            usedOffline = true,
                            statusMsg = "Error: ${e.message}"
                        )
                    )
                }
            }
        }
        scriptDebugItems.clear()
        scriptDebugItems.addAll(scriptList)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Debug & Script Quality Test") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back", color = Color.White) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A), titleContentColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF0B0B12))
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF1E293B),
                contentColor = Color.Cyan
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Theme Grid (12 Topics)", color = if (selectedTab == 0) Color.Cyan else Color.LightGray) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Script Debug (6 Topics)", color = if (selectedTab == 1) Color.Cyan else Color.LightGray) }
                )
            }

            if (selectedTab == 0) {
                if (previews.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.Cyan)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(previews) { item ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Image(
                                        bitmap = item.bitmap.asImageBitmap(),
                                        contentDescription = item.topic,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                    )
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = item.topic.uppercase(),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color.Cyan
                                        )
                                        Text(
                                            text = "Seed: ${item.seed.toString().takeLast(4)} | Time: ${item.renderTimeMs}ms",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.LightGray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                if (apiKeyStatus.isNotBlank()) {
                    Surface(
                        color = Color(0xFF334155),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "⚠️ $apiKeyStatus",
                            color = Color(0xFFFDE047),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                if (scriptDebugItems.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color.Cyan)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Generating scripts with Pass 1 + Pass 2 refinement...", color = Color.LightGray, fontSize = 13.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(scriptDebugItems) { item ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "📌 ${item.topic}",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Cyan,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Surface(
                                            color = Color(0xFF334155),
                                            shape = MaterialTheme.shapes.small
                                        ) {
                                            Text(
                                                text = item.niche,
                                                color = Color(0xFFF472B6),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    if (!item.statusMsg.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = item.statusMsg,
                                            fontSize = 12.sp,
                                            color = if (item.usedOffline) Color.Yellow else Color(0xFF34D399)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    item.slides.forEachIndexed { sIdx, slide ->
                                        HorizontalDivider(color = Color(0xFF334155), thickness = 0.5.dp)
                                        Spacer(modifier = Modifier.height(4.dp))

                                        val wordCount = slide.voiceText.split(" ").filter { it.isNotBlank() }.size
                                        val bannedFound = AiGenerator.checkForBannedPhrases("${slide.headline} ${slide.body} ${slide.voiceText}")

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Slide ${sIdx + 1} [${slide.templateType.uppercase()} / ${slide.transition}]",
                                                fontSize = 12.sp,
                                                color = Color(0xFFA78BFA),
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = if (bannedFound != null) "⚠️ Banned: '$bannedFound'" else "✅ Clean",
                                                fontSize = 11.sp,
                                                color = if (bannedFound != null) Color(0xFFEF4444) else Color(0xFF34D399),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Text(
                                            text = "Headline: ${slide.headline}",
                                            fontSize = 13.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Body: ${slide.body}",
                                            fontSize = 12.sp,
                                            color = Color.LightGray
                                        )
                                        Text(
                                            text = "VoiceText ($wordCount words): \"${slide.voiceText}\"",
                                            fontSize = 13.sp,
                                            color = if (wordCount in 8..15) Color(0xFF34D399) else Color(0xFFFACC15)
                                        )
                                        if (slide.emphasisWords.isNotEmpty()) {
                                            Text(
                                                text = "Emphasis: ${slide.emphasisWords.joinToString(", ")} | Style: ${slide.voiceStyle}",
                                                fontSize = 11.sp,
                                                color = Color(0xFF38BDF8)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
