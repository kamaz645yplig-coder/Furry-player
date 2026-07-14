package com.example

import android.app.Application
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.data.AppDatabase
import com.example.data.FileRepository
import com.example.data.PlayedFile
import com.example.ui.ActiveFile
import com.example.ui.FileCategory
import com.example.ui.MainViewModel
import com.example.ui.Tab
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.components.FileUploadComponent
import com.example.ui.components.PlaylistSidePanel
import com.example.ui.components.PlaylistSidePanel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext)
        val repository = FileRepository(database.playedFileDao())

        setContent {
            val viewModel: MainViewModel by viewModels {
                object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return MainViewModel(application, repository) as T
                    }
                }
            }

            MyApplicationTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val activeFile by viewModel.activeFile.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    val recentFiles by viewModel.recentFiles.collectAsStateWithLifecycle()
    val favoriteFiles by viewModel.favoriteFiles.collectAsStateWithLifecycle()

    var showUrlDialog by remember { mutableStateOf(false) }

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            var name = "Selected File"
            var size = 0L
            val mimeType = context.contentResolver.getType(it) ?: "application/octet-stream"

            try {
                context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIndex != -1) name = cursor.getString(nameIndex)
                        if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            viewModel.playFile(it, name, mimeType, size)
        }
    }

    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    Scaffold(
        modifier = Modifier.fillMaxSize().testTag("main_scaffold"),
        bottomBar = {
            if (!isTablet) {
                FrostedBottomNavBar(
                    currentTab = currentTab,
                    onTabSelected = { viewModel.selectTab(it) },
                    hasActiveFile = activeFile != null
                )
            }
        },
        containerColor = Color(0xFFF7F9FF)
    ) { innerPadding ->
        if (isTablet) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            ) {
                Surface(
                    modifier = Modifier
                        .width(320.dp)
                        .fillMaxHeight(),
                    color = Color(0xFFF3F3F7),
                    border = BorderStroke(1.dp, Color(0xFFDDE2EA))
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "🐾",
                                fontSize = 28.sp,
                                modifier = Modifier.testTag("app_logo_tablet")
                            )
                            Column {
                                Text(
                                    text = "Furry Player",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1A1C1E)
                                )
                                Text(
                                    text = "Frosted multi-format engine",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF625B71)
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            TabletTabItem(
                                label = "Library",
                                icon = Icons.Rounded.Home,
                                isSelected = currentTab == Tab.LIBRARY,
                                onClick = { viewModel.selectTab(Tab.LIBRARY) }
                            )
                            TabletTabItem(
                                label = "History",
                                icon = Icons.Rounded.List,
                                isSelected = currentTab == Tab.HISTORY,
                                onClick = { viewModel.selectTab(Tab.HISTORY) }
                            )
                            TabletTabItem(
                                label = "Favorites",
                                icon = Icons.Rounded.Favorite,
                                isSelected = currentTab == Tab.FAVORITES,
                                onClick = { viewModel.selectTab(Tab.FAVORITES) }
                            )
                            if (activeFile != null) {
                                TabletTabItem(
                                    label = "Active Player",
                                    icon = Icons.Rounded.PlayCircleFilled,
                                    isSelected = currentTab == Tab.PLAYER,
                                    onClick = { viewModel.selectTab(Tab.PLAYER) }
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { filePickerLauncher.launch("*/*") },
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("picker_btn_tablet"),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4))
                            ) {
                                Icon(Icons.Rounded.FolderOpen, contentDescription = "Open File")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Open File", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { showUrlDialog = true },
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("url_btn_tablet"),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, Color(0xFF6750A4)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF6750A4))
                            ) {
                                Icon(Icons.Rounded.Link, contentDescription = "Stream URL")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Stream URL", fontSize = 14.sp)
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color(0xFFF7F9FF))
                ) {
                    when (currentTab) {
                        Tab.LIBRARY -> LibraryScreen(
                            viewModel = viewModel,
                            activeFile = activeFile,
                            onPickFile = { mimeType -> filePickerLauncher.launch(mimeType) },
                            onStreamUrl = { showUrlDialog = true }
                        )
                        Tab.HISTORY -> HistoryScreen(
                            files = recentFiles,
                            searchQuery = searchQuery,
                            onQueryChanged = { viewModel.updateSearchQuery(it) },
                            onPlayFile = { played ->
                                viewModel.playFile(
                                    uri = Uri.parse(played.uriString),
                                    name = played.fileName,
                                    mimeType = played.mimeType,
                                    size = played.fileSize
                                )
                            },
                            onDelete = { viewModel.deleteFile(it) },
                            onToggleFav = { viewModel.toggleFavorite(it) },
                            onQueueFile = { viewModel.addPlayedFileToQueue(it) }
                        )
                        Tab.FAVORITES -> FavoritesScreen(
                            files = favoriteFiles,
                            searchQuery = searchQuery,
                            onQueryChanged = { viewModel.updateSearchQuery(it) },
                            onPlayFile = { played ->
                                viewModel.playFile(
                                    uri = Uri.parse(played.uriString),
                                    name = played.fileName,
                                    mimeType = played.mimeType,
                                    size = played.fileSize
                                )
                            },
                            onToggleFav = { viewModel.toggleFavorite(it) },
                            onQueueFile = { viewModel.addPlayedFileToQueue(it) }
                        )
                        Tab.PLAYER -> PlayerViewScreen(
                            activeFile = activeFile,
                            viewModel = viewModel
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentTab) {
                    Tab.LIBRARY -> LibraryScreen(
                        viewModel = viewModel,
                        activeFile = activeFile,
                        onPickFile = { mimeType -> filePickerLauncher.launch(mimeType) },
                        onStreamUrl = { showUrlDialog = true }
                    )
                    Tab.HISTORY -> HistoryScreen(
                        files = recentFiles,
                        searchQuery = searchQuery,
                        onQueryChanged = { viewModel.updateSearchQuery(it) },
                        onPlayFile = { played ->
                            viewModel.playFile(
                                uri = Uri.parse(played.uriString),
                                name = played.fileName,
                                mimeType = played.mimeType,
                                size = played.fileSize
                            )
                        },
                        onDelete = { viewModel.deleteFile(it) },
                        onToggleFav = { viewModel.toggleFavorite(it) },
                        onQueueFile = { viewModel.addPlayedFileToQueue(it) }
                    )
                    Tab.FAVORITES -> FavoritesScreen(
                        files = favoriteFiles,
                        searchQuery = searchQuery,
                        onQueryChanged = { viewModel.updateSearchQuery(it) },
                        onPlayFile = { played ->
                            viewModel.playFile(
                                uri = Uri.parse(played.uriString),
                                name = played.fileName,
                                mimeType = played.mimeType,
                                size = played.fileSize
                            )
                        },
                        onToggleFav = { viewModel.toggleFavorite(it) },
                        onQueueFile = { viewModel.addPlayedFileToQueue(it) }
                    )
                    Tab.PLAYER -> PlayerViewScreen(
                        activeFile = activeFile,
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    if (showUrlDialog) {
        UrlStreamDialog(
            onDismiss = { showUrlDialog = false },
            onConfirm = { url, name, format ->
                showUrlDialog = false
                val mimeType = when (format) {
                    "Video" -> "video/mp4"
                    "Audio" -> "audio/mp3"
                    "PDF" -> "application/pdf"
                    "Text" -> "text/plain"
                    else -> "image/jpeg"
                }
                viewModel.playFile(
                    uri = Uri.parse(url),
                    name = name.ifBlank { url.substringAfterLast("/") },
                    mimeType = mimeType,
                    size = 0L
                )
            }
        )
    }
}

@Composable
fun TabletTabItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        color = if (isSelected) Color(0xFFEADDFF) else Color.Transparent,
        contentColor = if (isSelected) Color(0xFF21005D) else Color(0xFF44474E)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) Color(0xFF21005D) else Color(0xFF44474E)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
fun FrostedBottomNavBar(
    currentTab: Tab,
    onTabSelected: (Tab) -> Unit,
    hasActiveFile: Boolean
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        color = Color(0xFFF3F3F7),
        border = BorderStroke(1.dp, Color(0xFFDDE2EA).copy(alpha = 0.8f))
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            NavBarItem(
                label = "Library",
                icon = Icons.Rounded.Home,
                badge = "🐾",
                isSelected = currentTab == Tab.LIBRARY,
                onClick = { onTabSelected(Tab.LIBRARY) }
            )
            NavBarItem(
                label = "History",
                icon = Icons.Rounded.List,
                badge = null,
                isSelected = currentTab == Tab.HISTORY,
                onClick = { onTabSelected(Tab.HISTORY) }
            )
            NavBarItem(
                label = "Favorites",
                icon = Icons.Rounded.Favorite,
                badge = "💖",
                isSelected = currentTab == Tab.FAVORITES,
                onClick = { onTabSelected(Tab.FAVORITES) }
            )
            if (hasActiveFile) {
                NavBarItem(
                    label = "Player",
                    icon = Icons.Rounded.PlayCircleFilled,
                    badge = "🎮",
                    isSelected = currentTab == Tab.PLAYER,
                    onClick = { onTabSelected(Tab.PLAYER) }
                )
            }
        }
    }
}

