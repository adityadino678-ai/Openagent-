# OpenAgent: Minimalist Multi-Provider Android AI Agent

OpenAgent is a minimalist, developer-grade AI agent designed after OpenCode. It gives users complete control over their mobile device through autonomous skills, configurable LLM providers, encrypted local storage, and persistent background execution with lock-screen quick controls.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural decisions incorporate your Phase 1 choices and define the core operational model for OpenAgent:

- **Confirmed Provider Strategy**: Pre-configure all major AI providers (Google Gemini, OpenAI, Anthropic Claude, Groq, Mistral, DeepSeek, and custom self-hosted/Ollama endpoints). Every provider profile includes a 1-tap direct link to the provider's official API key console (`aistudio.google.com`, `platform.openai.com`, etc.) so you can generate and paste credentials in seconds.
- **Confirmed Automation Scope**: Bundle and download skills covering system phone control (flashlight, volume, battery telemetry, connectivity), app launcher intents, alarms/reminders, clipboard management, and custom script execution (HTTP webhooks, JSON transform, task pipelines).
- **Confirmed Background Execution**: A lightweight Android Foreground Service with a persistent lock screen notification, quick-toggle voice input listener, task status badges, and battery-safe execution when the screen is turned off or locked.
- **End-to-End Local Security**: Encrypt all personal configuration data, API tokens, custom scripts, and session logs using hardware-backed AES-256-GCM encryption with Android Keystore.

---

## 1. Overview & Core Concept

OpenAgent brings the transparency, scriptability, and speed of modern AI developer tools (like OpenCode) directly into an Android handheld experience. Rather than locking users into a single proprietary LLM or closed ecosystem, OpenAgent acts as an open agent orchestrator that can query whichever provider you configure and translate user intent—via text or voice—into concrete device actions and automations.

### Key Capabilities
- **Universal Provider Hub**: Switch seamlessly between Gemini, OpenAI, Claude, Groq, Mistral, DeepSeek, or local Ollama instances, with live status checks and direct links to API key dashboards.
- **Modular Skills Marketplace**: Browse, enable, edit, or download specialized automation skills that run natively on Android (system actions, app launchers, clipboard, alarms, webhooks, and custom script blocks).
- **Hands-Free Voice Input & Lock Screen Control**: Dictate commands on the fly with speech recognition; view active agent tasks from the notification drawer even when the device is locked.
- **Widget-Based System Dashboard**: Real-time status cards displaying active engine health, memory state, battery guard status, background service toggles, and recent automation logs.
- **Zero-Cloud Configuration Storage**: All provider keys, scripts, and logs stay strictly on-device in AES-256-GCM encrypted persistence.

---

## 2. User Experience & Visual Design

### Aesthetic Direction: Modern Minimalist Terminal
OpenAgent adopts an ultra-clean, utilitarian, dark-mode-first aesthetic inspired by high-end CLI tools and developer workstations:
- **Atmosphere**: Deep matte background (`#0D1117`), dark slate card surfaces (`#161B22`), crisp border dividers (`#30363D`), and electric emerald/cyan accent glows (`#2EA043` / `#58A6FF`).
- **Typography & Hierarchy**: Monospace metadata headers (e.g. `PROVIDER: ACTIVE`, `STATUS: IDLE`, `LATENCY: 142ms`) paired with clean sans-serif body text for high readability and fast scannability.
- **Micro-Interactions**: Smooth spring animations for expandable skill cards, pulsing audio ring when listening to voice input, and live streaming execution chips for running tasks.

### Key User Flows

```
┌─────────────────────────────────────────────────────────────┐
│ 1. Home Dashboard                                           │
│    • Status Bar (Active Provider, Battery Shield, Service)  │
│    • Voice Command Quick-Bar (Tap to speak / Type command)  │
│    • Live Telemetry Widgets (RAM, Battery, Active Tasks)    │
│    • Recent Automation Feed & Quick Actions                 │
└──────────────────────────────┬──────────────────────────────┘
                               │
       ┌───────────────────────┼───────────────────────┐
       ▼                       ▼                       ▼
┌──────────────┐       ┌──────────────┐        ┌──────────────┐
│ Provider Hub │       │  Skills Hub  │        │ Background   │
│ • API Keys   │       │ • System Ctl │        │   Service    │
│ • Key Links  │       │ • App Launch │        │ • Lockscreen │
│ • Model Pick │       │ • Scripts    │        │ • Voice Wake │
│ • Test Ping  │       │ • Download   │        │ • Batt Guard │
└──────────────┘       └──────────────┘        └──────────────┘
```

1. **Quick Command Flow**:
   - Tap the microphone button or type a prompt like `"Set timer for 15 mins and turn on flashlight"`.
   - The agent parses the intent using the active AI provider, matches corresponding local skills, executes them sequentially, and announces or prints completion status.
2. **Provider Setup Flow**:
   - Open Provider Settings -> Select OpenAI/Gemini/Groq -> Tap *"Get Key from Portal"* (opens official key generation page in browser) -> Paste key into encrypted input field -> Tap *"Verify Connection"*.
3. **Skills Marketplace & Editor**:
   - Browse built-in skills (System Controls, App Launcher, Reminders, Clipboard, Webhooks).
   - Toggle skills on/off or create a custom skill with intent triggers and action payloads.
