package com.hamza.reelcraft.ui.screens

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
            text = "Create faceless Urdu/English reels in 3 taps",
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

@Composable
fun CreateScreen(viewModel: ReelViewModel, onNavigatePreview: () -> Unit) {
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

        OutlinedTextField(
            value = viewModel.apiKey,
            onValueChange = { viewModel.apiKey = it },
            label = { Text("Gemini API Key (Optional for AI)") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
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

        // Theme selector
        Text(text = "Theme Vibe", fontWeight = FontWeight.SemiBold)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            viewModel.themes.forEach { th ->
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
        Spacer(modifier = Modifier.height(24.dp))

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
                Text(text = "Render & Save to Gallery")
            }
        }
    }
}
