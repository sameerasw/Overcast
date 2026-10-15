package com.sameerasw.overcast.weather.ai

data class AiProvider(
    val id: String,
    val displayName: String,
    val defaultModel: String,
    val defaultBaseUrl: String?,
    val signupUrl: String?,
    val needsBaseUrl: Boolean = false,
)

object AiProviders {
    val gemini = AiProvider(
        id = "gemini",
        displayName = "Gemini (AI Studio)",
        defaultModel = "gemini-3.5-flash-lite",
        defaultBaseUrl = null,
        signupUrl = "https://aistudio.google.com/apikey",
    )
    val openai = AiProvider(
        id = "openai",
        displayName = "OpenAI",
        defaultModel = "gpt-4o-mini",
        defaultBaseUrl = "https://api.openai.com/v1",
        signupUrl = "https://platform.openai.com/api-keys",
        needsBaseUrl = true,
    )
    val openrouter = AiProvider(
        id = "openrouter",
        displayName = "OpenRouter",
        defaultModel = "google/gemini-flash-1.5",
        defaultBaseUrl = "https://openrouter.ai/api/v1",
        signupUrl = "https://openrouter.ai/keys",
        needsBaseUrl = true,
    )

    val all: List<AiProvider> = listOf(gemini, openai, openrouter)

    fun byId(id: String?): AiProvider = all.firstOrNull { it.id == id } ?: gemini
}
