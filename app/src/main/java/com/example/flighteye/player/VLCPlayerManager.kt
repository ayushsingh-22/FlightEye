package com.example.flighteye.player

import android.content.Context
import androidx.core.net.toUri
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

class VLCPlayerManager(context: Context) {

    var libVLC: LibVLC = LibVLC(context, VLCConfig.getDefaultOptions())
    var mediaPlayer: MediaPlayer = MediaPlayer(libVLC)



    fun attachSurface(videoLayout: VLCVideoLayout) {
        mediaPlayer.attachViews(videoLayout, null, false, false)
    }


    fun playStream(rtspUrl: String) {
        val media = Media(libVLC, rtspUrl.toUri())
        media.setHWDecoderEnabled(true, true)

        mediaPlayer.media = media
        mediaPlayer.play()
    }

    fun playStreamWithRecording(rtspUrl: String, filePath: String) {
        val media = Media(libVLC, rtspUrl.toUri())

        media.setHWDecoderEnabled(true, false)

        // Record + Play simultaneously
        media.addOption(":sout=#duplicate{dst=display,dst=standard{access=file,mux=mp4,dst=$filePath}}")
        media.addOption(":sout-keep")
        media.addOption(":network-caching=1000")

        mediaPlayer.media = media
        mediaPlayer.play()
    }


    fun stopPlayback() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()
        }
    }

}
