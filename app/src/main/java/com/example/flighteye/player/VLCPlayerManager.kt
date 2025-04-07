package com.example.flighteye.player

import android.content.Context
import androidx.core.net.toUri
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

class VLCPlayerManager(private val context: Context) {

    private var libVLC: LibVLC? = LibVLC(context, VLCConfig.getDefaultOptions())
    private var mediaPlayer: MediaPlayer? = MediaPlayer(libVLC)
    private var media: Media? = null

    private var isSurfaceAttached = false

    fun attachSurface(videoLayout: VLCVideoLayout) {
        mediaPlayer?.let {
            if (isSurfaceAttached) it.detachViews()
            it.attachViews(videoLayout, null, false, false)
            isSurfaceAttached = true
        }
    }

    fun detachSurface() {
        mediaPlayer?.detachViews()
        isSurfaceAttached = false
    }

    fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying == true
    }

    fun playStream(rtspUrl: String) {
        ensurePlayerInitialized()
        stopPlayback()

        media = Media(libVLC, rtspUrl.toUri()).apply {
            setHWDecoderEnabled(true, true)
        }

        mediaPlayer?.media = media
        mediaPlayer?.play()
    }

    fun playStreamWithRecording(rtspUrl: String, filePath: String) {
        ensurePlayerInitialized()
        stopPlayback()

        media = Media(libVLC, rtspUrl.toUri()).apply {
            setHWDecoderEnabled(true, false)
            addOption(":sout=#duplicate{dst=display,dst=standard{access=file,mux=mp4,dst=$filePath}}")
            addOption(":sout-keep")
            addOption(":network-caching=1000")
        }

        mediaPlayer?.media = media
        mediaPlayer?.play()
    }

    fun stopPlayback() {
        mediaPlayer?.apply {
            stop()
            detachViews()
        }
        media?.release()
        media = null
    }

    fun releasePlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
        libVLC?.release()
        libVLC = null
    }

    private fun ensurePlayerInitialized() {
        if (libVLC == null || mediaPlayer == null) {
            libVLC = LibVLC(context, VLCConfig.getDefaultOptions())
            mediaPlayer = MediaPlayer(libVLC)
        }
    }
}


object VLCPlayerManagerSingleton {
    private var manager: VLCPlayerManager? = null

    fun getInstance(context: Context): VLCPlayerManager {
        if (manager == null) {
            manager = VLCPlayerManager(context.applicationContext)
        }
        return manager!!
    }
}
