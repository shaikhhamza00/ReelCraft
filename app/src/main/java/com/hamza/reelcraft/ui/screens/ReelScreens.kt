package com.hamza.reelcraft.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import com.hamza.reelcraft.model.Theme
import com.hamza.reelcraft.ui.ReelViewModel
import com.hamza.reelcraft.ui.components.*
import com.hamza.reelcraft.ui.theme.*

@Composable
fun HomeScreen(
    viewModel: ReelViewModel,
    onNavigateCreate: () -> Unit,
    onNavigateSettings: () -> Unit
) {
    val gradientBrush = Brush.horizontalGradient(ReelGradientPrimary)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ReelBackground)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Top Bar: Gradient App Name + Settings Icon
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
                        .background(Brush.linearGradient(ReelGradientPrimary)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MovieCreation,
                        contentDescription = "Logo",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    text = "ReelCraft",
                    style = MaterialTheme.typography.titleLarge.copy(brush = gradientBrush),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(
                onClick = onNavigateSettings,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ReelSurfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = ReelTextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Faceless shorts in 3 taps",
            style = MaterialTheme.typography.bodyMedium,
            color = ReelTextSecondary
        )
        Spacer(modifier = Modifier.height(20.dp))

        // Recent Projects Placeholder Section
        SectionHeader(title = "Recent Projects")
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(3) { index ->
                Card(
                    modifier = Modifier
                        .width(130.dp)
                        .height(80.dp),
                    colors = CardDefaults.cardColors(containerColor = ReelSurface),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Text(
                            text = "Draft Reel #${index + 1}",
                            style = MaterialTheme.typography.labelLarge,
                            color = ReelTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        SectionHeader(title = "Select a Niche")
        Spacer(modifier = Modifier.height(6.dp))

        // Niche Grid (2 columns)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(viewModel.niches) { niche ->
                val isSelected = viewModel.selectedNiche == niche
                EnhancedNicheCard(
                    title = niche,
                    isSelected = isSelected,
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

@Composable
fun EnhancedNicheCard(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val (icon, tintColor) = getNicheStyle(title)
    val borderModifier = if (isSelected) {
        Modifier.border(2.dp, Brush.linearGradient(ReelGradientPrimary), RoundedCornerShape(16.dp))
    } else {
        Modifier
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = ReelSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(borderModifier)
                .padding(14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ReelSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = tintColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ReelTextPrimary
                )
            }
        }
    }
}

fun getNicheStyle(niche: String): Pair<ImageVector, Color> {
    return when (niche.lowercase()) {
        "riddle" -> Pair(Icons.Default.Extension, Color(0xFF22D3EE))
        "facts" -> Pair(Icons.Default.Lightbulb, Color(0xFFFACC15))
        "motivation" -> Pair(Icons.Default.Bolt, Color(0xFFFF4D8D))
        "story" -> Pair(Icons.AutoMirrored.Filled.MenuBook, Color(0xFF7C4DFF))
        "quotes" -> Pair(Icons.Default.FormatQuote, Color(0xFF38BDF8))
        "quiz" -> Pair(Icons.Default.Quiz, Color(0xFF34D399))
        "shayari" -> Pair(Icons.Default.Edit, Color(0xFFF472B6))
        "horror" -> Pair(Icons.Default.Nightlight, Color(0xFFA78BFA))
        else -> Pair(Icons.Default.Movie, Color(0xFF22D3EE))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: ReelViewModel, onBack: () -> Unit) {
    var expandedKeyDropdown by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ReelBackground)
    ) {
        ReelTopBar(title = "Settings & API Keys", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            SectionHeader(title = "Gemini API Key Management")
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Save and manage your Gemini AI API keys for dynamic reel generation.",
                style = MaterialTheme.typography.bodyMedium,
                color = ReelTextSecondary
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (viewModel.savedApiKeys.isNotEmpty()) {
                ExposedDropdownMenuBox(
                    expanded = expandedKeyDropdown,
                    onExpandedChange = { expandedKeyDropdown = !expandedKeyDropdown }
                ) {
                    OutlinedTextField(
                        value = viewModel.selectedApiKey.ifBlank { "Select saved API Key" },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Active API Key") },
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
                Spacer(modifier = Modifier.height(16.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = viewModel.newKeyInput,
                    onValueChange = { viewModel.newKeyInput = it },
                    label = { Text("Enter New Gemini API Key") },
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
        }
    }
}

@Composable
fun CreateScreen(
    viewModel: ReelViewModel,
    onBack: () -> Unit,
    onNavigatePreview: () -> Unit,
    onNavigateSettings: () -> Unit
) {
    val surpriseTopics = listOf(
        "99% log yeh riddle solve nahi kar paate",
        "Secrets of the Ancient Pyramids",
        "The Untold Story of Lahore",
        "Why do we procrastinate and how to stop",
        "Top 3 Habits of Millionaires",
        "The Haunted Mansion Mystery"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ReelBackground)
    ) {
        ReelTopBar(title = "Configure Reel", onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Selected Niche Badge
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ReelPrimaryViolet.copy(alpha = 0.2f),
                modifier = Modifier.align(Alignment.Start)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tag,
                        contentDescription = null,
                        tint = ReelPrimaryPink,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Niche: ${viewModel.selectedNiche}",
                        style = MaterialTheme.typography.labelLarge,
                        color = ReelPrimaryPink,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // API Key Warning Banner if missing
            if (viewModel.savedApiKeys.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ReelSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VpnKey,
                                contentDescription = null,
                                tint = Color(0xFFFACC15)
                            )
                            Text(
                                text = "API key add karein for live AI generation",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ReelTextPrimary
                            )
                        }
                        TextButton(onClick = onNavigateSettings) {
                            Text(text = "Settings", color = ReelSecondaryCyan, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Topic Input
            OutlinedTextField(
                value = viewModel.topicText,
                onValueChange = { viewModel.topicText = it },
                label = { Text("What's your reel about?") },
                placeholder = { Text("e.g. History of Lahore, Brain teaser riddle...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ReelPrimaryViolet,
                    unfocusedBorderColor = ReelSurfaceVariant,
                    focusedLabelColor = ReelPrimaryViolet,
                    unfocusedLabelColor = ReelTextSecondary,
                    focusedTextColor = ReelTextPrimary,
                    unfocusedTextColor = ReelTextPrimary
                )
            )

            // Surprise me button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = {
                        viewModel.topicText = surpriseTopics.random()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = ReelSecondaryCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Surprise me ✨",
                        color = ReelSecondaryCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Language Selector (Horizontal Scrollable Row of SelectableChips)
            SectionHeader(title = "Language")
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                viewModel.languages.forEach { lang ->
                    SelectableChip(
                        text = lang,
                        isSelected = viewModel.selectedLanguage == lang,
                        onClick = { viewModel.selectedLanguage = lang }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Vibe / Theme Visual Thumbnail Picker (Horizontal Scroll)
            SectionHeader(title = "Cinematic Vibe & Template")
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Theme.allThemes.forEach { th ->
                    val isSelected = viewModel.selectedThemeName == th.name
                    ThemeThumbnailCard(
                        themeName = th.name,
                        bgColorHex = th.backgroundColorHex,
                        accentColorHex = th.accentColorHex,
                        isSelected = isSelected,
                        onClick = { viewModel.selectedThemeName = th.name }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Sticky Bottom Generate Button
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = ReelSurface,
            shadowElevation = 8.dp,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Box(modifier = Modifier.padding(20.dp)) {
                val isTopicValid = viewModel.topicText.isNotBlank()
                GradientButton(
                    text = "Generate Reel",
                    isLoading = viewModel.isGenerating,
                    enabled = isTopicValid,
                    onClick = {
                        viewModel.generateSlides {
                            onNavigatePreview()
                        }
                    },
                    modifier = Modifier.alpha(if (isTopicValid) 1f else 0.5f)
                )
            }
        }
    }
}

@Composable
fun ThemeThumbnailCard(
    themeName: String,
    bgColorHex: String,
    accentColorHex: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgParsedColor = try { Color(android.graphics.Color.parseColor(bgColorHex)) } catch (e: Exception) { ReelSurface }

    val borderMod = if (isSelected) {
        Modifier.border(2.5.dp, Brush.linearGradient(ReelGradientPrimary), RoundedCornerShape(16.dp))
    } else {
        Modifier.border(1.dp, ReelSurfaceVariant, RoundedCornerShape(16.dp))
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(85.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .width(85.dp)
                .height(130.dp) // 9:16 aspect ratio thumbnail shape
                .clip(RoundedCornerShape(16.dp))
                .background(bgParsedColor)
                .then(borderMod)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(accentColorHex.toColor())
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = themeName,
            style = MaterialTheme.typography.labelLarge,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) ReelPrimaryPink else ReelTextSecondary,
            maxLines = 1
        )
    }
}

fun String.toColorType(): Color {
    return try {
        Color(android.graphics.Color.parseColor(this))
    } catch (e: Exception) {
        Color.White
    }
}

fun String.toColor(): Color = toColorType()

@Composable
fun PreviewScreen(viewModel: ReelViewModel, onBack: () -> Unit, onNavigateExport: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { viewModel.slides.size })
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ReelBackground)
    ) {
        ReelTopBar(title = "Preview", onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // HorizontalPager with 9:16 aspect ratio card and peek (content padding)
            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = 48.dp),
                pageSpacing = 16.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(480.dp)
            ) { page ->
                val slide = viewModel.slides[page]
                Card(
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(9f / 16f)
                        .clip(RoundedCornerShape(28.dp)),
                    colors = CardDefaults.cardColors(containerColor = ReelSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Small pill badge for template type
                        Surface(
                            shape = CircleShape,
                            color = ReelPrimaryViolet.copy(alpha = 0.25f)
                        ) {
                            Text(
                                text = slide.templateType.uppercase(),
                                color = ReelPrimaryPink,
                                style = MaterialTheme.typography.labelLarge,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = slide.headline,
                            style = MaterialTheme.typography.titleLarge,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = ReelTextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = slide.body,
                            style = MaterialTheme.typography.bodyLarge,
                            fontSize = 14.sp,
                            color = ReelTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Page Indicator Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(viewModel.slides.size) { iteration ->
                    val isSelected = pagerState.currentPage == iteration
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) ReelPrimaryPink else ReelSurfaceVariant)
                    )
                }
            }
        }

        // Bottom Action Buttons: Secondary "Edit text" (outline) and Primary "Export" (gradient)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = ReelSurface,
            shadowElevation = 8.dp,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        Toast.makeText(context, "Editing slide text...", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.5.dp, ReelPrimaryViolet)
                ) {
                    Text(
                        text = "Edit text",
                        color = ReelTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    GradientButton(
                        text = "Export",
                        onClick = onNavigateExport
                    )
                }
            }
        }
    }
}

@Composable
fun ExportScreen(
    viewModel: ReelViewModel,
    onBack: () -> Unit,
    onNavigateHome: () -> Unit,
    androidContext: android.content.Context
) {
    val context = LocalContext.current
    var isSavedToGallery by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ReelBackground)
    ) {
        ReelTopBar(title = "Export & Play", onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when {
                viewModel.isGenerating -> {
                    // Rendering state with linear progress and message
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        colors = CardDefaults.cardColors(containerColor = ReelSurface),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = ReelPrimaryPink,
                                strokeWidth = 4.dp,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = "Aapki reel ban rahi hai...",
                                style = MaterialTheme.typography.titleLarge,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = ReelTextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = viewModel.exportStatus,
                                style = MaterialTheme.typography.bodyMedium,
                                color = ReelTextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                viewModel.exportedVideoFile != null -> {
                    val videoFile = viewModel.exportedVideoFile!!

                    // 9:16 Aspect Ratio Card with rounded corners for video player
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(420.dp)
                            .aspectRatio(9f / 16f)
                            .clip(RoundedCornerShape(28.dp))
                            .background(ReelSurface)
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

                viewModel.exportStatus.contains("failed", ignoreCase = true) -> {
                    // Error state card with retry button
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        colors = CardDefaults.cardColors(containerColor = ReelSurface),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = "Error",
                                tint = ReelError,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Export Failed",
                                style = MaterialTheme.typography.titleLarge,
                                color = ReelTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = viewModel.exportStatus,
                                style = MaterialTheme.typography.bodyMedium,
                                color = ReelTextSecondary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            GradientButton(
                                text = "Retry Export",
                                onClick = {
                                    viewModel.exportVideo(androidContext) { _ -> }
                                }
                            )
                        }
                    }
                }

                else -> {
                    // Initial State before rendering
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MovieFilter,
                            contentDescription = null,
                            tint = ReelPrimaryPink,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Ready to Export Your Cinematic Reel",
                            style = MaterialTheme.typography.titleLarge,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center,
                            color = ReelTextPrimary
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        GradientButton(
                            text = "Render & Save to Gallery",
                            onClick = {
                                viewModel.exportVideo(androidContext) { file ->
                                    if (file != null) isSavedToGallery = true
                                }
                            }
                        )
                    }
                }
            }
        }

        // 3 Actions at bottom when video is successfully exported
        if (viewModel.exportedVideoFile != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = ReelSurface,
                shadowElevation = 8.dp,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 1. Save to Gallery / Saved Button
                        OutlinedButton(
                            onClick = {
                                isSavedToGallery = true
                                Toast.makeText(context, "Saved to Gallery!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.5.dp, if (isSavedToGallery) Color(0xFF34D399) else ReelPrimaryViolet)
                        ) {
                            Icon(
                                imageVector = if (isSavedToGallery) Icons.Default.Check else Icons.Default.Download,
                                contentDescription = null,
                                tint = if (isSavedToGallery) Color(0xFF34D399) else ReelTextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isSavedToGallery) "Saved" else "Save to Gallery",
                                color = if (isSavedToGallery) Color(0xFF34D399) else ReelTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        // 2. Share Button (Primary Gradient)
                        Box(modifier = Modifier.weight(1f)) {
                            GradientButton(
                                text = "Share",
                                onClick = {
                                    val videoFile = viewModel.exportedVideoFile
                                    if (videoFile != null) {
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "video/mp4"
                                            putExtra(
                                                Intent.EXTRA_STREAM,
                                                FileProvider.getUriForFile(androidContext, "${androidContext.packageName}.fileprovider", videoFile)
                                            )
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        androidContext.startActivity(Intent.createChooser(intent, "Share Reel"))
                                    }
                                }
                            )
                        }
                    }

                    // 3. Create another (Outline Button)
                    OutlinedButton(
                        onClick = onNavigateHome,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.5.dp, ReelSurfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = ReelTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Create another",
                            color = ReelTextSecondary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
