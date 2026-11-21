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
import java.net.SocketTimeoutException
import java.net.URL

data class GenerationResult(
    val slides: List<SlideData>,
    val usedOffline: Boolean,
    val errorMessage: String? = null,
    val modelUsed: String? = null,
    val httpCode: Int? = null,
    val finishReason: String? = null,
    val latencyMs: Long = 0L
)

data class QuickTestResult(
    val modelUsed: String?,
    val httpCode: Int?,
    val finishReason: String?,
    val latencyMs: Long,
    val responseTextOrError: String,
    val flashModelsCount: Int,
    val selectedModelFromApi: String?
)

object AiGenerator {
    private const val TAG = "AiGenerator"

    suspend fun generateSlidesForTopic(
        context: Context,
        niche: String,
        topic: String,
        apiKey: String,
        language: String = "Roman Urdu",
        allowOfflineFallback: Boolean = false
    ): GenerationResult {
        val trimmedKey = apiKey.trim()
        if (trimmedKey.isBlank()) {
            val errMsg = "API key sahi nahi ya missing hai. Settings mein valid Gemini API key enter karein."
            return if (allowOfflineFallback) {
                GenerationResult(
                    slides = getOfflineFallbackSlides(niche, topic),
                    usedOffline = true,
                    errorMessage = errMsg
                )
            } else {
                GenerationResult(
                    slides = emptyList(),
                    usedOffline = false,
                    errorMessage = errMsg
                )
            }
        }

        // Force IPv4 stack to prevent IPv6 connection failures on certain Android emulators/networks
        System.setProperty("java.net.preferIPv4Stack", "true")

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
                You are an expert viral scriptwriter for Instagram Reels and YouTube Shorts writing strictly in language '$language'.
                Create a high-tension, fascinating script about '$cleanTopic' in niche '$niche'.
                Return ONLY a valid JSON array of exactly $targetSlideCount objects. No markdown formatting, no code blocks, pure JSON array only.

                Each object must have these exact keys:
                - "templateType": "story" or "riddle" or "quote" or "listicle"
                - "headline": punchy hook
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

            val primaryModel = ModelResolver.resolveModel(context, trimmedKey)
            // Step 4: LinkedHashSet deduplication & max 3 models
            val modelsToTry = LinkedHashSet(listOf(primaryModel) + ModelResolver.FALLBACK_MODELS).take(3).toList()

            var responseStr: String? = null
            var lastErrorMsg = ""
            var lastModelUsed: String? = null
            var lastHttpCode: Int? = null
            var lastFinishReason: String? = null
            var totalLatencyMs = 0L

            val startTimeGlobal = System.currentTimeMillis()
            val totalLimitMs = 2 * 60 * 1000L // 2 minutes total limit

            var currentMaxOutputTokens = 8192

            modelLoop@ for (modelName in modelsToTry) {
                if (System.currentTimeMillis() - startTimeGlobal > totalLimitMs) {
                    Log.w(TAG, "Total generation time limit (2 min) reached. Stopping model attempts.")
                    break
                }

                lastModelUsed = modelName
                var includeThinking = modelName.lowercase().startsWith("gemini-3")

                var attempt = 0
                val maxAttempts = 3
                var successForModel = false

                while (attempt < maxAttempts && !successForModel) {
                    val callStart = System.currentTimeMillis()
                    try {
                        Log.d(TAG, "Trying model: $modelName (attempt ${attempt + 1}/$maxAttempts)")

                        val jsonBody = JSONObject().apply {
                            put("contents", JSONArray().put(
                                JSONObject().put("parts", JSONArray().put(
                                    JSONObject().put("text", prompt)
                                ))
                            ))
                            put("generationConfig", JSONObject().apply {
                                put("responseMimeType", "application/json")
                                put("temperature", 0.9)
                                put("topP", 0.95)
                                put("maxOutputTokens", currentMaxOutputTokens)
                                if (includeThinking) {
                                    put("thinkingConfig", JSONObject().put("thinkingLevel", "low"))
                                }
                            })
                        }

                        // Step 5: x-goog-api-key header (NO key in URL)
                        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent")
                        val connection = (url.openConnection() as HttpURLConnection).apply {
                            requestMethod = "POST"
                            setRequestProperty("Content-Type", "application/json; charset=utf-8")
                            setRequestProperty("x-goog-api-key", trimmedKey)
                            doOutput = true
                            connectTimeout = 15000
                            readTimeout = 45000
                        }

                        connection.outputStream.use { os ->
                            val input = jsonBody.toString().toByteArray(Charsets.UTF_8)
                            os.write(input, 0, input.size)
                        }

                        val responseCode = connection.responseCode
                        lastHttpCode = responseCode
                        val callLatency = System.currentTimeMillis() - callStart
                        totalLatencyMs += callLatency

                        Log.i(TAG, "Model $modelName attempt ${attempt + 1} response code: $responseCode (${callLatency}ms)")

                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            val responseStream = BufferedReader(InputStreamReader(connection.inputStream))
                            val rawResp = responseStream.readText()
                            responseStream.close()

                            // Check finishReason
                            val root = JSONObject(rawResp)
                            val candidates = root.optJSONArray("candidates")
                            if (candidates != null && candidates.length() > 0) {
                                val cand0 = candidates.getJSONObject(0)
                                val fReason = cand0.optString("finishReason", "STOP")
                                lastFinishReason = fReason

                                if (fReason == "MAX_TOKENS" && currentMaxOutputTokens < 16384) {
                                    Log.w(TAG, "finishReason is MAX_TOKENS. Doubling token budget to 16384 and retrying...")
                                    currentMaxOutputTokens = 16384
                                    attempt++
                                    continue
                                } else if (fReason == "SAFETY") {
                                    lastErrorMsg = "Gemini safety filter ne request block kar di (finishReason: SAFETY)"
                                    Log.e(TAG, lastErrorMsg)
                                    break
                                }
                            }

                            responseStr = rawResp
                            successForModel = true
                            Log.i(TAG, "Successfully generated content using model: $modelName on attempt ${attempt + 1}")
                            break@modelLoop
                        } else {
                            val errorStreamStr = connection.errorStr() ?: "No error body"
                            Log.e(TAG, "Model $modelName failed HTTP $responseCode: $errorStreamStr")

                            if (responseCode == 400) {
                                val lowerError = errorStreamStr.lowercase()
                                // Step 3: Retry if thinking/thinkingConfig error
                                if (includeThinking && (lowerError.contains("thinking") || lowerError.contains("thinkingconfig"))) {
                                    Log.w(TAG, "HTTP 400 related to thinkingConfig. Retrying without thinkingConfig...")
                                    includeThinking = false
                                    attempt++
                                    continue
                                }

                                ModelResolver.removeCachedModel(context, modelName)
                                lastErrorMsg = parseGoogleErrorMessage(responseCode, errorStreamStr)
                                break
                            } else if (responseCode == 401 || responseCode == 403) {
                                ModelResolver.removeCachedModel(context, modelName)
                                lastErrorMsg = "Key unauthorized hai ya Gemini API enable nahi hai (HTTP $responseCode)."
                                break
                            } else if (responseCode == 404) {
                                ModelResolver.removeCachedModel(context, modelName)
                                lastErrorMsg = "Model $modelName nahi mila (HTTP 404)."
                                Log.w(TAG, "Model $modelName returned 404. Blacklisted from cache.")
                                break
                            } else if (responseCode == 503 || responseCode == 429) {
                                val backoffMs = when (attempt) {
                                    0 -> 2000L
                                    1 -> 4000L
                                    else -> 8000L
                                }
                                lastErrorMsg = "Server masroof hai (HTTP $responseCode). Retrying in ${backoffMs}ms..."
                                Log.w(TAG, lastErrorMsg)
                                delay(backoffMs)
                                attempt++
                                continue
                            } else {
                                lastErrorMsg = parseGoogleErrorMessage(responseCode, errorStreamStr)
                                break
                            }
                        }
                    } catch (e: SocketTimeoutException) {
                        lastErrorMsg = "Timeout on model $modelName (attempt ${attempt + 1}): ${e.message}"
                        Log.w(TAG, lastErrorMsg)
                        attempt++
                        if (attempt >= maxAttempts) break
                    } catch (e: Exception) {
                        lastErrorMsg = "Exception on model $modelName: ${e.message}"
                        Log.e(TAG, lastErrorMsg, e)
                        break
                    }
                }
            }

