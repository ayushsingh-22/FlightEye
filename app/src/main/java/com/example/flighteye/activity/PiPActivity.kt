package com.example.flighteye.activity

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Bundle
import android.util.Rational
import android.view.SurfaceView
import androidx.appcompat.app.AppCompatActivity
import com.example.flighteye.player.VLCPlayerManager
import org.videolan.libvlc.util.VLCVideoLayout

class PiPActivity : AppCompatActivity() {

    private lateinit var videoLayout: VLCVideoLayout
    private lateinit var playerManager: VLCPlayerManager

    private val rtspUrl by lazy {
        intent.getStringExtra("rtsp_url") ?: "rtsp://192.168.1.2:5540/ch0"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        videoLayout = VLCVideoLayout(this)
        setContentView(videoLayout)

        playerManager.attachSurface(videoLayout)

    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        enterPiPMode()
    }

    private fun enterPiPMode() {
        val aspectRatio = Rational(16, 9)
        val pipParams = PictureInPictureParams.Builder()
            .setAspectRatio(aspectRatio)
            .build()
        enterPictureInPictureMode(pipParams)
    }

    override fun onStop() {
        super.onStop()
        playerManager.stopPlayback()
        finish()
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (!isInPictureInPictureMode) {
            finish() // close when PiP exits
        }
    }
}
