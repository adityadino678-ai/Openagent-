package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.ProviderConfig
import com.example.data.model.ProviderType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AiClient {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Sends prompt to the active provider and returns the raw response string and latency.
     */
    suspend fun queryProvider(
        provider: ProviderConfig,
        prompt: String,
        systemInstruction: String = "You are OpenAgent, a minimalistic autonomous mobile agent. Respond concisely. If user asks to execute a device task (flashlight, volume, timer, app, clipboard, webhook), format your response with clear instructions or a JSON action block."
    ): Pair<String, Long> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val responseText = when (provider.type) {
                ProviderType.GEMINI -> queryGemini(provider, prompt, systemInstruction)
                ProviderType.ANTHROPIC -> queryAnthropic(provider, prompt, systemInstruction)
                ProviderType.OPENAI,
                ProviderType.GROQ,
                ProviderType.DEEPSEEK,
                ProviderType.MISTRAL,
                ProviderType.CUSTOM -> queryOpenAiCompatible(provider, prompt, systemInstruction)
            }
            val latency = System.currentTimeMillis() - startTime
            Pair(responseText, latency)
        } catch (e: Exception) {
            Log.e("AiClient", "Query failed for ${provider.name}: ${e.message}", e)
            val latency = System.currentTimeMillis() - startTime
            Pair("Error querying ${provider.name}: ${e.localizedMessage ?: "Network or key error"}", latency)
        }
    }

    /**
     * Pings the provider with a trivial prompt to verify connectivity and calculate latency.
     */
    suspend fun pingProvider(provider: ProviderConfig): Pair<Boolean, Long> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val (result, _) = queryProvider(provider, "Ping. Reply with PONG.")
            val latency = System.currentTimeMillis() - startTime
            val isSuccess = !result.startsWith("Error querying") && result.isNotBlank()
            Pair(isSuccess, latency)
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            Pair(false, latency)
        }
    }

    private fun queryGemini(provider: ProviderConfig, prompt: String, systemInstruction: String): String {
        val effectiveKey = if (provider.apiKey.isNotBlank()) {
            provider.apiKey
        } else {
            // Fallback to BuildConfig if available
            try {
                BuildConfig::class.java.getField("GEMINI_API_KEY").get(null) as? String ?: ""
            } catch (e: Exception) {
                ""
            }
        }

        if (effectiveKey.isBlank()) {
            return "Notice: No API key configured for Google Gemini. Please tap 'Get Key from Portal' in the Providers tab to configure your key."
        }

        val model = if (provider.selectedModel.isNotBlank()) provider.selectedModel else "gemini-3.5-flash"
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$effectiveKey"

        val bodyJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    })
                })
            })
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", systemInstruction) })
                })
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(bodyJson.toString().toRequestBody(jsonMediaType))
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            val responseString = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return "Gemini HTTP ${response.code}: $responseString"
            }
            val root = JSONObject(responseString)
            val candidates = root.optJSONArray("candidates")
            val candidate = candidates?.optJSONObject(0)
            val content = candidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text")
            return text ?: "Empty response from Gemini."
        }
    }

    private fun queryOpenAiCompatible(provider: ProviderConfig, prompt: String, systemInstruction: String): String {
        if (provider.apiKey.isBlank() && provider.type != ProviderType.CUSTOM) {
            return "Notice: No API key configured for ${provider.name}. Please enter your key in the Providers tab."
        }

        val baseUrl = provider.baseUrl.trimEnd('/') + "/chat/completions"
        val bodyJson = JSONObject().apply {
            put("model", provider.selectedModel)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemInstruction)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
            put("temperature", 0.7)
        }

        val requestBuilder = Request.Builder()
            .url(baseUrl)
            .post(bodyJson.toString().toRequestBody(jsonMediaType))

        if (provider.apiKey.isNotBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer ${provider.apiKey}")
        }

        okHttpClient.newCall(requestBuilder.build()).execute().use { response ->
            val responseString = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return "${provider.name} HTTP ${response.code}: $responseString"
            }
            val root = JSONObject(responseString)
            val choices = root.optJSONArray("choices")
            val choice = choices?.optJSONObject(0)
            val message = choice?.optJSONObject("message")
            return message?.optString("content") ?: "Empty response from ${provider.name}."
        }
    }

    private fun queryAnthropic(provider: ProviderConfig, prompt: String, systemInstruction: String): String {
        if (provider.apiKey.isBlank()) {
            return "Notice: No API key configured for Anthropic. Please configure your key in Providers."
        }

        val url = provider.baseUrl.trimEnd('/') + "/messages"
        val bodyJson = JSONObject().apply {
            put("model", provider.selectedModel)
            put("max_tokens", 1024)
            put("system", systemInstruction)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("x-api-key", provider.apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .post(bodyJson.toString().toRequestBody(jsonMediaType))
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            val responseString = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return "Anthropic HTTP ${response.code}: $responseString"
            }
            val root = JSONObject(responseString)
            val content = root.optJSONArray("content")
            val firstBlock = content?.optJSONObject(0)
            return firstBlock?.optString("text") ?: "Empty response from Anthropic."
        }
    }
}
