package com.example.ui.components

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.example.ui.ActiveFile
import com.example.ui.MainViewModel

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerView(
    viewModel: MainViewModel,
    activeFile: ActiveFile,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isPlaying by viewModel.isPlaying.collectAsState()
    val position by viewModel.playbackPosition.collectAsState()
    val duration by viewModel.mediaDuration.collectAsState()
    val speed by viewModel.playbackSpeed.collectAsState()
    
    var showSpeedMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Custom Glass Top Bar
        GlassmorphicCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            cornerRadius = 16.dp,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.closeActiveFile() },
                    modifier = Modifier.testTag("video_back_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBack,
                        contentDescription = "Go Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activeFile.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Video Player • ${(activeFile.size / (1024 * 1024))} MB",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = { viewModel.toggleFavorite(activeFile.uriString) },
                    modifier = Modifier.testTag("video_fav_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FavoriteBorder, // Always accessible state
                        contentDescription = "Add to Favorites",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Video Player Box
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = viewModel.exoPlayer
                        useController = true
                        setShowNextButton(false)
                        setShowPreviousButton(false)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Custom Glass Playback Controls Panel
        GlassmorphicCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Slider Progress Bar
                val progress = if (duration > 0) position.toFloat() / duration.toFloat() else 0f
                Slider(
                    value = progress,
                    onValueChange = { percent ->
                        val targetMs = (percent * duration).toLong()
                        viewModel.seekTo(targetMs)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("video_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF21005D),
                        activeTrackColor = Color(0xFF21005D),
                        inactiveTrackColor = Color(0xFFEADDFF)
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(position),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = formatTime(duration),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Speed Selector
                    Box {
                        Button(
                            onClick = { showSpeedMenu = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE8DEF8),
                                contentColor = Color(0xFF1D192B)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("video_speed_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "${speed}x", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        DropdownMenu(
                            expanded = showSpeedMenu,
                            onDismissRequest = { showSpeedMenu = false },
                            modifier = Modifier
                                .background(Color(0xE0161B22), shape = RoundedCornerShape(12.dp))
                                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)), shape = RoundedCornerShape(12.dp))
                        ) {
                            listOf(0.5f, 1.0f, 1.5f, 2.0f).forEach { speedVal ->
                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            text = "${speedVal}x" + if (speedVal == 1.0f) " (Normal)" else "",
                                            color = if (speed == speedVal) Color(0xFF58A6FF) else Color.White,
                                            fontWeight = if (speed == speedVal) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        viewModel.setPlaybackSpeed(speedVal)
                                        showSpeedMenu = false
                                    },
                                    colors = MenuDefaults.itemColors(
                                        textColor = Color.White,
                                        leadingIconColor = Color.White
                                    )
                                )
                            }
                        }
                    }

                    // Play Pause Main Circular Control
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFD0BCFF))
                            .clickable { viewModel.togglePlayPause() }
                            .testTag("video_play_pause_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Clear else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color(0xFF21005D),
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Seek forward 10 seconds
                    IconButton(
                        onClick = { viewModel.seekTo((position + 10000).coerceAtMost(duration)) },
                        modifier = Modifier.testTag("video_forward_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh, // Standard icon representing skip/refresh
                            contentDescription = "Forward 10s",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
