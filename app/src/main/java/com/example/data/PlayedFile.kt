package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "played_files")
data class PlayedFile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val uriString: String,
    val fileName: String,
    val fileSize: Long,
    val mimeType: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val lastPosition: Long = 0L
)
