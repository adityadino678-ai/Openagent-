package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.service.AgentForegroundService
import com.example.ui.components.VoiceModalDialog
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ProvidersScreen
import com.example.ui.screens.SkillsScreen
import com.example.ui.screens.TasksScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentMint
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.viewmodel.AgentViewModel

enum class NavigationScreen(val title: String, val activeIcon: ImageVector, val inactiveIcon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    PROVIDERS("Providers", Icons.Filled.Hub, Icons.Outlined.Hub),
    SKILLS("Skills", Icons.Filled.Bolt, Icons.Outlined.Bolt),
    TASKS("Logs", Icons.Filled.FormatListNumbered, Icons.Outlined.FormatListNumbered),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

class MainActivity : ComponentActivity() {

    private var initialTriggerVoice = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (intent?.action == AgentForegroundService.ACTION_TRIGGER_VOICE) {
            initialTriggerVoice = true
        }

        setContent {
            val agentViewModel: AgentViewModel = viewModel()
            val uiState by agentViewModel.uiState.collectAsState()

            // Permission launcher for Microphone and Notifications
            val permissionsLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { /* Permissions result processed */ }

            LaunchedEffect(Unit) {
                val neededPermissions = mutableListOf<String>()
                if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    neededPermissions.add(Manifest.permission.RECORD_AUDIO)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        neededPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                if (neededPermissions.isNotEmpty()) {
                    permissionsLauncher.launch(neededPermissions.toTypedArray())
                }

                if (initialTriggerVoice) {
                    initialTriggerVoice = false
                    agentViewModel.startVoiceInput()
                }
            }

            MyApplicationTheme(darkTheme = uiState.isDarkMode) {
                var currentScreen by remember { mutableStateOf(NavigationScreen.DASHBOARD) }

                // BackHandler for secondary screens
                if (currentScreen != NavigationScreen.DASHBOARD) {
                    BackHandler {
                        currentScreen = NavigationScreen.DASHBOARD
                    }
                }

                Scaffold(
                    contentWindowInsets = WindowInsets.safeDrawing,
                    containerColor = DarkBackground,
                    bottomBar = {
                        NavigationBar(
                            containerColor = DarkSurface,
                            contentColor = TextSecondary,
                            tonalElevation = androidx.compose.ui.unit.Dp(0f),
                            modifier = Modifier.testTag("main_bottom_nav")
                        ) {
                            NavigationScreen.values().forEach { screen ->
                                val selected = currentScreen == screen
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { currentScreen = screen },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) screen.activeIcon else screen.inactiveIcon,
                                            contentDescription = screen.title
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = screen.title,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = DarkSurface,
                                        selectedTextColor = AccentMint,
                                        indicatorColor = AccentMint,
                                        unselectedIconColor = TextTertiary,
                                        unselectedTextColor = TextTertiary
                                    ),
                                    modifier = Modifier.testTag("nav_tab_${screen.name.lowercase()}")
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentScreen) {
                            NavigationScreen.DASHBOARD -> DashboardScreen(
                                viewModel = agentViewModel,
                                onNavigateToProviders = { currentScreen = NavigationScreen.PROVIDERS },
                                onNavigateToSkills = { currentScreen = NavigationScreen.SKILLS }
                            )
                            NavigationScreen.PROVIDERS -> ProvidersScreen(
                                viewModel = agentViewModel
                            )
                            NavigationScreen.SKILLS -> SkillsScreen(
                                viewModel = agentViewModel
                            )
                            NavigationScreen.TASKS -> TasksScreen(
                                viewModel = agentViewModel
                            )
                            NavigationScreen.SETTINGS -> SettingsScreen(
                                viewModel = agentViewModel
                            )
                        }

                        // Voice Listening Modal Overlay
                        if (uiState.isVoiceModalOpen) {
                            VoiceModalDialog(
                                voiceManager = agentViewModel.voiceManager,
                                recognizedText = uiState.voiceRecognizedText,
                                errorMessage = uiState.voiceError,
                                onDismiss = { agentViewModel.stopVoiceInput() }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == AgentForegroundService.ACTION_TRIGGER_VOICE) {
            initialTriggerVoice = true
        }
    }
}
