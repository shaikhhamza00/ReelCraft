package com.hamza.reelcraft.data

import android.content.Context
import android.util.Log
import com.hamza.reelcraft.model.LayerConfig
import com.hamza.reelcraft.model.LayerType
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
            try {
                val cleanTopic = if (topic.isBlank()) niche else topic
                val defaultStyle = SlideData.getDefaultVoiceStyle(niche)
                val prompt = """
                    Tum ek mahir short-video scriptwriter ho jo Urdu/Roman Urdu/Hindi mein viral Reels likhta hai. Tumhara kaam 25-40 second ki voiceover script likhna hai jo viewer ko pehle 3 second mein pakar le aur aakhir tak rokay rakhe. Topic: '$cleanTopic' in niche '$niche'.

                    STRUCTURE (6-8 slides):
                    1. HOOK (slide 1): 3 second se kam. Seedha ajeeb, khofnaak, hairan karne wali ya sawaliya baat. Taaruf nahi. 'Aaj hum...' ya 'Dosto...' se shuru karna mana hai.
                    2. SETUP (1-2 slides): maahol aur kirdar, ek khaas tafseel ke saath (waqt, jagah, awaaz, chiz).
                    3. ESCALATION (2-3 slides): har slide pichli se zyada tension ya hairat. Har slide ka aakhir ek adhoora sawal ya moor ho.
                    4. TWIST/PAYOFF (1 slide): ghair mutawaqqe, yaad rehne wali baat.
                    5. LOOP/CLIFFHANGER (aakhri slide): aisa khatma jo pehli slide se jur jaye ya Part 2 ki khwahish chhor de.

                    STYLE KE QAIDE:
                    - Har slide ka voiceText 8-15 lafz. Chhote jumlay. Bolchal ki zubaan, kitabi nahi. Jaise dost ko raaz bata rahe ho.
                    - Khaas tafseel use karo: naam, adad, waqt, jagah, awaaz, bu, ehsaas. 'Ek din kuch hua' jaisa aam jumla mana hai.
                    - Mana hai: 'aaj hum dekhenge', 'dosto', 'is kahani se humein yeh sabaq milta hai', moral lecture, cliche filler, hashiya, aur 'ajeeb', 'dilchasp' jaise khokhle lafz.
                    - Zubaan: jo language user ne chuni, usi mein. Roman Urdu ho to roz marra bolchal wali (jaise 'main ne', 'wo', 'phir', 'achanak'), ghalat ya ajeeb hijje nahi.
                    - Har slide ka ek 'emphasisWords' (1-2 sab se ahem lafz as an array of strings).

                    NICHE KE HISAB SE TONE:
                    - Horror: dheema, sarsarata, tafseeli dehshat. Anjaam mein zabardast twist.
                    - Story: jazbati, mod aur takrao wala.
                    - Motivation: sachchi kahani se shuru (naam, shehar, mushkil), phir 1 dhamaakedaar sabaq jo shuru mein hook ban chuka ho.
                    - Riddle/Quiz: ajeeb sawal, 3 second ki khamoshi ka suspense, phir jawab jo hairat mein daale.
                    - Facts: 'tumhe pata hai...' ki bajaye seedha hairan kar dene wala adad ya haqiqat.
                    - Shayari: naram, jazbati, 2-2 misron ke tukron mein.
                    - Islamic: adab aur sukoon ke saath, sahih hawalay, ghair mustanad baat nahi.

                    FEW-SHOT EXAMPLES (Style learning only, do not copy verbatim):
                    Example 1 (Horror):
                    [{"templateType":"story","headline":"Raat 3 baje","body":"Raat 3 baje ammi ne mujhe awaaz di. Masla yeh tha... ammi ko guzre 2 saal ho chuke the.","voiceText":"Raat 3 baje ammi ne mujhe awaaz di.","voiceStyle":"WHISPER_SLOW","bgMood":"horror","bgScene":"NIGHT_SKY","emphasisWords":["raat 3 baje","ammi"],"accentColor":"#FF4D8D"},
                    {"templateType":"story","headline":"Khamoshi","body":"Main ne razai munh par kheench li. Awaaz phir aayi... bilkul darwaze ke peechay se.","voiceText":"Awaaz phir aayi darwaze ke peechay se.","voiceStyle":"WHISPER_SLOW","bgMood":"horror","bgScene":"NIGHT_SKY","emphasisWords":["razai","peechay"],"accentColor":"#FF4D8D"}]

                    Example 2 (Riddle):
                    [{"templateType":"riddle","headline":"Dunya ki aisi cheez","body":"Dunya ki aisi kaun si cheez hai jo paida hote hi udne lagti hai? Bina paron ke.","voiceText":"Paida hote hi udne lagti hai bina paron ke.","voiceStyle":"SUSPENSE","bgMood":"riddle","bgScene":"CYBER_GRID","emphasisWords":["udne lagti hai"],"accentColor":"#22D3EE"},
                    {"templateType":"riddle","headline":"Sochiye","body":"99% log pehli koshish mein haar maan lete hain. Waqt khatam ho raha hai.","voiceText":"99% log haar maan lete hain.","voiceStyle":"SUSPENSE","bgMood":"riddle","bgScene":"CYBER_GRID","emphasisWords":["99% log"],"accentColor":"#22D3EE"}]

                    AKHRI CHEEZ: Pehle andar hi andar 3 alag hook sochho, sab se mazboot wala chuno, aur sirf wohi likho.
                    Return ONLY a valid JSON array of 6 to 8 objects with keys: 'templateType', 'headline', 'body', 'voiceText', 'voiceStyle', 'bgMood', 'bgScene', 'emphasisWords', 'accentColor'. No markdown, pure JSON array.
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
                    put("generationConfig", JSONObject().put("responseMimeType", "application/json").put("temperature", 1.1))
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
                    if (slides.isNotEmpty()) return@withContext slides
                } else {
                    Log.e(TAG, "Gemini API error code: $responseCode")
                }
            } catch (_: Exception) {
                Log.e(TAG, "Failed to call Gemini API, falling back")
            }
            getFallbackSlides(context, niche, topic)
        }
    }

    private fun getFallbackSlides(context: Context, niche: String, topic: String): List<SlideData> {
        val securePrefs = SecurePreferences(context)
        val recentPresets = securePrefs.getRecentPresetIds()
        val projectSeed = System.currentTimeMillis()
        val cleanTopic = if (topic.isBlank()) niche else topic
        val defaultStyle = SlideData.getDefaultVoiceStyle(niche)

        val rawSlides = when (niche.lowercase()) {
            "horror" -> listOf(
                SlideData("story", "Raat 3 baje", "Raat 3 baje ammi ne mujhe awaaz di. Masla yeh tha... ammi ko guzre 2 saal ho chuke the.", voiceText = "Raat 3 baje ammi ne mujhe awaaz di.", voiceStyle = "WHISPER_SLOW", accentColor = "#FF4D8D"),
                SlideData("story", "Razai ke andar", "Main ne razai munh par kheench li. Awaaz phir aayi... bilkul darwaze ke peechay se.", voiceText = "Awaaz phir aayi darwaze ke peechay se.", voiceStyle = "WHISPER_SLOW", accentColor = "#FF4D8D"),
                SlideData("story", "Mobile Screen", "Phir mobile bajaa. Screen par likha tha: Ammi calling.", voiceText = "Mobile screen par likha tha: Ammi calling.", voiceStyle = "SUSPENSE", accentColor = "#FF4D8D"),
                SlideData("story", "Awaaz", "Main ne call uthayi. Dusri taraf se ammi ki awaaz aayi.", voiceText = "Dusri taraf se ammi ki awaaz aayi.", voiceStyle = "WHISPER_SLOW", accentColor = "#FF4D8D"),
                SlideData("story", "Che चेतावनी", "'Darwaza mat kholna. Jo bahar hai, wo main nahi hoon.'", voiceText = "'Darwaza mat kholna, wo main nahi hoon.'", voiceStyle = "WHISPER_SLOW", accentColor = "#FF4D8D"),
                SlideData("story", "Dastak", "Aur phir... achanak darwaze par zor se dastak hui.", voiceText = "Aur phir darwaze par dastak hui.", voiceStyle = "WHISPER_SLOW", accentColor = "#FF4D8D")
            )
            "motivation" -> listOf(
                SlideData("quote", "50 rupay jeb mein", "50 rupay jeb mein aur ek purani cycle lekar us ne shehar chhoda tha.", voiceText = "Pachas rupay aur ek purani cycle.", voiceStyle = "ENERGETIC", accentColor = "#F59E0B"),
                SlideData("story", "Raatein", "Pehle 3 saal roz bhooke sona pada, logon ne taanay diye.", voiceText = "Pehle 3 saal roz bhooke sona pada.", voiceStyle = "CALM_NARRATOR", accentColor = "#F59E0B"),
                SlideData("listicle", "Turning Point", "Lekin us ne ek aadat nahi chhodi: roz subah 4 baje uthkar mehnat karna.", voiceText = "Roz subah chaar baje uthkar mehnat.", voiceStyle = "ENERGETIC", accentColor = "#F59E0B"),
                SlideData("quote", "Asli Sach", "Kamyabi kisi ki mohtaj nahi hoti, bas zid aur consistency chahiye.", voiceText = "Kamyabi ko zid aur consistency chahiye.", voiceStyle = "ENERGETIC", accentColor = "#F59E0B"),
                SlideData("quote", "Aaj", "Aaj wo usi shehar ka sab se bada CEO hai. Zid karo, haar mat maano.", voiceText = "Aaj wo usi shehar ka sab se bada CEO hai.", voiceStyle = "ENERGETIC", accentColor = "#F59E0B")
            )
            "riddle" -> listOf(
                SlideData("riddle", "Ajeeb sawal", "Dunya ki aisi kaun si cheez hai jo paida hote hi udne lagti hai? Bina paron ke.", voiceText = "Paida hote hi udne lagti hai bina paron ke.", voiceStyle = "SUSPENSE", accentColor = "#22D3EE"),
                SlideData("story", "Sochiye", "99% log pehli koshish mein haar maan lete hain. Kya aapko pata hai?", voiceText = "99% log pehli koshish mein haar maan gaye.", voiceStyle = "SUSPENSE", accentColor = "#22D3EE"),
                SlideData("quote", "Jawab", "Sahi jawab hai... dhuan! Kaisa laga?", voiceText = "Sahi jawab hai dhuan!", voiceStyle = "SUSPENSE", accentColor = "#22D3EE")
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
