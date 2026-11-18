package com.hamza.reelcraft.data

import android.content.Context
import android.util.Log
import com.hamza.reelcraft.model.SceneGenerator
import com.hamza.reelcraft.model.SlideData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
            throw IllegalArgumentException("API key sahi nahi ya missing hai. Settings mein valid Gemini API key enter karein.")
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

            val primaryModel = ModelResolver.resolveModel(context, apiKey)
            val modelsToTry = listOf(primaryModel) + ModelResolver.FALLBACK_MODELS

            var responseStr: String? = null
            var lastErrorMsg = ""

            for (modelName in modelsToTry) {
                var attempt = 0
                var successForModel = false

                while (attempt < 2 && !successForModel) {
                    try {
                        Log.d(TAG, "Trying model: $modelName (attempt ${attempt + 1})")
                        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey")
                        val connection = (url.openConnection() as HttpURLConnection).apply {
                            requestMethod = "POST"
                            setRequestProperty("Content-Type", "application/json; charset=utf-8")
                            doOutput = true
                            connectTimeout = 15000
                            readTimeout = 60000 // 60s read timeout
                        }

                        connection.outputStream.use { os ->
                            val input = jsonBody.toString().toByteArray(Charsets.UTF_8)
                            os.write(input, 0, input.size)
                        }

                        val responseCode = connection.responseCode
                        Log.i(TAG, "Model $modelName response code: $responseCode")

                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            val responseStream = BufferedReader(InputStreamReader(connection.inputStream))
                            responseStr = responseStream.readText()
                            responseStream.close()
                            successForModel = true
                            break
                        } else {
                            val errorStream = connection.errorStr() ?: "No error stream"
                            lastErrorMsg = "Model $modelName failed with HTTP $responseCode: $errorStream"
                            Log.e(TAG, lastErrorMsg)

                            // Error Handling (Step 2)
                            if (responseCode == 400) {
                                throw IllegalArgumentException("API key sahi nahi hai (HTTP 400 Bad Request).")
                            }
                            if (responseCode == 401 || responseCode == 403) {
                                throw SecurityException("API key unauthorized hai ya key mein Gemini API enable nahi hai (HTTP $responseCode).")
                            }
                            if (responseCode == 429) {
                                Log.w(TAG, "Rate limit (429) hit for model $modelName. Retrying in 2 seconds...")
                                delay(2000)
                                attempt++
                                continue
                            }
                            // 404, 503, or other server errors move to next model / retry
                            if (responseCode == 404 || responseCode == 503 || responseCode >= 500) {
                                break // try next model
                            } else {
                                throw RuntimeException(lastErrorMsg)
                            }
                        }
                    } catch (e: Exception) {
                        if (e is IllegalArgumentException || e is SecurityException) throw e
                        lastErrorMsg = "Exception on model $modelName: ${e.message}"
                        Log.e(TAG, lastErrorMsg, e)
                        // On timeout/socket exception, try next model
                        break
                    }
                }
                if (!responseStr.isNullOrBlank()) break
            }

            if (responseStr.isNullOrBlank()) {
                throw RuntimeException("Tamam Gemini models fail ho gaye hain. Asal wajah: $lastErrorMsg")
            }

            var textOutput = extractValidTextOutput(responseStr)

            var jsonArray: JSONArray? = null
            var parseAttempt = 0
            while (parseAttempt < 2 && jsonArray == null) {
                try {
                    jsonArray = JSONArray(textOutput)
                } catch (e: Exception) {
                    parseAttempt++
                    if (parseAttempt >= 2) {
                        throw RuntimeException("Invalid JSON response from Gemini: $textOutput", e)
                    }
                    textOutput = extractValidTextOutput(responseStr)
                }
            }

            val slides = mutableListOf<SlideData>()
            for (i in 0 until (jsonArray?.length() ?: 0)) {
                val obj = jsonArray!!.optJSONObject(i) ?: continue
                val bodyText = obj.optString("body", "Body content")
                val vText = obj.optString("voiceText", bodyText.take(80))
                val bgMood = obj.optString("bgMood", niche)

                val geminiScene = obj.optString("bgScene", "")
                val validPresets = SceneGenerator.presetNames
                val assignedScene = if (validPresets.contains(geminiScene.uppercase())) {
                    geminiScene.uppercase()
                } else if (bgMood.isNotBlank()) {
                    bgMood
                } else {
                    SceneGenerator.pickUniquePreset(recentPresets, projectSeed + i)
                }
                securePrefs.addRecentPresetId(assignedScene)

                slides.add(
                    SlideData(
                        templateType = obj.optString("templateType", "story"),
                        headline = obj.optString("headline", "Heading"),
                        body = bodyText,
                        voiceText = vText,
                        voiceStyle = obj.optString("voiceStyle", defaultStyle),
                        bgScene = assignedScene,
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

    private fun HttpURLConnection.errorStr(): String? {
        return try {
            errorStream?.bufferedReader()?.use { it.readText() }
        } catch (_: Exception) {
            null
        }
    }

    private fun extractValidTextOutput(responseStr: String): String {
        val root = JSONObject(responseStr)
        val candidates = root.getJSONArray("candidates")
        val content = candidates.getJSONObject(0).getJSONObject("content")
        val parts = content.getJSONArray("parts")
        
        val sb = StringBuilder()
        for (i in 0 until parts.length()) {
            val part = parts.getJSONObject(i)
            val isThought = part.optBoolean("thought", false)
            if (!isThought && part.has("text")) {
                sb.append(part.getString("text"))
            }
        }

        var textOutput = sb.toString().trim()
        if (textOutput.isEmpty() && parts.length() > 0) {
            textOutput = parts.getJSONObject(0).optString("text", "").trim()
        }

        if (textOutput.startsWith("```json")) {
            textOutput = textOutput.removePrefix("```json").removeSuffix("```").trim()
        } else if (textOutput.startsWith("```")) {
            textOutput = textOutput.removePrefix("```").removeSuffix("```").trim()
        }

        return textOutput
    }
}
