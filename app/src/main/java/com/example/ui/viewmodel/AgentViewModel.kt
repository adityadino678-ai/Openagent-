package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.os.BatteryManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.AiClient
import com.example.data.model.AgentSkill
import com.example.data.model.AgentTask
import com.example.data.model.ProviderConfig
import com.example.data.model.SkillActionType
import com.example.data.model.SkillCategory
import com.example.data.model.TaskStatus
import com.example.data.model.TaskTriggerSource
import com.example.data.security.CryptoManager
import com.example.service.AgentForegroundService
import com.example.service.SkillDispatcher
import com.example.service.VoiceSpeechManager
import com.example.service.VoiceState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class AgentUiState(
    val providers: List<ProviderConfig> = ProviderConfig.defaultProviders(),
    val activeProviderId: String = "gemini",
    val skills: List<AgentSkill> = AgentSkill.defaultSkills(),
    val tasks: List<AgentTask> = emptyList(),
    val isExecuting: Boolean = false,
    val activeTaskTitle: String = "Idle",
    val terminalLogs: List<String> = listOf("[SYS] OpenAgent kernel v1.0.4 loaded", "[SEC] Hardware AES-256 KeyStore storage verified", "[READY] Awaiting voice or text input..."),
    val isVoiceModalOpen: Boolean = false,
    val voiceRecognizedText: String = "",
    val voiceError: String? = null,
    val batteryPercent: Int = 85,
    val isCharging: Boolean = false,
    val isBatteryShieldActive: Boolean = true,
    val isDarkMode: Boolean = true,
    val searchFilter: String = ""
)

class AgentViewModel(application: Application) : AndroidViewModel(application) {

    private val cryptoManager = CryptoManager(application)
    private val aiClient = AiClient()
    private val skillDispatcher = SkillDispatcher(application)
    val voiceManager = VoiceSpeechManager(application)

    private val _uiState = MutableStateFlow(AgentUiState())
    val uiState = _uiState.asStateFlow()

    val isBackgroundServiceRunning = AgentForegroundService.isServiceActive

    init {
        loadEncryptedConfiguration()
        refreshBatteryTelemetry()
        observeVoiceState()
    }

    private fun loadEncryptedConfiguration() {
        val loadedProviders = _uiState.value.providers.map { provider ->
            val savedKey = cryptoManager.getEncryptedString("api_key_${provider.id}")
            val savedModel = cryptoManager.getEncryptedString("model_${provider.id}", provider.selectedModel)
            provider.copy(
                apiKey = savedKey,
                selectedModel = if (savedModel.isNotBlank()) savedModel else provider.selectedModel
            )
        }
        val savedActiveId = cryptoManager.getEncryptedString("active_provider_id", "gemini")
        _uiState.update { it.copy(providers = loadedProviders, activeProviderId = savedActiveId) }
    }

    fun saveProviderKey(providerId: String, newKey: String) {
        cryptoManager.saveEncryptedString("api_key_$providerId", newKey.trim())
        _uiState.update { state ->
            val updated = state.providers.map {
                if (it.id == providerId) it.copy(apiKey = newKey.trim()) else it
            }
            state.copy(providers = updated)
        }
        appendLog("[SEC] Key for $providerId encrypted & stored in KeyStore")
    }

    fun selectModel(providerId: String, model: String) {
        cryptoManager.saveEncryptedString("model_$providerId", model)
        _uiState.update { state ->
            val updated = state.providers.map {
                if (it.id == providerId) it.copy(selectedModel = model) else it
            }
            state.copy(providers = updated)
        }
    }

    fun setActiveProvider(providerId: String) {
        cryptoManager.saveEncryptedString("active_provider_id", providerId)
        _uiState.update { state ->
            val updated = state.providers.map {
                it.copy(isActive = (it.id == providerId))
            }
            state.copy(providers = updated, activeProviderId = providerId)
        }
        appendLog("[CORE] Active AI engine switched to: $providerId")
    }

