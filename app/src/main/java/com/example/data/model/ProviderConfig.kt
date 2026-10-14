package com.example.data.model

enum class ProviderType {
    GEMINI,
    OPENAI,
    ANTHROPIC,
    GROQ,
    MISTRAL,
    DEEPSEEK,
    CUSTOM
}

data class ProviderConfig(
    val id: String,
    val name: String,
    val type: ProviderType,
    val apiKey: String = "",
    val baseUrl: String = "",
    val selectedModel: String,
    val availableModels: List<String>,
    val keyConsoleUrl: String,
    val description: String,
    val isActive: Boolean = false,
    val latencyMs: Long? = null,
    val isVerified: Boolean = false
) {
    val isKeyConfigured: Boolean
        get() = apiKey.isNotBlank()

    val maskedKey: String
        get() = if (apiKey.length > 8) {
            "${apiKey.take(4)}••••••••${apiKey.takeLast(4)}"
        } else if (apiKey.isNotEmpty()) {
            "••••••••"
        } else {
            "Not configured"
        }

    companion object {
        fun defaultProviders(): List<ProviderConfig> {
            return listOf(
                ProviderConfig(
                    id = "gemini",
                    name = "Google Gemini",
                    type = ProviderType.GEMINI,
                    baseUrl = "https://generativelanguage.googleapis.com/",
                    selectedModel = "gemini-3.5-flash",
                    availableModels = listOf("gemini-3.5-flash", "gemini-3.1-pro-preview", "gemini-2.5-flash-preview-tts"),
                    keyConsoleUrl = "https://aistudio.google.com/app/apikey",
                    description = "High-speed reasoning, multimodal analysis & native tool calling.",
                    isActive = true
                ),
                ProviderConfig(
                    id = "openai",
                    name = "OpenAI",
                    type = ProviderType.OPENAI,
                    baseUrl = "https://api.openai.com/v1/",
                    selectedModel = "gpt-4o-mini",
                    availableModels = listOf("gpt-4o-mini", "gpt-4o", "o1-mini"),
                    keyConsoleUrl = "https://platform.openai.com/api-keys",
                    description = "Industry standard structured output and versatile task processing."
                ),
                ProviderConfig(
                    id = "anthropic",
                    name = "Anthropic Claude",
                    type = ProviderType.ANTHROPIC,
                    baseUrl = "https://api.anthropic.com/v1/",
                    selectedModel = "claude-3-5-sonnet-20241022",
                    availableModels = listOf("claude-3-5-sonnet-20241022", "claude-3-5-haiku-20241022"),
                    keyConsoleUrl = "https://console.anthropic.com/settings/keys",
                    description = "Exceptional nuanced instruction following, scripting & system control."
                ),
                ProviderConfig(
                    id = "groq",
                    name = "Groq LPU",
                    type = ProviderType.GROQ,
                    baseUrl = "https://api.groq.com/openai/v1/",
                    selectedModel = "llama-3.3-70b-versatile",
                    availableModels = listOf("llama-3.3-70b-versatile", "mixtral-8x7b-32768", "llama-3.1-8b-instant"),
                    keyConsoleUrl = "https://console.groq.com/keys",
                    description = "Ultra-low latency inference engine for instantaneous voice command execution."
                ),
                ProviderConfig(
                    id = "deepseek",
                    name = "DeepSeek",
                    type = ProviderType.DEEPSEEK,
                    baseUrl = "https://api.deepseek.com/v1/",
                    selectedModel = "deepseek-chat",
                    availableModels = listOf("deepseek-chat", "deepseek-reasoner"),
                    keyConsoleUrl = "https://platform.deepseek.com/api_keys",
                    description = "Advanced reasoning, code synthesis & economic token efficiency."
                ),
                ProviderConfig(
                    id = "mistral",
                    name = "Mistral AI",
                    type = ProviderType.MISTRAL,
                    baseUrl = "https://api.mistral.ai/v1/",
                    selectedModel = "mistral-small-latest",
                    availableModels = listOf("mistral-small-latest", "mistral-large-latest", "codestral-latest"),
                    keyConsoleUrl = "https://console.mistral.ai/api-keys",
                    description = "European open-weights models optimized for high density inference."
                ),
                ProviderConfig(
                    id = "custom",
                    name = "Local Ollama / Custom",
                    type = ProviderType.CUSTOM,
                    baseUrl = "http://localhost:11434/v1/",
                    selectedModel = "llama3:latest",
                    availableModels = listOf("llama3:latest", "mistral:latest", "phi3:latest", "qwen2.5:latest"),
                    keyConsoleUrl = "https://ollama.com",
                    description = "Self-hosted private endpoint or local workstation on your Wi-Fi network."
                )
            )
        }
    }
}
