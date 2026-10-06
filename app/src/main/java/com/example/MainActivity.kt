package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.model.AppThemeMode
import com.example.ui.screens.BatchScreen
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PresetsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SignatureScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.StudioTab
import com.example.ui.viewmodel.UtilityViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: UtilityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val isDark = when (uiState.appSettings.themeMode) {
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
                else -> isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = isDark) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = "home"
                    ) {
                        composable("home") {
                            HomeScreen(
                                viewModel = viewModel,
                                onNavigateToEditor = { tab ->
                                    viewModel.selectTab(tab)
                                    navController.navigate("editor")
                                },
                                onNavigateToSignature = {
                                    navController.navigate("signature")
                                },
                                onNavigateToBatch = {
                                    navController.navigate("batch")
                                },
                                onNavigateToHistory = {
                                    navController.navigate("history")
                                },
                                onNavigateToPresets = {
                                    navController.navigate("presets")
                                },
                                onNavigateToFavorites = {
                                    navController.navigate("favorites")
                                },
                                onNavigateToSettings = {
                                    navController.navigate("settings")
                                }
                            )
                        }

                        composable("settings") {
                            SettingsScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("editor") {
                            EditorScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("signature") {
                            SignatureScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("batch") {
                            BatchScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("history") {
                            HistoryScreen(
                                viewModel = viewModel,
                                onNavigateToEditor = {
                                    navController.navigate("editor")
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("presets") {
                            PresetsScreen(
                                viewModel = viewModel,
                                onNavigateToEditor = { tab ->
                                    viewModel.selectTab(tab)
                                    navController.navigate("editor")
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("favorites") {
                            FavoritesScreen(
                                viewModel = viewModel,
                                onNavigateToEditor = { tab ->
                                    viewModel.selectTab(tab)
                                    navController.navigate("editor")
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}

