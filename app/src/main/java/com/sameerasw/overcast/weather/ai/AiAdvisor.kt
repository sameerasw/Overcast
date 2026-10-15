package com.sameerasw.overcast.weather.ai

import com.sameerasw.overcast.weather.model.WeatherSnapshot
import com.sameerasw.overcast.weather.provider.ProviderHttp
import com.sameerasw.overcast.weather.provider.WeatherProviderException
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

class AiException(message: String?) : Exception(message)

object AiAdvisor {
    fun buildPrompt(snapshot: WeatherSnapshot, locale: Locale = Locale.getDefault()): String {
        val lang = locale.displayLanguage.ifBlank { "English" }
        val alerts = snapshot.activeAlerts().take(3).joinToString("; ") { it.headline }
        return buildString {
            append("Current weather in ${snapshot.locationName}: ")
            append("${"%.1f".format(Locale.US, snapshot.tempC)}C, feels like ${"%.1f".format(Locale.US, snapshot.feelsLikeC)}C, ")
            append("${snapshot.conditionText}, humidity ${snapshot.humidity ?: "?"}%, ")
            append("wind ${"%.1f".format(Locale.US, snapshot.windKph)} km/h, ")
            append("rain chance ${snapshot.chanceOfRain ?: 0}%, ")
            append("today high ${"%.1f".format(Locale.US, snapshot.todayHighC())}C low ${"%.1f".format(Locale.US, snapshot.todayLowC())}C. ")
            if (alerts.isNotBlank()) append("Alerts: $alerts. ")
            append("Reply in $lang, max 90 words, as short bullet lines with emoji: ")
            append("what to wear, take an umbrella or not, best time to go out, one health tip.")
        }
    }

    suspend fun advise(
        providerId: String,
        apiKey: String,
        model: String?,
        baseUrl: String?,
        snapshot: WeatherSnapshot,
        locale: Locale = Locale.getDefault(),
    ): String {
        val provider = AiProviders.byId(providerId)
        val prompt = buildPrompt(snapshot, locale)
        return if (provider.id == "gemini") {
            adviseGemini(apiKey, model?.ifBlank { null } ?: provider.defaultModel, prompt)
        } else {
            adviseOpenAiCompatible(
                baseUrl?.ifBlank { null } ?: provider.defaultBaseUrl.orEmpty(),
                apiKey,
                model?.ifBlank { null } ?: provider.defaultModel,
                prompt,
            )
        }
    }

    private suspend fun adviseGemini(apiKey: String, model: String, prompt: String): String {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/${ProviderHttp.encode(model)}:generateContent?key=${ProviderHttp.encode(apiKey)}"
        val body = JSONObject()
            .put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
            .toString()
        val raw = postGuessing(body, url, emptyMap())
        val json = JSONObject(raw)
        json.optJSONObject("error")?.let { throw AiException(it.optString("message", "AI request failed")) }
        return json.optJSONArray("candidates")
            ?.optJSONObject(0)?.optJSONObject("content")
            ?.optJSONArray("parts")?.optJSONObject(0)?.optString("text")?.trim()
            ?.takeIf { it.isNotBlank() } ?: throw AiException("Empty AI response")
    }

    private suspend fun adviseOpenAiCompatible(baseUrl: String, apiKey: String, model: String, prompt: String): String {
        val url = baseUrl.trimEnd('/') + "/chat/completions"
        val body = JSONObject()
            .put("model", model)
            .put("max_tokens", 300)
            .put("temperature", 0.6)
            .put("messages", JSONArray()
                .put(JSONObject().put("role", "system").put("content", "You are a concise weather assistant."))
                .put(JSONObject().put("role", "user").put("content", prompt)))
            .toString()
        val raw = postGuessing(body, url, mapOf("Authorization" to "Bearer $apiKey"))
        val json = JSONObject(raw)
        json.optJSONObject("error")?.let { throw AiException(it.optString("message", "AI request failed")) }
        return json.optJSONArray("choices")
            ?.optJSONObject(0)?.optJSONObject("message")?.optString("content")?.trim()
            ?.takeIf { it.isNotBlank() } ?: throw AiException("Empty AI response")
    }

    private suspend fun postGuessing(body: String, url: String, headers: Map<String, String>): String {
        try {
            return ProviderHttp.post(url, body, headers)
        } catch (e: WeatherProviderException) {
            throw AiException(
                when (e.reason) {
                    WeatherProviderException.Reason.INVALID_KEY -> "AI key was rejected"
                    WeatherProviderException.Reason.RATE_LIMITED -> "AI rate limit, try later"
                    WeatherProviderException.Reason.NETWORK -> "No connection to AI service"
                    WeatherProviderException.Reason.BAD_RESPONSE -> e.message ?: "AI request failed"
                },
            )
        }
    }
}
