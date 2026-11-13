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
    val slides: List<SlideData>
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

        // Load 6 new script debug items via AiGenerator fallback scripts
        val scriptList = mutableListOf<ScriptDebugItem>()
        withContext(Dispatchers.IO) {
            for (t in newTopics) {
                // Use reflection / helper or fallback generator
                val slides = AiGenerator.generateSlidesForTopic(context, t, t, "")
                scriptList.add(ScriptDebugItem(t, slides))
            }
        }
        scriptDebugItems.clear()
        scriptDebugItems.addAll(scriptList)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Debug & Theme Test Screen") },
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
                    text = { Text("Script Debug (6 New Topics)", color = if (selectedTab == 1) Color.Cyan else Color.LightGray) }
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
                if (scriptDebugItems.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.Cyan)
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
                                    Text(
                                        text = "📌 Topic: ${item.topic}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Cyan
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    item.slides.forEachIndexed { sIdx, slide ->
                                        HorizontalDivider(color = Color(0xFF334155), thickness = 0.5.dp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Slide ${sIdx + 1} [${slide.voiceStyle}]:",
                                            fontSize = 12.sp,
                                            color = Color(0xFFA78BFA),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Headline: ${slide.headline}",
                                            fontSize = 13.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "VoiceText (${slide.voiceText.split(" ").size} words): \"${slide.voiceText}\"",
                                            fontSize = 13.sp,
                                            color = Color(0xFF34D399)
                                        )
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
