package com.hamza.reelcraft

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import com.hamza.reelcraft.ui.screens.PreviewScreen
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
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("home") {
                            HomeScreen(viewModel = viewModel) {
                                navController.navigate("create")
                            }
                        }
                        composable("create") {
                            CreateScreen(viewModel = viewModel) {
                                navController.navigate("preview")
                            }
                        }
                        composable("preview") {
                            PreviewScreen(viewModel = viewModel) {
                                navController.navigate("export")
                            }
                        }
                        composable("export") {
                            ExportScreen(viewModel = viewModel, androidContext = this@MainActivity)
                        }
                    }
                }
            }
        }
    }
}
