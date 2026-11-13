package com.hamza.reelcraft.ui.screens

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.hamza.reelcraft.ui.ReelViewModel

@Composable
fun HomeScreen(viewModel: ReelViewModel, onNavigateCreate: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "ReelCraft 🎬",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Create cinematic Urdu/English reels in 3 taps",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Select a Niche",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(12.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(viewModel.niches) { niche ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .clickable {
                            viewModel.selectedNiche = niche
                            onNavigateCreate()
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (viewModel.selectedNiche == niche)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = niche,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onNavigateCreate,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Create Custom Reel")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateScreen(viewModel: ReelViewModel, onNavigatePreview: () -> Unit) {
    var expandedKeyDropdown by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "Configure Reel",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = viewModel.topicText,
            onValueChange = { viewModel.topicText = it },
            label = { Text("Topic or Question (e.g. History of Lahore)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        // API Key Management UI
        if (viewModel.savedApiKeys.isNotEmpty()) {
            ExposedDropdownMenuBox(
                expanded = expandedKeyDropdown,
                onExpandedChange = { expandedKeyDropdown = !expandedKeyDropdown }
            ) {
                OutlinedTextField(
                    value = viewModel.selectedApiKey.ifBlank { "Select saved API Key" },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Saved Gemini API Keys") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedKeyDropdown) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expandedKeyDropdown,
                    onDismissRequest = { expandedKeyDropdown = false }
                ) {
                    viewModel.savedApiKeys.forEach { key ->
                        DropdownMenuItem(
                            text = { Text(if (key.length > 10) "${key.take(8)}...${key.takeLast(4)}" else key) },
                            onClick = {
                                viewModel.selectedApiKey = key
                                expandedKeyDropdown = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = viewModel.newKeyInput,
                onValueChange = { viewModel.newKeyInput = it },
                label = { Text("Add New Gemini API Key") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = { viewModel.addApiKey(viewModel.newKeyInput) },
                enabled = viewModel.newKeyInput.isNotBlank()
            ) {
                Text("Save Key")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Selected Niche: ${viewModel.selectedNiche}", fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(8.dp))

        // Language selector
        Text(text = "Language", fontWeight = FontWeight.SemiBold)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            viewModel.languages.forEach { lang ->
                FilterChip(
                    selected = viewModel.selectedLanguage == lang,
                    onClick = { viewModel.selectedLanguage = lang },
                    label = { Text(lang) }
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Theme / Cinematic Style selector
        Text(text = "Cinematic Vibe & Templates", fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            viewModel.themes.take(4).forEach { th ->
                FilterChip(
                    selected = viewModel.selectedThemeName == th,
                    onClick = { viewModel.selectedThemeName = th },
                    label = { Text(th) }
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            viewModel.themes.drop(4).forEach { th ->
                FilterChip(
                    selected = viewModel.selectedThemeName == th,
                    onClick = { viewModel.selectedThemeName = th },
                    label = { Text(th) }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                viewModel.generateSlides {
                    onNavigatePreview()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !viewModel.isGenerating
        ) {
            Text(text = if (viewModel.isGenerating) "Generating with AI..." else "Generate Slides")
        }
    }
}

@Composable
fun PreviewScreen(viewModel: ReelViewModel, onNavigateExport: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { viewModel.slides.size })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Preview Slides (${viewModel.slides.size})",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { page ->
            val slide = viewModel.slides[page]
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "Template: ${slide.templateType.uppercase()}", color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = slide.headline, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = slide.body, fontSize = 16.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onNavigateExport,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Proceed to Export")
        }
    }
}

@Composable
fun ExportScreen(viewModel: ReelViewModel, androidContext: android.content.Context) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Export Reel",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = viewModel.exportStatus)
        Spacer(modifier = Modifier.height(16.dp))

        // Video Player if video is rendered
        viewModel.exportedVideoFile?.let { videoFile ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
                    .padding(bottom = 16.dp)
            ) {
                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            setVideoURI(Uri.fromFile(videoFile))
                            setMediaController(MediaController(ctx))
                            start()
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        if (viewModel.isGenerating) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    viewModel.exportVideo(androidContext) { file ->
                        // exported
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = if (viewModel.exportedVideoFile == null) "Render & Save to Gallery" else "Re-render Video")
            }
        }
    }
}