@Composable
fun RowScope.NavBarItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    badge: String?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxHeight()
        ) {
            Box(
                modifier = Modifier
                    .size(width = 64.dp, height = 32.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) Color(0xFFEADDFF) else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isSelected) Color(0xFF21005D) else Color(0xFF44474E),
                    modifier = Modifier.size(24.dp)
                )
                if (badge != null && isSelected) {
                    Text(
                        text = badge,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 4.dp, top = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color(0xFF1D1B20) else Color(0xFF44474E)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UrlStreamDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var url by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var selectedFormat by remember { mutableStateOf("Video") }
    val formats = listOf("Video", "Audio", "PDF", "Image", "Text")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            border = BorderStroke(1.dp, Color(0xFFE0E0E0))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Stream Web URL 🐾",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF21005D)
                )

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Stream URL (HTTP/HTTPS)") },
                    placeholder = { Text("https://example.com/movie.mp4") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("url_input_field"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("File Name (Optional)") },
                    placeholder = { Text("My Forest Walk.mp4") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("url_name_field"),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    shape = RoundedCornerShape(12.dp)
                )

                Text(
                    text = "Select Stream Format Type:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF44474E)
                )

                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    formats.forEach { format ->
                        FilterChip(
                            selected = selectedFormat == format,
                            onClick = { selectedFormat = format },
                            label = { Text(format) },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFEADDFF),
                                selectedLabelColor = Color(0xFF21005D)
                            )
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (url.isNotBlank()) {
                                onConfirm(url.trim(), name.trim(), selectedFormat)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                        shape = RoundedCornerShape(12.dp),
                        enabled = url.isNotBlank()
                    ) {
                        Text("Start Streaming 🐾")
                    }
                }
            }
        }
    }
}

