package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ActiveFile
import com.example.ui.FileCategory
import com.example.ui.MainViewModel
import com.example.ui.Tab

@Composable
fun FileUploadComponent(
    onPickFile: (String) -> Unit,
    activeFile: ActiveFile?,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    // Breathing/Pulse animation for the upload icon to make the interface feel alive
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_upload")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Animated upload progress bar simulation when a file is newly picked
    var animatedProgress by remember { mutableStateOf(0f) }
    LaunchedEffect(activeFile) {
        if (activeFile != null) {
            animatedProgress = 0f
            animate(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = tween(durationMillis = 800, easing = LinearOutSlowInEasing)
            ) { value, _ ->
                animatedProgress = value
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("file_upload_component"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Upload & Play Media 🐾",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1C1E)
        )

        AnimatedContent(
            targetState = activeFile,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
            },
            label = "file_upload_state"
        ) { currentActiveFile ->
            if (currentActiveFile == null) {
                // Dashed Dropzone Box (Empty state)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF6750A4).copy(alpha = 0.03f),
                                    Color(0xFFD0BCFF).copy(alpha = 0.06f),
                                    Color(0xFFADC6FF).copy(alpha = 0.03f)
                                )
                            )
                        )
                        .drawBehind {
                            val stroke = Stroke(
                                width = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 16f), 0f)
                            )
                            drawRoundRect(
                                color = Color(0xFF6750A4).copy(alpha = 0.35f),
                                style = stroke,
                                cornerRadius = CornerRadius(24.dp.toPx())
                            )
                        }
                        .clickable { onPickFile("*/*") }
                        .testTag("upload_dropzone"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(20.dp)
                    ) {
                        // Bouncing/breathing folder upload icon
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFEADDFF).copy(alpha = 0.7f))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CloudUpload,
                                contentDescription = "Upload Cloud Icon",
                                tint = Color(0xFF21005D),
                                modifier = Modifier
                                    .size(32.dp)
                                    .align(Alignment.Center)
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Select or Drop a File Here 🐾",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1D1B20)
                            )
                            Text(
                                text = "Select audio, video, PDF or text files from your system",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }

                        // Category specific shortcuts
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Spacer(modifier = Modifier.width(4.dp))
                            CategoryShortcutChip(
                                label = "Video",
                                emoji = "🎥",
                                onClick = { onPickFile("video/*") }
                            )
                            CategoryShortcutChip(
                                label = "Audio",
                                emoji = "🎵",
                                onClick = { onPickFile("audio/*") }
                            )
                            CategoryShortcutChip(
                                label = "PDF",
                                emoji = "📄",
                                onClick = { onPickFile("application/pdf") }
                            )
                            CategoryShortcutChip(
                                label = "Code/Text",
                                emoji = "📝",
                                onClick = { onPickFile("text/*") }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                    }
                }
            } else {
                // File Selected Display State
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color(0xFFEADDFF)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("uploaded_file_card")
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // High-contrast format emoji badge
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        when (currentActiveFile.category) {
                                            FileCategory.VIDEO -> Color(0xFFEADDFF)
                                            FileCategory.AUDIO -> Color(0xFFD3E4FF)
                                            FileCategory.DOCUMENT -> Color(0xFFF3E7FF)
                                            FileCategory.TEXT -> Color(0xFFE2F4C5)
                                            else -> Color(0xFFF3F3F7)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when (currentActiveFile.category) {
                                        FileCategory.VIDEO -> "🎥"
                                        FileCategory.AUDIO -> "🎵"
                                        FileCategory.IMAGE -> "🖼️"
                                        FileCategory.DOCUMENT -> "📄"
                                        FileCategory.TEXT -> "📝"
                                        else -> "📦"
                                    },
                                    fontSize = 28.sp
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentActiveFile.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1A1C1E),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.testTag("uploaded_file_name")
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = formatLocalSize(currentActiveFile.size),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(3.dp)
                                            .clip(RoundedCornerShape(1.dp))
                                            .background(Color.Gray)
                                    )
                                    Text(
                                        text = currentActiveFile.category.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = when (currentActiveFile.category) {
                                            FileCategory.VIDEO -> Color(0xFF533E85)
                                            FileCategory.AUDIO -> Color(0xFF004380)
                                            FileCategory.DOCUMENT -> Color(0xFF5D3E85)
                                            FileCategory.TEXT -> Color(0xFF2E6330)
                                            else -> Color.DarkGray
                                        }
                                    )
                                }
                            }

                            IconButton(
                                onClick = { viewModel.closeActiveFile() },
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFFF1F1F5), RoundedCornerShape(10.dp))
                                    .testTag("clear_uploaded_file")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Unload file",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Satisfying file analysis/upload progress bar
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (animatedProgress < 1f) "Analyzing stream bits..." else "File completely ready 🐾",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${(animatedProgress * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6750A4)
                                )
                            }

                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = Color(0xFF6750A4),
                                trackColor = Color(0xFFEADDFF).copy(alpha = 0.4f),
                            )
                        }

                        // Play/Open actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { viewModel.selectTab(Tab.PLAYER) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("play_uploaded_file_btn"),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4))
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = "Play"
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Play in Furry Player",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            OutlinedButton(
                                onClick = { onPickFile("*/*") },
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("upload_different_btn"),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, Color(0xFF6750A4).copy(alpha = 0.5f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF6750A4))
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.FolderOpen,
                                    contentDescription = "Change File"
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Replace",
                                    fontSize = 14.sp
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
fun CategoryShortcutChip(
    label: String,
    emoji: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
        modifier = modifier
            .clickable { onClick() }
            .testTag("upload_shortcut_${label.lowercase()}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = emoji, fontSize = 14.sp)
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF44474E)
            )
        }
    }
}

private fun formatLocalSize(size: Long): String {
    if (size <= 0) return "Unknown size"
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var doubleSize = size.toDouble()
    var unitIndex = 0
    while (doubleSize >= 1024 && unitIndex < units.size - 1) {
        doubleSize /= 1024
        unitIndex++
    }
    return String.format("%.1f %s", doubleSize, units[unitIndex])
}
