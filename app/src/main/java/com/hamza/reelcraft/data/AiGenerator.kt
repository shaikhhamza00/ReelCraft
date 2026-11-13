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
                val prompt = """
                    Generate exactly 4 slides for a short video in niche '$niche' about topic '$cleanTopic'.
                    Return ONLY a valid JSON array where each element has keys: 'templateType' (must be one of: 'riddle', 'story', 'quote', 'listicle'), 'headline', 'body'.
                    No markdown, no explanation, pure JSON array.
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

                    // Parse Gemini response
                    val root = JSONObject(responseStr)
                    val candidates = root.getJSONArray("candidates")
                    val content = candidates.getJSONObject(0).getJSONObject("content")
                    val parts = content.getJSONArray("parts")
                    val textOutput = parts.getJSONObject(0).getString("text")

                    // Parse JSON array of slides
                    val jsonArray = JSONArray(textOutput)
                    val slides = mutableListOf<SlideData>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        slides.add(
                            SlideData(
                                templateType = obj.optString("templateType", "story"),
                                headline = obj.optString("headline", "Heading"),
                                body = obj.optString("body", "Body content")
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
        return when (niche.lowercase()) {
            "riddle" -> listOf(
                SlideData("riddle", "99% log yeh riddle solve nahi kar paate!", "Topic: $cleanTopic. Socho aur comment karo!"),
                SlideData("story", "Clue #1", "Iska jawab aapke aas paas hi hai. Dhyan se sochiye."),
                SlideData("quote", "Jawab janne ke liye part 2 dekhein!", "Subscribe for Daily Riddles")
            )
            "facts" -> listOf(
                SlideData("listicle", "Amazing Fact about $cleanTopic", "Kya aapko pata hai yeh hairan kun haqeeqat?"),
                SlideData("story", "Detail #1", "Science aur history ke mutabiq yeh baat 100% sach hai."),
                SlideData("quote", "Aise mazedar facts ke liye follow karein!", "ReelCraft Daily")
            )
            "motivation" -> listOf(
                SlideData("quote", "Success is not final, failure is not fatal.", "Topic: $cleanTopic"),
                SlideData("story", "Himmat mat haro", "Mushkil waqt thodi der ka hota hai, lekin aapki mehnat hamesha rehti hai."),
                SlideData("quote", "Apne sapno ke peechhe bhago!", "Stay Strong")
            )
            else -> listOf(
                SlideData("story", "Story of $cleanTopic", "Ek aisi kahani jo aapko sochne par majboor kar degi."),
                SlideData("listicle", "Key Highlights", "1. Shuruaat\n2. Mod\n3. Anjaam"),
                SlideData("quote", "Like aur share karna na bhulein!", "ReelCraft")
            )
        }
    }
}
