package com.hamza.reelcraft.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.Locale
import kotlin.coroutines.resume

class AndroidTtsProvider(private val context: Context) : VoiceProvider {
    companion object {
        private const val TAG = "AndroidTtsProvider"
    }

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private suspend fun ensureInitialized(): Boolean {
        if (isInitialized && tts != null) return true

        return suspendCancellableCoroutine { continuation ->
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    isInitialized = true
                    continuation.resume(true)
                } else {
                    Log.e(TAG, "TTS initialization failed with status $status")
                    continuation.resume(false)
                }
            }
        }
    }

    override fun isLanguageSupported(language: String): Boolean {
        val locale = when (language.lowercase()) {
            "urdu" -> Locale("ur", "PK")
            "hindi" -> Locale("hi", "IN")
            "roman urdu" -> Locale("ur", "PK")
            else -> Locale.ENGLISH
        }
        val result = tts?.isLanguageAvailable(locale)
        return result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
    }

    override suspend fun synthesize(text: String, voiceStyle: String, language: String, outputFile: File): Boolean {
        if (!ensureInitialized()) return false

        val locale = when (language.lowercase()) {
            "urdu", "roman urdu" -> Locale("ur", "PK")
            "hindi" -> Locale("hi", "IN")
            else -> Locale.ENGLISH
        }

        tts?.language = locale

        // Adjust pitch and speechRate according to voiceStyle (Step B)
        when (voiceStyle.uppercase()) {
            "WHISPER_SLOW" -> {
                tts?.setPitch(0.8f)
                tts?.setSpeechRate(0.85f)
            }
            "ENERGETIC" -> {
                tts?.setPitch(1.2f)
                tts?.setSpeechRate(1.15f)
            }
            "SUSPENSE" -> {
                tts?.setPitch(0.9f)
                tts?.setSpeechRate(0.9f)
            }
            "SOFT_EMOTIONAL" -> {
                tts?.setPitch(1.0f)
                tts?.setSpeechRate(0.95f)
            }
            "SERENE_MALE" -> {
                tts?.setPitch(0.85f)
                tts?.setSpeechRate(1.0f)
            }
            else -> { // CALM_NARRATOR
                tts?.setPitch(1.0f)
                tts?.setSpeechRate(1.0f)
            }
        }

        return suspendCancellableCoroutine { continuation ->
            val utteranceId = "tts_${System.currentTimeMillis()}"
            
            // Set synthesis callback if needed or use synthesizeToFile
            val result = tts?.synthesizeToFile(text, null, outputFile, utteranceId)
            
            // In modern Android, synthesizeToFile is asynchronous or synchronous depending on engine.
            // We can check if file exists and has size after a short delay or via UtteranceProgressListener.
            tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                override fun onStart(id: String?) {}
                override fun onDone(id: String?) {
                    if (id == utteranceId && outputFile.exists() && outputFile.length() > 0) {
                        continuation.resume(true)
                    } else {
                        continuation.resume(false)
                    }
                }
                override fun onError(id: String?) {
                    continuation.resume(false)
                }
            })

            if (result != TextToSpeech.SUCCESS) {
                continuation.resume(false)
            }
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}
