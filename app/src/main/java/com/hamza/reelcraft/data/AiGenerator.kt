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
        val securePrefs = SecurePreferences(context)
        val recentPresets = securePrefs.getRecentPresetIds()
        val projectSeed = System.currentTimeMillis()

        if (apiKey.isBlank()) {
            return getFallbackSlides(context, niche, topic)
        }

        return withContext(Dispatchers.IO) {
            var attempt = 0
            while (attempt < 2) {
                try {
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
                        Create a high-tension, fascinating 6-slide script about '$cleanTopic' in niche '$niche'.
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

                    val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey")
                    val connection = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        setRequestProperty("Content-Type", "application/json; charset=utf-8")
                        doOutput = true
                        connectTimeout = 15000
                        readTimeout = 15000
                    }

                    val jsonBody = JSONObject().apply {
                        put("contents", JSONArray().put(
                            JSONObject().put("parts", JSONArray().put(
                                JSONObject().put("text", prompt)
                            ))
                        ))
                        put("generationConfig", JSONObject().put("responseMimeType", "application/json").put("temperature", 0.9).put("topP", 0.95))
                    }

                    connection.outputStream.use { os ->
                        val input = jsonBody.toString().toByteArray(Charsets.UTF_8)
                        os.write(input, 0, input.size)
                    }

                    val responseCode = connection.responseCode
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        val responseStream = BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8))
                        val responseStr = responseStream.readText()
                        responseStream.close()

                        val root = JSONObject(responseStr)
                        val candidates = root.getJSONArray("candidates")
                        val content = candidates.getJSONObject(0).getJSONObject("content")
                        val parts = content.getJSONArray("parts")
                        var textOutput = parts.getJSONObject(0).getString("text").trim()

                        // Robustly strip any markdown code blocks if present
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

                        if (validateAndCleanSlides(slides, targetSlideCount)) {
                            return@withContext slides
                        }
                    } else {
                        Log.e(TAG, "Gemini API error code: $responseCode")
                    }
                } catch (_: Exception) {
                    Log.e(TAG, "Failed to call Gemini API on attempt $attempt, retrying")
                }
                attempt++
            }
            getFallbackSlides(context, niche, topic)
        }
    }

    private fun validateAndCleanSlides(slides: List<SlideData>, targetCount: Int): Boolean {
        if (slides.isEmpty()) return false
        if (slides.size !in (targetCount - 1)..(targetCount + 1)) return false

        val firstWords = slides[0].voiceText.split(" ").filter { it.isNotBlank() }
        if (firstWords.size > 15) return false

        val banned = listOf("aaj hum", "dosto", "sabaq", "zid karo", "mehnat karo", "haar mat maano", "kaisa laga", "sab se bada ceo", "is kahani se")
        for (slide in slides) {
            val text = (slide.voiceText + " " + slide.body).lowercase()
            for (b in banned) {
                if (text.contains(b)) return false
            }
        }
        return true
    }

    private fun getFallbackSlides(context: Context, niche: String, topic: String): List<SlideData> {
        val securePrefs = SecurePreferences(context)
        val recentPresets = securePrefs.getRecentPresetIds()
        val projectSeed = System.currentTimeMillis()
        val defaultStyle = SlideData.getDefaultVoiceStyle(niche)

        val rawSlides = when (topic.lowercase()) {
            "bhoot wali purani haveli" -> listOf(
                SlideData("story", "Haveli ka darwaza", "Karachi ke cantonment area mein haveli par 1947 se tala laga tha.", voiceText = "Karachi ki is haveli par 1947 se tala tha.", voiceStyle = "WHISPER_SLOW", accentColor = "#FF4D8D"),
                SlideData("story", "Awaaz", "Raat 1:30 baje andar se piano bajne ki awaaz aane lagi.", voiceText = "Raat dedh baje andar se piano bajne laga.", voiceStyle = "WHISPER_SLOW", accentColor = "#FF4D8D"),
                SlideData("story", "Chaukidar", "Chaukidar ne jhaank kar dekha toh piano khud ba khud chal raha tha.", voiceText = "Chaukidar ne dekha piano khud chal raha tha.", voiceStyle = "WHISPER_SLOW", accentColor = "#FF4D8D"),
                SlideData("story", "Pehchan", "Jaise hi us ne torch maari, piano par kisi ki kati hui ungliyan rakhi theen.", voiceText = "Torch ki roshni mein piano par kati ungliyan theen.", voiceStyle = "SUSPENSE", accentColor = "#FF4D8D"),
                SlideData("story", "Twist", "Aur tab chaukidar ke kandhe par kisi ne thandi ungliyon se touch kiya.", voiceText = "Tab chaukidar ke kandhe par kisi ne touch kiya.", voiceStyle = "WHISPER_SLOW", accentColor = "#FF4D8D"),
                SlideData("story", "Anjaam", "Subah chaukidar ki laash usi piano ke andar milti hui payi gayi.", voiceText = "Subah chaukidar ki laash piano ke andar mili.", voiceStyle = "WHISPER_SLOW", accentColor = "#FF4D8D")
            )
            "rickshaw chalane wala jo doctor bana" -> listOf(
                SlideData("story", "Safar", "Lahore ki sard raat mein 45 saal ka Aslam rickshaw chala raha tha.", voiceText = "Lahore ki sard raat mein Aslam rickshaw chala raha tha.", voiceStyle = "ENERGETIC", accentColor = "#F59E0B"),
                SlideData("story", "Kitab", "Sari raat sawari ka intezar karte waqt wo seat ke niche se biology ki kitab nikalta.", voiceText = "Sawari ke intezar mein wo biology ki kitab padhta.", voiceStyle = "CALM_NARRATOR", accentColor = "#F59E0B"),
                SlideData("story", "Mehnat", "Din mein 10 ghante pedling, raat ko hospital ki emergency mein duty aur padhai.", voiceText = "Din mein pedal aur raat ko hospital duty.", voiceStyle = "ENERGETIC", accentColor = "#F59E0B"),
                SlideData("story", "Nateeja", "Saal 2023 mein Aslam ne MBBS ka imtihaan 3.8 GPA se pass kar liya.", voiceText = "Saal 2023 mein Aslam ne MBBS pass kar liya.", voiceStyle = "ENERGETIC", accentColor = "#F59E0B"),
                SlideData("story", "Asli Jeet", "Aaj wo Jinnah hospital ke surgical ward mein junior doctor hai.", voiceText = "Aaj wo Jinnah hospital mein junior doctor hai.", voiceStyle = "ENERGETIC", accentColor = "#F59E0B")
            )
            "aisi cheez jo jitni dhoyo utni gandi hoti hai" -> listOf(
                SlideData("riddle", "Paheli", "Aisi kaun si cheez hai jo jitni zyada dhoyi jaye utni hi gandi hoti hai?", voiceText = "Jitni zyada dhoyi jaye utni hi gandi hoti hai.", voiceStyle = "SUSPENSE", accentColor = "#22D3EE"),
                SlideData("riddle", "Hint", "Iska taluq paani aur sabun se hai, lekin yeh kapda nahi hai.", voiceText = "Iska taluq paani aur sabun se hai.", voiceStyle = "SUSPENSE", accentColor = "#22D3EE"),
                SlideData("riddle", "Countdown", "Sochiye, 3 seconds hain aapke paas. 3, 2, 1.", voiceText = "Sochiye, teen seconds hain aapke paas.", voiceStyle = "SUSPENSE", accentColor = "#22D3EE"),
                SlideData("riddle", "Jawab", "Sahi jawab hai... paani! Kyunki paani khud dusri cheezon ko saaf karta hai.", voiceText = "Sahi jawab hai paani, kyunki yeh saaf karta hai.", voiceStyle = "SUSPENSE", accentColor = "#22D3EE"),
                SlideData("riddle", "Fact", "Paani hi dunya ka sab se bara universal solvent hai.", voiceText = "Paani dunya ka sab se bara universal solvent hai.", voiceStyle = "SUSPENSE", accentColor = "#22D3EE")
            )
            "insaan ke dil ke baare mein hairan kar dene wali haqiqat" -> listOf(
                SlideData("facts", "Dil ki taqat", "Aapka dil ek din mein itni energy paida karta hai jo truck ko 32 kilometer chala de.", voiceText = "Aapka dil ek din mein truck chala sakta hai.", voiceStyle = "SERENE_MALE", accentColor = "#38BDF8"),
                SlideData("facts", "Dhadkan", "Jab aap kisi se pyaar karte hain, toh aap dono ke dil ki dhadkan sync ho jati hai.", voiceText = "Pyaar karne par dil ki dhadkan sync ho jati hai.", voiceStyle = "SERENE_MALE", accentColor = "#38BDF8"),
                SlideData("facts", "Pump", "Zindagi ke 70 saal mein dil 2.5 billion baar dhadakta hai bina ek second ruke.", voiceText = "Zindagi mein dil 2.5 billion baar dhadakta hai.", voiceStyle = "SERENE_MALE", accentColor = "#38BDF8"),
                SlideData("facts", "Cells", "Dil ke cells kabhi divide nahi hote, isliye dil ke cancer ke mamle na-ke-barabar hain.", voiceText = "Dil ke cells kabhi divide nahi hote hain.", voiceStyle = "SERENE_MALE", accentColor = "#38BDF8"),
                SlideData("facts", "Conclusion", "Jism ka yeh chhota sa pump poori kainaat chala raha hai.", voiceText = "Jism ka yeh chhota sa pump kainaat chala raha hai.", voiceStyle = "SERENE_MALE", accentColor = "#38BDF8")
            )
            "barish ki raat ka intezar" -> listOf(
                SlideData("quote", "Bheegi raat", "Khidki ke paas baith kar tumhara intezar kar raha hoon.", voiceText = "Khidki ke paas baith kar intezar hai.", voiceStyle = "SOFT_EMOTIONAL", accentColor = "#E879F9"),
                SlideData("quote", "Boonda baandi", "Barish ki har boond par bas tumhara hi naam likha hai.", voiceText = "Barish ki har boond par tumhara naam hai.", voiceStyle = "SOFT_EMOTIONAL", accentColor = "#E879F9"),
                SlideData("quote", "Hawao", "Hawayen bhi keh rahi hain ke tum jaldi laut kar aaoge.", voiceText = "Hawayen keh rahi hain tum laut kar aaoge.", voiceStyle = "SOFT_EMOTIONAL", accentColor = "#E879F9"),
                SlideData("quote", "Aakhri umeed", "Yeh raat lambi sahi, magar subah tumhare sath hogi.", voiceText = "Yeh raat lambi sahi, subah tumhare sath hogi.", voiceStyle = "SOFT_EMOTIONAL", accentColor = "#E879F9")
            )
            "hazrat ali (ra) ka ek waqia" -> listOf(
                SlideData("quote", "Insaaf", "Hazrat Ali (RA) ke paas ek yahudi aur ek musalman insaaf ke liye aaye.", voiceText = "Hazrat Ali ke paas insaaf ke liye do log aaye.", voiceStyle = "CALM_NARRATOR", accentColor = "#34D399"),
                SlideData("quote", "Faisla", "Aap (RA) ne bina kisi farq ke sach ka sath diya aur haqdar ko uska haq diya.", voiceText = "Aap ne bina kisi farq ke sach ka sath diya.", voiceStyle = "CALM_NARRATOR", accentColor = "#34D399"),
                SlideData("quote", "Sabaq", "Insaaf sirf apnon ke liye nahi, har insaan ke liye lazmi hai.", voiceText = "Insaaf har ek insaan ke liye lazmi hai.", voiceStyle = "CALM_NARRATOR", accentColor = "#34D399")
            )
            else -> listOf(
                SlideData("story", "Ghaflat", "Waqt kisi ke liye nahi rukta. Jab tak aap so rahe hain, koi aur jeet raha hai.", voiceText = "Waqt kisi ke liye nahi rukta.", voiceStyle = defaultStyle, accentColor = "#7C4DFF"),
                SlideData("story", "Faisla", "Aaj ka ek chhota qadam aapka kal badal sakta hai.", voiceText = "Aaj ka ek chhota qadam kal badal dega.", voiceStyle = defaultStyle, accentColor = "#7C4DFF"),
                SlideData("quote", "Utho", "Der mat karo, abhi shuru karo.", voiceText = "Der mat karo, abhi shuru karo.", voiceStyle = "ENERGETIC", accentColor = "#7C4DFF")
            )
        }

        return rawSlides.mapIndexed { index, slide ->
            val uniqueScene = SceneGenerator.pickUniquePreset(recentPresets, projectSeed + index)
            securePrefs.addRecentPresetId(uniqueScene)
            slide.copy(
                bgScene = uniqueScene,
                seed = projectSeed + index
            )
        }
    }
}
