package com.hamza.reelcraft.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.io.File
import java.util.Locale

class TtsManager(context: Context, private val onInitialized: () -> Unit) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    var isReady = false
        private set

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.forLanguageTag("ur-PK"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w("TtsManager", "Urdu language not supported, fallback to default")
                tts?.language = Locale.getDefault()
            }
            isReady = true
            onInitialized()
        } else {
            Log.e("TtsManager", "TTS initialization failed")
        }
    }

    fun synthesizeSlideAudio(text: String, outputFile: File, onComplete: (Boolean) -> Unit) {
        if (!isReady || tts == null) {
            onComplete(false)
            return
        }

        val result = tts?.synthesizeToFile(text, null, outputFile, "tts_utterance_${System.currentTimeMillis()}")
        onComplete(result == TextToSpeech.SUCCESS)
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}
