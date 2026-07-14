package com.example.ui.components

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import com.example.ui.ActiveFile
import com.example.ui.FileCategory
import com.example.ui.MainViewModel
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.media.MediaMetadataRetriever
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PlaylistSidePanel(
    playlist: List<ActiveFile>,
    activeFile: ActiveFile?,
    viewModel: MainViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }

    val filteredPlaylistWithIndices = remember(playlist, searchQuery) {
        playlist.mapIndexed { index, file -> index to file }
            .filter { (_, file) ->
                file.name.contains(searchQuery, ignoreCase = true)
            }
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(320.dp)
            .testTag("playlist_side_panel")
    ) {
        // Frosted Glass container
        GlassmorphicCard(
            modifier = Modifier.fillMaxSize(),
            cornerRadius = 0.dp,
            borderWidth = 1.dp,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 16.dp)
            ) {
                // Header of Side Panel
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "🐾 Cozy Play Queue",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF6750A4), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = playlist.size.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White.copy(alpha = 0.1f), CircleShape)
                            .testTag("playlist_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close Playlist",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Divider(
                    color = Color.White.copy(alpha = 0.15f),
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                // Playlist Actions & Search Bar
                if (playlist.isNotEmpty()) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .testTag("playlist_search_bar"),
                        placeholder = {
                            Text(
                                text = "Search queued files... 🔍",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = "Search Icon",
                                tint = Color.White.copy(alpha = 0.6f)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.testTag("playlist_search_clear")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Clear,
                                        contentDescription = "Clear Search",
                                        tint = Color.White.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFD0BCFF),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            cursorColor = Color(0xFFD0BCFF),
                            focusedContainerColor = Color.White.copy(alpha = 0.05f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.02f)
                        ),
                        shape = RoundedCornerShape(24.dp),
                        textStyle = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (searchQuery.isEmpty()) "Up Next" else "Filtered Results",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.LightGray
                        )

                        TextButton(
                            onClick = { viewModel.clearPlaylist() },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFFB4AB)),
                            modifier = Modifier.testTag("playlist_clear_all")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteSweep,
                                contentDescription = "Clear Queue",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear Queue", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Scrollable List of Queue items
                Box(modifier = Modifier.weight(1f)) {
                    if (playlist.isEmpty()) {
                        // Empty cozy state
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("💤", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Queue is empty",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Add tracks or videos from your library to keep the cozy vibes going! 🐾",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    } else if (filteredPlaylistWithIndices.isEmpty()) {
                        // Empty search results state
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("🔍", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No matching files",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Try searching for another cozy name in your queue! 🐾",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
                            itemsIndexed(
                                items = filteredPlaylistWithIndices,
                                key = { _, (_, file) -> file.uriString }
                            ) { filteredIndex, (originalIndex, file) ->
                                val density = LocalDensity.current
                                val isActive = activeFile?.uriString == file.uriString
                                val isPlaying by viewModel.isPlaying.collectAsState()

                                val isCurrentlyDragged = draggedIndex == originalIndex
                                val translationY = if (isCurrentlyDragged) dragOffsetY else 0f
                                val scale = if (isCurrentlyDragged) 1.04f else 1f
                                val shadowElevation = if (isCurrentlyDragged) 8.dp else 0.dp
                                val itemAlpha = if (draggedIndex == null) 1f else if (isCurrentlyDragged) 0.9f else 0.5f

                                QueueItemRow(
                                    file = file,
                                    index = originalIndex,
                                    isActive = isActive,
                                    isPlaying = isPlaying,
                                    onPlay = {
                                        viewModel.playFile(
                                            uri = Uri.parse(file.uriString),
                                            name = file.name,
                                            mimeType = file.mimeType,
                                            size = file.size,
                                            isSample = file.isSample
                                        )
                                    },
                                    onRemove = {
                                        viewModel.removeFromPlaylist(originalIndex)
                                    },
                                    modifier = Modifier
                                        .animateItem()
                                        .graphicsLayer {
                                            this.translationY = translationY
                                            this.scaleX = scale
                                            this.scaleY = scale
                                        }
                                        .shadow(shadowElevation, shape = RoundedCornerShape(16.dp))
                                        .alpha(itemAlpha)
                                        .pointerInput(originalIndex) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = {
                                                    draggedIndex = originalIndex
                                                    dragOffsetY = 0f
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    dragOffsetY += dragAmount.y

                                                    val itemHeightPx = with(density) { 72.dp.toPx() }

                                                    val currentDragged = draggedIndex
                                                    if (currentDragged != null) {
                                                        val currentFilteredPos = filteredPlaylistWithIndices.indexOfFirst { it.first == currentDragged }
                                                        if (currentFilteredPos != -1) {
                                                            if (dragOffsetY > itemHeightPx * 0.75f) {
                                                                if (currentFilteredPos < filteredPlaylistWithIndices.lastIndex) {
                                                                    val nextOriginalIndex = filteredPlaylistWithIndices[currentFilteredPos + 1].first
                                                                    viewModel.reorderPlaylist(currentDragged, nextOriginalIndex)
                                                                    draggedIndex = nextOriginalIndex
                                                                    dragOffsetY -= itemHeightPx
                                                                }
                                                            } else if (dragOffsetY < -itemHeightPx * 0.75f) {
                                                                if (currentFilteredPos > 0) {
                                                                    val prevOriginalIndex = filteredPlaylistWithIndices[currentFilteredPos - 1].first
                                                                    viewModel.reorderPlaylist(currentDragged, prevOriginalIndex)
                                                                    draggedIndex = prevOriginalIndex
                                                                    dragOffsetY += itemHeightPx
                                                                }
                                                            }
                                                        }
                                                    }
                                                },
                                                onDragEnd = {
                                                    draggedIndex = null
                                                    dragOffsetY = 0f
                                                },
                                                onDragCancel = {
                                                    draggedIndex = null
                                                    dragOffsetY = 0f
                                                }
                                            )
                                        }
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
fun QueueItemRow(
    file: ActiveFile,
    index: Int,
    isActive: Boolean,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_active_row")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "border_alpha"
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (isActive) Color(0xFF6750A4).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.04f),
        animationSpec = tween(durationMillis = 350, easing = EaseInOutCubic),
        label = "row_bg"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isActive) Color(0xFFD0BCFF).copy(alpha = borderAlpha) else Color.White.copy(alpha = 0.1f),
        animationSpec = tween(durationMillis = 350, easing = EaseInOutCubic),
        label = "row_border"
    )

    val nameTextColor by animateColorAsState(
        targetValue = if (isActive) Color(0xFFD0BCFF) else Color.White,
        animationSpec = tween(durationMillis = 350, easing = EaseInOutCubic),
        label = "name_text_color"
    )

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clickable { onPlay() }
            .testTag("playlist_item_$index")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Drag Handle Indicator for reordering
            Icon(
                imageVector = Icons.Rounded.DragHandle,
                contentDescription = "Drag to reorder",
                tint = Color.White.copy(alpha = 0.4f),
                modifier = Modifier
                    .size(20.dp)
                    .testTag("playlist_item_drag_handle_$index")
            )

            // Beautiful Frosted Glass Thumbnail Preview
            MediaThumbnail(
                file = file,
                isActive = isActive,
                isPlaying = isPlaying
            )

            // Track details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    color = nameTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Track ${index + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.LightGray.copy(alpha = 0.6f)
                    )
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .clip(CircleShape)
                            .background(Color.Gray.copy(alpha = 0.5f))
                    )
                    Text(
                        text = file.category.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.LightGray.copy(alpha = 0.8f)
                    )
                }
            }

            // Delete / Remove button
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .size(28.dp)
                    .background(Color.White.copy(alpha = 0.08f), CircleShape)
                    .testTag("playlist_item_remove_$index")
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Remove from Queue",
                    tint = Color.LightGray,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun MediaThumbnail(
    file: ActiveFile,
    isActive: Boolean,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Asynchronously generate/retrieve bitmap if applicable
    val generatedBitmap by produceState<Bitmap?>(initialValue = null, file.uriString) {
        value = withContext(Dispatchers.IO) {
            try {
                when (file.category) {
                    FileCategory.VIDEO -> {
                        var retriever: MediaMetadataRetriever? = null
                        try {
                            retriever = MediaMetadataRetriever()
                            if (file.uriString.startsWith("http")) {
                                retriever.setDataSource(file.uriString, HashMap())
                            } else {
                                retriever.setDataSource(context, Uri.parse(file.uriString))
                            }
                            // Retrieve a frame at 1 second
                            retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                                ?: retriever.frameAtTime
                        } catch (e: Exception) {
                            null
                        } finally {
                            try { retriever?.release() } catch (e: Exception) {}
                        }
                    }
                    FileCategory.AUDIO -> {
                        var retriever: MediaMetadataRetriever? = null
                        try {
                            retriever = MediaMetadataRetriever()
                            if (file.uriString.startsWith("http")) {
                                retriever.setDataSource(file.uriString, HashMap())
                            } else {
                                retriever.setDataSource(context, Uri.parse(file.uriString))
                            }
                            val artBytes = retriever.embeddedPicture
                            if (artBytes != null) {
                                BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size)
                            } else {
                                null
                            }
                        } catch (e: Exception) {
                            null
                        } finally {
                            try { retriever?.release() } catch (e: Exception) {}
                        }
                    }
                    FileCategory.DOCUMENT -> {
                        if (!file.uriString.startsWith("http") && !file.uriString.startsWith("sample://")) {
                            var pfd: ParcelFileDescriptor? = null
                            var renderer: PdfRenderer? = null
                            var page: PdfRenderer.Page? = null
                            try {
                                pfd = context.contentResolver.openFileDescriptor(Uri.parse(file.uriString), "r")
                                if (pfd != null) {
                                    renderer = PdfRenderer(pfd)
                                    if (renderer.pageCount > 0) {
                                        page = renderer.openPage(0)
                                        val bitmap = Bitmap.createBitmap(120, 160, Bitmap.Config.ARGB_8888)
                                        val canvas = android.graphics.Canvas(bitmap)
                                        canvas.drawColor(android.graphics.Color.WHITE)
                                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                        bitmap
                                    } else null
                                } else null
                            } catch (e: Exception) {
                                null
                            } finally {
                                try { page?.close() } catch (e: Exception) {}
                                try { renderer?.close() } catch (e: Exception) {}
                                try { pfd?.close() } catch (e: Exception) {}
                            }
                        } else null
                    }
                    else -> null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    val thumbnailBgColor by animateColorAsState(
        targetValue = if (isActive) Color(0xFFEADDFF).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f),
        animationSpec = tween(durationMillis = 350, easing = EaseInOutCubic),
        label = "thumbnail_bg"
    )

    val borderStartColor by animateColorAsState(
        targetValue = if (isActive) Color(0xFFD0BCFF).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.2f),
        animationSpec = tween(durationMillis = 350, easing = EaseInOutCubic),
        label = "border_start"
    )

    val borderEndColor by animateColorAsState(
        targetValue = if (isActive) Color(0xFFD0BCFF).copy(alpha = 0.1f) else Color.White.copy(alpha = 0.05f),
        animationSpec = tween(durationMillis = 350, easing = EaseInOutCubic),
        label = "border_end"
    )

    // Glassmorphic / Frosted outer container
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(thumbnailBgColor)
            .border(
                BorderStroke(
                    1.dp,
                    Brush.linearGradient(
                        colors = listOf(borderStartColor, borderEndColor)
                    )
                ),
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        // Content rendering (actual thumbnail or frosted placeholder)
        if (isActive) {
            // Show active indicator overlay if loaded, or just the active cozy icon
            Text(
                text = if (isPlaying) "🦊" else "💤",
                fontSize = 18.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            when {
                file.category == FileCategory.IMAGE -> {
                    AsyncImage(
                        model = file.uriString,
                        contentDescription = file.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                    )
                }
                generatedBitmap != null -> {
                    Image(
                        bitmap = generatedBitmap!!.asImageBitmap(),
                        contentDescription = file.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                    )
                }
                file.category == FileCategory.TEXT -> {
                    // Mini code/text graphic layout with glassmorphic style
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Box(modifier = Modifier.fillMaxWidth(0.8f).height(3.dp).background(Color(0x8080CBC4), RoundedCornerShape(1.5.dp)))
                        Box(modifier = Modifier.fillMaxWidth(0.5f).height(3.dp).background(Color(0x80E57373), RoundedCornerShape(1.5.dp)))
                        Box(modifier = Modifier.fillMaxWidth(0.9f).height(3.dp).background(Color(0x8064B5F6), RoundedCornerShape(1.5.dp)))
                        Box(modifier = Modifier.fillMaxWidth(0.6f).height(3.dp).background(Color(0x80FFD54F), RoundedCornerShape(1.5.dp)))
                    }
                }
                else -> {
                    // Fallback icons
                    Text(
                        text = when (file.category) {
                            FileCategory.VIDEO -> "🎥"
                            FileCategory.AUDIO -> "🎵"
                            FileCategory.DOCUMENT -> "📄"
                            else -> "📦"
                        },
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}
