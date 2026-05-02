package com.magenplay002.app.ui.editor

import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.magenplay002.app.ui.theme.*
import com.magenplay002.app.util.FFmpegUtils
import com.magenplay002.app.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoEditorScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedVideoPath by remember { mutableStateOf("") }
    var selectedVideoName by remember { mutableStateOf("") }
    var videoDuration by remember { mutableStateOf(0L) }

    // Trim range in milliseconds
    var startMs by remember { mutableStateOf(0L) }
    var endMs by remember { mutableStateOf(0L) }

    var isProcessing by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var statusMessage by remember { mutableStateOf("") }
    var isCompleted by remember { mutableStateOf(false) }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val path = getRealPathFromUri(context, it)
                if (path != null) {
                    selectedVideoPath = path
                    selectedVideoName = FileUtils.getFileName(context, it)
                    val info = FFmpegUtils.getMediaInfo(path)
                    if (info != null) {
                        videoDuration = info.duration
                        startMs = 0L
                        endMs = info.duration
                    }
                } else {
                    // Try to copy file to cache for SAF URIs
                    val copiedPath = copyUriToCache(context, it)
                    if (copiedPath != null) {
                        selectedVideoPath = copiedPath
                        selectedVideoName = FileUtils.getFileName(context, it)
                        val info = FFmpegUtils.getMediaInfo(copiedPath)
                        if (info != null) {
                            videoDuration = info.duration
                            startMs = 0L
                            endMs = info.duration
                        }
                    }
                }
            } catch (e: Exception) {
                statusMessage = "Error selecting video: ${e.message}"
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AnimeDarkBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Top Bar
            TopAppBar(
                title = {
                    Text(
                        "VIDEO EDITOR",
                        style = MaterialTheme.typography.headlineMedium,
                        color = AnimePink
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = AnimeTextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AnimeDarkBg.copy(alpha = 0.95f)
                )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Select Video Button
                AnimeButton(
                    text = if (selectedVideoPath.isEmpty()) "Select Video" else "Change Video",
                    icon = Icons.Default.VideoFile,
                    color = AnimePink,
                    onClick = { videoPicker.launch("video/*") }
                )

                if (selectedVideoPath.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Video Info Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = AnimeDarkCard)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Movie,
                                    contentDescription = null,
                                    tint = AnimePink,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    selectedVideoName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = AnimeTextPrimary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row {
                                Text(
                                    "Duration: ${FileUtils.formatDuration(videoDuration)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AnimeTextSecondary
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    "Size: ${FileUtils.formatFileSize(File(selectedVideoPath).length())}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AnimeTextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Trim Controls
                    Text(
                        "Trim Video",
                        style = MaterialTheme.typography.titleLarge,
                        color = AnimeTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Select the start and end time",
                        style = MaterialTheme.typography.bodySmall,
                        color = AnimeTextTertiary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Visual Timeline
                    AnimeTimeline(
                        duration = videoDuration,
                        startMs = startMs,
                        endMs = endMs,
                        onStartChange = { startMs = it },
                        onEndChange = { endMs = it }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Time Display
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TimeDisplay("Start", FileUtils.formatDuration(startMs), AnimeBlue)
                        TimeDisplay("End", FileUtils.formatDuration(endMs), AnimePink)
                        TimeDisplay("Duration", FileUtils.formatDuration(endMs - startMs), AnimePurple)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Trim Button
                    AnimeButton(
                        text = "Trim Video",
                        icon = Icons.Default.ContentCut,
                        color = AnimePink,
                        enabled = !isProcessing && startMs < endMs,
                        onClick = {
                            scope.launch {
                                isProcessing = true
                                isCompleted = false
                                statusMessage = "Processing..."
                                progress = 0f

                                try {
                                    val outputDir = getOutputDirectory()
                                    FFmpegUtils.ensureOutputDir(outputDir)

                                    val outputName = "${FileUtils.getFileNameWithoutExtension(selectedVideoName)}_trimmed.mp4"
                                    val outputPath = "$outputDir/$outputName"

                                    withContext(Dispatchers.IO) {
                                        FFmpegUtils.trimVideo(
                                            inputPath = selectedVideoPath,
                                            outputPath = outputPath,
                                            startTimeMs = startMs,
                                            endTimeMs = endMs,
                                            onProgress = { p ->
                                                progress = p.toFloat()
                                            },
                                            onComplete = { success, error ->
                                                isProcessing = false
                                                if (success) {
                                                    isCompleted = true
                                                    statusMessage = "Video trimmed successfully! Saved to: $outputPath"
                                                } else {
                                                    statusMessage = "Error: $error"
                                                }
                                            }
                                        )
                                    }
                                } catch (e: Exception) {
                                    isProcessing = false
                                    statusMessage = "Error: ${e.message}"
                                }
                            }
                        }
                    )

                    // Progress
                    if (isProcessing) {
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = AnimePink,
                            trackColor = AnimeDarkElevated
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            statusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = AnimeTextSecondary
                        )
                    }

                    if (isCompleted) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = AnimeSuccess.copy(alpha = 0.15f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = AnimeSuccess,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Video trimmed successfully!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AnimeSuccess
                                )
                            }
                        }
                    }

                    if (statusMessage.isNotEmpty() && !isCompleted && !isProcessing) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = AnimeError.copy(alpha = 0.15f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = null,
                                    tint = AnimeError,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    statusMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AnimeError,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnimeTimeline(
    duration: Long,
    startMs: Long,
    endMs: Long,
    onStartChange: (Long) -> Unit,
    onEndChange: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AnimeDarkCard)
            .padding(16.dp)
    ) {
        // Timeline bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(AnimeDarkElevated)
        ) {
            // Selected range
            val startPos = if (duration > 0) startMs.toFloat() / duration else 0f
            val endPos = if (duration > 0) endMs.toFloat() / duration else 1f

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(
                        start = (startPos * 280).dp,
                        end = ((1f - endPos) * 280).dp
                    )
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(AnimePink.copy(alpha = 0.4f), AnimePurple.copy(alpha = 0.4f))
                        )
                    )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Start slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Start", style = MaterialTheme.typography.labelMedium, color = AnimeBlue)
            Spacer(modifier = Modifier.width(8.dp))
            Slider(
                value = startMs.toFloat(),
                onValueChange = { onStartChange(it.toLong()) },
                valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = AnimeBlue,
                    activeTrackColor = AnimeBlue
                )
            )
        }

        // End slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("End", style = MaterialTheme.typography.labelMedium, color = AnimePink)
            Spacer(modifier = Modifier.width(8.dp))
            Slider(
                value = endMs.toFloat(),
                onValueChange = { onEndChange(it.toLong()) },
                valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = AnimePink,
                    activeTrackColor = AnimePink
                )
            )
        }
    }
}

