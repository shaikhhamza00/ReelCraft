package com.hamza.reelcraft.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hamza.reelcraft.render.ExplainerRenderer
import kotlin.system.measureTimeMillis

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplainerTestScreen(onBack: () -> Unit) {
    val propTypes = listOf("CLOCK", "CALENDAR", "DIGITAL_PHONE", "MAP_SLICES", "COUNTER_NUMBER", "BAR_COMPARISON")
    var selectedProp by remember { mutableStateOf("CLOCK") }
    var progress by remember { mutableStateOf(0.5f) }

    val sampleCaption = "Time zones were created in 1884 to synchronize global railway schedules."
    val highlightWords = listOf("1884", "railway")

    var renderTimeMs by remember { mutableStateOf(0L) }
    val bitmap = remember(selectedProp, progress) {
        val bmp = Bitmap.createBitmap(360, 640, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        renderTimeMs = measureTimeMillis {
            ExplainerRenderer.renderExplainerScene(
                canvas = canvas,
                captionText = sampleCaption,
                highlightWords = highlightWords,
                propType = selectedProp,
                propParamValue = progress,
                progress = progress
            )
        }
        bmp
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Explainer Mode POC (Captions & Props)") },
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Frame render time info
            Text(
                text = "⚡ Frame Render Time: ${renderTimeMs}ms (< 33ms target)",
                color = if (renderTimeMs <= 33) Color(0xFF34D399) else Color(0xFFEF4444),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Preview Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                modifier = Modifier
                    .width(300.dp)
                    .height(480.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Explainer Preview",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Prop Selector Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(propTypes) { prop ->
                    FilterChip(
                        selected = selectedProp == prop,
                        onClick = { selectedProp = prop },
                        label = { Text(prop) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Animation Progress Slider
            Text("Animation / Time Progress: ${(progress * 100).toInt()}%", color = Color.LightGray, fontSize = 12.sp)
            Slider(
                value = progress,
                onValueChange = { progress = it },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