4. **Background & Lock Screen Flow**:
   - Enable Background Service. A quiet, battery-optimized foreground notification appears.
   - Even when screen is locked, tap *"Quick Voice"* from the notification to speak a command or trigger scheduled automations.

---

## 3. Key Product Decisions & Trade-Offs

### Decision 1: Direct Multi-Provider REST Client vs Heavy Cloud Gateway
- **Chosen Approach**: Built-in HTTP client (OkHttp / Retrofit / Kotlinx Serialization) communicating directly from the device to standard provider endpoints (OpenAI-compatible endpoints, Anthropic Messages API, and Gemini REST API).
- **Why**: Zero middleman, absolute privacy. User keys never touch any intermediary proxy server.
- **Trade-Off**: Device handles JSON parsing and formatting; kept lightweight via direct streaming.

### Decision 2: Hardware-Backed AES-256 Encryption for Keys & Data
- **Chosen Approach**: Android Keystore backed MasterKey with AES-256-GCM cipher for all credentials, custom automation scripts, and command logs.
- **Why**: Prevents plain-text leakage in root or ADB backups, fulfilling the strict end-to-end local security requirement.
- **Trade-Off**: Slightly more initialization ceremony on first launch, but completely transparent to the user thereafter.

### Decision 3: Foreground Service with Battery Health Shield
- **Chosen Approach**: Foreground service with `FOREGROUND_SERVICE_SPECIAL_USE` / `FOREGROUND_SERVICE_DATA_SYNC` and low-power interval polling rather than an aggressive infinite CPU wakelock.
- **Why**: Ensures Android OS does not kill the agent when the screen is turned off while preserving battery life and complying with Android 14+ background execution policies.

---

## 4. Technical Architecture & Data Strategy

### System Architecture Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Jetpack Compose UI Layer                        │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────────┐  │
│  │ DashboardWidget  │  │   ProviderHub    │  │     SkillsMarket     │  │
│  │ (Status/Voice)   │  │ (Keys/WebLinks)  │  │ (Executors/Downloader│  │
│  └─────────┬────────┘  └────────┬─────────┘  └──────────┬───────────┘  │
└────────────┼────────────────────┼───────────────────────┼──────────────┘
             ▼                    ▼                       ▼
┌────────────────────────────────────────────────────────────────────────┐
│                         OpenAgent ViewModel                            │
│    • Command Dispatcher       • Voice State Coordinator                │
│    • Active Provider Selector • Skill Pipeline Engine                  │
└────────────────────────────────────┬───────────────────────────────────┘
                                     │
         ┌───────────────────────────┴───────────────────────────┐
         ▼                                                       ▼
┌─────────────────────────────────┐             ┌────────────────────────────────┐
│      Execution Engine           │             │     Encrypted Storage Layer    │
│  ┌───────────────────────────┐  │             │  ┌──────────────────────────┐  │
│  │ Provider REST Clients     │  │             │  │ AES-256 Keystore Storage │  │
│  │ (Gemini/OpenAI/Anthropic) │  │             │  │ (API Keys, Skill Config) │  │
│  ├───────────────────────────┤  │             │  └──────────────────────────┘  │
│  │ Built-in Skills Toolkit   │  │             │  ┌──────────────────────────┐  │
│  │ (Flashlight, Apps, Clips) │  │             │  │ Task History & Memory    │  │
│  └───────────────────────────┘  │             │  │ (Encrypted SQLite/Room)  │  │
└────────────────┬────────────────┘             └────────────────────────────────┘
                 │
                 ▼
┌────────────────────────────────────────────────────────────────────────┐
│              Background Service & System Hardware Bridge               │
│    • Foreground Service Notification with Lock Screen Action Buttons   │
│    • Android SpeechRecognizer Speech-to-Text Bridge                    │
│    • Battery Health Monitor & Partial WakeLock Governor                │
└────────────────────────────────────────────────────────────────────────┘
```

### Core Entities & Data Models
- **`ProviderConfig`**: ID, Name, Base URL, Encrypted API Key, Model Name, Official Key URL, Active status, Ping latency.
- **`AgentSkill`**: ID, Title, Description, Category (System, Apps, Productivity, Scripts), Icon, Enabled status, Actions (Intent, Shell, Webhook, Transform), Parameters schema.
- **`AgentTask`**: ID, Timestamp, Input Command, Provider Used, Trigger Source (Voice, Dashboard, Background Service), Executed Skills, Status (Success, Failed, Running), Result Summary.
- **`ServiceState`**: IsRunning, VoiceListening, BatteryLevel, BatteryOptimizationActive, QueuedTasksCount.

### State Transitions & Interaction Flow
1. **Voice Input Event**:
   - User triggers mic -> `SpeechRecognizerManager` starts listening -> Live audio level indicator animates -> Speech converted to text.
2. **Intent Parsing & Skill Routing**:
   - Prompt sent to active Provider via `ProviderRepository` -> Provider returns structured action JSON or tool call -> `SkillDispatcher` checks user permissions and executes action (e.g. `CameraManager.setTorchMode`, `Intent.ACTION_VIEW`, `ClipboardManager.setPrimaryClip`).
3. **Background Notification Bridge**:
   - Foreground notification updates with live status and action pending -> User can tap "Stop Task" or "Voice Input" directly from notification without unlocking device.
