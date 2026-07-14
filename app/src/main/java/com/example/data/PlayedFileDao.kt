package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayedFileDao {
    @Query("SELECT * FROM played_files ORDER BY timestamp DESC")
    fun getAllPlayedFiles(): Flow<List<PlayedFile>>

    @Query("SELECT * FROM played_files ORDER BY timestamp DESC LIMIT 30")
    fun getRecentFiles(): Flow<List<PlayedFile>>

    @Query("SELECT * FROM played_files WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteFiles(): Flow<List<PlayedFile>>

    @Query("SELECT * FROM played_files WHERE uriString = :uriString LIMIT 1")
    suspend fun getFileByUri(uriString: String): PlayedFile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayedFile(file: PlayedFile)

    @Update
    suspend fun updatePlayedFile(file: PlayedFile)

    @Delete
    suspend fun deletePlayedFile(file: PlayedFile)

    @Query("DELETE FROM played_files WHERE uriString = :uriString")
    suspend fun deletePlayedFileByUri(uriString: String)
}
