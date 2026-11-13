package com.hamza.reelcraft.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.hamza.reelcraft.model.SceneGenerator
import com.hamza.reelcraft.render.SceneRenderer
import kotlin.system.measureTimeMillis

data class ThemePreviewItem(
    val topic: String,
    val seed: Long,
    val bitmap: Bitmap,
    val renderTimeMs: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeTestScreen(onBack: () -> Unit) {
    val topics = listOf(
        "horror", "space", "motivation", "Islamic quote",
        "shayari", "cricket", "history", "ocean",
        "riddle", "technology", "love", "funny"
    )

    val previews = remember {
        mutableStateListOf<ThemePreviewItem>()
    }

    LaunchedEffect(Unit) {
        val list = mutableListOf<ThemePreviewItem>()
        for (topic in topics) {
            for (seedOffset in listOf(111L, 999L)) {
                val seed = topic.hashCode().toLong() + seedOffset
                var renderMs = 0L
                val bmp = Bitmap.createBitmap(360, 640, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)

                renderMs = measureTimeMillis {
                    val spec = SceneGenerator.getContextualSpec(topic, "General", topic, seed, 0, 1)
                    SceneRenderer.render(canvas, spec, 1.5f)
                }
                list.add(ThemePreviewItem(topic, seed, bmp, renderMs))
            }
        }
        previews.clear()
        previews.addAll(list)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Theme Test Grid (12 Topics x 2 Seeds)") },
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
        }
    }
}
