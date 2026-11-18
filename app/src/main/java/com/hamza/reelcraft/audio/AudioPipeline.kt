package com.hamza.reelcraft.audio

import android.content.Context
import android.util.Log
import com.hamza.reelcraft.model.SlideData
import java.io.File

class AudioPipeline(private val context: Context) {
    companion object {
        private const val TAG = "AudioPipeline"
    }

    private val ttsProvider = AndroidTtsProvider(context)

    suspend fun prepareAudioForSlides(slides: List<SlideData>, language: String): List<Pair<SlideData, File?>> {
        Log.i(TAG, "prepareAudioForSlides starting with ${slides.size} slides, language: $language")
        val results = mutableListOf<Pair<SlideData, File?>>()

        for ((index, slide) in slides.withIndex()) {
            val textToSpeech = if (slide.voiceText.isNotBlank()) slide.voiceText else slide.headline
            val audioFile = File(context.cacheDir, "slide_voice_${index}_${System.currentTimeMillis()}.wav")
            
            val startTime = System.currentTimeMillis()
            val success = ttsProvider.synthesize(textToSpeech, slide.voiceStyle, language, audioFile)
            val synthTime = System.currentTimeMillis() - startTime
            Log.i(TAG, "PERFORMANCE: TTS synthesis for slide $index success=$success took $synthTime ms")

            val finalAudioFile = if (success && audioFile.exists() && audioFile.length() > 44) {
                audioFile
            } else {
                Log.w(TAG, "Audio file invalid or synthesis failed for slide $index")
                null
            }

            val calculatedDuration = if (finalAudioFile != null) {
                val pcmSize = finalAudioFile.length() - 44
                val durationMs = (pcmSize * 1000L) / 32000L
                durationMs.coerceAtLeast(2500L) + 300L
            } else {
                3000L
            }

            val updatedSlide = slide.copy(durationMs = calculatedDuration)
            results.add(updatedSlide to finalAudioFile)
        }

        Log.i(TAG, "prepareAudioForSlides completed successfully for ${results.size} slides")
        return results
    }

    fun shutdown() {
        Log.i(TAG, "AudioPipeline shutting down")
        ttsProvider.shutdown()
    }
}