@Composable
fun LibraryScreen(
    viewModel: MainViewModel,
    activeFile: ActiveFile?,
    onPickFile: (String) -> Unit,
    onStreamUrl: () -> Unit
) {
    val context = LocalContext.current
    val recentFiles by viewModel.recentFiles.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("library_scroll_container"),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Furry Player 🐾",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1C1E)
                    )
                    Text(
                        text = "Frosted glass high-fidelity media engine",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF625B71)
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFEADDFF),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("🦊", fontSize = 24.sp)
                    }
                }
            }
        }

        item {
            FrostedHeroSection(
                activeFile = activeFile,
                viewModel = viewModel
            )
        }

        item {
            FileUploadComponent(
                onPickFile = onPickFile,
                activeFile = activeFile,
                viewModel = viewModel
            )
        }

        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable { onStreamUrl() }
                    .testTag("stream_url_button"),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFEADDFF).copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFD3E4FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Link, contentDescription = "Stream web link", tint = Color(0xFF001D36))
                        }
                        Column {
                            Text(
                                text = "Stream from Web Link",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1C1B1F)
                            )
                            Text(
                                text = "Directly stream online audio, video, or documents",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = "Open Web URL Streamer",
                        tint = Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Explore Formats & Sample Files 🐾",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1C1E)
                )

                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FormatSampleCard(
                        emoji = "🐱",
                        title = "Video Screen",
                        subtitle = "Purring_Nyan.mp4",
                        color = Color(0xFFEADDFF),
                        textColor = Color(0xFF21005D),
                        onClick = {
                            viewModel.playFile(
                                uri = Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"),
                                name = "Purring_Nyan_Dance.mp4",
                                mimeType = "video/mp4",
                                size = 12930210L,
                                isSample = true
                            )
                        },
                        onQueueClick = {
                            viewModel.addToQueue(
                                uri = Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"),
                                name = "Purring_Nyan_Dance.mp4",
                                mimeType = "video/mp4",
                                size = 12930210L,
                                isSample = true
                            )
                        }
                    )

                    FormatSampleCard(
                        emoji = "🐺",
                        title = "Audio Screen",
                        subtitle = "Wolf_Lofi_Beats.mp3",
                        color = Color(0xFFE8DEF8),
                        textColor = Color(0xFF1D192B),
                        onClick = {
                            viewModel.playFile(
                                uri = Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"),
                                name = "Furry_Campfire_Acoustics.mp3",
                                mimeType = "audio/mp3",
                                size = 6430129L,
                                isSample = true
                            )
                        },
                        onQueueClick = {
                            viewModel.addToQueue(
                                uri = Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"),
                                name = "Furry_Campfire_Acoustics.mp3",
                                mimeType = "audio/mp3",
                                size = 6430129L,
                                isSample = true
                            )
                        }
                    )

                    FormatSampleCard(
                        emoji = "🦊",
                        title = "Image Screen",
                        subtitle = "Fennec_Chillout.jpg",
                        color = Color(0xFFD3E4FF),
                        textColor = Color(0xFF001D36),
                        onClick = {
                            viewModel.playFile(
                                uri = Uri.parse("sample_art"),
                                name = "Fennec_Fox_Lofi_Beat.jpg",
                                mimeType = "image/jpeg",
                                size = 2390123L,
                                isSample = true
                            )
                        },
                        onQueueClick = {
                            viewModel.addToQueue(
                                uri = Uri.parse("sample_art"),
                                name = "Fennec_Fox_Lofi_Beat.jpg",
                                mimeType = "image/jpeg",
                                size = 2390123L,
                                isSample = true
                            )
                        }
                    )

                    FormatSampleCard(
                        emoji = "🦉",
                        title = "Doc Reader",
                        subtitle = "Furry_Manual.pdf",
                        color = Color(0xFFF3E7FF),
                        textColor = Color(0xFF21005D),
                        onClick = {
                            val contextCacheDir = context.cacheDir
                            val samplePdfFile = File(contextCacheDir, "Universal_Player_Introduction.pdf")
                            if (samplePdfFile.exists()) {
                                viewModel.playFile(
                                    uri = Uri.fromFile(samplePdfFile),
                                    name = "Furry_Player_Manual.pdf",
                                    mimeType = "application/pdf",
                                    size = samplePdfFile.length(),
                                    isSample = true
                                )
                            } else {
                                Toast.makeText(context, "Preparing cache document... try again in a sec!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onQueueClick = {
                            val contextCacheDir = context.cacheDir
                            val samplePdfFile = File(contextCacheDir, "Universal_Player_Introduction.pdf")
                            if (samplePdfFile.exists()) {
                                viewModel.addToQueue(
                                    uri = Uri.fromFile(samplePdfFile),
                                    name = "Furry_Player_Manual.pdf",
                                    mimeType = "application/pdf",
                                    size = samplePdfFile.length(),
                                    isSample = true
                                )
                            }
                        }
                    )

                    FormatSampleCard(
                        emoji = "🐼",
                        title = "Text / Code",
                        subtitle = "FurryPlayerApp.kt",
                        color = Color(0xFFE2F4C5),
                        textColor = Color(0xFF1E3F20),
                        onClick = {
                            val file = File(context.cacheDir, "FurryPlayerApp.kt")
                            try {
                                file.writeText("""
                                    package com.example.ui
                                    
                                    import android.app.Application
                                    import androidx.lifecycle.AndroidViewModel
                                    import com.example.data.PlayedFile
                                    
                                    /**
                                     * 🐾 Welcome to the Furry Player Code Viewer! 🐾
                                     * This text component supports monospaced fonts, line numbers,
                                     * syntax highlighting for Kotlin keywords, and text scaling.
                                     */
                                    class FurryPlayerApp(application: Application) : AndroidViewModel(application) {
                                        val engineName = "Furry Player 🐾"
                                        val version = "2.0.1"
                                        val supportsFrostedGlass = true
                                        
                                        fun playMediaFile(uri: String) {
                                            println("🐾 Initializing fluffy playback audio-video pipeline for: " + uri)
                                        }
                                    }
                                """.trimIndent())
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            viewModel.playFile(
                                uri = Uri.fromFile(file),
                                name = "FurryPlayerApp.kt",
                                mimeType = "text/plain",
                                size = file.length(),
                                isSample = true
                            )
                        },
                        onQueueClick = {
                            val file = File(context.cacheDir, "FurryPlayerApp.kt")
                            try {
                                file.writeText("""
                                    package com.example.ui
                                    
                                    import android.app.Application
                                    import androidx.lifecycle.AndroidViewModel
                                    import com.example.data.PlayedFile
                                    
                                    /**
                                     * 🐾 Welcome to the Furry Player Code Viewer! 🐾
                                     * This text component supports monospaced fonts, line numbers,
                                     * syntax highlighting for Kotlin keywords, and text scaling.
                                     */
                                    class FurryPlayerApp(application: Application) : AndroidViewModel(application) {
                                        val engineName = "Furry Player 🐾"
                                        val version = "2.0.1"
                                        val supportsFrostedGlass = true
                                        
                                        fun playMediaFile(uri: String) {
                                            println("🐾 Initializing fluffy playback audio-video pipeline for: " + uri)
                                        }
                                    }
                                """.trimIndent())
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            viewModel.addToQueue(
                                uri = Uri.fromFile(file),
                                name = "FurryPlayerApp.kt",
                                mimeType = "text/plain",
                                size = file.length(),
                                isSample = true
                            )
                        }
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Recent Activity 🐾",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1C1E)
                )
                TextButton(onClick = { viewModel.selectTab(Tab.HISTORY) }) {
                    Text("View History", color = Color(0xFF6750A4), fontWeight = FontWeight.Bold)
                }
            }
        }

        if (recentFiles.isEmpty()) {
            item {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color(0xFFEADDFF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🐾", fontSize = 32.sp)
                        Text(
                            text = "No Played Files Yet!",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1C1E)
                        )
                        Text(
                            text = "Click 'Local File' or play one of our cute sample files above to start!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(recentFiles.take(4)) { file ->
                ActivityFileRow(
                    file = file,
                    onPlay = {
                        viewModel.playFile(
                            uri = Uri.parse(file.uriString),
                            name = file.fileName,
                            mimeType = file.mimeType,
                            size = file.fileSize
                        )
                    },
                    onToggleFav = { viewModel.toggleFavorite(file.uriString) },
                    onDelete = { viewModel.deleteFile(file) },
                    onQueue = { viewModel.addPlayedFileToQueue(file) }
                )
            }
        }
    }
}

@Composable
fun FormatSampleCard(
    emoji: String,
    title: String,
    subtitle: String,
    color: Color,
    textColor: Color,
    onClick: () -> Unit,
    onQueueClick: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier
            .width(150.dp)
            .height(140.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .testTag("sample_${title.lowercase().replace(" ", "_")}"),
        color = color,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.padding(16.dp).fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(emoji, fontSize = 20.sp)
                }

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (onQueueClick != null) {
                IconButton(
                    onClick = {
                        onQueueClick()
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(28.dp)
                        .background(Color.White.copy(alpha = 0.4f), CircleShape)
                        .testTag("sample_queue_${title.lowercase().replace(" ", "_")}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlaylistAdd,
                        contentDescription = "Add to Queue",
                        tint = textColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FrostedHeroSection(
    activeFile: ActiveFile?,
    viewModel: MainViewModel
) {
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val playbackPosition by viewModel.playbackPosition.collectAsStateWithLifecycle()
    val mediaDuration by viewModel.mediaDuration.collectAsStateWithLifecycle()

    val displayTitle = activeFile?.name ?: "No Active File"
    val displayCategory = activeFile?.category?.label ?: "Ready to play"

    val brush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF625B71),
            Color(0xFFD0BCFF),
            Color(0xFFADC6FF)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(brush)
            .testTag("hero_section")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = 0.22f))
                .border(BorderStroke(1.2.dp, Color.White.copy(alpha = 0.35f)), shape = RoundedCornerShape(28.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (activeFile != null) "Currently Playing 🐾" else "Welcome to Furry Player 🐾",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF21005D).copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = displayTitle,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1A1C1E),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("hero_title")
                        )
                        Text(
                            text = displayCategory,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF1D192B),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (activeFile != null) {
                        IconButton(
                            onClick = { viewModel.closeActiveFile() },
                            modifier = Modifier.size(28.dp).background(Color.White.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close player", modifier = Modifier.size(16.dp))
                        }
                    }
                }

                if (activeFile != null && (activeFile.category == FileCategory.AUDIO || activeFile.category == FileCategory.VIDEO)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.togglePlayPause() },
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color(0xFF21005D), CircleShape)
                                .testTag("hero_play_pause")
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White
                            )
                        }

                        val progressFraction = if (mediaDuration > 0) playbackPosition.toFloat() / mediaDuration else 0f
                        Column(modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.3f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progressFraction)
                                        .fillMaxHeight()
                                        .background(Color(0xFF21005D))
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = formatTime(playbackPosition),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF21005D).copy(alpha = 0.8f)
                                )
                                Text(
                                    text = formatTime(mediaDuration),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF21005D).copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("🐱", fontSize = 16.sp)
                            Text(
                                text = "Lofi, secure & offline processing.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF21005D).copy(alpha = 0.8f)
                            )
                        }

                        if (activeFile != null) {
                            Button(
                                onClick = { viewModel.selectTab(Tab.PLAYER) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF21005D)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Open View", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityFileRow(
    file: PlayedFile,
    onPlay: () -> Unit,
    onToggleFav: () -> Unit,
    onDelete: () -> Unit,
    onQueue: (() -> Unit)? = null
) {
    val categoryIcon = when {
        file.mimeType.startsWith("video/") -> "🎥"
        file.mimeType.startsWith("audio/") -> "🎵"
        file.mimeType.startsWith("image/") -> "🖼️"
        file.mimeType == "application/pdf" -> "📄"
        else -> "📝"
    }

    Surface(
        color = Color.White,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFFEADDFF).copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() }
            .testTag("activity_row_${file.id}")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFF3F3F7)),
                contentAlignment = Alignment.Center
            ) {
                Text(categoryIcon, fontSize = 24.sp)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1C1E),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${formatSize(file.fileSize)} • ${formatDate(file.timestamp)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onQueue != null) {
                    IconButton(
                        onClick = {
                            onQueue()
                        },
                        modifier = Modifier.testTag("activity_row_queue_${file.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlaylistAdd,
                            contentDescription = "Add to Queue",
                            tint = Color(0xFF6750A4)
                        )
                    }
                }

                IconButton(onClick = onToggleFav) {
                    Icon(
                        imageVector = if (file.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (file.isFavorite) Color(0xFFFFC107) else Color.LightGray
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Delete",
                        tint = Color.Red.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryScreen(
    files: List<PlayedFile>,
    searchQuery: String,
    onQueryChanged: (String) -> Unit,
    onPlayFile: (PlayedFile) -> Unit,
    onDelete: (PlayedFile) -> Unit,
    onToggleFav: (String) -> Unit,
    onQueueFile: (PlayedFile) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Playback History ⏰",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1C1E)
        )
        Text(
            text = "Resume recently opened media or search files",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onQueryChanged,
            placeholder = { Text("Search files by name...") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onQueryChanged("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear search")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().testTag("history_search_input"),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() })
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (files.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🍿", fontSize = 48.sp)
                    Text("No history matches found", fontWeight = FontWeight.Bold, color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(files) { file ->
                    ActivityFileRow(
                        file = file,
                        onPlay = { onPlayFile(file) },
                        onToggleFav = { onToggleFav(file.uriString) },
                        onDelete = { onDelete(file) },
                        onQueue = { onQueueFile(file) }
                    )
                }
            }
        }
    }
}

@Composable
fun FavoritesScreen(
    files: List<PlayedFile>,
    searchQuery: String,
    onQueryChanged: (String) -> Unit,
    onPlayFile: (PlayedFile) -> Unit,
    onToggleFav: (String) -> Unit,
    onQueueFile: (PlayedFile) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "My Favorites 💖",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1C1E)
        )
        Text(
            text = "Quickly access your starred fluffy items",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onQueryChanged,
            placeholder = { Text("Search favorites...") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onQueryChanged("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear search")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().testTag("favorites_search_input"),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() })
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (files.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("💫", fontSize = 48.sp)
                    Text("No favorites yet. Star files to add them here!", fontWeight = FontWeight.Bold, color = Color.Gray, textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(files) { file ->
                    ActivityFileRow(
                        file = file,
                        onPlay = { onPlayFile(file) },
                        onToggleFav = { onToggleFav(file.uriString) },
                        onDelete = {},
                        onQueue = { onQueueFile(file) }
                    )
                }
            }
        }
    }
}

