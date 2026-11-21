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
    private const val KEY_MODEL = "cached_model_name"
    private const val KEY_TIME = "cached_model_time"
    private const val CACHE_DURATION_MS = 24 * 60 * 60 * 1000L // 24 hours

    val FALLBACK_MODELS = listOf("gemini-3.8-flash", "gemini-3.7-flash", "gemini-3.6-flash", "gemini-3.5-flash-lite", "gemini-flash-latest")

    suspend fun resolveModel(context: Context, apiKey: String): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cachedModel = prefs.getString(KEY_MODEL, null)
        val cachedTime = prefs.getLong(KEY_TIME, 0L)
        val now = System.currentTimeMillis()

        if (!cachedModel.isNullOrBlank() && (now - cachedTime < CACHE_DURATION_MS)) {
            Log.i(TAG, "Using cached Gemini model: $cachedModel")
            return cachedModel
        }

        try {
            val info = fetchModelsFromApi(apiKey)
            if (info.selectedModel != null) {
                prefs.edit()
                    .putString(KEY_MODEL, info.selectedModel)
                    .putLong(KEY_TIME, now)
                    .apply()
                Log.i(TAG, "Resolved and cached Gemini model: ${info.selectedModel}")
                return info.selectedModel
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch model list from API, using fallback", e)
        }

        return FALLBACK_MODELS.first()
    }

    fun removeCachedModel(context: Context, badModel: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cached = prefs.getString(KEY_MODEL, null)
        if (cached == badModel) {
            prefs.edit().remove(KEY_MODEL).remove(KEY_TIME).apply()
            Log.w(TAG, "Removed blacklisted bad model from cache: $badModel")
        }
    }

    fun fetchModelsFromApi(apiKey: String): ModelListInfo {
        if (apiKey.isBlank()) {
            return ModelListInfo(0, null, emptyList(), 400, "API key is blank")
        }
        val url = URL("https://generativelanguage.googleapis.com/v1beta/models?pageSize=100")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("x-goog-api-key", apiKey)
            connectTimeout = 15000
            readTimeout = 15000
        }

        val responseCode = connection.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val responseStr = reader.readText()
            reader.close()

            val root = JSONObject(responseStr)
            val models = root.optJSONArray("models") ?: return ModelListInfo(0, null, emptyList(), responseCode, null)
            val flashModels = mutableListOf<String>()
            val liteModels = mutableListOf<String>()
            val latestModels = mutableListOf<String>()

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
                        if (lower.contains("latest")) {
                            latestModels.add(name)
                        } else if (lower.contains("lite")) {
                            liteModels.add(name)
                        } else {
                            flashModels.add(name)
                        }
                    }
                }
            }

            flashModels.sortWith(Comparator { a, b ->
                val verA = extractVersion(a)
                val verB = extractVersion(b)
                if (verA != verB) {
                    verB.compareTo(verA)
                } else {
                    val isPreviewA = a.lowercase().contains("preview")
                    val isPreviewB = b.lowercase().contains("preview")
                    if (isPreviewA == isPreviewB) 0 else if (isPreviewA) 1 else -1
                }
            })

            val combined = mutableListOf<String>()
            combined.addAll(flashModels)
            combined.addAll(liteModels)
            combined.addAll(latestModels)

            return ModelListInfo(
                flashModelsCount = combined.size,
                selectedModel = combined.firstOrNull(),
                allFlashModels = combined,
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
