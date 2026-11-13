package com.hamza.reelcraft.audio

import java.io.File

interface VoiceProvider {
    suspend fun synthesize(text: String, voiceStyle: String, language: String, outputFile: File): Boolean
    fun isLanguageSupported(language: String): Boolean
}
