package com.example.ui

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer

object PlaybackManager {
    private var player: ExoPlayer? = null

    fun getSharedPlayer(context: Context): ExoPlayer {
        return player ?: synchronized(this) {
            player ?: ExoPlayer.Builder(context.applicationContext).build().also {
                player = it
            }
        }
    }

    fun hasPlayer(): Boolean {
        return player != null
    }

    fun releasePlayer() {
        player?.release()
        player = null
    }
}
