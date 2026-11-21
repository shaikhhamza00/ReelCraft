package com.hamza.reelcraft.data

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class ModelListInfo(
    val flashModelsCount: Int,
    val selectedModel: String?,
    val allFlashModels: List<String>,
    val httpCode: Int,
    val errorBody: String? = null
)

object ModelResolver {
    private const val TAG = "ModelResolver"
    private const val PREFS_NAME = "gemini_model_cache"

    val FALLBACK_MODELS = listOf(
        "gemini-3.5-flash-lite",
        "gemini-3.8-flash",
        "gemini-3.7-flash",
        "gemini-3.6-flash",
        "gemini-flash-latest"
    )

    fun blacklistModel(context: Context, modelName: String, durationMs: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val expireTime = System.currentTimeMillis() + durationMs
        prefs.edit().putLong("blacklist_$modelName", expireTime).apply()
        Log.w(TAG, "Blacklisted model $modelName for ${durationMs / 1000}s (until $expireTime)")
    }

    fun isBlacklisted(context: Context, modelName: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val expireTime = prefs.getLong("blacklist_$modelName", 0L)
        val now = System.currentTimeMillis()
        if (expireTime > now) {
            return true
        }
        if (expireTime > 0L) {
            prefs.edit().remove("blacklist_$modelName").apply()
        }
        return false
    }

    fun saveWorkingModel(context: Context, modelName: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString("last_working_model", modelName)
            .putLong("last_working_time", System.currentTimeMillis())
            .apply()
        Log.i(TAG, "Saved last working model: $modelName")
    }

    fun getWorkingModel(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val model = prefs.getString("last_working_model", null)
        val time = prefs.getLong("last_working_time", 0L)
        val sixHoursMs = 6 * 60 * 60 * 1000L
        if (!model.isNullOrBlank() && (System.currentTimeMillis() - time < sixHoursMs)) {
            if (!isBlacklisted(context, model)) {
                return model
            }
        }
        return null
    }

    fun getCandidateModels(context: Context, apiKey: String): List<String> {
        val candidates = mutableListOf<String>()

        // (a) Last working model if < 6 hrs and not blacklisted
        val working = getWorkingModel(context)
        if (!working.isNullOrBlank()) {
            candidates.add(working)
        }

        // Fetch ListModels from API
        val info = fetchModelsFromApi(apiKey)
        if (info.allFlashModels.isNotEmpty()) {
            val liteModels = mutableListOf<String>()
            val flashModels = mutableListOf<String>()
            val latestModels = mutableListOf<String>()

            for (m in info.allFlashModels) {
                val lower = m.lowercase()
                if (lower.contains("latest")) {
                    latestModels.add(m)
                } else if (lower.contains("lite")) {
                    liteModels.add(m)
                } else {
                    flashModels.add(m)
                }
            }

            val versionComparator = Comparator<String> { a, b ->
                val verA = extractVersion(a)
                val verB = extractVersion(b)
                if (verA != verB) verB.compareTo(verA) else a.compareTo(b)
            }
            liteModels.sortWith(versionComparator)
            flashModels.sortWith(versionComparator)
            latestModels.sortWith(versionComparator)

            // Order: (b) flash-lite, (c) flash, (d) latest
            candidates.addAll(liteModels)
            candidates.addAll(flashModels)
            candidates.addAll(latestModels)
        }

        candidates.addAll(FALLBACK_MODELS)

        val result = candidates
            .distinct()
            .filter { !isBlacklisted(context, it) }

        Log.i(TAG, "Candidate models (${result.size}): $result")
        return if (result.isNotEmpty()) result else FALLBACK_MODELS.filter { !isBlacklisted(context, it) }.ifEmpty { FALLBACK_MODELS }
    }

    fun fetchModelsFromApi(apiKey: String): ModelListInfo {
        if (apiKey.isBlank()) {
            return ModelListInfo(0, null, emptyList(), 400, "API key is blank")
        }
        val url = URL("https://generativelanguage.googleapis.com/v1beta/models?pageSize=100")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("x-goog-api-key", apiKey)
            connectTimeout = 10000
            readTimeout = 10000
        }

        val responseCode = connection.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val responseStr = reader.readText()
            reader.close()

            val root = JSONObject(responseStr)
            val models = root.optJSONArray("models") ?: return ModelListInfo(0, null, emptyList(), responseCode, null)
            val flashModels = mutableListOf<String>()

            for (i in 0 until models.length()) {
                val m = models.optJSONObject(i) ?: continue
                val name = m.optString("name", "").removePrefix("models/")
                val methods = m.optJSONArray("supportedGenerationMethods")
                
                var supportsGenerate = false
                if (methods != null) {
                    for (j in 0 until methods.length()) {
                        if (methods.optString(j) == "generateContent") {
                            supportsGenerate = true
                            break
                        }
                    }
                }

                if (supportsGenerate) {
                    val lower = name.lowercase()
                    val excluded = listOf("image", "tts", "live", "audio", "robotics", "embedding", "imagen", "veo", "lyria")
                    if (lower.contains("flash") && excluded.none { lower.contains(it) }) {
                        flashModels.add(name)
                    }
                }
            }

            return ModelListInfo(
                flashModelsCount = flashModels.size,
                selectedModel = flashModels.firstOrNull(),
                allFlashModels = flashModels,
                httpCode = responseCode,
                errorBody = null
            )
        } else {
            val errStr = try {
                connection.errorStream?.bufferedReader()?.use { it.readText() }
            } catch (_: Exception) {
                null
            }
            return ModelListInfo(
                flashModelsCount = 0,
                selectedModel = null,
                allFlashModels = emptyList(),
                httpCode = responseCode,
                errorBody = errStr ?: "HTTP $responseCode"
            )
        }
    }

    private fun extractVersion(modelName: String): Float {
        val regex = Regex("(\\d+\\.\\d+|\\d+)")
        val match = regex.find(modelName)
        return match?.value?.toFloatOrNull() ?: 0f
    }
}
