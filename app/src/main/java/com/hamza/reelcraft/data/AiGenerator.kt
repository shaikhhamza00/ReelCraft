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
                        else -> 6 // Horror/Story 6-7
                    }

                    val prompt = """
                        Tum ek mahir short-video scriptwriter ho jo Urdu/Roman Urdu/Hindi mein viral Reels likhta hai. Tumhara kaam 25-40 second ki voiceover script likhna hai jo viewer ko pehle 3 second mein pakar le aur aakhir tak rokay rakhe. Topic: '$cleanTopic' in niche '$niche'.

                        SLIDE COUNT RULE: Is niche ke liye exact $targetSlideCount slides honi chahiyein.

                        STRUCTURE:
                        1. HOOK (slide 1): 3 second se kam. Seedha ajeeb, khofnaak, hairan karne wali ya sawaliya baat. Taaruf nahi. 'Aaj hum...' ya 'Dosto...' se shuru karna mana hai.
                        2. SETUP (1-2 slides): maahol aur kirdar, ek khaas tafseel ke saath (waqt, jagah, awaaz, chiz).
                        3. ESCALATION (2-3 slides): har slide pichli se zyada tension ya hairat. Har slide ka aakhir ek adhoora sawal ya moor ho.
                        4. TWIST/PAYOFF (1 slide): ghair mutawaqqe, yaad rehne wali baat.
                        5. LOOP/CLIFFHANGER (aakhri slide): aisa khatma jo pehli slide se jur jaye ya Part 2 ki khwahish chhor de.

                        STYLE KE QAIDE & BANNED LIST:
                        - Misaal ko copy ya dohrana sakht mana hai. Har dafa naya kirdar, naya waqt, naya twist, naya manzar.
                        - Har slide ka voiceText 8-15 lafz. Chhote jumlay. Bolchal ki zubaan, kitabi nahi. Jaise dost ko raaz bata rahe ho.
                        - Khaas tafseel use karo: asli lagne wala naam, shehar, adad, kharcha, tareekh, waqt, awaaz, bu, ehsaas. 'Ek din kuch hua' jaisa aam jumla mana hai.
                        - BANNED PHRASES & CONCEPTS: 'aaj hum', 'dosto', 'sabaq', 'zid karo', 'mehnat karo', 'haar mat maano', 'kaisa laga', 'sab se bada CEO', moral lecture, nasihat, cliche filler.
                        - SHOW, DON'T TELL: Sabaq ya nateeja sirf kahani ke natije se khud nazar aaye, batao mat.
                        - Riddle/Quiz rule: Hook sawal, ek hint, suspense/countdown, jawab, aur ek hairan karne wali tafseel jawab ke baare mein. Paheli aisi ho jo sach mein solve ho (ek hi sahi jawab).

                        SELF-CHECK LIST (Model ko jawab dene se pehle run karni hai):
                        (a) Kya hook 15 lafz se kam aur bina taaruf ke hai?
                        (b) Kya har slide mein ek khaas tafseel (naam, adad, waqt, shehar) hai?
                        (c) Kya koi moral, nasihat ya banned jumla ('zid karo', 'sabaq', 'dosto') to nahi hai?
                        (d) Kya slide count $targetSlideCount hai?
                        (e) Kya twist ghair mutawaqqe hai?
                        (Agar koi sawal fail ho, toh script dobara likho).

                        DO-PASS GENERATION: Pehle draft likho, phir self-critique karke mazeed tezz aur tension wali banao.

                        FEW-SHOT EXAMPLES (Style learning only, do not copy verbatim):
                        Example 1 (Horror):
                        [{"templateType":"story","headline":"Ghar number 42","body":"Lahore ke Gulshan block mein ghar number 42 par tala pichle 10 saal se laga tha.","voiceText":"Lahore ke ghar number 42 par 10 saal se tala tha.","voiceStyle":"WHISPER_SLOW","bgMood":"horror","bgScene":"NIGHT_SKY","emphasisWords":["lahore","10 saal"],"accentColor":"#FF4D8D"},
                        {"templateType":"story","headline":"Awaaz","body":"Raat ke 2:15 par andar se ghanti bajne ki awaaz aayi.","voiceText":"Raat 2:15 par andar se ghanti ki awaaz aayi.","voiceStyle":"WHISPER_SLOW","bgMood":"horror","bgScene":"NIGHT_SKY","emphasisWords":["raat 2:15","ghanti"],"accentColor":"#FF4D8D"}]

                        Example 2 (Riddle):
                        [{"templateType":"riddle","headline":"Ajeeb cheez","body":"Aisi kaun si cheez hai jo zameen par chale toh pani peeti hai, aur agar pani pi le toh mar jati hai?","voiceText":"Zameen par chale toh pani peeti hai, pani peete hi mar jati hai.","voiceStyle":"SUSPENSE","bgMood":"riddle","bgScene":"CYBER_GRID","emphasisWords":["pani peeti hai","mar jati hai"],"accentColor":"#22D3EE"}]

                        Return ONLY a valid JSON array of exactly $targetSlideCount objects with keys: 'templateType', 'headline', 'body', 'voiceText', 'voiceStyle', 'bgMood', 'bgScene', 'emphasisWords', 'accentColor'. No markdown, pure JSON array.
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
                        put("generationConfig", JSONObject().put("responseMimeType", "application/json").put("temperature", 1.2).put("topP", 0.95))
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

                        // Step 4 Validation check (banned phrases, word counts, slide count)
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

        for (slide in slides) {
            val vWords = slide.voiceText.split(" ").filter { it.isNotBlank() }
            if (vWords.size !in 8..15) return false
        }

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
                SlideData("story", "Nateeja", "Saal 2023 mein Aslam ne MBBS ka imtihaan 3.8 GPA se pass kar liya.",voiceText = "Saal 2023 mein Aslam ne MBBS pass kar liya.", voiceStyle = "ENERGETIC", accentColor = "#F59E0B"),
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
                SlideData("story", "Ghaflat", "Waqt kisi ke liye nahi rukta. Jab tak aap so rahe hain, koi aur jeet raha hai.", voiceText = "Waqt kisi ke liye nahi rukta.", voiceStyle = "CALM_NARRATOR", accentColor = "#7C4DFF"),
                SlideData("story", "Faisla", "Aaj ka ek chhota qadam aapka kal badal sakta hai.", voiceText = "Aaj ka ek chhota qadam kal badal dega.", voiceStyle = "CALM_NARRATOR", accentColor = "#7C4DFF"),
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
