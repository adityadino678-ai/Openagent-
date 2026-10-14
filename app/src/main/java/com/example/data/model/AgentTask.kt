package com.example.data.model

enum class TaskStatus {
    QUEUED,
    RUNNING,
    COMPLETED,
    FAILED
}

enum class TaskTriggerSource {
    VOICE_COMMAND,
    DASHBOARD_INPUT,
    NOTIFICATION_QUICK_ACTION,
    SCHEDULED_BACKGROUND
}

data class AgentTask(
    val id: String,
    val timestamp: Long = System.currentTimeMillis(),
    val command: String,
    val providerId: String,
    val modelUsed: String,
    val triggerSource: TaskTriggerSource,
    val matchedSkillId: String? = null,
    val status: TaskStatus = TaskStatus.QUEUED,
    val resultOutput: String = "",
    val executionTimeMs: Long = 0,
    val terminalLogs: List<String> = emptyList()
)
