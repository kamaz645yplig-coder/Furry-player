package com.example.ui

import android.content.Intent
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        try {
            val player = PlaybackManager.getSharedPlayer(this)
            mediaSession = MediaSession.Builder(this, player).build()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player != null && !player.playWhenReady) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        try {
            mediaSession?.run {
                player.release()
                release()
                mediaSession = null
            }
            PlaybackManager.releasePlayer()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        super.onDestroy()
    }
}
