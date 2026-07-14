package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.example.ui.ActiveFile
import com.example.ui.MainViewModel

@Composable
fun AudioPlayerView(
    viewModel: MainViewModel,
    activeFile: ActiveFile,
    modifier: Modifier = Modifier
) {
    val isPlaying by viewModel.isPlaying.collectAsState()
    val position by viewModel.playbackPosition.collectAsState()
    val duration by viewModel.mediaDuration.collectAsState()
    val speed by viewModel.playbackSpeed.collectAsState()

    var showSpeedMenu by remember { mutableStateOf(false) }

    // Disk Rotation Animation
    val infiniteTransition = rememberInfiniteTransition(label = "Disk Rotation")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Disk Angle"
    )
    val rotation = if (isPlaying) angle else 0f

    // Center Sticker Cozy Breathing Pulse Animation
    val infiniteTransitionCenter = rememberInfiniteTransition(label = "Label Pulse")
    val pulseScale by infiniteTransitionCenter.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Label Scale"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Custom Glass Top Bar
        GlassmorphicCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
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
                    modifier = Modifier.testTag("audio_back_button")
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
                        text = "Audio Player • ${(activeFile.size / (1024 * 1024))} MB",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = { viewModel.toggleFavorite(activeFile.uriString) },
                    modifier = Modifier.testTag("audio_fav_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Animated Rotating Disk / Vinyl Visualizer Section
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            // Elegant Background Glow
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0x33D0BCFF),
                                Color(0x00D0BCFF)
                            )
                        ),
                        shape = CircleShape
                    )
            )

            // Cozy animated circular audio visualizer around the vinyl disk
            CircularAudioVisualizer(
                isPlaying = isPlaying,
                modifier = Modifier
                    .size(284.dp)
                    .testTag("audio_circular_visualizer")
            )

            // Floating particles like paws/hearts rising from the vinyl center
            FloatingCozyParticles(
                isPlaying = isPlaying,
                modifier = Modifier.size(290.dp)
            )

            // The Vinyl Disk
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .rotate(rotation)
                    .testTag("audio_vinyl_disk"),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Draw outer record rings
                    drawCircle(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF0D1117), Color(0xFF161B22), Color(0xFF21262D))
                        ),
                        radius = size.minDimension / 2
                    )
                    
                    // Subtle grooves
                    val radiusStep = size.minDimension / 2 / 10
                    for (i in 2..9) {
                        drawCircle(
                            color = Color(0x1AFFFFFF),
                            radius = radiusStep * i,
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }
                }

                // Inner vinyl record label (frosted glass center)
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF625B71), Color(0xFFD0BCFF))
                            ),
                            shape = CircleShape
                        )
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isPlaying) "🦊" else "💤",
                        fontSize = 32.sp,
                        modifier = Modifier.graphicsLayer(
                            scaleX = if (isPlaying) pulseScale else 0.9f,
                            scaleY = if (isPlaying) pulseScale else 0.9f
                        )
                    )
                }

                // Spindle Hole
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(Color.Black, shape = CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Title & Artist (Filename)
        Text(
            text = activeFile.name.substringBeforeLast("."),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Text(
            text = "Stereo Audio Track",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF6750A4),
            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
        )

        // Cozy Linear Waveform Visualizer
        LinearAudioWaveform(
            isPlaying = isPlaying,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(horizontal = 24.dp)
                .testTag("audio_linear_waveform")
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Custom Glass Playback Controls Panel
        GlassmorphicCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
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
                        .testTag("audio_slider"),
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

                // Action Controls Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Speed Control
                    Box {
                        Button(
                            onClick = { showSpeedMenu = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE8DEF8),
                                contentColor = Color(0xFF1D192B)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("audio_speed_button")
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

                    // Play Pause circular button
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFD0BCFF))
                            .clickable { viewModel.togglePlayPause() }
                            .testTag("audio_play_pause_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Clear else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color(0xFF21005D),
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Forward button
                    IconButton(
                        onClick = { viewModel.seekTo((position + 10000).coerceAtMost(duration)) },
                        modifier = Modifier.testTag("audio_forward_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Skip 10s Forward",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CircularAudioVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "circular_visualizer")
    
    // Create multiple animated phases to simulate complex audio frequency bands
    val animTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "anim_time"
    )

    val wave1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave_1"
    )

    val wave2 by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave_2"
    )

    val wave3 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave_3"
    )

    Canvas(modifier = modifier) {
        val center = size / 2f
        val baseRadius = size.minDimension / 2.36f
        val maxBarLength = size.minDimension * 0.12f
        val barCount = 72

        for (i in 0 until barCount) {
            val angleDegrees = (i * 360f / barCount)
            val angleRadians = Math.toRadians(angleDegrees.toDouble()).toFloat()

            // Calculate fluctuating bar height multiplier
            val multiplier = if (isPlaying) {
                val indexFactor = i.toFloat() / barCount
                val sine1 = kotlin.math.sin((animTime + angleRadians * 4f).toDouble()).toFloat()
                val sine2 = kotlin.math.cos((animTime * 1.5f - angleRadians * 7f).toDouble()).toFloat()
                
                // Combine waves for organic, high-fidelity visualizer motion
                (0.4f + 0.3f * sine1 + 0.3f * sine2) * when {
                    i % 3 == 0 -> wave1
                    i % 3 == 1 -> wave2
                    else -> wave3
                }.coerceIn(0.1f, 1.5f)
            } else {
                // Gentle idle breathing state
                val idleSine = kotlin.math.sin((animTime * 0.5f + angleRadians).toDouble()).toFloat()
                0.08f + 0.04f * idleSine
            }

            val barLength = maxBarLength * multiplier
            val startX = center.width + kotlin.math.cos(angleRadians) * baseRadius
            val startY = center.height + kotlin.math.sin(angleRadians) * baseRadius
            val endX = center.width + kotlin.math.cos(angleRadians) * (baseRadius + barLength)
            val endY = center.height + kotlin.math.sin(angleRadians) * (baseRadius + barLength)

            // Cozy warm gradient colors matching MaterialTheme/Furry theme
            val barColor = when {
                i % 4 == 0 -> Color(0xFFD0BCFF) // Soft Lavender
                i % 4 == 1 -> Color(0xFF6750A4) // Royal Purple
                i % 4 == 2 -> Color(0xFFADC6FF) // Sky Blue
                else -> Color(0xFFEADDFF)       // Light Lilac
            }

            drawLine(
                color = barColor,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun FloatingCozyParticles(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isPlaying) return

    val infiniteTransition = rememberInfiniteTransition(label = "floating_particles")

    val p1Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "p1"
    )
    val p2Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = EaseOutQuad),
            repeatMode = RepeatMode.Restart
        ),
        label = "p2"
    )
    val p3Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3800, easing = EaseInQuad),
            repeatMode = RepeatMode.Restart
        ),
        label = "p3"
    )
    val p4Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "p4"
    )
    val p5Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "p5"
    )

    Box(modifier = modifier.fillMaxSize()) {
        FloatingItem(
            emoji = "🐾",
            progress = p1Offset,
            horizontalOffset = (-60).dp,
            scale = 1.1f
        )
        FloatingItem(
            emoji = "🎵",
            progress = p2Offset,
            horizontalOffset = 50.dp,
            scale = 0.9f
        )
        FloatingItem(
            emoji = "💖",
            progress = p3Offset,
            horizontalOffset = (-30).dp,
            scale = 1.0f
        )
        FloatingItem(
            emoji = "🐾",
            progress = p4Offset,
            horizontalOffset = 70.dp,
            scale = 1.2f
        )
        FloatingItem(
            emoji = "✨",
            progress = p5Offset,
            horizontalOffset = (-15).dp,
            scale = 0.8f
        )
    }
}