@Composable
fun PlayerWithPlaylistContainer(
    activeFile: ActiveFile,
    viewModel: MainViewModel,
    showPlaylist: Boolean,
    onClosePlaylist: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playlist by viewModel.playlist.collectAsStateWithLifecycle()

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Main player content with beautiful smooth Crossfade transition
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Crossfade(
                targetState = activeFile.category,
                animationSpec = tween(durationMillis = 400, easing = EaseInOutCubic),
                label = "media_format_switch"
            ) { category ->
                when (category) {
                    FileCategory.VIDEO -> VideoPlayerWidget(viewModel)
                    FileCategory.AUDIO -> AudioPlayerWidget(activeFile, viewModel)
                    FileCategory.IMAGE -> ImageViewerWidget(activeFile)
                    FileCategory.DOCUMENT -> PdfDocumentViewerWidget(activeFile, viewModel)
                    FileCategory.TEXT -> TextViewerWidget(activeFile, viewModel)
                    FileCategory.UNKNOWN -> UnknownFormatWidget(activeFile)
                }
            }
        }

        // Playlist Side Panel sliding transition
        AnimatedVisibility(
            visible = showPlaylist,
            enter = slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = spring(stiffness = Spring.StiffnessLow)
            ) + fadeIn(),
            exit = slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = spring(stiffness = Spring.StiffnessLow)
            ) + fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            PlaylistSidePanel(
                playlist = playlist,
                activeFile = activeFile,
                viewModel = viewModel,
                onClose = onClosePlaylist
            )
        }
    }
}