@Composable
fun TimeDisplay(label: String, time: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = AnimeTextTertiary
        )
        Text(
            time,
            style = MaterialTheme.typography.titleSmall,
            color = color
        )
    }
}

@Composable
fun AnimeButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color.copy(alpha = 0.2f),
            contentColor = color,
            disabledContainerColor = AnimeDarkElevated,
            disabledContentColor = AnimeTextTertiary
        )
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

private fun getRealPathFromUri(context: android.content.Context, uri: Uri): String? {
    try {
        val projection = arrayOf(android.provider.MediaStore.Video.Media.DATA)
        val cursor = context.contentResolver.query(uri, projection, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val columnIndex = it.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media.DATA)
                val path = it.getString(columnIndex)
                if (path != null && File(path).exists()) {
                    return path
                }
            }
        }
    } catch (e: Exception) {
        // Fallback for SAF URIs
    }
    return null
}

private fun copyUriToCache(context: android.content.Context, uri: Uri): String? {
    return try {
        val fileName = FileUtils.getFileName(context, uri)
        val tempFile = File(context.cacheDir, fileName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
        tempFile.absolutePath
    } catch (e: Exception) {
        null
    }
}

private fun getOutputDirectory(): String {
    return Environment.getExternalStoragePublicDirectory(
        Environment.DIRECTORY_MOVIES
    ).absolutePath + "/MagenPlay"
}