@Composable
fun BoxScope.FloatingItem(
    emoji: String,
    progress: Float,
    horizontalOffset: Dp,
    scale: Float
) {
    val alpha = if (progress < 0.1f) {
        progress / 0.1f
    } else if (progress > 0.7f) {
        (1f - progress) / 0.3f
    } else {
        1f
    }

    val yOffset = (-100 * progress).dp
    val xWobble = (kotlin.math.sin(progress * 2 * Math.PI.toFloat()) * 20).dp

    Text(
        text = emoji,
        fontSize = (16 * scale).sp,
        modifier = Modifier
            .align(Alignment.Center)
            .offset(
                x = horizontalOffset + xWobble,
                y = yOffset - 40.dp
            )
            .graphicsLayer(
                alpha = alpha,
                scaleX = scale,
                scaleY = scale,
                rotationZ = progress * 360f
            )
    )
}

@Composable
fun LinearAudioWaveform(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "linear_waveform")
    val animPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(modifier = modifier) {
        val barCount = 36
        val spacing = 4.dp.toPx()
        val totalSpacing = spacing * (barCount - 1)
        val barWidth = (size.width - totalSpacing) / barCount
        val maxBarHeight = size.height

        for (i in 0 until barCount) {
            val progress = i.toFloat() / barCount
            val heightMultiplier = if (isPlaying) {
                // A nice organic waveform curve using multi-frequency sine wave
                val sine1 = kotlin.math.sin((animPhase + progress * 8f).toDouble()).toFloat()
                val sine2 = kotlin.math.cos((animPhase * 2f - progress * 14f).toDouble()).toFloat()
                (0.3f + 0.4f * sine1 + 0.3f * sine2).coerceIn(0.1f, 1f)
            } else {
                // Minimal flat line with very gentle idle ripple
                0.1f + 0.05f * kotlin.math.sin((animPhase * 0.5f + progress * 4f).toDouble()).toFloat()
            }

            val barHeight = maxBarHeight * heightMultiplier
            val x = i * (barWidth + spacing) + barWidth / 2f
            val yStart = (size.height - barHeight) / 2f
            val yEnd = yStart + barHeight

            val brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF6750A4), // Royal Purple at top
                    Color(0xFFD0BCFF), // Light Lavender
                    Color(0xFFADC6FF)  // Soft blue at bottom
                )
            )

            drawLine(
                brush = brush,
                start = Offset(x, yStart),
                end = Offset(x, yEnd),
                strokeWidth = barWidth,
                cap = StrokeCap.Round
            )
        }
    }
}
