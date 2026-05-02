package com.magenplay002.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.magenplay002.app.ui.converter.VideoConverterScreen
import com.magenplay002.app.ui.editor.VideoEditorScreen
import com.magenplay002.app.ui.player.VideoListScreen
import com.magenplay002.app.ui.player.VideoPlayerScreen
import com.magenplay002.app.ui.theme.MagenPlay002Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
    val context = LocalContext.current
    val navController = rememberNavController()
    var selectedTab by rememberSaveable { mutableStateOf(0) }

    // Permission handling
    var hasStoragePermission by remember {
        mutableStateOf(checkStoragePermission(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasStoragePermission = permissions.values.all { it }
        if (!hasStoragePermission) {
            Toast.makeText(context, "Storage permission is required to access videos", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasStoragePermission) {
            requestStoragePermissions(permissionLauncher)
        }
    }

    if (!hasStoragePermission) {
        // Show permission request screen
        PermissionRequestScreen(
            onRequestPermission = {
                requestStoragePermissions(permissionLauncher)
            }
        )
        return
    }

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
            arguments = listOf(androidx.navigation.navArgument("videoPath") { type = androidx.navigation.NavType.StringType })
        ) { backStackEntry ->
            val videoPath = backStackEntry.arguments?.getString("videoPath") ?: ""
            VideoPlayerScreen(
                videoPath = videoPath,
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
        0 -> VideoListScreen(
            onVideoClick = onVideoClick,
            selectedTab = selectedTab,
            onTabSelected = onTabSelected
        )
        1 -> VideoEditorScreen(onBack = { onTabSelected(0) })
        2 -> VideoConverterScreen(onBack = { onTabSelected(0) })
    }
}

@Composable
fun PermissionRequestScreen(
    onRequestPermission: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.VideoLibrary,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Storage Permission Required",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Magen Play needs access to your storage to find and play videos on your device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            ) {
                Text("Grant Permission", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

private fun checkStoragePermission(context: android.content.Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_MEDIA_VIDEO
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }
}

private fun requestStoragePermissions(
    launcher: androidx.activity.compose.ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        launcher.launch(
            arrayOf(
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        )
    } else {
        launcher.launch(
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        )
    }
}

@Composable
private fun rememberSaveable(initial: () -> Int): MutableState<Int> {
    return remember { mutableStateOf(initial()) }
}
