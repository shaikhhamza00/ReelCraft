package com.hamza.reelcraft.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hamza.reelcraft.data.AiGenerator
import com.hamza.reelcraft.model.SlideData
import com.hamza.reelcraft.model.Theme
import com.hamza.reelcraft.render.VideoRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ReelViewModel : ViewModel() {
    var selectedNiche by mutableStateOf("Riddle")
    var topicText by mutableStateOf("")
    
    // Multiple API Keys management
    val savedApiKeys = mutableStateListOf<String>()
    var selectedApiKey by mutableStateOf("")
    var newKeyInput by mutableStateOf("")

    var selectedLanguage by mutableStateOf("Roman Urdu")
    var selectedThemeName by mutableStateOf("Cinematic Room")
    
    var slides by mutableStateOf<List<SlideData>>(emptyList())
    var isGenerating by mutableStateOf(false)
    var exportStatus by mutableStateOf("Ready")
    var exportedVideoFile by mutableStateOf<File?>(null)

    val niches = listOf("Riddle", "Facts", "Motivation", "Story", "Quotes", "Quiz", "Shayari", "Horror")
    val languages = listOf("Roman Urdu", "Urdu", "Hindi", "English")
    val themes = Theme.allThemes.map { it.name }

    fun addApiKey(key: String) {
        val trimmed = key.trim()
        if (trimmed.isNotBlank() && !savedApiKeys.contains(trimmed)) {
            savedApiKeys.add(trimmed)
            selectedApiKey = trimmed
            newKeyInput = ""
        }
    }

    fun generateSlides(onComplete: () -> Unit) {
        isGenerating = true
        viewModelScope.launch {
            slides = AiGenerator.generateSlidesForTopic(selectedNiche, topicText, selectedApiKey)
            isGenerating = false
            onComplete()
        }
    }

    fun exportVideo(context: Context, onComplete: (File?) -> Unit) {
        isGenerating = true
        exportStatus = "Rendering Cinematic Video..."
        viewModelScope.launch(Dispatchers.IO) {
            val renderer = VideoRenderer(context)
            val theme = Theme.fromName(selectedThemeName)
            val file = renderer.renderProjectToMp4(slides, theme)
            withContext(Dispatchers.Main) {
                isGenerating = false
                exportedVideoFile = file
                exportStatus = if (file != null) "Exported successfully!" else "Export failed."
                onComplete(file)
            }
        }
    }
}
