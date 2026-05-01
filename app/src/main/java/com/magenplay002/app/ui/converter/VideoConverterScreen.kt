package com.magenplay002.app.ui.converter

import android.net.Uri
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.magenplay002.app.MagenPlayApp
import com.magenplay002.app.ui.theme.*
import com.magenplay002.app.util.FFmpegUtils
import com.magenplay002.app.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class AudioFormat(
    val name: String,
    val extension: String,
    val icon: ImageVector,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoConverterScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedVideoPath by remember { mutableStateOf("") }
    var selectedVideoName by remember { mutableStateOf("") }
    var videoDuration by remember { mutableStateOf(0L) }

    var selectedFormat by remember { mutableStateOf("mp3") }
    var selectedBitrate by remember { mutableStateOf("192k") }

    var isProcessing by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var statusMessage by remember { mutableStateOf("") }
    var isCompleted by remember { mutableStateOf(false) }
    var outputFilePath by remember { mutableStateOf("") }

    val audioFormats = listOf(
        AudioFormat("MP3", "mp3", Icons.Default.Audiotrack, AnimeBlue),
        AudioFormat("AAC", "aac", Icons.Default.GraphicEq, AnimePink),
        AudioFormat("WAV", "wav", Icons.Default.Waves, AnimePurple),
        AudioFormat("FLAC", "flac", Icons.Default.HighQuality, AnimeOrange),
        AudioFormat("OGG", "ogg", Icons.Default.MusicNote, AnimeSuccess),
        AudioFormat("M4A", "m4a", Icons.Default.QueueMusic, AnimeWarning)
    )

    val bitrateOptions = listOf("128k", "192k", "256k", "320k")

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val path = getRealPathFromUri(context, it)
            if (path != null) {
                selectedVideoPath = path
                selectedVideoName = FileUtils.getFileName(context, it)
                val info = FFmpegUtils.getMediaInfo(path)
                if (info != null) {
                    videoDuration = info.duration
                }
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
                        "VIDEO TO MP3",
                        style = MaterialTheme.typography.headlineMedium,
                        color = AnimePurple
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
                AnimeConverterButton(
                    text = if (selectedVideoPath.isEmpty()) "Select Video File" else "Change Video",
                    icon = Icons.Default.VideoFile,
                    color = AnimePurple,
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Movie,
                                    contentDescription = null,
                                    tint = AnimePurple,
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

                    // Output Format Selection
                    Text(
                        "Output Format",
                        style = MaterialTheme.typography.titleLarge,
                        color = AnimeTextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Format Grid - Row 1
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        audioFormats.take(3).forEach { format ->
                            FormatChip(
                                format = format,
                                isSelected = selectedFormat == format.extension,
                                onClick = { selectedFormat = format.extension },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    // Format Grid - Row 2
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        audioFormats.drop(3).forEach { format ->
                            FormatChip(
                                format = format,
                                isSelected = selectedFormat == format.extension,
                                onClick = { selectedFormat = format.extension },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Bitrate Selection
                    Text(
                        "Audio Bitrate",
                        style = MaterialTheme.typography.titleLarge,
                        color = AnimeTextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        bitrateOptions.forEach { bitrate ->
                            BitrateChip(
                                bitrate = bitrate,
                                isSelected = selectedBitrate == bitrate,
                                onClick = { selectedBitrate = bitrate },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Convert Button
                    AnimeConverterButton(
                        text = "Convert to ${selectedFormat.uppercase()}",
                        icon = Icons.Default.Transform,
                        color = AnimePurple,
                        enabled = !isProcessing,
                        onClick = {
                            scope.launch {
                                isProcessing = true
                                isCompleted = false
                                statusMessage = "Converting..."
                                progress = 0f

                                val outputDir = MagenPlayApp.getAudioOutputDirectory()
                                FFmpegUtils.ensureOutputDir(outputDir)

                                val outputName = "${FileUtils.getFileNameWithoutExtension(selectedVideoName)}.${selectedFormat}"
                                outputFilePath = "$outputDir/$outputName"

                                withContext(Dispatchers.IO) {
                                    FFmpegUtils.convertVideoToMp3(
                                        inputPath = selectedVideoPath,
                                        outputPath = outputFilePath,
                                        bitrate = selectedBitrate,
                                        onProgress = { p ->
                                            progress = p.toFloat()
                                        },
                                        onComplete = { success, error ->
                                            isProcessing = false
                                            if (success) {
                                                isCompleted = true
                                                statusMessage = "Conversion complete!"
                                            } else {
                                                statusMessage = "Error: $error"
                                            }
                                        }
                                    )
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
                            color = AnimePurple,
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Conversion complete!",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = AnimeSuccess
                                    )
                                    Text(
                                        "Saved to: $outputFilePath",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AnimeTextSecondary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FormatChip(
    format: AudioFormat,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) format.color.copy(alpha = 0.2f) else AnimeDarkCard
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                format.icon,
                contentDescription = format.name,
                tint = if (isSelected) format.color else AnimeTextTertiary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                format.name,
                style = MaterialTheme.typography.labelMedium,
                color = if (isSelected) format.color else AnimeTextTertiary
            )
        }
    }
}

@Composable
fun BitrateChip(
    bitrate: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) AnimeBlue.copy(alpha = 0.2f) else AnimeDarkCard
        ),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                bitrate,
                style = MaterialTheme.typography.labelMedium,
                color = if (isSelected) AnimeBlue else AnimeTextTertiary
            )
        }
    }
}

@Composable
fun AnimeConverterButton(
    text: String,
    icon: ImageVector,
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
    val projection = arrayOf(android.provider.MediaStore.Video.Media.DATA)
    val cursor = context.contentResolver.query(uri, projection, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val columnIndex = it.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media.DATA)
            return it.getString(columnIndex)
        }
    }
    return null
}
