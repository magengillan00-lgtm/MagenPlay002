package com.magenplay002.app.ui.player

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.magenplay002.app.ui.theme.*
import com.magenplay002.app.util.FileUtils
import com.magenplay002.app.util.VideoItem
import com.magenplay002.app.viewmodel.VideoListViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoListScreen(
    onVideoClick: (String) -> Unit,
    viewModel: VideoListViewModel = viewModel()
) {
    val videos by viewModel.videos.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadVideos(context)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AnimeDarkBg)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top App Bar with Anime Gradient
            TopAppBar(
                title = {
                    Text(
                        "MAGEN PLAY",
                        style = MaterialTheme.typography.headlineMedium,
                        color = AnimeBlue
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AnimeDarkBg.copy(alpha = 0.95f),
                    titleContentColor = AnimeBlue
                ),
                actions = {
                    IconButton(onClick = { /* Search */ }) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search",
                            tint = AnimeTextSecondary
                        )
                    }
                    IconButton(onClick = { /* Settings */ }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = AnimeTextSecondary
                        )
                    }
                }
            )

            // Tab Row - Anime Style
            AnimeTabRow(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                tabs = listOf("Player", "Editor", "MP3 Converter")
            )

            // Video Grid/List
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = AnimeBlue,
                        modifier = Modifier.size(48.dp)
                    )
                }
            } else {
                val filteredVideos = if (searchQuery.isEmpty()) {
                    videos
                } else {
                    videos.filter { it.name.contains(searchQuery, ignoreCase = true) }
                }

                if (filteredVideos.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.VideoLibrary,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp),
                                tint = AnimeTextTertiary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "No videos found",
                                style = MaterialTheme.typography.titleMedium,
                                color = AnimeTextSecondary
                            )
                            Text(
                                "Videos on your device will appear here",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AnimeTextTertiary
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredVideos, key = { it.id }) { video ->
                            AnimeVideoCard(
                                video = video,
                                onClick = { onVideoClick(video.path) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnimeTabRow(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    tabs: List<String>
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(AnimeDarkCard),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        tabs.forEachIndexed { index, title ->
            val isSelected = selectedTab == index
            val tabColor = when (index) {
                0 -> AnimeBlue
                1 -> AnimePink
                2 -> AnimePurple
                else -> AnimeBlue
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) tabColor.copy(alpha = 0.2f)
                        else Color.Transparent
                    )
                    .clickable { onTabSelected(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val icon = when (index) {
                        0 -> Icons.Default.PlayCircleFilled
                        1 -> Icons.Default.ContentCut
                        2 -> Icons.Default.Audiotrack
                        else -> Icons.Default.PlayCircleFilled
                    }
                    Icon(
                        icon,
                        contentDescription = title,
                        tint = if (isSelected) tabColor else AnimeTextTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        title,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) tabColor else AnimeTextTertiary
                    )
                    if (isSelected) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .width(20.dp)
                                .height(2.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(tabColor)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnimeVideoCard(
    video: VideoItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = AnimeDarkCard
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(width = 140.dp, height = 80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AnimeDarkElevated),
                contentAlignment = Alignment.Center
            ) {
                if (video.thumbnail != null) {
                    AsyncImage(
                        model = video.thumbnail,
                        contentDescription = video.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.PlayCircleFilled,
                        contentDescription = null,
                        tint = AnimeBlue,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Duration overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        FileUtils.formatDuration(video.duration),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Video Info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    video.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = AnimeTextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    if (video.resolution.isNotEmpty()) {
                        Text(
                            video.resolution,
                            style = MaterialTheme.typography.labelSmall,
                            color = AnimeBlue
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        FileUtils.formatFileSize(video.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = AnimeTextTertiary
                    )
                }
            }

            // Play button
            IconButton(onClick = onClick) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = AnimeBlue,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun VideoPlayerScreen(
    videoPath: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(Uri.parse(videoPath))
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
    }

    var showControls by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    LaunchedEffect(showControls) {
        if (showControls) {
            delay(3000)
            showControls = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Video Player
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .clickable { showControls = !showControls }
        )

        // Controls overlay
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    videoPath.substringAfterLast("/"),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Center play/pause
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            IconButton(
                onClick = {
                    if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                },
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    if (exoPlayer.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (exoPlayer.isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}
