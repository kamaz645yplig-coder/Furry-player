package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.FileRepository
import com.example.data.PlayedFile
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

enum class Tab {
    LIBRARY,
    HISTORY,
    FAVORITES,
    PLAYER
}

enum class FileCategory(val icon: String, val label: String) {
    VIDEO("video", "Video"),
    AUDIO("audio", "Audio"),
    IMAGE("image", "Image"),
    DOCUMENT("document", "PDF Document"),
    TEXT("text", "Text & Code"),
    UNKNOWN("unknown", "Other")
}

data class ActiveFile(
    val uriString: String,
    val name: String,
    val size: Long,
    val mimeType: String,
    val category: FileCategory,
    val isSample: Boolean = false,
    val textContent: String? = null,
    val lastPosition: Long = 0L
)

class MainViewModel(
    application: Application,
    private val repository: FileRepository
) : AndroidViewModel(application) {

    private val context: Context get() = getApplication()

    // Tab State
    private val _currentTab = MutableStateFlow(Tab.LIBRARY)
    val currentTab: StateFlow<Tab> = _currentTab.asStateFlow()

    // Active File State
    private val _activeFile = MutableStateFlow<ActiveFile?>(null)
    val activeFile: StateFlow<ActiveFile?> = _activeFile.asStateFlow()

    // Playback state from ExoPlayer
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackPosition = MutableStateFlow(0L)
    val playbackPosition: StateFlow<Long> = _playbackPosition.asStateFlow()

    private val _mediaDuration = MutableStateFlow(0L)
    val mediaDuration: StateFlow<Long> = _mediaDuration.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Playlist / Queue State
    private val _playlist = MutableStateFlow<List<ActiveFile>>(emptyList())
    val playlist: StateFlow<List<ActiveFile>> = _playlist.asStateFlow()

    // Playback listener property so it can be cleanly detached later
    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
            if (isPlaying) {
                startProgressTracker()
            } else {
                stopProgressTracker()
            }
        }

        override fun onPlaybackStateChanged(state: Int) {
            if (state == Player.STATE_READY) {
                _mediaDuration.value = exoPlayer.duration
            } else if (state == Player.STATE_ENDED) {
                _isPlaying.value = false
                stopProgressTracker()
                playNextInPlaylist()
            }
        }
    }

    // ExoPlayer Instance
    val exoPlayer: ExoPlayer by lazy {
        PlaybackManager.getSharedPlayer(context).apply {
            addListener(playerListener)
        }
    }

    private var progressJob: Job? = null

    // Room Database Streams
    val allFiles = repository.allFiles.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val recentFiles = combine(repository.recentFiles, _searchQuery) { files, query ->
        if (query.isBlank()) files else files.filter { it.fileName.contains(query, ignoreCase = true) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val favoriteFiles = combine(repository.favoriteFiles, _searchQuery) { files, query ->
        if (query.isBlank()) files else files.filter { it.fileName.contains(query, ignoreCase = true) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val sampleFiles = listOf(
        PlayedFile(
            id = -1,
            uriString = "cache://Universal_Player_Introduction.pdf",
            fileName = "Universal Player Guide.pdf",
            fileSize = 145000L,
            mimeType = "application/pdf"
        ),
        PlayedFile(
            id = -2,
            uriString = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
            fileName = "Lofi Ambient Beat (Online Stream).mp3",
            fileSize = 6120000L,
            mimeType = "audio/mp3"
        ),
        PlayedFile(
            id = -3,
            uriString = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            fileName = "Nature Wild Trailer (Online Stream).mp4",
            fileSize = 15800000L,
            mimeType = "video/mp4"
        ),
        PlayedFile(
            id = -4,
            uriString = "https://images.unsplash.com/photo-1506318137071-a8e063b4bec0?auto=format&fit=crop&w=1200&q=80",
            fileName = "Deep Cosmic Wallpaper (Online Stream).webp",
            fileSize = 850000L,
            mimeType = "image/webp"
        ),
        PlayedFile(
            id = -5,
            uriString = "sample://Player_Core_Implementation.kt",
            fileName = "Player_Core_Implementation.kt",
            fileSize = 4500L,
            mimeType = "text/plain"
        )
    )

    init {
        // Prepare sample PDF in cache so the user can test the PDF reader
        viewModelScope.launch {
            createSamplePdfInCache()
        }
    }

    fun selectTab(tab: Tab) {
        _currentTab.value = tab
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addToQueue(uri: Uri, name: String, mimeType: String, size: Long, isSample: Boolean = false) {
        viewModelScope.launch {
            val uriString = uri.toString()
            val category = getFileCategory(name, mimeType)
            
            var textContent: String? = null
            if (category == FileCategory.TEXT) {
                textContent = loadTextFromUri(uri)
            }

            val fileToAdd = ActiveFile(
                uriString = uriString,
                name = name,
                size = size,
                mimeType = mimeType,
                category = category,
                isSample = isSample,
                textContent = textContent
            )
            val currentList = _playlist.value.toMutableList()
            if (!currentList.any { it.uriString == fileToAdd.uriString }) {
                currentList.add(fileToAdd)
                _playlist.value = currentList
            }
            
            // Also insert/update file in DB history so it shows in the UI lists
            val savedFile = repository.getFileByUri(uriString)
            val playedFile = PlayedFile(
                uriString = uriString,
                fileName = name,
                fileSize = size,
                mimeType = mimeType,
                isFavorite = savedFile?.isFavorite ?: false,
                lastPosition = savedFile?.lastPosition ?: 0L
            )
            repository.insertOrUpdateFile(playedFile)

            // If nothing is currently active, play this file!
            if (_activeFile.value == null) {
                playFile(uri, name, mimeType, size, isSample)
            }
        }
    }

    fun addPlayedFileToQueue(playedFile: PlayedFile) {
        addToQueue(
            uri = Uri.parse(playedFile.uriString),
            name = playedFile.fileName,
            mimeType = playedFile.mimeType,
            size = playedFile.fileSize
        )
    }

    fun removeFromPlaylist(index: Int) {
        val currentList = _playlist.value.toMutableList()
        if (index in currentList.indices) {
            val removed = currentList.removeAt(index)
            _playlist.value = currentList
            // If the removed file was active, play next or close
            if (_activeFile.value?.uriString == removed.uriString) {
                if (currentList.isNotEmpty()) {
                    val nextIndex = index.coerceAtMost(currentList.lastIndex)
                    val nextFile = currentList[nextIndex]
                    playFile(
                        uri = Uri.parse(nextFile.uriString),
                        name = nextFile.name,
                        mimeType = nextFile.mimeType,
                        size = nextFile.size,
                        isSample = nextFile.isSample
                    )
                } else {
                    closeActiveFile()
                }
            }
        }
    }

    fun playNextInPlaylist() {
        val currentList = _playlist.value
        val active = _activeFile.value
        if (currentList.isNotEmpty() && active != null) {
            val currentIndex = currentList.indexOfFirst { it.uriString == active.uriString }
            if (currentIndex != -1 && currentIndex < currentList.lastIndex) {
                val nextFile = currentList[currentIndex + 1]
                playFile(
                    uri = Uri.parse(nextFile.uriString),
                    name = nextFile.name,
                    mimeType = nextFile.mimeType,
                    size = nextFile.size,
                    isSample = nextFile.isSample
                )
            }
        }
    }

    fun playPreviousInPlaylist() {
        val currentList = _playlist.value
        val active = _activeFile.value
        if (currentList.isNotEmpty() && active != null) {
            val currentIndex = currentList.indexOfFirst { it.uriString == active.uriString }
            if (currentIndex > 0) {
                val prevFile = currentList[currentIndex - 1]
                playFile(
                    uri = Uri.parse(prevFile.uriString),
                    name = prevFile.name,
                    mimeType = prevFile.mimeType,
                    size = prevFile.size,
                    isSample = prevFile.isSample
                )
            }
        }
    }

    fun clearPlaylist() {
        _playlist.value = emptyList()
        closeActiveFile()
    }

    fun reorderPlaylist(fromIndex: Int, toIndex: Int) {
        val currentList = _playlist.value.toMutableList()
        if (fromIndex in currentList.indices && toIndex in currentList.indices) {
            val item = currentList.removeAt(fromIndex)
            currentList.add(toIndex, item)
            _playlist.value = currentList
        }
    }

    fun playFile(uri: Uri, name: String, mimeType: String, size: Long, isSample: Boolean = false) {
        viewModelScope.launch {
            val uriString = uri.toString()
            val category = getFileCategory(name, mimeType)

            // Save/Update in DB history
            val savedFile = repository.getFileByUri(uriString)
            val initialPosition = savedFile?.lastPosition ?: 0L

            val playedFile = PlayedFile(
                uriString = uriString,
                fileName = name,
                fileSize = size,
                mimeType = mimeType,
                isFavorite = savedFile?.isFavorite ?: false,
                lastPosition = initialPosition
            )
            repository.insertOrUpdateFile(playedFile)

            var textContent: String? = null
            if (category == FileCategory.TEXT) {
                textContent = loadTextFromUri(uri)
            }

            // Load into active file state
            val active = ActiveFile(
                uriString = uriString,
                name = name,
                size = size,
                mimeType = mimeType,
                category = category,
                isSample = isSample,
                textContent = textContent,
                lastPosition = initialPosition
            )
            _activeFile.value = active

            // Add to playlist if not present
            val currentList = _playlist.value.toMutableList()
            if (!currentList.any { it.uriString == uriString }) {
                currentList.add(active)
                _playlist.value = currentList
            }

            // If audio or video, setup ExoPlayer
            if (category == FileCategory.AUDIO || category == FileCategory.VIDEO) {
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
                
                // Build metadata for system notification/lockscreen integration
                val mediaMetadata = androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(name)
                    .setArtist("Universal Player")
                    .setMediaType(
                        if (category == FileCategory.AUDIO) {
                            androidx.media3.common.MediaMetadata.MEDIA_TYPE_MUSIC
                        } else {
                            androidx.media3.common.MediaMetadata.MEDIA_TYPE_VIDEO
                        }
                    )
                    .build()

                val mediaItem = MediaItem.Builder()
                    .setUri(uri)
                    .setMediaMetadata(mediaMetadata)
                    .build()

                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.playbackParameters = PlaybackParameters(_playbackSpeed.value)
                exoPlayer.prepare()
                if (initialPosition > 0L) {
                    exoPlayer.seekTo(initialPosition)
                }
                exoPlayer.playWhenReady = true
                _isPlaying.value = true
                startProgressTracker()

                // Start PlaybackService for background play support
                try {
                    val serviceIntent = Intent(context, PlaybackService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Switch to player tab
            _currentTab.value = Tab.PLAYER
        }
    }

    fun closeActiveFile() {
        val active = _activeFile.value
        if (active != null) {
            viewModelScope.launch {
                // Save last position
                val currentPos = if (active.category == FileCategory.AUDIO || active.category == FileCategory.VIDEO) {
                    exoPlayer.currentPosition
                } else {
                    active.lastPosition
                }
                repository.updateLastPosition(active.uriString, currentPos)
                
                // Stop media
                if (active.category == FileCategory.AUDIO || active.category == FileCategory.VIDEO) {
                    exoPlayer.stop()
                    stopProgressTracker()
                    _isPlaying.value = false
                }
                
                _activeFile.value = null
                _currentTab.value = Tab.LIBRARY
            }
        }
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
            _isPlaying.value = false
        } else {
            exoPlayer.play()
            _isPlaying.value = true
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
        _playbackPosition.value = positionMs
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        exoPlayer.playbackParameters = PlaybackParameters(speed)
    }

    fun toggleFavorite(uriString: String) {
        viewModelScope.launch {
            repository.toggleFavorite(uriString)
            // Sync with active file if active
            val active = _activeFile.value
            if (active != null && active.uriString == uriString) {
                // To force state recomposition, we can fetch updated favorites or keep track
            }
        }
    }

    fun updateReadingPosition(position: Long) {
        val active = _activeFile.value
        if (active != null) {
            _activeFile.value = active.copy(lastPosition = position)
            viewModelScope.launch {
                repository.updateLastPosition(active.uriString, position)
            }
        }
    }

    fun deleteFile(file: PlayedFile) {
        viewModelScope.launch {
            if (_activeFile.value?.uriString == file.uriString) {
                closeActiveFile()
            }
            repository.deleteFile(file)
        }
    }

    private fun startProgressTracker() {
        stopProgressTracker()
        progressJob = viewModelScope.launch {
            while (true) {
                _playbackPosition.value = exoPlayer.currentPosition
                delay(500)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun loadTextFromUri(uri: Uri): String {
        val uriString = uri.toString()
        if (uriString.startsWith("sample://")) {
            return """
                package com.example.ui

                import android.app.Application
                import androidx.lifecycle.AndroidViewModel
                import androidx.media3.exoplayer.ExoPlayer

                /**
                 * Universal Player Core Implementation
                 * Fully on-device, responsive, high-performance file resolution.
                 */
                class UniversalPlayerEngine(application: Application) : AndroidViewModel(application) {
                    
                    private val player: ExoPlayer by lazy {
                        ExoPlayer.Builder(getApplication()).build()
                    }

                    fun initialize() {
                        // High fidelity media rendering
                        player.playWhenReady = true
                    }
                    
                    fun release() {
                        player.release()
                    }
                }
            """.trimIndent()
        }
        return try {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                // Read max 100KB to avoid memory pressure
                val charBuffer = CharArray(1024 * 100)
                val readBytes = reader.read(charBuffer)
                if (readBytes > 0) {
                    String(charBuffer, 0, readBytes)
                } else {
                    ""
                }
            } ?: ""
        } catch (e: Exception) {
            "Error reading file contents: ${e.localizedMessage}"
        }
    }

    fun getFileCategory(name: String, mime: String): FileCategory {
        val lowercaseMime = mime.lowercase()
        if (lowercaseMime.startsWith("video/")) return FileCategory.VIDEO
        if (lowercaseMime.startsWith("audio/")) return FileCategory.AUDIO
        if (lowercaseMime.startsWith("image/")) return FileCategory.IMAGE
        if (lowercaseMime == "application/pdf") return FileCategory.DOCUMENT
        if (lowercaseMime.startsWith("text/") || lowercaseMime.contains("json") || lowercaseMime.contains("javascript") || lowercaseMime.contains("xml") || lowercaseMime.contains("yaml") || lowercaseMime.contains("toml")) return FileCategory.TEXT
        
        val ext = name.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "mp4", "mkv", "webm", "avi", "3gp", "mov", "flv", "wmv" -> FileCategory.VIDEO
            "mp3", "wav", "flac", "aac", "ogg", "m4a", "wma", "amr" -> FileCategory.AUDIO
            "jpg", "jpeg", "png", "webp", "gif", "bmp", "svg" -> FileCategory.IMAGE
            "pdf" -> FileCategory.DOCUMENT
            "txt", "md", "csv", "json", "xml", "html", "js", "ts", "kt", "java", "css", "py", "sh", "yaml", "toml", "log" -> FileCategory.TEXT
            else -> FileCategory.UNKNOWN
        }
    }

    // Helper to generate a sample PDF in cache
    private fun createSamplePdfInCache(): File {
        val file = File(context.cacheDir, "Universal_Player_Introduction.pdf")
        if (file.exists()) return file
        
        val pdfDocument = android.graphics.pdf.PdfDocument()
        
        // Page 1
        val pageInfo1 = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page1 = pdfDocument.startPage(pageInfo1)
        val canvas1 = page1.canvas
        val paint = android.graphics.Paint()
        
        // Draw elegant title
        paint.textSize = 26f
        paint.isFakeBoldText = true
        paint.color = android.graphics.Color.rgb(18, 30, 49) // Deep Slate
        canvas1.drawText("Universal Player Guide", 50f, 80f, paint)
        
        // Subtitle
        paint.textSize = 14f
        paint.isFakeBoldText = false
        paint.color = android.graphics.Color.GRAY
        canvas1.drawText("High-fidelity multi-format player for Android", 50f, 110f, paint)
        
        // Horizontal Line
        paint.color = android.graphics.Color.LTGRAY
        canvas1.drawLine(50f, 130f, 545f, 130f, paint)
        
        // Core features header
        paint.textSize = 16f
        paint.isFakeBoldText = true
        paint.color = android.graphics.Color.rgb(103, 58, 183) // Purple
        canvas1.drawText("Key Features", 50f, 170f, paint)
        
        // List points
        paint.textSize = 12f
        paint.isFakeBoldText = false
        paint.color = android.graphics.Color.BLACK
        canvas1.drawText("• Video Player: Supports MP4, MKV, WebM with hardware acceleration.", 50f, 205f, paint)
        canvas1.drawText("• Audio Player: Supports MP3, WAV, FLAC, complete with speed control.", 50f, 235f, paint)
        canvas1.drawText("• Document Viewer: Native PDF page renderer with zoom and resume.", 50f, 265f, paint)
        canvas1.drawText("• Text Viewer: Formatted monospace typography with line numbering.", 50f, 295f, paint)
        canvas1.drawText("• Image Viewer: Fullscreen image load with smart hardware scaling.", 50f, 325f, paint)
        
        // Accent Info Box
        paint.color = android.graphics.Color.argb(25, 33, 150, 243) // Semi-transparent Blue
        canvas1.drawRect(50f, 370f, 545f, 480f, paint)
        
        paint.color = android.graphics.Color.rgb(33, 150, 243)
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas1.drawText("Offline and Secure", 70f, 405f, paint)
        
        paint.color = android.graphics.Color.BLACK
        paint.textSize = 11f
        paint.isFakeBoldText = false
        canvas1.drawText("All file resolving, parsing, and rendering happens fully on your device.", 70f, 430f, paint)
        canvas1.drawText("No server-side processing, keeping your confidential files private and secure.", 70f, 450f, paint)
        
        // Footer
        paint.color = android.graphics.Color.GRAY
        paint.textSize = 10f
        canvas1.drawText("Page 1 of 2", 270f, 800f, paint)
        
        pdfDocument.finishPage(page1)
        
        // Page 2
        val pageInfo2 = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 2).create()
        val page2 = pdfDocument.startPage(pageInfo2)
        val canvas2 = page2.canvas
        
        paint.textSize = 20f
        paint.isFakeBoldText = true
        paint.color = android.graphics.Color.rgb(18, 30, 49)
        canvas2.drawText("File Formats Catalog", 50f, 80f, paint)
        
        paint.textSize = 11f
        paint.isFakeBoldText = false
        paint.color = android.graphics.Color.BLACK
        
        var yPos = 130f
        val catalog = listOf(
            "🎥 Video Format:" to "MP4, MKV, WebM, 3GP, AVI, FLV",
            "🎵 Audio Format:" to "MP3, WAV, FLAC, M4A, OGG, AAC",
            "🖼️ Image Format:" to "PNG, JPEG, WebP, GIF, BMP",
            "📄 Document Format:" to "PDF (interactive paginated reader)",
            "📝 Text & Code Format:" to "TXT, MD, CSV, JSON, XML, Java, Kotlin, Log"
        )
        
        for ((cat, desc) in catalog) {
            paint.isFakeBoldText = true
            paint.color = android.graphics.Color.rgb(103, 58, 183)
            canvas2.drawText(cat, 50f, yPos, paint)
            
            paint.isFakeBoldText = false
            paint.color = android.graphics.Color.BLACK
            canvas2.drawText(desc, 180f, yPos, paint)
            
            yPos += 35f
        }
        
        // Draw decorative footer element
        paint.color = android.graphics.Color.argb(20, 0, 0, 0)
        canvas2.drawRect(50f, yPos + 30f, 545f, yPos + 150f, paint)
        
        paint.color = android.graphics.Color.rgb(46, 125, 50) // Green
        paint.textSize = 13f
        paint.isFakeBoldText = true
        canvas2.drawText("Built with Kotlin & Jetpack Compose", 70f, yPos + 70f, paint)
        
        paint.color = android.graphics.Color.BLACK
        paint.textSize = 11f
        paint.isFakeBoldText = false
        canvas2.drawText("Leverages modern Android Jetpack libraries for smooth asynchronous rendering,", 70f, yPos + 95f, paint)
        canvas2.drawText("local database caching, and adaptive edge-to-edge UI layouts.", 70f, yPos + 115f, paint)
        
        // Footer
        paint.color = android.graphics.Color.GRAY
        paint.textSize = 10f
        canvas2.drawText("Page 2 of 2", 270f, 800f, paint)
        
        pdfDocument.finishPage(page2)
        
        try {
            val fos = FileOutputStream(file)
            pdfDocument.writeTo(fos)
            pdfDocument.close()
            fos.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return file
    }

    override fun onCleared() {
        super.onCleared()
        // Detach listener to prevent memory leaks, while leaving the ExoPlayer alive for background playback
        try {
            exoPlayer.removeListener(playerListener)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
