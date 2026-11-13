package com.hamza.reelcraft

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hamza.reelcraft.ui.ReelViewModel
import com.hamza.reelcraft.ui.screens.CreateScreen
import com.hamza.reelcraft.ui.screens.ExportScreen
import com.hamza.reelcraft.ui.screens.HomeScreen
import com.hamza.reelcraft.ui.screens.MultiStyleGalleryScreen
import com.hamza.reelcraft.ui.screens.PreviewScreen
import com.hamza.reelcraft.ui.screens.SettingsScreen
import com.hamza.reelcraft.ui.screens.ThemeTestScreen
import com.hamza.reelcraft.ui.theme.ReelCraftTheme

class MainActivity : ComponentActivity() {
    private val viewModel: ReelViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReelCraftTheme {
                val navController = rememberNavController()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        modifier = Modifier.padding(innerPadding),
                        enterTransition = {
                            fadeIn(animationSpec = tween(300)) + slideInHorizontally(
                                initialOffsetX = { 300 },
                                animationSpec = tween(300)
                            )
                        },
                        exitTransition = {
                            fadeOut(animationSpec = tween(300)) + slideOutHorizontally(
                                targetOffsetX = { -300 },
                                animationSpec = tween(300)
                            )
                        },
                        popEnterTransition = {
                            fadeIn(animationSpec = tween(300)) + slideInHorizontally(
                                initialOffsetX = { -300 },
                                animationSpec = tween(300)
                            )
                        },
                        popExitTransition = {
                            fadeOut(animationSpec = tween(300)) + slideOutHorizontally(
                                targetOffsetX = { 300 },
                                animationSpec = tween(300)
                            )
                        }
                    ) {
                        composable("home") {
                            HomeScreen(
                                viewModel = viewModel,
                                onNavigateCreate = { navController.navigate("create") },
                                onNavigateSettings = { navController.navigate("settings") },
                                onNavigateThemeTest = { navController.navigate("theme_test") }
                            )
                        }
                        composable("theme_test") {
                            ThemeTestScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("create") {
                            CreateScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() },
                                onNavigatePreview = { navController.navigate("preview") },
                                onNavigateSettings = { navController.navigate("settings") }
                            )
                        }
                        composable("preview") {
                            PreviewScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() },
                                onNavigateMultiStyle = { navController.navigate("multi_style_gallery") },
                                onNavigateExport = { navController.navigate("export") }
                            )
                        }
                        composable("multi_style_gallery") {
                            MultiStyleGalleryScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() },
                                onSelectStyle = { styleName ->
                                    navController.navigate("export")
                                }
                            )
                        }
                        composable("export") {
                            ExportScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() },
                                onNavigateHome = {
                                    navController.navigate("home") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                },
                                androidContext = this@MainActivity
                            )
                        }
                    }
                }
            }
        }
    }
}
