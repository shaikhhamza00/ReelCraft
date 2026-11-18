package com.hamza.reelcraft.data

import android.content.Context
import android.util.Log
import com.hamza.reelcraft.model.SceneGenerator
import com.hamza.reelcraft.model.SlideData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object AiGenerator {
    private const val TAG = "AiGenerator"

    suspend fun generateSlidesForTopic(context: Context, niche: String, topic: String, apiKey: String): List<SlideData> {
        if (apiKey.isBlank()) {
            throw IllegalArgumentException("Gemini API Key missing! Please enter your API Key in Settings.")
        }

        val securePrefs = SecurePreferences(context)
        val recentPresets = securePrefs.getRecentPresetIds()
        val projectSeed = System.currentTimeMillis()

        return withContext(Dispatchers.IO) {
            val cleanTopic = if (topic.isBlank()) niche else topic
            val defaultStyle = SlideData.getDefaultVoiceStyle(niche)
            val targetSlideCount = when (niche.lowercase()) {
                "riddle", "quiz", "facts" -> 5
                "motivation" -> 6
                "shayari", "quotes" -> 4
                else -> 6
            }

            val prompt = """
                You are an expert viral scriptwriter for Instagram Reels and YouTube Shorts writing in Urdu/Roman Urdu/Hindi.
                Create a high-tension, fascinating script about '$cleanTopic' in niche '$niche'.
                Return ONLY a valid JSON array of exactly $targetSlideCount objects. No markdown formatting, no code blocks, pure JSON array only.

                Each object must have these exact keys:
                - "templateType": "story" or "riddle" or "quote" or "listicle"
                - "headline": punchy hook in Roman Urdu/Urdu
                - "body": fascinating detailed fact or story sentence
                - "voiceText": short 8 to 15 words spoken voiceover text
                - "voiceStyle": "WHISPER_SLOW", "ENERGETIC", "CALM_NARRATOR", "SUSPENSE", "SOFT_EMOTIONAL", or "SERENE_MALE"
                - "bgMood": mood keyword (e.g. "horror", "motivation", "space", "islamic", "shayari", "riddle")
                - "bgScene": "NIGHT_SKY", "OCEAN", "FIRE_EMBERS", "CYBER_GRID", "AURORA_BOREALIS", "GOLDEN_SAND", or "MYSTIC_PURPLE"
                - "accentColor": hex color code e.g. "#22D3EE"

                Rules:
                1. Hook in slide 1 must grab attention in under 3 seconds. Do not start with "Aaj hum" or "Dosto".
                2. VoiceText on every slide must be between 8 and 15 words.
                3. No moral lectures ("zid karo", "sabaq", "haar mat maano"). Show, don't tell.
            """.trimIndent()

            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().put(
                    JSONObject().put("parts", JSONArray().put(
                        JSONObject().put("text", prompt)
                    ))
                ))
                put("generationConfig", JSONObject().put("responseMimeType", "application/json").put("temperature", 0.9).put("topP", 0.95))
            }

            val apiVersions = listOf("v1", "v1beta")
            val modelsToTry = listOf("gemini-1.5-flash", "gemini-1.5-pro", "gemini-pro", "gemini-1.5-flash-latest", "gemini-2.0-flash")
            var responseStr: String? = null
            var lastErrorMsg = ""

            for (apiVersion in apiVersions) {
                for (modelName in modelsToTry) {
                    try {
                        Log.d(TAG, "Trying API version: $apiVersion, model: $modelName")
                        val url = URL("https://generativelanguage.googleapis.com/$apiVersion/models/$modelName:generateContent?key=$apiKey")
                        val connection = (url.openConnection() as HttpURLConnection).apply {
                            requestMethod = "POST"
                            setRequestProperty("Content-Type", "application/json; charset=utf-8")
                            doOutput = true
                            connectTimeout = 15000
                            readTimeout = 15000
                        }

                        connection.outputStream.use { os ->
                            val input = jsonBody.toString().toByteArray(Charsets.UTF_8)
                            os.write(input, 0, input.size)
                        }

                        val responseCode = connection.responseCode
                        Log.i(TAG, "API $apiVersion / Model $modelName response code: $responseCode")
                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            val responseStream = BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8))
                            responseStr = responseStream.readText()
                            responseStream.close()
                            Log.i(TAG, "Success with API $apiVersion and model $modelName")
                            break
                        } else {
                            val errorStream = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "No error stream"
                            lastErrorMsg = "API $apiVersion / Model $modelName (HTTP $responseCode): $errorStream"
                            Log.e(TAG, lastErrorMsg)
                        }
                    } catch (e: Exception) {
                        lastErrorMsg = "API $apiVersion / Model $modelName Exception: ${e.message}"
                        Log.e(TAG, lastErrorMsg, e)
                    }
                }
                if (!responseStr.isNullOrBlank()) break
            }

            if (responseStr.isNullOrBlank()) {
                throw RuntimeException("All Gemini API versions and models failed. Last error: $lastErrorMsg")
            }

            val root = JSONObject(responseStr)
            val candidates = root.getJSONArray("candidates")
            val content = candidates.getJSONObject(0).getJSONObject("content")
            val parts = content.getJSONArray("parts")
            var textOutput = parts.getJSONObject(0).getString("text").trim()

            if (textOutput.startsWith("```json")) {
                textOutput = textOutput.removePrefix("```json").removeSuffix("```").trim()
            } else if (textOutput.startsWith("```")) {
                textOutput = textOutput.removePrefix("```").removeSuffix("```").trim()
            }

            val jsonArray = JSONArray(textOutput)
            val slides = mutableListOf<SlideData>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.optJSONObject(i) ?: continue
                val bodyText = obj.optString("body", "Body content")
                val vText = obj.optString("voiceText", bodyText.take(80))
                val bgMood = obj.optString("bgMood", niche)

                val uniqueScene = SceneGenerator.pickUniquePreset(recentPresets, projectSeed + i)
                securePrefs.addRecentPresetId(uniqueScene)

                slides.add(
                    SlideData(
                        templateType = obj.optString("templateType", "story"),
                        headline = obj.optString("headline", "Heading"),
                        body = bodyText,
                        voiceText = vText,
                        voiceStyle = obj.optString("voiceStyle", defaultStyle),
                        bgScene = bgMood.ifBlank { uniqueScene },
                        accentColor = obj.optString("accentColor", "#22D3EE"),
                        seed = projectSeed + i
                    )
                )
            }

            if (slides.isNotEmpty()) {
                slides
            } else {
                throw RuntimeException("Gemini returned empty slides array.")
            }
        }
    }
}
