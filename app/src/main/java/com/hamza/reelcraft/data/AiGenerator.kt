package com.hamza.reelcraft.data

import android.util.Log
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

    suspend fun generateSlidesForTopic(niche: String, topic: String, apiKey: String): List<SlideData> {
        if (apiKey.isBlank()) {
            return getFallbackSlides(niche, topic)
        }

        return withContext(Dispatchers.IO) {
            try {
                val cleanTopic = if (topic.isBlank()) niche else topic
                val defaultStyle = SlideData.getDefaultVoiceStyle(niche)
                val prompt = """
                    You are an expert viral scriptwriter for Instagram Reels and YouTube Shorts.
                    Create a highly engaging, fascinating, and deeply informative 6-slide script about the topic '$cleanTopic' in niche '$niche'.
                    For each slide, provide:
                    - 'templateType': one of 'story', 'riddle', 'quote', 'listicle'
                    - 'headline': A punchy, gripping headline or hook in Urdu / Roman Urdu / English.
                    - 'body': Rich, fascinating, and detailed factual information or story related to the topic.
                    - 'voiceText': Short voiceover text (8-15 words) for TTS speech synthesis.
                    - 'voiceStyle': One of 'WHISPER_SLOW', 'ENERGETIC', 'CALM_NARRATOR', 'SUSPENSE', 'SOFT_EMOTIONAL', 'SERENE_MALE'.
                    - 'bgScene': One of 'NIGHT_SKY', 'OCEAN', 'FIRE_EMBERS'.
                    - 'accentColor': A vibrant hex color code (e.g. '#22D3EE', '#FF4D8D', '#FACC15').
                    Return ONLY a valid JSON array of 6 objects with keys: 'templateType', 'headline', 'body', 'voiceText', 'voiceStyle', 'bgScene', 'accentColor'. No markdown, pure JSON array.
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
                    put("generationConfig", JSONObject().put("responseMimeType", "application/json"))
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
                    val textOutput = parts.getJSONObject(0).getString("text")

                    val jsonArray = JSONArray(textOutput)
                    val slides = mutableListOf<SlideData>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val bodyText = obj.optString("body", "Body content")
                        val vText = obj.optString("voiceText", bodyText.take(80))
                        slides.add(
                            SlideData(
                                templateType = obj.optString("templateType", "story"),
                                headline = obj.optString("headline", "Heading"),
                                body = bodyText,
                                voiceText = vText,
                                voiceStyle = obj.optString("voiceStyle", defaultStyle),
                                bgScene = obj.optString("bgScene", "NIGHT_SKY"),
                                accentColor = obj.optString("accentColor", "#22D3EE")
                            )
                        )
                    }
                    if (slides.isNotEmpty()) return@withContext slides
                } else {
                    Log.e(TAG, "Gemini API error code: $responseCode")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to call Gemini API, falling back", e)
            }
            getFallbackSlides(niche, topic)
        }
    }

    private fun getFallbackSlides(niche: String, topic: String): List<SlideData> {
        val cleanTopic = if (topic.isBlank()) niche else topic
        val defaultStyle = SlideData.getDefaultVoiceStyle(niche)
        return when (niche.lowercase()) {
            "riddle" -> listOf(
                SlideData("riddle", "99% log yeh riddle solve nahi kar paate!", "Aisi kaun si cheez hai jo samandar mein paida hoti hai aur ghar mein aate hi mar jati hai? Sochiye!", voiceText = "Aisi kaun si cheez hai jo samandar mein paida hoti hai?", voiceStyle = defaultStyle, bgScene = "OCEAN", accentColor = "#22D3EE"),
                SlideData("story", "Sochne ka waqt khatam!", "Iska jawab chhipa hai hamari rozmarra ki zindagi mein. Kya aap guess kar paye?", voiceText = "Iska jawab chhipa hai hamari rozmarra ki zindagi mein.", voiceStyle = defaultStyle),
                SlideData("listicle", "Hints jo aapko madad karenge:", "1. Yeh namak se judi hai\n2. Pani mein ghul jati hai", voiceText = "Yeh namak se judi hai aur pani mein ghul jati hai.", voiceStyle = defaultStyle),
                SlideData("quote", "Sahi Jawab:", "Baraf ya Namak! Kaisa laga yeh challenging puzzle?", voiceText = "Sahi jawab hai baraf ya namak!", voiceStyle = defaultStyle),
                SlideData("story", "Dimaag ki kasrat jari rakhein", "Aise hi mazedar aur dimaghi riddles ke liye hamare sath jude rahein.", voiceText = "Aise hi mazedar riddles ke liye jude rahein.", voiceStyle = defaultStyle),
                SlideData("quote", "Part 2 ke liye follow karein!", "ReelCraft Brain Teasers", voiceText = "Part 2 ke liye follow karein.", voiceStyle = defaultStyle)
            )
            "facts" -> listOf(
                SlideData("listicle", "Hairan kun Haqeeqat about $cleanTopic", "Kya aap jante hain ke duniya mein kuch aisi baatein hain jo science ko bhi hairan kar deti hain?", voiceText = "Kya aap jante hain yeh hairan kun haqeeqat?", voiceStyle = defaultStyle, bgScene = "NIGHT_SKY", accentColor = "#FACC15"),
                SlideData("story", "Pehla Raaz", "Scientific research ke mutabiq yeh phenomenon sadiyon se chala aa raha hai.", voiceText = "Yeh phenomenon sadiyon se chala aa raha hai.", voiceStyle = defaultStyle),
                SlideData("listicle", "Key Discovery Points:", "1. Unexpected origin\n2. Hidden scientific value", voiceText = "Inke peeche chhupe hain bade raaz.", voiceStyle = defaultStyle),
                SlideData("story", "Gheri Haqeeqat", "Jab scientists ne iski gehrai mein jaakar dekha, to pata chala.", voiceText = "Scientists ne iski gehrai mein jaakar dekha.", voiceStyle = defaultStyle),
                SlideData("quote", "Knowledge is Power", "Aisi hi informative aur deep facts ke liye rozana hamari videos dekhein.", voiceText = "Aisi hi informative facts ke liye videos dekhein.", voiceStyle = defaultStyle),
                SlideData("quote", "Share with your friends!", "ReelCraft Knowledge", voiceText = " apne doston ke sath share karein.", voiceStyle = defaultStyle)
            )
            "motivation" -> listOf(
                SlideData("quote", "Aapki zindagi tab badalti hai jab aap bahane banana chhor dete hain.", "Topic: $cleanTopic", voiceText = "Zindagi tab badalti hai jab bahane chhor dete hain.", voiceStyle = defaultStyle, bgScene = "FIRE_EMBERS", accentColor = "#FF4D8D"),
                SlideData("story", "Asli Safar", "Kamyabi raaton raat nahi milti. Iske peeche saalon ki khamosh mehnat hoti hai.", voiceText = "Kamyabi raaton raat nahi milti.", voiceStyle = defaultStyle),
                SlideData("listicle", "3 Golden Rules of Success:", "1. Consistency\n2. Resilience\n3. Focus", voiceText = "Success ke teen golden rules yaad rakhein.", voiceStyle = defaultStyle),
                SlideData("story", "Khud par yaqeen rakhein", "Jab puri duniya aap par shak kare, tab aapka khud par bharosa bada hathiyar banta hai.", voiceText = "Khud par bharosa sabse bada hathiyar hai.", voiceStyle = defaultStyle),
                SlideData("quote", "Aap jo chahte hain wo pa sakte hain.", "Bas haar mat maaniye!", voiceText = "Bas haar mat maaniye!", voiceStyle = defaultStyle),
                SlideData("quote", "Stay Unstoppable!", "ReelCraft Motivation", voiceText = "Stay unstoppable with ReelCraft.", voiceStyle = defaultStyle)
            )
            else -> listOf(
                SlideData("story", "Kahani $cleanTopic ki", "Ek aisi gehari aur dilchasp dastan jo aapko shuru se aakhir tak baandh kar rakhegi.", voiceText = "Ek aisi dilchasp dastan.", voiceStyle = defaultStyle, bgScene = "OCEAN", accentColor = "#7C4DFF"),
                SlideData("story", "Mod aur Haqeeqat", "Har kahani ka aik pehlu hota hai jo samne aata hai.", voiceText = "Har kahani ka aik pehlu hota hai.", voiceStyle = defaultStyle),
                SlideData("listicle", "Yeh Khel kahan tak gaya?", "1. Shuruaat\n2. Mod\n3. Anjaam", voiceText = "Yeh safar kahan tak gaya?", voiceStyle = defaultStyle),
                SlideData("story", "Sabaq", "Is safar se humein yehi sabaq milta hai ke waqt sab kuch badal deta hai.", voiceText = "Waqt sab kuch badal deta hai.", voiceStyle = defaultStyle),
                SlideData("quote", "Aisi hi aur kahaniyan sunne ke liye", "Jude rahiye hamare sath.", voiceText = "Aisi hi aur kahaniyan sunne ke liye jude rahein.", voiceStyle = defaultStyle),
                SlideData("quote", "Part 2 Coming Soon", "ReelCraft Stories", voiceText = "Part 2 coming soon.", voiceStyle = defaultStyle)
            )
        }
    }
}
