package com.hamza.reelcraft.ui

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hamza.reelcraft.audio.AudioPipeline
import com.hamza.reelcraft.data.AiGenerator
import com.hamza.reelcraft.data.SecurePreferences
import com.hamza.reelcraft.model.SlideData
import com.hamza.reelcraft.model.Theme
import com.hamza.reelcraft.render.VideoRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ReelViewModel(application: Application) : AndroidViewModel(application) {
    private val securePrefs = SecurePreferences(application)

    var selectedNiche by mutableStateOf("Riddle")
    var topicText by mutableStateOf("")
    
    // Multiple API Keys management with EncryptedSharedPreferences persistence
    val savedApiKeys = mutableStateListOf<String>()
    var selectedApiKey by mutableStateOf("")
    var newKeyInput by mutableStateOf("")

    var selectedLanguage by mutableStateOf("Roman Urdu")
    var selectedThemeName by mutableStateOf("KINETIC")
    
    var slides by mutableStateOf<List<SlideData>>(emptyList())
    var isGenerating by mutableStateOf(false)
    var exportStatus by mutableStateOf("Ready")
    var exportedVideoFile by mutableStateOf<File?>(null)

    // Multi-style generation storage (Map of Style Name -> Video File)
    var stylePreviews by mutableStateOf<Map<String, File?>>(emptyMap())

    val niches = listOf("Riddle", "Facts", "Motivation", "Story", "Quotes", "Quiz", "Shayari", "Horror")
    val languages = listOf("Roman Urdu", "Urdu", "Hindi", "English")
    val themes = Theme.allThemes.map { it.name }

    init {
        val keys = securePrefs.getApiKeys()
        savedApiKeys.addAll(keys)
        val active = securePrefs.getActiveApiKey()
        if (active.isNotBlank() && savedApiKeys.contains(active)) {
            selectedApiKey = active
        } else if (savedApiKeys.isNotEmpty()) {
            selectedApiKey = savedApiKeys.first()
        }
    }

    fun addApiKey(key: String) {
        val trimmed = key.trim()
        if (trimmed.isNotBlank() && !savedApiKeys.contains(trimmed)) {
            savedApiKeys.add(trimmed)
            selectedApiKey = trimmed
            newKeyInput = ""
            securePrefs.saveApiKeys(savedApiKeys.toSet())
            securePrefs.saveActiveApiKey(selectedApiKey)
        }
    }

    fun selectApiKey(key: String) {
        selectedApiKey = key
        securePrefs.saveActiveApiKey(key)
    }

    fun generateSlides(onComplete: () -> Unit) {
        isGenerating = true
        viewModelScope.launch {
            slides = AiGenerator.generateSlidesForTopic(getApplication(), selectedNiche, topicText, selectedApiKey)
            isGenerating = false
            onComplete()
        }
    }

    /**
     * Generates 6 different video style variations for the same topic
     */
    fun generateAllStyles(context: Context, onComplete: () -> Unit) {
        isGenerating = true
        exportStatus = "Rendering 6 cinematic style variations..."
        viewModelScope.launch(Dispatchers.IO) {
            val audioPipeline = AudioPipeline(context)
            val audioPreparedSlides = audioPipeline.prepareAudioForSlides(slides, selectedLanguage)
            val updatedSlides = audioPreparedSlides.map { it.first }

            val renderer = VideoRenderer(context)
            val map = mutableMapOf<String, File?>()
            for (theme in Theme.allThemes) {
                val file = renderer.renderProjectToMp4(updatedSlides, theme, "preview_${theme.name.lowercase()}_${System.currentTimeMillis()}.mp4")
                map[theme.name] = file
            }
            audioPipeline.shutdown()

            withContext(Dispatchers.Main) {
                isGenerating = false
                slides = updatedSlides
                stylePreviews = map
                onComplete()
            }
        }
    }

    fun exportVideo(context: Context, onComplete: (File?) -> Unit) {
        isGenerating = true
        exportStatus = "Synthesizing voiceover & rendering..."
        viewModelScope.launch(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            val audioPipeline = AudioPipeline(context)
            val audioPreparedSlides = audioPipeline.prepareAudioForSlides(slides, selectedLanguage)
            val updatedSlides = audioPreparedSlides.map { it.first }

            val renderer = VideoRenderer(context)
            val theme = Theme.fromName(selectedThemeName)
            val file = renderer.renderProjectToMp4(updatedSlides, theme)
            
            audioPipeline.shutdown()
            val totalTime = System.currentTimeMillis() - startTime
            Log.i("ReelViewModel", "PERFORMANCE: Total audio + video render time = $totalTime ms")

            withContext(Dispatchers.Main) {
                isGenerating = false
                slides = updatedSlides
                exportedVideoFile = file
                exportStatus = if (file != null) "Exported with Voiceover successfully!" else "Export failed."
                onComplete(file)
            }
        }
    }
}
