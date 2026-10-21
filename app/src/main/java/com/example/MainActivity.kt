package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.benchmark.BenchmarkCategory
import com.example.ui.BenchmarkViewModel
import com.example.ui.screens.DeviceInfoScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TestsScreen
import com.example.ui.screens.UpdaterScreen
import com.example.ui.theme.SkitefiTheme

enum class MainTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    HOME("Главная", Icons.Default.Home),
    TESTS("Тесты", Icons.Default.Assessment),
    HISTORY("История", Icons.Default.History),
    SETTINGS("Настройки", Icons.Default.Settings)
}

enum class AppScreen {
    ONBOARDING,
    MAIN,
    DEVICE_INFO,
    UPDATES
}

class MainActivity : ComponentActivity() {

    private val viewModel: BenchmarkViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()

            SkitefiTheme(
                themeMode = settings.themeMode,
                accentColor = settings.accentColor,
                cardRadiusDp = settings.cardCornerRadiusDp
            ) {
                var currentScreen by remember {
                    mutableStateOf(if (settings.onboardingCompleted) AppScreen.MAIN else AppScreen.ONBOARDING)
                }
                var currentTab by remember { mutableStateOf(MainTab.HOME) }

                // Check intent extras for notification navigation
                LaunchedEffect(intent) {
                    val navigateTo = intent.getStringExtra("NAVIGATE_TO")
                    if (navigateTo == "updates") {
                        currentScreen = AppScreen.UPDATES
                    } else if (navigateTo == "tests") {
                        currentScreen = AppScreen.MAIN
                        currentTab = MainTab.TESTS
                    }
                }

                when (currentScreen) {
                    AppScreen.ONBOARDING -> {
                        OnboardingScreen(
                            onFinish = {
                                viewModel.setOnboardingCompleted(true)
                                currentScreen = AppScreen.MAIN
                            }
                        )
                    }

                    AppScreen.DEVICE_INFO -> {
                        BackHandler { currentScreen = AppScreen.MAIN }
                        DeviceInfoScreen(
                            viewModel = viewModel,
                            onBack = { currentScreen = AppScreen.MAIN }
                        )
                    }

                    AppScreen.UPDATES -> {
                        BackHandler { currentScreen = AppScreen.MAIN }
                        Scaffold(
                            topBar = {
                                TopAppBar(
                                    title = { Text("Обновления и Patcher") },
                                    navigationIcon = {
                                        IconButton(onClick = { currentScreen = AppScreen.MAIN }) {
                                            Icon(
                                                imageVector = Icons.Default.Home,
                                                contentDescription = "Главная"
                                            )
                                        }
                                    }
                                )
                            }
                        ) { innerPadding ->
                            UpdaterScreen(
                                viewModel = viewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }

                    AppScreen.MAIN -> {
                        MainScaffold(
                            currentTab = currentTab,
                            onTabSelected = { currentTab = it },
                            viewModel = viewModel,
                            onNavigateToDeviceInfo = { currentScreen = AppScreen.DEVICE_INFO },
                            onNavigateToUpdates = { currentScreen = AppScreen.UPDATES },
                            onRestartOnboarding = { currentScreen = AppScreen.ONBOARDING }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(
    currentTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    viewModel: BenchmarkViewModel,
    onNavigateToDeviceInfo: () -> Unit,
    onNavigateToUpdates: () -> Unit,
    onRestartOnboarding: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Skitefi",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToUpdates,
                        modifier = Modifier.testTag("top_bar_updates_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = "Обновления",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { onTabSelected(tab) },
                        icon = { Icon(imageVector = tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) },
                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
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
            when (currentTab) {
                MainTab.HOME -> HomeScreen(
                    viewModel = viewModel,
                    onNavigateToTests = { cat ->
                        onTabSelected(MainTab.TESTS)
                        if (cat != null) {
                            viewModel.runBenchmark(cat)
                        }
                    },
                    onNavigateToHistory = { onTabSelected(MainTab.HISTORY) },
                    onNavigateToDeviceInfo = onNavigateToDeviceInfo,
                    onNavigateToUpdates = onNavigateToUpdates
                )

                MainTab.TESTS -> TestsScreen(
                    viewModel = viewModel,
                    onViewResult = { onTabSelected(MainTab.HISTORY) }
                )

                MainTab.HISTORY -> HistoryScreen(
                    viewModel = viewModel
                )

                MainTab.SETTINGS -> SettingsScreen(
                    viewModel = viewModel,
                    onRestartOnboarding = onRestartOnboarding
                )
            }
        }
    }
}
