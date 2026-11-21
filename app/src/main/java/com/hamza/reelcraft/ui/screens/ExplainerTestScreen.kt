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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hamza.reelcraft.data.AiGenerator
import com.hamza.reelcraft.data.SecurePreferences
import com.hamza.reelcraft.model.SlideData
import com.hamza.reelcraft.render.ExplainerRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.system.measureTimeMillis

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplainerTestScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var topicInput by remember { mutableStateOf("Time zones kyun bane") }
    var selectedNiche by remember { mutableStateOf("Facts") }
    var isGenerating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var generatedSlides by remember { mutableStateOf<List<SlideData>>(emptyList()) }
    var currentSceneIndex by remember { mutableStateOf(0) }
    var progress by remember { mutableStateOf(0.5f) }
    var renderTimeMs by remember { mutableStateOf(0L) }

    // Initial load with default topic
    LaunchedEffect(Unit) {
        isGenerating = true
        withContext(Dispatchers.IO) {
            try {
                val activeKey = SecurePreferences(context).getActiveApiKey()
                if (activeKey.isBlank()) {
                    withContext(Dispatchers.Main) {
                        errorMessage = "Settings mein key daalo"
                    }
                }
                val res = AiGenerator.generateSlidesForTopic(
                    context = context,
                    nicheInput = selectedNiche,
                    topic = topicInput,
                    apiKey = activeKey,
                    allowOfflineFallback = true
                )
                withContext(Dispatchers.Main) {
                    generatedSlides = res.slides
                    if (res.usedOffline && res.errorMessage != null) {
                        errorMessage = "Offline script: ${res.errorMessage}"
                    }
                    isGenerating = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    errorMessage = "Error: ${e.message}"
                    isGenerating = false
                }
            }
        }
    }

    val currentSlide = generatedSlides.getOrNull(currentSceneIndex)
    val captionText = currentSlide?.voiceText ?: currentSlide?.body ?: "Enter a topic to generate dynamic explainer scenes."
    val highlightWords = currentSlide?.emphasisWords ?: emptyList()

    // Map topic / slide content to appropriate prop type dynamically
    val propType = remember(captionText) {
        val q = captionText.lowercase()
        when {
            q.contains("time") || q.contains("baje") || q.contains("ghanta") || q.contains("hour") -> "CLOCK"
            q.contains("date") || q.contains("saal") || q.contains("year") || q.contains("1884") -> "CALENDAR"
            q.contains("phone") || q.contains("screen") || q.contains("mobile") -> "DIGITAL_PHONE"
            q.contains("map") || q.contains("dunya") || q.contains("global") || q.contains("region") -> "MAP_SLICES"
            q.contains("log") || q.contains("percent") || q.contains("aadad") || q.contains("bar") -> "COUNTER_NUMBER"
            else -> "BAR_COMPARISON"
        }
    }

    val bitmap = remember(currentSlide, progress, propType) {
        val bmp = Bitmap.createBitmap(360, 640, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        renderTimeMs = measureTimeMillis {
            ExplainerRenderer.renderExplainerScene(
                canvas = canvas,
                captionText = captionText,
                highlightWords = highlightWords,
                propType = propType,
                propParamValue = progress,
                progress = progress
            )
        }
        bmp
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Explainer Mode (Dynamic Input)") },
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
            // Topic Input Bar
            OutlinedTextField(
                value = topicInput,
                onValueChange = { topicInput = it },
                label = { Text("Enter Explainer Topic") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Cyan,
                    unfocusedBorderColor = Color.Gray,
                    focusedLabelColor = Color.Cyan,
                    unfocusedTextColor = Color.White,
                    focusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    isGenerating = true
                    errorMessage = null
                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            val activeKey = SecurePreferences(context).getActiveApiKey()
                            if (activeKey.isBlank()) {
                                withContext(Dispatchers.Main) {
                                    errorMessage = "Settings mein key daalo"
                                }
                            }
                            val res = AiGenerator.generateSlidesForTopic(
                                context = context,
                                nicheInput = selectedNiche,
                                topic = topicInput,
                                apiKey = activeKey,
                                allowOfflineFallback = true
                            )
                            withContext(Dispatchers.Main) {
                                generatedSlides = res.slides
                                currentSceneIndex = 0
                                if (res.usedOffline && res.errorMessage != null) {
                                    errorMessage = "Offline script: ${res.errorMessage}"
                                }
                                isGenerating = false
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                errorMessage = "Error: ${e.message}"
                                isGenerating = false
                            }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                } else {
                    Text("🚀 Generate Dynamic Explainer Scenes", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = errorMessage!!,
                    color = Color.Yellow,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Frame render time info
            Text(
                text = "⚡ Frame Render Time: ${renderTimeMs}ms (< 33ms target) | Prop: $propType",
                color = if (renderTimeMs <= 33) Color(0xFF34D399) else Color(0xFFEF4444),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Preview Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                modifier = Modifier
                    .width(260.dp)
                    .height(380.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (isGenerating) {
                        CircularProgressIndicator(color = Color.Cyan)
                    } else {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Explainer Preview",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Scene / Slide Selector Chips
            if (generatedSlides.isNotEmpty()) {
                Text("Scenes (${currentSceneIndex + 1}/${generatedSlides.size}):", color = Color.LightGray, fontSize = 12.sp)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(generatedSlides.indices.toList()) { idx ->
                        FilterChip(
                            selected = currentSceneIndex == idx,
                            onClick = { currentSceneIndex = idx },
                            label = { Text("Scene ${idx + 1}") }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Animation Progress Slider
            Text("Animation Progress: ${(progress * 100).toInt()}%", color = Color.LightGray, fontSize = 12.sp)
            Slider(
                value = progress,
                onValueChange = { progress = it },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
