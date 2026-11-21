package com.hamza.reelcraft.data

import android.content.Context
import android.util.Log
import com.hamza.reelcraft.model.SceneGenerator
import com.hamza.reelcraft.model.SlideData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
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
    val latencyMs: Long = 0L,
    val pass1Ms: Long = 0L,
    val pass2Ms: Long = 0L,
    val pass2Used: Boolean = false,
    val validationWarning: String? = null
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

data class ModelProbeItem(
    val modelName: String,
    val httpCode: Int,
    val latencyMs: Long,
    val statusOrError: String,
    val isRecommended: Boolean = false
)

data class ModelProbeResult(
    val items: List<ModelProbeItem>,
    val recommendedModel: String?
)

object AiGenerator {
    private const val TAG = "AiGenerator"

    val BANNED_PHRASES = listOf(
        "aaj hum",
        "dosto",
        "is kahani se",
        "sabaq",
        "zid karo",
        "mehnat karo",
        "haar mat maano",
        "kaisa laga"
    )

    fun detectNicheForTopic(topic: String): String {
        val lower = topic.lowercase()
        return when {
            lower.contains("bhoot") || lower.contains("haveli") || lower.contains("horror") || lower.contains("chudail") -> "Horror"
            lower.contains("doctor") || lower.contains("rickshaw") || lower.contains("motivation") || lower.contains("kamyabi") -> "Motivation"
            lower.contains("paheli") || lower.contains("riddle") || lower.contains("dhoyo") -> "Riddle"
            lower.contains("dil") || lower.contains("haqiqat") || lower.contains("fact") || lower.contains("insaan") -> "Facts"
            lower.contains("barish") || lower.contains("shayari") || lower.contains("intezar") -> "Shayari"
            lower.contains("ali") || lower.contains("islamic") || lower.contains("waqia") -> "Islamic"
            else -> "Story"
        }
    }

    fun checkForBannedPhrases(text: String): String? {
        val lower = text.lowercase()
        for (banned in BANNED_PHRASES) {
            if (lower.contains(banned)) return banned
        }
        return null
    }