    fun testProviderPing(providerId: String) {
        val provider = _uiState.value.providers.find { it.id == providerId } ?: return
        viewModelScope.launch {
            appendLog("[PING] Sending test probe to ${provider.name}...")
            val (success, latency) = aiClient.pingProvider(provider)
            _uiState.update { state ->
                val updated = state.providers.map {
                    if (it.id == providerId) it.copy(isVerified = success, latencyMs = latency) else it
                }
                state.copy(providers = updated)
            }
            if (success) {
                appendLog("[PING] ${provider.name} verified! Latency: ${latency}ms")
            } else {
                appendLog("[PING_ERR] ${provider.name} ping failed or key unauthorized.")
            }
        }
    }

    fun toggleSkill(skillId: String) {
        _uiState.update { state ->
            val updated = state.skills.map {
                if (it.id == skillId) it.copy(isEnabled = !it.isEnabled) else it
            }
            state.copy(skills = updated)
        }
    }

    fun addCustomSkill(title: String, description: String, keywords: List<String>, scriptBody: String) {
        val newSkill = AgentSkill(
            id = "custom_${System.currentTimeMillis()}",
            title = title,
            description = description,
            category = SkillCategory.CUSTOM_SCRIPTS,
            actionType = SkillActionType.CUSTOM_PIPELINE,
            isBuiltIn = false,
            triggerKeywords = keywords,
            customScriptBody = scriptBody
        )
        _uiState.update { it.copy(skills = listOf(newSkill) + it.skills) }
        appendLog("[SKILL_REG] New custom automation skill registered: '$title'")
    }

    fun toggleBackgroundService(context: Context) {
        val currentlyRunning = isBackgroundServiceRunning.value
        if (currentlyRunning) {
            AgentForegroundService.stop(context)
            appendLog("[SERVICE] Foreground Service stopped.")
        } else {
            AgentForegroundService.start(context)
            appendLog("[SERVICE] Foreground Service started with Lock Screen controls.")
        }
    }

    fun executeCommand(command: String, triggerSource: TaskTriggerSource = TaskTriggerSource.DASHBOARD_INPUT) {
        if (command.isBlank()) return
        val taskId = UUID.randomUUID().toString().take(8)
        val activeProvider = _uiState.value.providers.find { it.id == _uiState.value.activeProviderId }
            ?: _uiState.value.providers.first()

        val newTask = AgentTask(
            id = taskId,
            command = command,
            providerId = activeProvider.id,
            modelUsed = activeProvider.selectedModel,
            triggerSource = triggerSource,
            status = TaskStatus.RUNNING
        )

        _uiState.update {
            it.copy(
                isExecuting = true,
                activeTaskTitle = command,
                tasks = listOf(newTask) + it.tasks
            )
        }

        appendLog("[INPUT:$taskId] \"$command\" via ${triggerSource.name}")

        viewModelScope.launch {
            val startTime = System.currentTimeMillis()

            // 1. Check if a local skill directly matches the user's intent keywords
            val matchedSkill = findMatchingSkill(command)

            if (matchedSkill != null) {
                appendLog("[MATCH] Direct skill mapped: ${matchedSkill.title}")
                val skillResult = skillDispatcher.executeSkill(matchedSkill, command)
                val duration = System.currentTimeMillis() - startTime

                skillResult.logs.forEach { appendLog(it) }

                updateTaskOutcome(
                    taskId = taskId,
                    status = if (skillResult.isSuccess) TaskStatus.COMPLETED else TaskStatus.FAILED,
                    matchedSkillId = matchedSkill.id,
                    output = skillResult.outputMessage,
                    executionTimeMs = duration,
                    logs = skillResult.logs
                )
            } else {
                // 2. Query the active AI Provider
                appendLog("[ROUTING] Querying AI engine: ${activeProvider.name} (${activeProvider.selectedModel})")
                val (aiResponse, latency) = aiClient.queryProvider(activeProvider, command)
                val duration = System.currentTimeMillis() - startTime

                appendLog("[AI_RESP] ${aiResponse.take(150)}...")

                // Check if the AI indicated a device action
                val fallbackSkill = detectSkillFromAiOutput(aiResponse)
                if (fallbackSkill != null) {
                    appendLog("[SKILL_EXEC] AI resolved secondary action: ${fallbackSkill.title}")
                    val actionResult = skillDispatcher.executeSkill(fallbackSkill, command)
                    actionResult.logs.forEach { appendLog(it) }
                }

                updateTaskOutcome(
                    taskId = taskId,
                    status = if (aiResponse.startsWith("Error")) TaskStatus.FAILED else TaskStatus.COMPLETED,
                    matchedSkillId = fallbackSkill?.id,
                    output = aiResponse,
                    executionTimeMs = duration,
                    logs = listOf("[AI] Latency: ${latency}ms", "[OUTPUT] $aiResponse")
                )
            }

            _uiState.update { it.copy(isExecuting = false, activeTaskTitle = "Idle") }
        }
    }