@Composable
fun PlayerViewScreen(
    activeFile: ActiveFile?,
    viewModel: MainViewModel
) {
    if (activeFile == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("🐾", fontSize = 64.sp)
                Text("Select a file from library or browse files to view!", fontWeight = FontWeight.Bold, color = Color.Gray)
            }
        }
        return
    }

    var showPlaylist by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C1017))
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF161B22),
            contentColor = Color.White
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { viewModel.closeActiveFile() }) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Back to Library", tint = Color.White)
                }

                Column(
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = activeFile.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("player_file_title")
                    )
                    Text(
                        text = activeFile.category.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { showPlaylist = !showPlaylist },
                        modifier = Modifier.testTag("toggle_playlist_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.QueueMusic,
                            contentDescription = "Show Playlist",
                            tint = if (showPlaylist) Color(0xFFD0BCFF) else Color.White.copy(alpha = 0.8f)
                        )
                    }

                    IconButton(onClick = { viewModel.toggleFavorite(activeFile.uriString) }) {
                        Icon(
                            imageVector = Icons.Rounded.Favorite,
                            contentDescription = "Favorite",
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            PlayerWithPlaylistContainer(
                activeFile = activeFile,
                viewModel = viewModel,
                showPlaylist = showPlaylist,
                onClosePlaylist = { showPlaylist = false }
            )
        }
    }
}

