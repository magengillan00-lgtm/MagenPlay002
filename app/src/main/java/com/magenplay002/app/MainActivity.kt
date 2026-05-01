package com.magenplay002.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.magenplay002.app.ui.converter.VideoConverterScreen
import com.magenplay002.app.ui.editor.VideoEditorScreen
import com.magenplay002.app.ui.player.VideoPlayerScreen
import com.magenplay002.app.ui.player.VideoListScreen
import com.magenplay002.app.ui.theme.MagenPlay002Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()

        setContent {
            MagenPlay002Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MagenPlayAppContent()
                }
            }
        }
    }
}

@Composable
fun MagenPlayAppContent() {
    val navController = rememberNavController()
    var selectedTab by rememberSaveable { mutableStateOf(0) }

    NavHost(
        navController = navController,
        startDestination = "main"
    ) {
        composable("main") {
            MainScreen(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                onVideoClick = { videoPath ->
                    navController.navigate("player/$videoPath")
                }
            )
        }
        composable(
            route = "player/{videoPath}",
            arguments = listOf(navArgument("videoPath") { type = NavType.StringType })
        ) { backStackEntry ->
            val videoPath = backStackEntry.arguments?.getString("videoPath") ?: ""
            VideoPlayerScreen(
                videoPath = videoPath,
                onBack = { navController.popBackStack() }
            )
        }
        composable("editor") {
            VideoEditorScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable("converter") {
            VideoConverterScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun MainScreen(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onVideoClick: (String) -> Unit
) {
    when (selectedTab) {
        0 -> VideoListScreen(onVideoClick = onVideoClick)
        1 -> VideoEditorScreen(onBack = { onTabSelected(0) })
        2 -> VideoConverterScreen(onBack = { onTabSelected(0) })
    }
}
