package com.example.data

import kotlinx.coroutines.flow.Flow

class FileRepository(private val dao: PlayedFileDao) {
    val allFiles: Flow<List<PlayedFile>> = dao.getAllPlayedFiles()
    val recentFiles: Flow<List<PlayedFile>> = dao.getRecentFiles()
    val favoriteFiles: Flow<List<PlayedFile>> = dao.getFavoriteFiles()

    suspend fun getFileByUri(uriString: String): PlayedFile? {
        return dao.getFileByUri(uriString)
    }

    suspend fun insertOrUpdateFile(file: PlayedFile) {
        val existing = dao.getFileByUri(file.uriString)
        if (existing != null) {
            val updated = existing.copy(
                fileName = file.fileName,
                fileSize = file.fileSize,
                mimeType = file.mimeType,
                timestamp = System.currentTimeMillis(),
                lastPosition = if (file.lastPosition != 0L) file.lastPosition else existing.lastPosition
            )
            dao.updatePlayedFile(updated)
        } else {
            dao.insertPlayedFile(file)
        }
    }

    suspend fun toggleFavorite(uriString: String) {
        val existing = dao.getFileByUri(uriString)
        if (existing != null) {
            dao.updatePlayedFile(existing.copy(isFavorite = !existing.isFavorite))
        }
    }

    suspend fun updateLastPosition(uriString: String, position: Long) {
        val existing = dao.getFileByUri(uriString)
        if (existing != null) {
            dao.updatePlayedFile(existing.copy(lastPosition = position))
        }
    }

    suspend fun deleteFile(file: PlayedFile) {
        dao.deletePlayedFile(file)
    }

    suspend fun deleteFileByUri(uriString: String) {
        dao.deletePlayedFileByUri(uriString)
    }
}