@Composable
fun VideoPlayerWidget(viewModel: MainViewModel) {
    val context = LocalContext.current
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val speed by viewModel.playbackSpeed.collectAsStateWithLifecycle()
    val position by viewModel.playbackPosition.collectAsStateWithLifecycle()
    val duration by viewModel.mediaDuration.collectAsStateWithLifecycle()

    var scaleMode by remember { mutableStateOf(1) }
    var showSpeedMenu by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = viewModel.exoPlayer
                    useController = false
                }
            },
            update = { view ->
                view.resizeMode = when (scaleMode) {
                    0 -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                    1 -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL
                    else -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                }
            },
            modifier = Modifier.fillMaxSize().testTag("video_view_surface")
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
                .background(Color(0xE0161B22), shape = RoundedCornerShape(24.dp))
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)), shape = RoundedCornerShape(24.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(formatTime(position), color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    
                    Slider(
                        value = if (duration > 0) position.toFloat() / duration else 0f,
                        onValueChange = { viewModel.seekTo((it * duration).toLong()) },
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF58A6FF),
                            activeTrackColor = Color(0xFF58A6FF),
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.weight(1f).testTag("video_slider")
                    )

                    Text(formatTime(duration), color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box {
                        TextButton(
                            onClick = { showSpeedMenu = true },
                            modifier = Modifier.testTag("video_speed_button")
                        ) {
                            Text(text = "Speed: ${speed}x 🐾", color = Color(0xFF58A6FF), fontWeight = FontWeight.Bold)
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

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconButton(onClick = { viewModel.playPreviousInPlaylist() }) {
                            Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous Track", tint = Color.White)
                        }

                        IconButton(onClick = { viewModel.seekTo((position - 10000).coerceAtLeast(0L)) }) {
                            Icon(Icons.Rounded.FastRewind, contentDescription = "Rewind 10s", tint = Color.White.copy(alpha = 0.8f))
                        }

                        IconButton(
                            onClick = { viewModel.togglePlayPause() },
                            modifier = Modifier.size(52.dp).background(Color(0xFF58A6FF), CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color(0xFF0C1017),
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        IconButton(onClick = { viewModel.seekTo((position + 10000).coerceAtMost(duration)) }) {
                            Icon(Icons.Rounded.FastForward, contentDescription = "Forward 10s", tint = Color.White.copy(alpha = 0.8f))
                        }

                        IconButton(onClick = { viewModel.playNextInPlaylist() }) {
                            Icon(Icons.Rounded.SkipNext, contentDescription = "Next Track", tint = Color.White)
                        }
                    }

                    IconButton(
                        onClick = { scaleMode = (scaleMode + 1) % 3 }
                    ) {
                        val icon = when (scaleMode) {
                            0 -> Icons.Default.AspectRatio
                            1 -> Icons.Default.Fullscreen
                            else -> Icons.Default.CropFree
                        }
                        Icon(icon, contentDescription = "Aspect mode", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun AudioPlayerWidget(activeFile: ActiveFile, viewModel: MainViewModel) {
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val position by viewModel.playbackPosition.collectAsStateWithLifecycle()
    val duration by viewModel.mediaDuration.collectAsStateWithLifecycle()
    val speed by viewModel.playbackSpeed.collectAsStateWithLifecycle()

    var showSpeedMenu by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "Vinyl rotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rotation angle"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround
    ) {
        Box(
            modifier = Modifier
                .size(260.dp)
                .clip(CircleShape)
                .background(Color(0xFF161B22))
                .border(BorderStroke(8.dp, Color(0xFF21262D)), CircleShape)
                .testTag("vinyl_player_card"),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .border(BorderStroke(2.dp, Color(0xFF56F3FF).copy(alpha = 0.5f)), CircleShape)
            )

            Column(
                modifier = Modifier
                    .graphicsLayer {
                        if (isPlaying) {
                            rotationAngle.let { rotationZ = it }
                        }
                    }
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.sweepGradient(
                            listOf(Color(0xFF625B71), Color(0xFFD0BCFF), Color(0xFFADC6FF), Color(0xFF625B71))
                        )
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("🐾", fontSize = 64.sp)
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0C1017))
                    .border(BorderStroke(4.dp, Color.White.copy(alpha = 0.4f)), CircleShape)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = activeFile.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "🐺 Furry Lofi Stream Engine • Hifi 320kbps",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF58A6FF)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().height(40.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            for (i in 1..16) {
                val heightScale = remember { Animatable(0.2f) }
                LaunchedEffect(isPlaying) {
                    if (isPlaying) {
                        while (true) {
                            heightScale.animateTo(
                                targetValue = 0.3f + 0.7f * kotlin.random.Random.nextFloat(),
                                animationSpec = tween(kotlin.random.Random.nextInt(150, 350), easing = FastOutSlowInEasing)
                            )
                        }
                    } else {
                        heightScale.animateTo(0.15f)
                    }
                }
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .width(4.dp)
                        .fillMaxHeight(heightScale.value)
                        .clip(CircleShape)
                        .background(Color(0xFF56F3FF))
                )
            }
        }

        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(formatTime(position), color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)

                Slider(
                    value = if (duration > 0) position.toFloat() / duration else 0f,
                    onValueChange = { viewModel.seekTo((it * duration).toLong()) },
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF56F3FF),
                        activeTrackColor = Color(0xFF56F3FF),
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.weight(1f).testTag("audio_slider")
                )

                Text(formatTime(duration), color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Box {
                TextButton(
                    onClick = { showSpeedMenu = true },
                    modifier = Modifier.testTag("audio_speed_button")
                ) {
                    Text(text = "Speed: ${speed}x 🐾", color = Color(0xFF58A6FF), fontWeight = FontWeight.Bold)
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

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconButton(onClick = { viewModel.playPreviousInPlaylist() }) {
                    Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous Track", tint = Color.White, modifier = Modifier.size(28.dp))
                }

                IconButton(onClick = { viewModel.seekTo((position - 10000).coerceAtLeast(0L)) }) {
                    Icon(Icons.Rounded.FastRewind, contentDescription = "Rewind", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(24.dp))
                }

                IconButton(
                    onClick = { viewModel.togglePlayPause() },
                    modifier = Modifier.size(64.dp).background(Color(0xFF56F3FF), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color(0xFF0C1017),
                        modifier = Modifier.size(40.dp)
                    )
                }

                IconButton(onClick = { viewModel.seekTo((position + 10000).coerceAtMost(duration)) }) {
                    Icon(Icons.Rounded.FastForward, contentDescription = "Forward", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(24.dp))
                }

                IconButton(onClick = { viewModel.playNextInPlaylist() }) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = "Next Track", tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }

            IconButton(onClick = { viewModel.seekTo(0L) }) {
                Icon(Icons.Rounded.Replay, contentDescription = "Restart", tint = Color.White)
            }
        }
    }
}

@Composable
fun ImageViewerWidget(activeFile: ActiveFile) {
    var scale by remember { mutableStateOf(1f) }
    var rotationAngle by remember { mutableStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF161B22))
                .testTag("image_view_container"),
            contentAlignment = Alignment.Center
        ) {
            if (activeFile.isSample && activeFile.uriString == "sample_art") {
                Image(
                    painter = painterResource(id = R.drawable.furry_sample_art),
                    contentDescription = activeFile.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            rotationZ = rotationAngle
                        }
                )
            } else if (activeFile.isSample && activeFile.uriString == "sample_launcher") {
                Image(
                    painter = painterResource(id = R.drawable.furry_launcher_icon),
                    contentDescription = activeFile.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            rotationZ = rotationAngle
                        }
                )
            } else {
                AsyncImage(
                    model = activeFile.uriString,
                    contentDescription = activeFile.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            rotationZ = rotationAngle
                        }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF161B22),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                IconButton(onClick = { scale = (scale - 0.25f).coerceAtLeast(0.5f) }) {
                    Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", tint = Color.White)
                }
                
                Text(
                    text = "${(scale * 100).toInt()}%",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                IconButton(onClick = { scale = (scale + 0.25f).coerceAtMost(3.0f) }) {
                    Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", tint = Color.White)
                }

                IconButton(onClick = { rotationAngle = (rotationAngle - 90f) % 360f }) {
                    Icon(Icons.Default.RotateLeft, contentDescription = "Rotate Left", tint = Color.White)
                }

                IconButton(onClick = { rotationAngle = (rotationAngle + 90f) % 360f }) {
                    Icon(Icons.Default.RotateRight, contentDescription = "Rotate Right", tint = Color.White)
                }

                IconButton(onClick = {
                    scale = 1.0f
                    rotationAngle = 0f
                }) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Reset", tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun PdfDocumentViewerWidget(activeFile: ActiveFile, viewModel: MainViewModel) {
    val context = LocalContext.current
    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var currentPageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var pageCount by remember { mutableStateOf(0) }
    var currentPageIndex by remember { mutableStateOf(0) }
    var renderError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(activeFile.uriString) {
        withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(activeFile.uriString)
                val parcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
                if (parcelFileDescriptor != null) {
                    val renderer = PdfRenderer(parcelFileDescriptor)
                    pdfRenderer = renderer
                    pageCount = renderer.pageCount
                    currentPageIndex = activeFile.lastPosition.toInt().coerceIn(0, renderer.pageCount - 1)
                } else {
                    renderError = "Could not open document descriptor"
                }
            } catch (e: Exception) {
                renderError = "PDF rendering failed: ${e.localizedMessage}"
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(currentPageIndex, pdfRenderer) {
        val renderer = pdfRenderer ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            try {
                if (currentPageIndex in 0 until pageCount) {
                    val page = renderer.openPage(currentPageIndex)
                    val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    currentPageBitmap = bitmap
                    page.close()
                    viewModel.updateReadingPosition(currentPageIndex.toLong())
                }
            } catch (e: Exception) {
                renderError = "Page render error: ${e.localizedMessage}"
                e.printStackTrace()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                pdfRenderer?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        if (renderError != null) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(text = renderError!!, color = Color.Red, textAlign = TextAlign.Center)
            }
        } else if (currentPageBitmap != null) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .border(BorderStroke(1.dp, Color.LightGray), RoundedCornerShape(20.dp))
                    .verticalScroll(rememberScrollState())
                    .horizontalScroll(rememberScrollState())
                    .testTag("pdf_viewport"),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = currentPageBitmap!!.asImageBitmap(),
                    contentDescription = "PDF Page ${currentPageIndex + 1}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                )
            }
        } else {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF58A6FF))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF161B22),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { if (currentPageIndex > 0) currentPageIndex-- },
                    enabled = currentPageIndex > 0
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBack,
                        contentDescription = "Previous page",
                        tint = if (currentPageIndex > 0) Color.White else Color.Gray
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Page ${currentPageIndex + 1} of $pageCount",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "🦉 Wise Owl Auto-Resume Active",
                        color = Color(0xFF58A6FF),
                        fontSize = 10.sp
                    )
                }

                IconButton(
                    onClick = { if (currentPageIndex < pageCount - 1) currentPageIndex++ },
                    enabled = currentPageIndex < pageCount - 1
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowForward,
                        contentDescription = "Next page",
                        tint = if (currentPageIndex < pageCount - 1) Color.White else Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun TextViewerWidget(activeFile: ActiveFile, viewModel: MainViewModel) {
    val textContent = activeFile.textContent ?: "Reading file details..."
    var wordWrap by remember { mutableStateOf(true) }
    var fontSizeMultiplier by remember { mutableStateOf(14f) }

    val lines = textContent.split("\n")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("text_viewer_container"),
            color = Color(0xFF161B22),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color(0xFF21262D))
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .horizontalScroll(rememberScrollState(), enabled = !wordWrap)
            ) {
                items(lines.size) { index ->
                    val line = lines[index]
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)
                    ) {
                        Text(
                            text = String.format("%3d  ", index + 1),
                            color = Color.Gray,
                            fontSize = fontSizeMultiplier.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.End,
                            modifier = Modifier.width(36.dp)
                        )

                        Text(
                            text = buildCodeHighlightSpan(line),
                            color = Color.White,
                            fontSize = fontSizeMultiplier.sp,
                            fontFamily = FontFamily.Monospace,
                            softWrap = wordWrap
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF161B22),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                TextButton(
                    onClick = { wordWrap = !wordWrap }
                ) {
                    Text(
                        text = if (wordWrap) "Word Wrap: ON" else "Word Wrap: OFF",
                        color = Color(0xFF58A6FF),
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { fontSizeMultiplier = (fontSizeMultiplier - 2f).coerceAtLeast(10f) }) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease font size", tint = Color.White)
                    }

                    Text(
                        text = "${fontSizeMultiplier.toInt()}sp",
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(onClick = { fontSizeMultiplier = (fontSizeMultiplier + 2f).coerceAtMost(24f) }) {
                        Icon(Icons.Default.Add, contentDescription = "Increase font size", tint = Color.White)
                    }
                }

                Text(
                    text = "🐼 Panda Editor",
                    color = Color(0xFF56F3FF),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

fun buildCodeHighlightSpan(text: String): androidx.compose.ui.text.AnnotatedString {
    val keywords = listOf(
        "package", "import", "class", "interface", "fun", "val", "var",
        "private", "public", "override", "return", "if", "else", "for",
        "while", "when", "null", "true", "false", "this", "super", "object", "string"
    )

    return buildAnnotatedString {
        val words = text.split("(?<=\\b)|(?=\\b)".toRegex())
        for (word in words) {
            if (keywords.contains(word.trim())) {
                withStyle(style = SpanStyle(color = Color(0xFF58A6FF), fontWeight = FontWeight.Bold)) {
                    append(word)
                }
            } else if (word.startsWith("\"") && word.endsWith("\"")) {
                withStyle(style = SpanStyle(color = Color(0xFF56F3FF))) {
                    append(word)
                }
            } else if (word.startsWith("//") || word.startsWith("/*")) {
                withStyle(style = SpanStyle(color = Color.Gray)) {
                    append(word)
                }
            } else {
                append(word)
            }
        }
    }
}

@Composable
fun UnknownFormatWidget(activeFile: ActiveFile) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Text("📦", fontSize = 72.sp)
            Text(
                text = "Unrecognized Format Type",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "File Name: ${activeFile.name}\nMIME: ${activeFile.mimeType}\nSize: ${formatSize(activeFile.size)}",
                color = Color.LightGray,
                textAlign = TextAlign.Center,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
            Text(
                text = "Furry Player was unable to identify a native renderer. You can try streaming it, or open it as a text file if it contains source code!",
                color = Color.Gray,
                textAlign = TextAlign.Center,
                fontSize = 12.sp
            )
        }
    }
}

fun formatTime(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val totalSecs = ms / 1000
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    return String.format("%02d:%02d", mins, secs)
}

fun formatSize(bytes: Long): String {
    if (bytes <= 0L) return "0 KB"
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024
    if (kb < 1024) return "$kb KB"
    val mb = kb / 1024
    if (mb < 1024) return "$mb MB"
    val gb = mb / 1024
    return "$gb GB"
}

fun formatDate(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60000 -> "Just now"
        diff < 3600000 -> "${diff / 60000}m ago"
        diff < 86400000 -> "${diff / 3600000}h ago"
        else -> "${diff / 86400000}d ago"
    }
}
