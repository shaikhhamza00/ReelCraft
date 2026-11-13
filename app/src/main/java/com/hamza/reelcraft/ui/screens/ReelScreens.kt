package com.hamza.reelcraft.ui.screens

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.hamza.reelcraft.ui.ReelViewModel
import com.hamza.reelcraft.ui.components.*
import com.hamza.reelcraft.ui.theme.*

@Composable
fun HomeScreen(viewModel: ReelViewModel, onNavigateCreate: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ReelBackground)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ReelSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VideoLibrary,
                        contentDescription = "Logo",
                        tint = ReelPrimaryPink
                    )
                }
                Text(
                    text = "ReelCraft",
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = ReelTextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Create cinematic Urdu & English reels in 3 taps",
            style = MaterialTheme.typography.bodyMedium,
            color = ReelTextSecondary
        )
        Spacer(modifier = Modifier.height(24.dp))

        SectionHeader(title = "Select a Niche")
        Spacer(modifier = Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(viewModel.niches) { niche ->
                NicheCard(
                    title = niche,
                    isSelected = viewModel.selectedNiche == niche,
                    onClick = {
                        viewModel.selectedNiche = niche
                        onNavigateCreate()
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        GradientButton(
            text = "Create Custom Reel",
            onClick = onNavigateCreate
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateScreen(viewModel: ReelViewModel, onBack: () -> Unit, onNavigatePreview: () -> Unit) {
    var expandedKeyDropdown by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ReelBackground)
    ) {
        ReelTopBar(title = "Configure Reel", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = viewModel.topicText,
                onValueChange = { viewModel.topicText = it },
                label = { Text("Topic or Question (e.g. History of Lahore)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ReelPrimaryViolet,
                    unfocusedBorderColor = ReelSurfaceVariant,
                    focusedLabelColor = ReelPrimaryViolet,
                    unfocusedLabelColor = ReelTextSecondary,
                    focusedTextColor = ReelTextPrimary,
                    unfocusedTextColor = ReelTextPrimary
                )
            )
            Spacer(modifier = Modifier.height(16.dp))

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
                            .menuAnchor(MenuAnchorType.PrimaryEditable, enabled = true),
                        shape = RoundedCornerShape(16.dp)
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
                Spacer(modifier = Modifier.height(12.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = viewModel.newKeyInput,
                    onValueChange = { viewModel.newKeyInput = it },
                    label = { Text("Add Gemini API Key") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                )
                Button(
                    onClick = { viewModel.addApiKey(viewModel.newKeyInput) },
                    enabled = viewModel.newKeyInput.isNotBlank(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ReelPrimaryViolet),
                    modifier = Modifier.height(56.dp)
                ) {
                    Text("Save")
                }
            }
            Spacer(modifier = Modifier.height(20.dp))

            SectionHeader(title = "Language")
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                viewModel.languages.forEach { lang ->
                    SelectableChip(
                        text = lang,
                        isSelected = viewModel.selectedLanguage == lang,
                        onClick = { viewModel.selectedLanguage = lang }
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))

            SectionHeader(title = "Cinematic Vibe")
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                viewModel.themes.take(4).forEach { th ->
                    SelectableChip(
                        text = th,
                        isSelected = viewModel.selectedThemeName == th,
                        onClick = { viewModel.selectedThemeName = th }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                viewModel.themes.drop(4).forEach { th ->
                    SelectableChip(
                        text = th,
                        isSelected = viewModel.selectedThemeName == th,
                        onClick = { viewModel.selectedThemeName = th }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            GradientButton(
                text = "Generate Slides",
                isLoading = viewModel.isGenerating,
                onClick = {
                    viewModel.generateSlides {
                        onNavigatePreview()
                    }
                }
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun PreviewScreen(viewModel: ReelViewModel, onBack: () -> Unit, onNavigateExport: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { viewModel.slides.size })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ReelBackground)
    ) {
        ReelTopBar(title = "Preview Slides (${viewModel.slides.size})", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))
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
                        .padding(vertical = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = ReelSurface),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(28.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ReelPrimaryViolet.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = slide.templateType.uppercase(),
                                color = ReelPrimaryPink,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = slide.headline,
                            style = MaterialTheme.typography.titleLarge,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = ReelTextPrimary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = slide.body,
                            style = MaterialTheme.typography.bodyLarge,
                            color = ReelTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            GradientButton(
                text = "Proceed to Export",
                onClick = onNavigateExport
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ExportScreen(viewModel: ReelViewModel, onBack: () -> Unit, androidContext: android.content.Context) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ReelBackground)
    ) {
        ReelTopBar(title = "Export & Play", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = viewModel.exportStatus,
                style = MaterialTheme.typography.bodyLarge,
                color = ReelTextPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Video Player if video is rendered
            viewModel.exportedVideoFile?.let { videoFile ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(ReelSurface)
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

            Spacer(modifier = Modifier.height(16.dp))

            if (viewModel.isGenerating) {
                CircularProgressIndicator(color = ReelPrimaryPink)
            } else {
                GradientButton(
                    text = if (viewModel.exportedVideoFile == null) "Render & Save to Gallery" else "Re-render Reel",
                    onClick = {
                        viewModel.exportVideo(androidContext) { file ->
                            // exported
                        }
                    }
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
