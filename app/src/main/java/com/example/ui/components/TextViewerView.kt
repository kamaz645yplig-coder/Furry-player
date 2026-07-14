package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ActiveFile
import com.example.ui.MainViewModel

@Composable
fun TextViewerView(
    viewModel: MainViewModel,
    activeFile: ActiveFile,
    modifier: Modifier = Modifier
) {
    val textContent = activeFile.textContent ?: "No text content loaded."
    val lines = remember(textContent) { textContent.split("\n") }
    
    var wordWrap by remember { mutableStateOf(true) }
    var fontSizeSp by remember { mutableStateOf(13) }
    
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

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
                    modifier = Modifier.testTag("text_back_button")
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
                        text = "Text & Code • ${lines.size} Lines",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = { viewModel.toggleFavorite(activeFile.uriString) },
                    modifier = Modifier.testTag("text_fav_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Code/Text Canvas
        val isDark = MaterialTheme.colorScheme.background.value == 0xFF0C1017UL
        val canvasBg = if (isDark) Color(0xFF131822) else Color(0xFFF0F4F8)
        val lineNumColor = if (isDark) Color(0xFF4F5B66) else Color(0xFF90A4AE)
        val codeTextColor = if (isDark) Color(0xFFC0C5CE) else Color(0xFF37474F)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(canvasBg)
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(verticalScrollState)
            ) {
                // IDE-Style Line Numbers Column
                Column(
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .width(40.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    lines.forEachIndexed { index, _ ->
                        Text(
                            text = (index + 1).toString(),
                            fontFamily = FontFamily.Monospace,
                            fontSize = fontSizeSp.sp,
                            color = lineNumColor,
                            fontWeight = FontWeight.Bold,
                            lineHeight = (fontSizeSp + 5).sp
                        )
                    }
                }

                // Split divider
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(lineNumColor.copy(alpha = 0.3f))
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Scrollable Content Column
                val textModifier = if (wordWrap) {
                    Modifier.fillMaxWidth()
                } else {
                    Modifier
                        .wrapContentWidth()
                        .horizontalScroll(horizontalScrollState)
                }

                Column(modifier = textModifier) {
                    lines.forEach { line ->
                        Text(
                            text = line,
                            fontFamily = FontFamily.Monospace,
                            fontSize = fontSizeSp.sp,
                            color = codeTextColor,
                            lineHeight = (fontSizeSp + 5).sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Custom Glass View Modifier Panel
        GlassmorphicCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 24.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Word wrap toggle
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Word Wrap",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = wordWrap,
                        onCheckedChange = { wordWrap = it },
                        modifier = Modifier.testTag("text_word_wrap_switch")
                    )
                }

                // Font Size Selector
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Font Size:",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    listOf(11, 13, 16).forEach { size ->
                        TextButton(
                            onClick = { fontSizeSp = size },
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(
                                text = when (size) {
                                    11 -> "A-"
                                    13 -> "A"
                                    else -> "A+"
                                },
                                fontWeight = if (fontSizeSp == size) FontWeight.Bold else FontWeight.Normal,
                                color = if (fontSizeSp == size) Color(0xFF21005D) else Color.Gray,
                                fontSize = if (size == 11) 12.sp else if (size == 13) 14.sp else 17.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
