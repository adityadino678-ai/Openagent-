package com.example.data.model

enum class SkillCategory {
    PHONE_CONTROL,
    APP_LAUNCHER,
    PRODUCTIVITY,
    CLIPBOARD,
    CUSTOM_SCRIPTS
}

enum class SkillActionType {
    FLASHLIGHT_TOGGLE,
    VOLUME_SET,
    BATTERY_CHECK,
    APP_LAUNCH,
    SET_TIMER,
    SET_ALARM,
    ADD_REMINDER,
    CLIPBOARD_READ,
    CLIPBOARD_WRITE,
    HTTP_WEBHOOK,
    CUSTOM_PIPELINE
}

data class AgentSkill(
    val id: String,
    val title: String,
    val description: String,
    val category: SkillCategory,
    val actionType: SkillActionType,
    val isEnabled: Boolean = true,
    val isBuiltIn: Boolean = true,
    val triggerKeywords: List<String>,
    val defaultParams: Map<String, String> = emptyMap(),
    val customScriptBody: String? = null,
    val executionsCount: Int = 0
) {
    companion object {
        fun defaultSkills(): List<AgentSkill> {
            return listOf(
                AgentSkill(
                    id = "torch_ctrl",
                    title = "Torch & Flashlight",
                    description = "Toggle device rear LED flashlight for dark conditions.",
                    category = SkillCategory.PHONE_CONTROL,
                    actionType = SkillActionType.FLASHLIGHT_TOGGLE,
                    triggerKeywords = listOf("torch", "flashlight", "light", "flash")
                ),
                AgentSkill(
                    id = "battery_telemetry",
                    title = "Battery & Power Guard",
                    description = "Query battery percentage, charging current, and health status.",
                    category = SkillCategory.PHONE_CONTROL,
                    actionType = SkillActionType.BATTERY_CHECK,
                    triggerKeywords = listOf("battery", "power", "charge", "percentage")
                ),
                AgentSkill(
                    id = "volume_control",
                    title = "Audio & Volume Manager",
                    description = "Adjust or mute media and ringer audio streams.",
                    category = SkillCategory.PHONE_CONTROL,
                    actionType = SkillActionType.VOLUME_SET,
                    triggerKeywords = listOf("volume", "mute", "unmute", "sound", "loud")
                ),
                AgentSkill(
                    id = "app_launcher",
                    title = "App & Utility Launcher",
                    description = "Launch camera, browser, maps, dialer, or installed apps.",
                    category = SkillCategory.APP_LAUNCHER,
                    actionType = SkillActionType.APP_LAUNCH,
                    triggerKeywords = listOf("open", "launch", "start", "run app", "camera", "browser", "settings")
                ),
                AgentSkill(
                    id = "timer_alarm",
                    title = "Timers & Alarms",
                    description = "Set countdown timers and system alarms on the phone.",
                    category = SkillCategory.PRODUCTIVITY,
                    actionType = SkillActionType.SET_TIMER,
                    triggerKeywords = listOf("timer", "alarm", "countdown", "wake me")
                ),
                AgentSkill(
                    id = "reminder_notes",
                    title = "Quick Reminders",
                    description = "Post persistent notification reminders to your drawer.",
                    category = SkillCategory.PRODUCTIVITY,
                    actionType = SkillActionType.ADD_REMINDER,
                    triggerKeywords = listOf("remind", "note", "remember", "todo")
                ),
                AgentSkill(
                    id = "clipboard_reader",
                    title = "Clipboard Reader",
                    description = "Inspect and summarize current text stored on clipboard.",
                    category = SkillCategory.CLIPBOARD,
                    actionType = SkillActionType.CLIPBOARD_READ,
                    triggerKeywords = listOf("read clipboard", "get clipboard", "paste", "what did i copy")
                ),
                AgentSkill(
                    id = "clipboard_writer",
                    title = "Clipboard Writer",
                    description = "Write generated output, code blocks, or keys to clipboard.",
                    category = SkillCategory.CLIPBOARD,
                    actionType = SkillActionType.CLIPBOARD_WRITE,
                    triggerKeywords = listOf("copy", "copy to clipboard", "save to clipboard")
                ),
                AgentSkill(
                    id = "http_webhook_skill",
                    title = "HTTP Webhook / cURL",
                    description = "Trigger cloud endpoints, Home Assistant webhooks, or REST APIs.",
                    category = SkillCategory.CUSTOM_SCRIPTS,
                    actionType = SkillActionType.HTTP_WEBHOOK,
                    triggerKeywords = listOf("webhook", "http", "curl", "post to", "ping url"),
                    defaultParams = mapOf(
                        "url" to "https://httpbin.org/post",
                        "method" to "POST"
                    )
                ),
                AgentSkill(
                    id = "script_pipeline_skill",
                    title = "Custom Script Runner",
                    description = "Execute multi-step automation pipelines with variable passing.",
                    category = SkillCategory.CUSTOM_SCRIPTS,
                    actionType = SkillActionType.CUSTOM_PIPELINE,
                    triggerKeywords = listOf("pipeline", "script", "automate", "workflow"),
                    customScriptBody = "echo 'OpenAgent automation pipeline initialized'"
                )
            )
        }
    }
}