            // Parse response if obtained
            if (!responseStr.isNullOrBlank()) {
                var textOutput = extractValidTextOutput(responseStr!!)
                var jsonArray: JSONArray? = null
                try {
                    jsonArray = JSONArray(textOutput)
                } catch (e: Exception) {
                    // Step 3: Print first 300 characters in Log.e without API key
                    Log.e(TAG, "JSON parse failed. First 300 chars of output: ${textOutput.take(300)}", e)
                    lastErrorMsg = "JSON response format sahi nahi tha (JSON parse failed)"
                }

                if (jsonArray != null) {
                    val parsedSlides = mutableListOf<SlideData>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.optJSONObject(i) ?: continue
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

                        parsedSlides.add(
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

                    if (parsedSlides.isNotEmpty()) {
                        return@withContext GenerationResult(
                            slides = parsedSlides,
                            usedOffline = false,
                            errorMessage = null,
                            modelUsed = lastModelUsed,
                            httpCode = 200,
                            finishReason = lastFinishReason ?: "STOP",
                            latencyMs = totalLatencyMs
                        )
                    }
                }
            }

            // Fallback decision
            Log.w(TAG, "Gemini generation failed. allowOfflineFallback=$allowOfflineFallback, lastErrorMsg=$lastErrorMsg")
            if (allowOfflineFallback) {
                GenerationResult(
                    slides = getOfflineFallbackSlides(niche, topic),
                    usedOffline = true,
                    errorMessage = if (lastErrorMsg.isBlank()) "Gemini fail hua" else lastErrorMsg,
                    modelUsed = lastModelUsed,
                    httpCode = lastHttpCode,
                    finishReason = lastFinishReason,
                    latencyMs = totalLatencyMs
                )
            } else {
                GenerationResult(
                    slides = emptyList(),
                    usedOffline = false,
                    errorMessage = if (lastErrorMsg.isBlank()) "Gemini response error" else lastErrorMsg,
                    modelUsed = lastModelUsed,
                    httpCode = lastHttpCode,
                    finishReason = lastFinishReason,
                    latencyMs = totalLatencyMs
                )
            }
        }
    }

    suspend fun runQuickTest(context: Context, apiKey: String): QuickTestResult {
        val trimmedKey = apiKey.trim()
        if (trimmedKey.isBlank()) {
            return QuickTestResult(
                modelUsed = null,
                httpCode = 400,
                finishReason = null,
                latencyMs = 0L,
                responseTextOrError = "API key is blank",
                flashModelsCount = 0,
                selectedModelFromApi = null
            )
        }

        return withContext(Dispatchers.IO) {
            val listInfo = ModelResolver.fetchModelsFromApi(trimmedKey)
            val modelToUse = listInfo.selectedModel ?: ModelResolver.FALLBACK_MODELS.first()
            val includeThinking = modelToUse.lowercase().startsWith("gemini-3")

            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().put(
                    JSONObject().put("parts", JSONArray().put(
                        JSONObject().put("text", "Respond ONLY with valid JSON: {\"ok\": true}")
                    ))
                ))
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("maxOutputTokens", 100)
                    if (includeThinking) {
                        put("thinkingConfig", JSONObject().put("thinkingLevel", "low"))
                    }
                })
            }

            val startTime = System.currentTimeMillis()
            var httpCode = 0
            var finishReason: String? = null
            var responseTextOrError = ""

            try {
                val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$modelToUse:generateContent")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    setRequestProperty("x-goog-api-key", trimmedKey)
                    doOutput = true
                    connectTimeout = 15000
                    readTimeout = 15000
                }

                connection.outputStream.use { os ->
                    val input = jsonBody.toString().toByteArray(Charsets.UTF_8)
                    os.write(input, 0, input.size)
                }

                httpCode = connection.responseCode
                if (httpCode == HttpURLConnection.HTTP_OK) {
                    val rawResp = connection.inputStream.bufferedReader().use { it.readText() }
                    val root = JSONObject(rawResp)
                    val cand = root.optJSONArray("candidates")?.optJSONObject(0)
                    finishReason = cand?.optString("finishReason", "STOP")
                    responseTextOrError = extractValidTextOutput(rawResp).take(200)
                } else {
                    val errBody = connection.errorStr() ?: "HTTP $httpCode"
                    responseTextOrError = parseGoogleErrorMessage(httpCode, errBody)
                }
            } catch (e: Exception) {
                responseTextOrError = "Error: ${e.message}"
            }

            val latency = System.currentTimeMillis() - startTime
            QuickTestResult(
                modelUsed = modelToUse,
                httpCode = httpCode,
                finishReason = finishReason,
                latencyMs = latency,
                responseTextOrError = responseTextOrError,
                flashModelsCount = listInfo.flashModelsCount,
                selectedModelFromApi = listInfo.selectedModel
            )
        }
    }

    private fun parseGoogleErrorMessage(responseCode: Int, errorStream: String): String {
        if (errorStream.contains("API_KEY_INVALID")) {
            return "API key sahi nahi hai (API_KEY_INVALID)."
        }
        return try {
            val root = JSONObject(errorStream)
            val errObj = root.optJSONObject("error")
            val msg = errObj?.optString("message")
            if (!msg.isNullOrBlank()) {
                "HTTP $responseCode: $msg"
            } else {
                "HTTP $responseCode: $errorStream"
            }
        } catch (_: Exception) {
            "HTTP $responseCode: $errorStream"
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
        val candidates = root.optJSONArray("candidates") ?: return ""
        if (candidates.length() == 0) return ""
        val content = candidates.getJSONObject(0).optJSONObject("content") ?: return ""
        val parts = content.optJSONArray("parts") ?: return ""
        
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

    fun getOfflineFallbackSlides(niche: String, topic: String): List<SlideData> {
        val cleanTopic = if (topic.isBlank()) niche else topic
        val defaultStyle = SlideData.getDefaultVoiceStyle(niche)
        val projectSeed = System.currentTimeMillis()

        val rawSlides = when (cleanTopic.lowercase()) {
            "bhoot wali purani haveli", "horror" -> listOf(
                SlideData("story", "Haveli ka darwaza", "Karachi ke cantonment area mein haveli par 1947 se tala laga tha.", voiceText = "Karachi ki is haveli par 1947 se tala tha.", voiceStyle = "WHISPER_SLOW", accentColor = "#FF4D8D"),
                SlideData("story", "Awaaz", "Raat 1:30 baje andar se piano bajne ki awaaz aane lagi.", voiceText = "Raat dedh baje andar se piano bajne laga.", voiceStyle = "WHISPER_SLOW", accentColor = "#FF4D8D"),
                SlideData("story", "Chaukidar", "Chaukidar ne jhaank kar dekha toh piano khud ba khud chal raha tha.", voiceText = "Chaukidar ne dekha piano khud chal raha tha.", voiceStyle = "WHISPER_SLOW", accentColor = "#FF4D8D"),
                SlideData("story", "Pehchan", "Jaise hi us ne torch maari, piano par kisi ki kati hui ungliyan rakhi theen.", voiceText = "Torch ki roshni mein piano par kati ungliyan theen.", voiceStyle = "SUSPENSE", accentColor = "#FF4D8D"),
                SlideData("story", "Twist", "Aur tab chaukidar ke kandhe par kisi ne thandi ungliyon se touch kiya.", voiceText = "Tab chaukidar ke kandhe par kisi ne touch kiya.", voiceStyle = "WHISPER_SLOW", accentColor = "#FF4D8D"),
                SlideData("story", "Anjaam", "Subah chaukidar ki laash usi piano ke andar milti hui payi gayi.", voiceText = "Subah chaukidar ki laash piano ke andar mili.", voiceStyle = "WHISPER_SLOW", accentColor = "#FF4D8D")
            )
            "rickshaw chalane wala jo doctor bana", "motivation" -> listOf(
                SlideData("story", "Safar", "Lahore ki sard raat mein 45 saal ka Aslam rickshaw chala raha tha.", voiceText = "Lahore ki sard raat mein Aslam rickshaw chala raha tha.", voiceStyle = "ENERGETIC", accentColor = "#F59E0B"),
                SlideData("story", "Kitab", "Sari raat sawari ka intezar karte waqt wo seat ke niche se biology ki kitab nikalta.", voiceText = "Sawari ke intezar mein wo biology ki kitab padhta.", voiceStyle = "CALM_NARRATOR", accentColor = "#F59E0B"),
                SlideData("story", "Mehnat", "Din mein 10 ghante pedling, raat ko hospital ki emergency mein duty aur padhai.", voiceText = "Din mein pedal aur raat ko hospital duty.", voiceStyle = "ENERGETIC", accentColor = "#F59E0B"),
                SlideData("story", "Nateeja", "Saal 2023 mein Aslam ne MBBS ka imtihaan 3.8 GPA se pass kar liya.", voiceText = "Saal 2023 mein Aslam ne MBBS pass kar liya.", voiceStyle = "ENERGETIC", accentColor = "#F59E0B"),
                SlideData("story", "Asli Jeet", "Aaj wo Jinnah hospital ke surgical ward mein junior doctor hai.", voiceText = "Aaj wo Jinnah hospital mein junior doctor hai.", voiceStyle = "ENERGETIC", accentColor = "#F59E0B")
            )
            "aisi cheez jo jitni dhoyo utni gandi hoti hai", "riddle" -> listOf(
                SlideData("riddle", "Paheli", "Aisi kaun si cheez hai jo jitni zyada dhoyi jaye utni hi gandi hoti hai?", voiceText = "Jitni zyada dhoyi jaye utni hi gandi hoti hai.", voiceStyle = "SUSPENSE", accentColor = "#22D3EE"),
                SlideData("riddle", "Hint", "Iska taluq paani aur sabun se hai, lekin yeh kapda nahi hai.", voiceText = "Iska taluq paani aur sabun se hai.", voiceStyle = "SUSPENSE", accentColor = "#22D3EE"),
                SlideData("riddle", "Countdown", "Sochiye, 3 seconds hain aapke paas. 3, 2, 1.", voiceText = "Sochiye, teen seconds hain aapke paas.", voiceStyle = "SUSPENSE", accentColor = "#22D3EE"),
                SlideData("riddle", "Jawab", "Sahi jawab hai... paani! Kyunki paani khud dusri cheezon ko saaf karta hai.", voiceText = "Sahi jawab hai paani, kyunki yeh saaf karta hai.", voiceStyle = "SUSPENSE", accentColor = "#22D3EE"),
                SlideData("riddle", "Fact", "Paani hi dunya ka sab se bara universal solvent hai.", voiceText = "Paani dunya ka sab se bara universal solvent hai.", voiceStyle = "SUSPENSE", accentColor = "#22D3EE")
            )
            "insaan ke dil ke baare mein hairan kar dene wali haqiqat", "facts" -> listOf(
                SlideData("facts", "Dil ki taqat", "Aapka dil ek din mein itni energy paida karta hai jo truck ko 32 kilometer chala de.", voiceText = "Aapka dil ek din mein truck chala sakta hai.", voiceStyle = "SERENE_MALE", accentColor = "#38BDF8"),
                SlideData("facts", "Dhadkan", "Jab aap kisi se pyaar karte hain, toh aap dono ke dil ki dhadkan sync ho jati hai.", voiceText = "Pyaar karne par dil ki dhadkan sync ho jati hai.", voiceStyle = "SERENE_MALE", accentColor = "#38BDF8"),
                SlideData("facts", "Pump", "Zindagi ke 70 saal mein dil 2.5 billion baar dhadakta hai bina ek second ruke.", voiceText = "Zindagi mein dil 2.5 billion baar dhadakta hai.", voiceStyle = "SERENE_MALE", accentColor = "#38BDF8"),
                SlideData("facts", "Cells", "Dil ke cells kabhi divide nahi hote, isliye dil ke cancer ke mamle na-ke-barabar hain.", voiceText = "Dil ke cells kabhi divide nahi hote hain.", voiceStyle = "SERENE_MALE", accentColor = "#38BDF8"),
                SlideData("facts", "Conclusion", "Jism ka yeh chhota sa pump poori kainaat chala raha hai.", voiceText = "Jism ka yeh chhota sa pump kainaat chala raha hai.", voiceStyle = "SERENE_MALE", accentColor = "#38BDF8")
            )
            "barish ki raat ka intezar", "shayari" -> listOf(
                SlideData("quote", "Bheegi raat", "Khidki ke paas baith kar tumhara intezar kar raha hoon.", voiceText = "Khidki ke paas baith kar intezar hai.", voiceStyle = "SOFT_EMOTIONAL", accentColor = "#E879F9"),
                SlideData("quote", "Boonda baandi", "Barish ki har boond par bas tumhara hi naam likha hai.", voiceText = "Barish ki har boond par tumhara naam hai.", voiceStyle = "SOFT_EMOTIONAL", accentColor = "#E879F9"),
                SlideData("quote", "Hawao", "Hawayen bhi keh rahi hain ke tum jaldi laut kar aaoge.", voiceText = "Hawayen keh rahi hain tum laut kar aaoge.", voiceStyle = "SOFT_EMOTIONAL", accentColor = "#E879F9"),
                SlideData("quote", "Aakhri umeed", "Yeh raat lambi sahi, magar subah tumhare sath hogi.", voiceText = "Yeh raat lambi sahi, subah tumhare sath hogi.", voiceStyle = "SOFT_EMOTIONAL", accentColor = "#E879F9")
            )
            "hazrat ali (ra) ka ek waqia", "islamic" -> listOf(
                SlideData("quote", "Insaaf", "Hazrat Ali (RA) ke paas ek yahudi aur ek musalman insaaf ke liye aaye.", voiceText = "Hazrat Ali ke paas insaaf ke liye do log aaye.", voiceStyle = "CALM_NARRATOR", accentColor = "#34D399"),
                SlideData("quote", "Faisla", "Aap (RA) ne bina kisi farq ke sach ka sath diya aur haqdar ko uska haq diya.", voiceText = "Aap ne bina kisi farq ke sach ka sath diya.", voiceStyle = "CALM_NARRATOR", accentColor = "#34D399"),
                SlideData("quote", "Sabaq", "Insaaf sirf apnon ke liye nahi, har insaan ke liye lazmi hai.", voiceText = "Insaaf har ek insaan ke liye lazmi hai.", voiceStyle = "CALM_NARRATOR", accentColor = "#34D399")
            )
            else -> listOf(
                SlideData("story", "Kahani $cleanTopic ki", "Ek aisi gehari aur dilchasp dastan jo aapko shuru se aakhir tak baandh kar rakhegi.", voiceText = "Ek aisi dilchasp dastan.", voiceStyle = defaultStyle, accentColor = "#7C4DFF"),
                SlideData("story", "Mod aur Haqeeqat", "Har kahani ka aik pehlu hota hai jo samne aata hai.", voiceText = "Har kahani ka aik pehlu hota hai.", voiceStyle = defaultStyle, accentColor = "#7C4DFF"),
                SlideData("listicle", "Safar", "Is safar mein chhupe hain bade raaz.", voiceText = "Is safar mein chhupe hain bade raaz.", voiceStyle = defaultStyle, accentColor = "#7C4DFF")
            )
        }

        return rawSlides.mapIndexed { index, slide ->
            val uniqueScene = SceneGenerator.pickUniquePreset(emptyList(), projectSeed + index)
            slide.copy(
                bgScene = uniqueScene,
                seed = projectSeed + index
            )
        }
    }
}