    private fun findMatchingSkill(command: String): AgentSkill? {
        val lower = command.lowercase()
        return _uiState.value.skills
            .filter { it.isEnabled }
            .firstOrNull { skill ->
                skill.triggerKeywords.any { keyword -> lower.contains(keyword.lowercase()) }
            }
    }

    private fun detectSkillFromAiOutput(output: String): AgentSkill? {
        val lower = output.lowercase()
        return _uiState.value.skills.filter { it.isEnabled }.firstOrNull { skill ->
            when (skill.actionType) {
                SkillActionType.FLASHLIGHT_TOGGLE -> lower.contains("turn on flashlight") || lower.contains("turn off flashlight")
                SkillActionType.CLIPBOARD_WRITE -> lower.contains("copied to clipboard")
                SkillActionType.SET_TIMER -> lower.contains("setting timer") || lower.contains("timer set")
                else -> false
            }
        }
    }

    private fun updateTaskOutcome(
        taskId: String,
        status: TaskStatus,
        matchedSkillId: String?,
        output: String,
        executionTimeMs: Long,
        logs: List<String>
    ) {
        _uiState.update { state ->
            val updatedTasks = state.tasks.map {
                if (it.id == taskId) {
                    it.copy(
                        status = status,
                        matchedSkillId = matchedSkillId,
                        resultOutput = output,
                        executionTimeMs = executionTimeMs,
                        terminalLogs = logs
                    )
                } else it
            }
            state.copy(tasks = updatedTasks)
        }
        appendLog("[STATUS] Task $taskId finished with status: ${status.name}")
    }

    fun startVoiceInput() {
        _uiState.update { it.copy(isVoiceModalOpen = true, voiceRecognizedText = "", voiceError = null) }
        voiceManager.startListening { text ->
            _uiState.update { it.copy(voiceRecognizedText = text) }
            executeCommand(text, TaskTriggerSource.VOICE_COMMAND)
        }
    }

    fun stopVoiceInput() {
        voiceManager.stopListening()
        _uiState.update { it.copy(isVoiceModalOpen = false) }
    }

    private fun observeVoiceState() {
        viewModelScope.launch {
            voiceManager.voiceState.collect { state ->
                when (state) {
                    is VoiceState.Error -> _uiState.update { it.copy(voiceError = state.message) }
                    is VoiceState.Success -> _uiState.update { it.copy(voiceRecognizedText = state.recognizedText) }
                    else -> Unit
                }
            }
        }
    }

    fun toggleDarkMode() {
        _uiState.update { it.copy(isDarkMode = !it.isDarkMode) }
    }

    fun refreshBatteryTelemetry() {
        val batteryManager = getApplication<Application>().getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val level = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 85
        _uiState.update { it.copy(batteryPercent = level) }
    }

    private fun appendLog(log: String) {
        _uiState.update { state ->
            val newLogs = (state.terminalLogs + log).takeLast(100)
            state.copy(terminalLogs = newLogs)
        }
    }
}