    suspend fun generateSlidesForTopic(
        context: Context,
        nicheInput: String,
        topic: String,
        apiKey: String,
        language: String = "Roman Urdu",
        allowOfflineFallback: Boolean = false,
        onProgress: (String) -> Unit = {}
    ): GenerationResult {
        val trimmedKey = apiKey.trim()
        if (trimmedKey.isBlank()) {
            val errMsg = "API key sahi nahi ya missing hai. Settings mein valid Gemini API key enter karein."
            return if (allowOfflineFallback) {
                GenerationResult(
                    slides = getOfflineFallbackSlides(nicheInput, topic),
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

        System.setProperty("java.net.preferIPv4Stack", "true")

        val securePrefs = SecurePreferences(context)
        val recentPresets = securePrefs.getRecentPresetIds()
        val projectSeed = System.currentTimeMillis()

        return withContext(Dispatchers.IO) {
            val cleanTopic = if (topic.isBlank()) nicheInput else topic
            val niche = if (nicheInput.isBlank() || nicheInput.lowercase() == "general") detectNicheForTopic(cleanTopic) else nicheInput
            val defaultStyle = SlideData.getDefaultVoiceStyle(niche)

            val targetSlideCount = when (niche.lowercase()) {
                "riddle", "quiz", "facts" -> 5
                "motivation" -> 5
                "shayari", "quotes" -> 4
                "horror", "story" -> 6
                else -> 5
            }

            // Step 1: System Instruction
            val systemInstructionText = """
                You are a master viral short-video scriptwriter for Instagram Reels and YouTube Shorts. You write 20-35 second voiceover scripts in the user's chosen language (Roman Urdu = everyday spoken Urdu in Latin script, like friends talk). Your script must hook in 3 seconds and keep the viewer to the end.

                STRUCTURE: 1) HOOK (slide 1, under 15 words, no intro: a strange, scary, shocking or question-based line). 2) SETUP (1-2 slides, with one specific detail: name, number, time, place, sound, smell). 3) ESCALATION (2-3 slides, each more tense or surprising than the last, each ending on an open question or turn). 4) TWIST/PAYOFF (1 slide, unexpected and memorable). 5) LOOP/CLIFFHANGER (last slide ties back to the hook or invites Part 2).

                STYLE: voiceText per slide 8-15 words, short sentences, spoken language, never bookish. Every slide has one concrete detail. SHOW, don't tell.

                BANNED: 'aaj hum', 'dosto', 'is kahani se sabaq', 'zid karo', 'mehnat karo', 'haar mat maano', 'kaisa laga', moral lectures, empty filler ('ajeeb', 'dilchasp', 'zabardast'), vague lines like 'ek din kuch hua' or 'sab se bada CEO'. Never repeat or copy the examples: every script needs a new character, time, place and twist.

                NICHE TONE: Horror: slow, whispering, specific dread, brutal twist. Story: emotional, with a turn. Motivation: begin with a true-sounding specific story (name, city, number, cost); the lesson must emerge from the outcome, never be stated. Riddle/Quiz: a genuinely solvable riddle with ONE correct answer where every word fits it; hook question, a hint, 3-2-1 suspense, answer, one surprising fact about the answer. Facts: start with the shocking number or truth, no 'did you know'. Shayari: soft, short couplet-like lines, emotional. Islamic: respectful, calm; only well-known, authentic stories; if unsure of a fact or hadith, do not write it.

                SLIDE COUNT by niche: Riddle/Quiz/Facts 5, Motivation 5-6, Shayari/Quotes 4-5, Horror/Story 6-7.

                SELF-CHECK before answering: (a) hook < 15 words and no intro? (b) each slide has one specific detail? (c) any moral/filler/banned phrase? (d) slide count right? (e) twist unexpected? If any check fails, rewrite silently. Think of 3 hooks internally and use the strongest. Output ONLY valid JSON.
            """.trimIndent()

            // Step 2: Few-Shot Examples inside User Prompt
            val userPrompt = """
                Create a viral short video script in language '$language' for topic '$cleanTopic' in niche '$niche'.
                Target exact slide count: $targetSlideCount objects in JSON array.

                STYLE REFERENCE ONLY (do not reuse these examples or topics):
                Example 1 - Horror:
                Slide 1: "Hostel ke kamray 14 mein aaj tak koi teen raat se zyada nahi ruka."
                Slide 2: "Warden ne chabi dete hue bas ek shart rakhi: raat ko aaina dhak dena."
                Slide 3: "Maine hans kar baat taal di. Pehli raat aaina khula hi rehne diya."
                Slide 4: "Raat do baje aaine mein meri parchhai mujh se pehle uth baithi."
                Slide 5: "Subah warden ne kamra khola. Bistar khali tha, aur aaine mein main so raha tha."

                Example 2 - Riddle:
                Slide 1: "Mere paas shehar hain lekin ghar nahi. Jungle hain lekin ped nahi. Main kya hoon?"
                Slide 2: "Hint: tum mujhe roz apne phone mein kholte ho."
                Slide 3: "Teen... do... ek..."
                Slide 4: "Jawab: naqsha! Map."
                Slide 5: "Sab se purane naqshe mitti ki takhtiyon par kheenche gaye the."

                Now write the NEW script for topic '$cleanTopic' in language '$language' as a pure JSON array.
            """.trimIndent()

            // Step 3: Response Schema JSON
            val responseSchemaJson = JSONObject().apply {
                put("type", "ARRAY")
                put("items", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject().apply {
                        put("templateType", JSONObject().put("type", "STRING"))
                        put("headline", JSONObject().put("type", "STRING"))
                        put("body", JSONObject().put("type", "STRING"))
                        put("voiceText", JSONObject().put("type", "STRING"))
                        put("voiceStyle", JSONObject().put("type", "STRING"))
                        put("emphasisWords", JSONObject().apply {
                            put("type", "ARRAY")
                            put("items", JSONObject().put("type", "STRING"))
                        })
                        put("bgMood", JSONObject().put("type", "STRING"))
                        put("bgScene", JSONObject().put("type", "STRING"))
                        put("accentColor", JSONObject().put("type", "STRING"))
                        put("transition", JSONObject().put("type", "STRING"))
                    })
                    put("required", JSONArray().apply {
                        put("templateType")
                        put("headline")
                        put("body")
                        put("voiceText")
                        put("voiceStyle")
                        put("emphasisWords")
                        put("bgMood")
                        put("bgScene")
                        put("accentColor")
                        put("transition")
                    })
                })
            }

            val candidates = ModelResolver.getCandidateModels(context, trimmedKey)

            var lastModelUsed: String? = null
            var lastHttpCode: Int? = null
            var lastFinishReason: String? = null
            val modelErrors = mutableListOf<String>()

            val startTimeGlobal = System.currentTimeMillis()
            val totalLimitMs = 90 * 1000L

            for ((index, modelName) in candidates.withIndex()) {
                if (System.currentTimeMillis() - startTimeGlobal > totalLimitMs) {
                    Log.w(TAG, "Total generation time limit (90s) reached.")
                    modelErrors.add("Time limit (90s) exceeded")
                    break
                }

                lastModelUsed = modelName
                val includeThinking = modelName.lowercase().startsWith("gemini-3")

                onProgress("Pass 1: Model $modelName try ho raha hai (${index + 1}/${candidates.size})...")

                val pass1Start = System.currentTimeMillis()
                val pass1Resp = executeContentCall(
                    modelName = modelName,
                    apiKey = trimmedKey,
                    systemInstruction = systemInstructionText,
                    userText = userPrompt,
                    responseSchema = responseSchemaJson,
                    includeThinking = includeThinking,
                    temperature = 1.0,
                    topP = 0.95,
                    maxTokens = 8192
                )

                val pass1Ms = System.currentTimeMillis() - pass1Start
                lastHttpCode = pass1Resp.httpCode
                lastFinishReason = pass1Resp.finishReason

                if (pass1Resp.httpCode == 200 && !pass1Resp.textOutput.isNullOrBlank()) {
                    var pass1Slides = parseSlidesFromJson(
                        rawJson = pass1Resp.textOutput,
                        niche = niche,
                        defaultStyle = defaultStyle,
                        recentPresets = recentPresets,
                        projectSeed = projectSeed,
                        securePrefs = securePrefs
                    )

                    if (pass1Slides.isNotEmpty()) {
                        ModelResolver.saveWorkingModel(context, modelName)
                        Log.i(TAG, "Pass 1 successful with model $modelName (${pass1Ms}ms). Proceeding to Pass 2 refinement...")

                        // Step 4: Pass 2 Refinement Call
                        onProgress("Pass 2: Refining script for peak viral tension...")
                        var pass2Slides: List<SlideData>? = null
                        var pass2Ms = 0L

                        val pass2Start = System.currentTimeMillis()
                        val pass2Prompt = """
                            Is script ko Reels viewer ki nazar se parho. Kaunsi slides thandi, aam ya lambi hain? Unhein tez, khaas aur tension wali banao. Hook sab se mazboot banao. Banned phrases ('aaj hum', 'dosto', 'sabaq', 'zid karo', 'haar mat maano') bilkul hata do. Wahi JSON schema, sirf final JSON array.

                            Pass 1 Script JSON:
                            ${pass1Resp.textOutput}
                        """.trimIndent()

                        try {
                            val pass2Resp = withTimeoutOrNull(25000L) {
                                executeContentCall(
                                    modelName = modelName,
                                    apiKey = trimmedKey,
                                    systemInstruction = systemInstructionText,
                                    userText = pass2Prompt,
                                    responseSchema = responseSchemaJson,
                                    includeThinking = includeThinking,
                                    temperature = 1.0,
                                    topP = 0.95,
                                    maxTokens = 8192
                                )
                            }
                            pass2Ms = System.currentTimeMillis() - pass2Start

                            if (pass2Resp != null && pass2Resp.httpCode == 200 && !pass2Resp.textOutput.isNullOrBlank()) {
                                val parsedPass2 = parseSlidesFromJson(
                                    rawJson = pass2Resp.textOutput,
                                    niche = niche,
                                    defaultStyle = defaultStyle,
                                    recentPresets = recentPresets,
                                    projectSeed = projectSeed,
                                    securePrefs = securePrefs
                                )
                                if (parsedPass2.isNotEmpty()) {
                                    pass2Slides = parsedPass2
                                    Log.i(TAG, "Pass 2 refinement successful (${pass2Ms}ms)!")
                                }
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Pass 2 refinement failed or timed out (${e.message}). Falling back to Pass 1 slides.")
                        }

                        val finalSlides = pass2Slides ?: pass1Slides
                        val isPass2Used = (pass2Slides != null)

                        // Step 5: Validation Check
                        var validationIssue = validateSlides(finalSlides)
                        var validatedSlides = finalSlides

                        if (validationIssue != null) {
                            Log.w(TAG, "Script failed validation: $validationIssue. Attempting 1 retry...")
                            val retryPrompt = "$userPrompt\n\nCRITICAL FIX REQUIRED: $validationIssue. Please fix and return pure JSON array."
                            val retryResp = executeContentCall(
                                modelName = modelName,
                                apiKey = trimmedKey,
                                systemInstruction = systemInstructionText,
                                userText = retryPrompt,
                                responseSchema = responseSchemaJson,
                                includeThinking = includeThinking,
                                temperature = 1.0,
                                topP = 0.95,
                                maxTokens = 8192
                            )
                            if (retryResp.httpCode == 200 && !retryResp.textOutput.isNullOrBlank()) {
                                val retriedSlides = parseSlidesFromJson(
                                    rawJson = retryResp.textOutput,
                                    niche = niche,
                                    defaultStyle = defaultStyle,
                                    recentPresets = recentPresets,
                                    projectSeed = projectSeed,
                                    securePrefs = securePrefs
                                )
                                if (retriedSlides.isNotEmpty()) {
                                    validatedSlides = retriedSlides
                                    validationIssue = validateSlides(validatedSlides)
                                }
                            }
                        }

                        val valWarning = if (validationIssue != null) "script ki quality kam ho sakti hai" else null

                        return@withContext GenerationResult(
                            slides = validatedSlides,
                            usedOffline = false,
                            errorMessage = valWarning,
                            modelUsed = modelName,
                            httpCode = 200,
                            finishReason = lastFinishReason ?: "STOP",
                            latencyMs = pass1Ms + pass2Ms,
                            pass1Ms = pass1Ms,
                            pass2Ms = pass2Ms,
                            pass2Used = isPass2Used,
                            validationWarning = valWarning
                        )
                    }
                } else if (pass1Resp.httpCode == 401 || (pass1Resp.errorBody?.contains("API_KEY_INVALID") == true)) {
                    val fatalErr = "API key sahi nahi hai (API_KEY_INVALID / 401)"
                    return@withContext GenerationResult(
                        slides = emptyList(),
                        usedOffline = false,
                        errorMessage = fatalErr,
                        modelUsed = modelName,
                        httpCode = pass1Resp.httpCode
                    )
                } else {
                    if (pass1Resp.httpCode == 403 || pass1Resp.httpCode == 404) {
                        ModelResolver.blacklistModel(context, modelName, 24 * 60 * 60 * 1000L)
                    } else if (pass1Resp.httpCode == 503 || pass1Resp.httpCode == 429) {
                        ModelResolver.blacklistModel(context, modelName, 15 * 60 * 1000L)
                    }
                    modelErrors.add("$modelName: HTTP ${pass1Resp.httpCode} ${pass1Resp.errorBody ?: ""}")
                }
            }

            val combinedErrorMsg = if (modelErrors.isNotEmpty()) {
                "Gemini models fail hue:\n" + modelErrors.distinct().joinToString("\n• ", prefix = "• ")
            } else {
                "Gemini se script nahi aa saki"
            }

            if (allowOfflineFallback) {
                GenerationResult(
                    slides = getOfflineFallbackSlides(niche, topic),
                    usedOffline = true,
                    errorMessage = combinedErrorMsg,
                    modelUsed = lastModelUsed,
                    httpCode = lastHttpCode,
                    finishReason = lastFinishReason
                )
            } else {
                GenerationResult(
                    slides = emptyList(),
                    usedOffline = false,
                    errorMessage = combinedErrorMsg,
                    modelUsed = lastModelUsed,
                    httpCode = lastHttpCode,
                    finishReason = lastFinishReason
                )
            }
        }
    }

    private fun validateSlides(slides: List<SlideData>): String? {
        if (slides.isEmpty()) return "Slides empty hain"
        
        // Slide 1 hook check
        val hookWords = slides[0].voiceText.split(" ").filter { it.isNotBlank() }.size
        if (hookWords > 15) return "Slide 1 Hook voiceText too long ($hookWords words > 15)"

        // Banned phrases check across all slides
        for ((idx, slide) in slides.withIndex()) {
            val fullText = "${slide.headline} ${slide.body} ${slide.voiceText}"
            val foundBanned = checkForBannedPhrases(fullText)
            if (foundBanned != null) {
                return "Slide ${idx + 1} contains banned phrase '$foundBanned'"
            }
        }
        return null
    }

    private data class CallResponse(
        val httpCode: Int,
        val textOutput: String?,
        val finishReason: String?,
        val errorBody: String?
    )

    private fun executeContentCall(
        modelName: String,
        apiKey: String,
        systemInstruction: String,
        userText: String,
        responseSchema: JSONObject,
        includeThinking: Boolean,
        temperature: Double,
        topP: Double,
        maxTokens: Int
    ): CallResponse {
        try {
            val jsonBody = JSONObject().apply {
                // Step 1: systemInstruction in generateContent body
                put("systemInstruction", JSONObject().put("parts", JSONArray().put(
                    JSONObject().put("text", systemInstruction)
                )))
                put("contents", JSONArray().put(
                    JSONObject().put("parts", JSONArray().put(
                        JSONObject().put("text", userText)
                    ))
                ))
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("responseSchema", responseSchema)
                    put("temperature", temperature)
                    put("topP", topP)
                    put("maxOutputTokens", maxTokens)
                    if (includeThinking) {
                        put("thinkingConfig", JSONObject().put("thinkingLevel", "low"))
                    }
                })
            }

            Log.i(TAG, "Request to $modelName (systemInstruction length: ${systemInstruction.length})")

            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("x-goog-api-key", apiKey)
                doOutput = true
                connectTimeout = 10000
                readTimeout = 30000
            }

            connection.outputStream.use { os ->
                val input = jsonBody.toString().toByteArray(Charsets.UTF_8)
                os.write(input, 0, input.size)
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val rawResp = connection.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(rawResp)
                val cand = root.optJSONArray("candidates")?.optJSONObject(0)
                val finishReason = cand?.optString("finishReason", "STOP")
                val textOut = extractValidTextOutput(rawResp)
                return CallResponse(200, textOut, finishReason, null)
            } else {
                val errBody = connection.errorStr() ?: "HTTP $responseCode"
                return CallResponse(responseCode, null, null, errBody)
            }
        } catch (e: Exception) {
            return CallResponse(0, null, null, e.message)
        }
    }

    private fun parseSlidesFromJson(
        rawJson: String,
        niche: String,
        defaultStyle: String,
        recentPresets: List<String>,
        projectSeed: Long,
        securePrefs: SecurePreferences
    ): List<SlideData> {
        return try {
            val jsonArray = JSONArray(rawJson)
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

                // Step 3: Parse emphasisWords and transition
                val empArray = obj.optJSONArray("emphasisWords")
                val empList = mutableListOf<String>()
                if (empArray != null) {
                    for (k in 0 until empArray.length()) {
                        val w = empArray.optString(k, "")
                        if (w.isNotBlank()) empList.add(w)
                    }
                } else {
                    val empStr = obj.optString("emphasisWords", "")
                    if (empStr.isNotBlank()) {
                        empList.addAll(empStr.split(",").map { it.trim() })
                    }
                }

                val transitionStr = obj.optString("transition", "FADE").uppercase()

                parsedSlides.add(
                    SlideData(
                        templateType = obj.optString("templateType", "story"),
                        headline = obj.optString("headline", "Heading"),
                        body = bodyText,
                        emphasisWords = empList,
                        bgScene = assignedScene,
                        accentColor = obj.optString("accentColor", "#22D3EE"),
                        transition = transitionStr,
                        voiceText = vText,
                        voiceStyle = obj.optString("voiceStyle", defaultStyle),
                        seed = projectSeed + i
                    )
                )
            }
            parsedSlides
        } catch (e: Exception) {
            Log.e(TAG, "JSON parse failed for slides: ${rawJson.take(300)}", e)
            emptyList()
        }
    }

    suspend fun runModelProbe(context: Context, apiKey: String): ModelProbeResult {
        val trimmedKey = apiKey.trim()
        if (trimmedKey.isBlank()) {
            return ModelProbeResult(emptyList(), null)
        }

        return withContext(Dispatchers.IO) {
            val candidateModels = ModelResolver.getCandidateModels(context, trimmedKey)
            val items = mutableListOf<ModelProbeItem>()
            var recommendedModel: String? = null

            for (modelName in candidateModels) {
                val includeThinking = modelName.lowercase().startsWith("gemini-3")
                val jsonBody = JSONObject().apply {
                    put("contents", JSONArray().put(
                        JSONObject().put("parts", JSONArray().put(
                            JSONObject().put("text", "Respond ONLY with valid JSON: {\"ok\":true}")
                        ))
                    ))
                    put("generationConfig", JSONObject().apply {
                        put("responseMimeType", "application/json")
                        put("maxOutputTokens", 64)
                        if (includeThinking) {
                            put("thinkingConfig", JSONObject().put("thinkingLevel", "low"))
                        }
                    })
                }

                val startTime = System.currentTimeMillis()
                var httpCode = 0
                var statusOrError = ""

                try {
                    val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent")
                    val connection = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        setRequestProperty("Content-Type", "application/json; charset=utf-8")
                        setRequestProperty("x-goog-api-key", trimmedKey)
                        doOutput = true
                        connectTimeout = 10000
                        readTimeout = 15000
                    }

                    connection.outputStream.use { os ->
                        val input = jsonBody.toString().toByteArray(Charsets.UTF_8)
                        os.write(input, 0, input.size)
                    }

                    httpCode = connection.responseCode
                    val latency = System.currentTimeMillis() - startTime

                    if (httpCode == HttpURLConnection.HTTP_OK) {
                        val rawResp = connection.inputStream.bufferedReader().use { it.readText() }
                        statusOrError = "200 OK: " + extractValidTextOutput(rawResp).take(60)
                        val isFirstRec = (recommendedModel == null)
                        if (isFirstRec) {
                            recommendedModel = modelName
                            ModelResolver.saveWorkingModel(context, modelName)
                        }
                        items.add(ModelProbeItem(modelName, httpCode, latency, statusOrError, isFirstRec))
                    } else {
                        val errBody = connection.errorStr() ?: "HTTP $httpCode"
                        statusOrError = parseGoogleErrorMessage(httpCode, errBody)
                        if (httpCode == 403 || httpCode == 404) {
                            ModelResolver.blacklistModel(context, modelName, 24 * 60 * 60 * 1000L)
                        } else if (httpCode == 503 || httpCode == 429) {
                            ModelResolver.blacklistModel(context, modelName, 15 * 60 * 1000L)
                        }
                        items.add(ModelProbeItem(modelName, httpCode, latency, statusOrError, false))
                    }
                } catch (e: SocketTimeoutException) {
                    val latency = System.currentTimeMillis() - startTime
                    statusOrError = "Timeout (>15s)"
                    ModelResolver.blacklistModel(context, modelName, 15 * 60 * 1000L)
                    items.add(ModelProbeItem(modelName, 408, latency, statusOrError, false))
                } catch (e: Exception) {
                    val latency = System.currentTimeMillis() - startTime
                    statusOrError = "Error: ${e.message}"
                    items.add(ModelProbeItem(modelName, 0, latency, statusOrError, false))
                }
            }

            ModelProbeResult(items, recommendedModel)
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
            val candidates = ModelResolver.getCandidateModels(context, trimmedKey)
            val modelToUse = candidates.firstOrNull() ?: ModelResolver.FALLBACK_MODELS.first()
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
                    connectTimeout = 10000
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
                    ModelResolver.saveWorkingModel(context, modelToUse)
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
        if (errorStream.contains("API_KEY_INVALID") || errorStream.contains("API key not valid")) {
            return "API key sahi nahi hai (API_KEY_INVALID)."
        }
        return try {
            val root = JSONObject(errorStream)
            val errObj = root.optJSONObject("error")
            val msg = errObj?.optString("message")
            val status = errObj?.optString("status")
            if (!msg.isNullOrBlank()) {
                if (!status.isNullOrBlank()) "HTTP $responseCode [$status]: $msg" else "HTTP $responseCode: $msg"
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
                SlideData("story", "Haveli ka darwaza", "Karachi ke cantonment area mein haveli par 1947 se tala laga tha.", voiceText = "Karachi ki is haveli par 1947 se tala tha.", voiceStyle = "WHISPER_SLOW", accentColor = "#FF4D8D", transition = "GLITCH"),
                SlideData("story", "Awaaz", "Raat 1:30 baje andar se piano bajne ki awaaz aane lagi.", voiceText = "Raat dedh baje andar se piano bajne laga.", voiceStyle = "WHISPER_SLOW", accentColor = "#FF4D8D", transition = "FADE"),
                SlideData("story", "Chaukidar", "Chaukidar ne jhaank kar dekha toh piano khud ba khud chal raha tha.", voiceText = "Chaukidar ne dekha piano khud chal raha tha.", voiceStyle = "WHISPER_SLOW", accentColor = "#FF4D8D", transition = "SLIDE_LEFT"),
                SlideData("story", "Pehchan", "Jaise hi us ne torch maari, piano par kisi ki kati hui ungliyan rakhi theen.", voiceText = "Torch ki roshni mein piano par kati ungliyan theen.", voiceStyle = "SUSPENSE", accentColor = "#FF4D8D", transition = "ZOOM_IN"),
                SlideData("story", "Twist", "Aur tab chaukidar ke kandhe par kisi ne thandi ungliyon se touch kiya.", voiceText = "Tab chaukidar ke kandhe par kisi ne touch kiya.", voiceStyle = "WHISPER_SLOW", accentColor = "#FF4D8D", transition = "GLITCH"),
                SlideData("story", "Anjaam", "Subah chaukidar ki laash usi piano ke andar milti hui payi gayi.", voiceText = "Subah chaukidar ki laash piano ke andar mili.", voiceStyle = "WHISPER_SLOW", accentColor = "#FF4D8D", transition = "FADE")
            )
            "rickshaw chalane wala jo doctor bana", "motivation" -> listOf(
                SlideData("story", "Safar", "Lahore ki sard raat mein 45 saal ka Aslam rickshaw chala raha tha.", voiceText = "Lahore ki sard raat mein Aslam rickshaw chala raha tha.", voiceStyle = "ENERGETIC", accentColor = "#F59E0B", transition = "SLIDE_LEFT"),
                SlideData("story", "Kitab", "Sari raat sawari ka intezar karte waqt wo seat ke niche se biology ki kitab nikalta.", voiceText = "Sawari ke intezar mein wo biology ki kitab padhta.", voiceStyle = "CALM_NARRATOR", accentColor = "#F59E0B", transition = "FADE"),
                SlideData("story", "Mehnat", "Din mein 10 ghante pedling, raat ko hospital ki emergency mein duty aur padhai.", voiceText = "Din mein pedal aur raat ko hospital duty.", voiceStyle = "ENERGETIC", accentColor = "#F59E0B", transition = "ZOOM_IN"),
                SlideData("story", "Nateeja", "Saal 2023 mein Aslam ne MBBS ka imtihaan 3.8 GPA se pass kar liya.", voiceText = "Saal 2023 mein Aslam ne MBBS pass kar liya.", voiceStyle = "ENERGETIC", accentColor = "#F59E0B", transition = "FLIP"),
                SlideData("story", "Asli Jeet", "Aaj wo Jinnah hospital ke surgical ward mein junior doctor hai.", voiceText = "Aaj wo Jinnah hospital mein junior doctor hai.", voiceStyle = "ENERGETIC", accentColor = "#F59E0B", transition = "FADE")
            )
            "aisi cheez jo jitni dhoyo utni gandi hoti hai", "riddle" -> listOf(
                SlideData("riddle", "Paheli", "Aisi kaun si cheez hai jo jitni zyada dhoyi jaye utni hi gandi hoti hai?", voiceText = "Jitni zyada dhoyi jaye utni hi gandi hoti hai.", voiceStyle = "SUSPENSE", accentColor = "#22D3EE", transition = "SLIDE_LEFT"),
                SlideData("riddle", "Hint", "Iska taluq paani aur sabun se hai, lekin yeh kapda nahi hai.", voiceText = "Iska taluq paani aur sabun se hai.", voiceStyle = "SUSPENSE", accentColor = "#22D3EE", transition = "FADE"),
                SlideData("riddle", "Countdown", "Sochiye, 3 seconds hain aapke paas. 3, 2, 1.", voiceText = "Sochiye, teen seconds hain aapke paas.", voiceStyle = "SUSPENSE", accentColor = "#22D3EE", transition = "ZOOM_IN"),
                SlideData("riddle", "Jawab", "Sahi jawab hai... paani! Kyunki paani khud dusri cheezon ko saaf karta hai.", voiceText = "Sahi jawab hai paani, kyunki yeh saaf karta hai.", voiceStyle = "SUSPENSE", accentColor = "#22D3EE", transition = "FLIP"),
                SlideData("riddle", "Fact", "Paani hi dunya ka sab se bara universal solvent hai.", voiceText = "Paani dunya ka sab se bara universal solvent hai.", voiceStyle = "SUSPENSE", accentColor = "#22D3EE", transition = "FADE")
            )
            "insaan ke dil ke baare mein hairan kar dene wali haqiqat", "facts" -> listOf(
                SlideData("facts", "Dil ki taqat", "Aapka dil ek din mein itni energy paida karta hai jo truck ko 32 kilometer chala de.", voiceText = "Aapka dil ek din mein truck chala sakta hai.", voiceStyle = "SERENE_MALE", accentColor = "#38BDF8", transition = "SLIDE_LEFT"),
                SlideData("facts", "Dhadkan", "Jab aap kisi se pyaar karte hain, toh aap dono ke dil ki dhadkan sync ho jati hai.", voiceText = "Pyaar karne par dil ki dhadkan sync ho jati hai.", voiceStyle = "SERENE_MALE", accentColor = "#38BDF8", transition = "FADE"),
                SlideData("facts", "Pump", "Zindagi ke 70 saal mein dil 2.5 billion baar dhadakta hai bina ek second ruke.", voiceText = "Zindagi mein dil 2.5 billion baar dhadakta hai.", voiceStyle = "SERENE_MALE", accentColor = "#38BDF8", transition = "ZOOM_IN"),
                SlideData("facts", "Cells", "Dil ke cells kabhi divide nahi hote, isliye dil ke cancer ke mamle na-ke-barabar hain.", voiceText = "Dil ke cells kabhi divide nahi hote hain.", voiceStyle = "SERENE_MALE", accentColor = "#38BDF8", transition = "FLIP"),
                SlideData("facts", "Conclusion", "Jism ka yeh chhota sa pump poori kainaat chala raha hai.", voiceText = "Jism ka yeh chhota sa pump kainaat chala raha hai.", voiceStyle = "SERENE_MALE", accentColor = "#38BDF8", transition = "FADE")
            )
            "barish ki raat ka intezar", "shayari" -> listOf(
                SlideData("quote", "Bheegi raat", "Khidki ke paas baith kar tumhara intezar kar raha hoon.", voiceText = "Khidki ke paas baith kar intezar hai.", voiceStyle = "SOFT_EMOTIONAL", accentColor = "#E879F9", transition = "FADE"),
                SlideData("quote", "Boonda baandi", "Barish ki har boond par bas tumhara hi naam likha hai.", voiceText = "Barish ki har boond par tumhara naam hai.", voiceStyle = "SOFT_EMOTIONAL", accentColor = "#E879F9", transition = "SLIDE_LEFT"),
                SlideData("quote", "Hawao", "Hawayen bhi keh rahi hain ke tum jaldi laut kar aaoge.", voiceText = "Hawayen keh rahi hain tum laut kar aaoge.", voiceStyle = "SOFT_EMOTIONAL", accentColor = "#E879F9", transition = "FADE"),
                SlideData("quote", "Aakhri umeed", "Yeh raat lambi sahi, magar subah tumhare sath hogi.", voiceText = "Yeh raat lambi sahi, subah tumhare sath hogi.", voiceStyle = "SOFT_EMOTIONAL", accentColor = "#E879F9", transition = "FADE")
            )
            "hazrat ali (ra) ka ek waqia", "islamic" -> listOf(
                SlideData("quote", "Insaaf", "Hazrat Ali (RA) ke paas ek yahudi aur ek musalman insaaf ke liye aaye.", voiceText = "Hazrat Ali ke paas insaaf ke liye do log aaye.", voiceStyle = "CALM_NARRATOR", accentColor = "#34D399", transition = "FADE"),
                SlideData("quote", "Faisla", "Aap (RA) ne bina kisi farq ke sach ka sath diya aur haqdar ko uska haq diya.", voiceText = "Aap ne bina kisi farq ke sach ka sath diya.", voiceStyle = "CALM_NARRATOR", accentColor = "#34D399", transition = "SLIDE_LEFT"),
                SlideData("quote", "Sabaq", "Insaaf sirf apnon ke liye nahi, har insaan ke liye lazmi hai.", voiceText = "Insaaf har ek insaan ke liye lazmi hai.", voiceStyle = "CALM_NARRATOR", accentColor = "#34D399", transition = "FADE")
            )
            else -> listOf(
                SlideData("story", "Kahani $cleanTopic ki", "Ek aisi gehari aur dilchasp dastan jo aapko shuru se aakhir tak baandh kar rakhegi.", voiceText = "Ek aisi dilchasp dastan.", voiceStyle = defaultStyle, accentColor = "#7C4DFF", transition = "FADE"),
                SlideData("story", "Mod aur Haqeeqat", "Har kahani ka aik pehlu hota hai jo samne aata hai.", voiceText = "Har kahani ka aik pehlu hota hai.", voiceStyle = defaultStyle, accentColor = "#7C4DFF", transition = "SLIDE_LEFT"),
                SlideData("listicle", "Safar", "Is safar mein chhupe hain bade raaz.", voiceText = "Is safar mein chhupe hain bade raaz.", voiceStyle = defaultStyle, accentColor = "#7C4DFF", transition = "FADE")
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
