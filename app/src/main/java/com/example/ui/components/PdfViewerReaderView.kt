package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ActiveFile
import com.example.ui.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun PdfViewerReaderView(
    viewModel: MainViewModel,
    activeFile: ActiveFile,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var totalPages by remember { mutableStateOf(1) }
    
    // Resume position / starting page from activeFile's lastPosition
    var currentPage by remember { mutableStateOf(activeFile.lastPosition.toInt().coerceAtLeast(0)) }
    var scaleFactor by remember { mutableStateOf(1.0f) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    // Render the page on background when activeFile or currentPage changes
    LaunchedEffect(activeFile.uriString, currentPage) {
        isLoading = true
        loadError = null
        withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(activeFile.uriString)
                val pfd = if (uri.scheme == "cache") {
                    val fileName = activeFile.uriString.substringAfter("cache://")
                    val cacheFile = File(context.cacheDir, fileName)
                    ParcelFileDescriptor.open(cacheFile, ParcelFileDescriptor.MODE_READ_ONLY)
                } else {
                    context.contentResolver.openFileDescriptor(uri, "r")
                }

                if (pfd != null) {
                    val renderer = PdfRenderer(pfd)
                    totalPages = renderer.pageCount
                    
                    // Safe page indexing
                    val index = currentPage.coerceIn(0, totalPages - 1)
                    if (index != currentPage) {
                        currentPage = index
                    }

                    val page = renderer.openPage(index)
                    
                    // Render 2x higher resolution for high-DPI crisp text rendering
                    val bitmapWidth = page.width * 2
                    val bitmapHeight = page.height * 2
                    val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
                    
                    // Fill background white
                    val canvas = android.graphics.Canvas(bitmap)
                    canvas.drawColor(android.graphics.Color.WHITE)
                    
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    
                    page.close()
                    renderer.close()
                    pfd.close()

                    withContext(Dispatchers.Main) {
                        pageBitmap = bitmap
                        isLoading = false
                        viewModel.updateReadingPosition(currentPage.toLong())
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        loadError = "Unable to open PDF file descriptor."
                        isLoading = false
                    }
                }
            } catch (e: SecurityException) {
                withContext(Dispatchers.Main) {
                    loadError = "Permission denied. Copy the file into the app directory or grant access."
                    isLoading = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadError = "Unable to read PDF: ${e.localizedMessage}"
                    isLoading = false
                }
            }
        }
    }

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
                    modifier = Modifier.testTag("pdf_back_button")
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
                        text = "PDF Reader • $totalPages Pages",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = { viewModel.toggleFavorite(activeFile.uriString) },
                    modifier = Modifier.testTag("pdf_fav_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // PDF Canvas Viewer Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFFE5E5E5))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(0xFF6750A4))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Rendering page ${currentPage + 1}...",
                        color = Color.DarkGray,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else if (loadError != null) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "⚠️",
                        fontSize = 32.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = loadError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                pageBitmap?.let { bitmap ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "PDF Page ${currentPage + 1}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer(
                                    scaleX = scaleFactor,
                                    scaleY = scaleFactor
                                )
                                .testTag("pdf_page_image")
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Custom Glass Reader Controls Panel
        GlassmorphicCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Page Indicator Slider (if multiple pages)
                if (totalPages > 1) {
                    val sliderVal = currentPage.toFloat().coerceIn(0f, (totalPages - 1).toFloat())
                    Slider(
                        value = sliderVal,
                        onValueChange = { currentPage = it.toInt() },
                        valueRange = 0f..(totalPages - 1).toFloat(),
                        steps = if (totalPages > 2) totalPages - 2 else 0,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pdf_page_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF21005D),
                            activeTrackColor = Color(0xFF21005D),
                            inactiveTrackColor = Color(0xFFEADDFF)
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Zoom Modifier Segment
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Zoom:",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        listOf(1.0f, 1.5f, 2.0f).forEach { zoom ->
                            TextButton(
                                onClick = { scaleFactor = zoom },
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(
                                    text = "${zoom}x",
                                    fontWeight = if (scaleFactor == zoom) FontWeight.Bold else FontWeight.Normal,
                                    color = if (scaleFactor == zoom) Color(0xFF21005D) else Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    // Page count display
                    Text(
                        text = "Page ${currentPage + 1} of $totalPages",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF21005D)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Previous / Next Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { if (currentPage > 0) currentPage-- },
                        enabled = currentPage > 0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEADDFF),
                            contentColor = Color(0xFF21005D),
                            disabledContainerColor = Color(0xFFE1E5EB)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                            .testTag("pdf_prev_button")
                    ) {
                        Icon(imageVector = Icons.Default.KeyboardArrowLeft, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Previous", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { if (currentPage < totalPages - 1) currentPage++ },
                        enabled = currentPage < totalPages - 1,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEADDFF),
                            contentColor = Color(0xFF21005D),
                            disabledContainerColor = Color(0xFFE1E5EB)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp)
                            .testTag("pdf_next_button")
                    ) {
                        Text("Next", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(imageVector = Icons.Default.KeyboardArrowRight, contentDescription = null)
                    }
                }
            }
        }
    }
}
