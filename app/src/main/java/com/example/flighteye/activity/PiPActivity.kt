package com.example.flighteye.activity

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Rational
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.example.flighteye.player.VLCPlayerManager
import org.videolan.libvlc.Media
import org.videolan.libvlc.util.VLCVideoLayout

class PiPActivity : AppCompatActivity() {
    private val TAG = "PiPActivity"
    private lateinit var videoLayout: VLCVideoLayout
    private lateinit var playerManager: VLCPlayerManager

    // Use a test stream if your actual stream fails
    private val rtspUrl by lazy {
        intent.getStringExtra("rtsp_url") ?: "rtsp://admin:admin@192.168.31.244:1935"
    }

    // Fallback stream for testing
    private val fallbackStream = "rtsp://wowzaec2demo.streamlock.net/vod/mp4:BigBuckBunny_115k.mp4"

    private var connectionAttempts = 0
    private val MAX_ATTEMPTS = 3
    private var currentStream = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Initialize the video layout and set it as content
        videoLayout = VLCVideoLayout(this)
        setContentView(videoLayout)

        // Initialize player manager
        playerManager = VLCPlayerManager(this)
        currentStream = rtspUrl

        // Set up error handling with automatic retry
        playerManager.onError = { error ->
            Log.e(TAG, "Player error: $error")
            connectionAttempts++

            if (connectionAttempts < MAX_ATTEMPTS) {
                Toast.makeText(this, "Reconnecting... (${connectionAttempts}/${MAX_ATTEMPTS})", Toast.LENGTH_SHORT).show()

                // Try reconnecting after a delay
                videoLayout.postDelayed({
                    playStream(currentStream)
                }, 2000)
            } else if (currentStream != fallbackStream) {
                // Switch to fallback stream after max attempts
                Toast.makeText(this, "Switching to test stream...", Toast.LENGTH_SHORT).show()
                currentStream = fallbackStream
                connectionAttempts = 0

                videoLayout.postDelayed({
                    playStream(currentStream)
                }, 1000)
            } else {
                Toast.makeText(this, "Could not connect to any stream", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        // Handle back button press for modern Android versions
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                enterPiPMode()
            }
        })

        // Attach surface and start playback
        playerManager.attachSurface(videoLayout)
        startPlayback()
    }

    private fun playStream(streamUrl: String) {
        try {
            Log.d(TAG, "Starting stream from: $streamUrl")

            // Stop any existing playback
            playerManager.stopPlayback()

            val media = Media(playerManager.libVLC, streamUrl.toUri())

            // Enhanced playback options for better PiP performance
            media.setHWDecoderEnabled(true, false)

            // Add all PiP-specific options to the media directly
            media.addOption(":network-caching=5000")
            media.addOption(":live-caching=5000")
            media.addOption(":rtsp-tcp")
            media.addOption(":rtsp-timeout=10000")
            media.addOption(":rtsp-frame-buffer-size=600000")
            media.addOption(":avcodec-hw=any")
            media.addOption(":file-caching=5000")
            media.addOption(":clock-jitter=0")
            media.addOption(":no-audio")
            media.addOption(":video-filter=none")

            playerManager.mediaPlayer.media = media
            media.release()
            playerManager.mediaPlayer.play()

            Log.d(TAG, "Playback started with URL: $streamUrl")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play stream: ${e.message}")
            playerManager.onError?.invoke("Failed to play stream: ${e.message}")
        }
    }

    private fun startPlayback() {
        playStream(currentStream)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        enterPiPMode()
    }

    private fun enterPiPMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                Log.d(TAG, "Attempting to enter PiP mode. Is playing: ${playerManager.isPlaying()}")

                // Don't enter PiP if we're not playing anything
                if (!playerManager.isPlaying()) {
                    Log.d(TAG, "Not entering PiP mode, no active stream")
                    return
                }

                val aspectRatio = Rational(16, 9)
                val pipParams = PictureInPictureParams.Builder()
                    .setAspectRatio(aspectRatio)
                    .build()

                enterPictureInPictureMode(pipParams)
                Log.d(TAG, "Successfully entered PiP mode")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to enter PiP mode: ${e.message}", e)
                Toast.makeText(this, "Failed to enter PiP mode", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!playerManager.isPlaying()) {
            // Reset connection attempts on resume
            connectionAttempts = 0
            playStream(currentStream)
            Log.d(TAG, "Resuming playback")
        }
    }

    override fun onPause() {
        super.onPause()
        // Important: Don't stop playback when in PiP mode
        if (!isInPictureInPictureMode) {
            Log.d(TAG, "Activity paused, stopping playback")
            playerManager.stopPlayback()
        } else {
            Log.d(TAG, "Activity paused but in PiP mode, continuing playback")
        }
    }

    // This is deprecated but kept for compatibility with older Android versions
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            enterPiPMode()
        } else {
            super.onBackPressed()
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isInPictureInPictureMode) {
            playerManager.stopPlayback()
            Log.d(TAG, "Activity stopped, stopping playback")
        } else {
            Log.d(TAG, "Activity stopped but in PiP mode, continuing playback")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        playerManager.release()
        Log.d(TAG, "PiPActivity destroyed, resources released")
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        Log.d(TAG, "PiP mode changed: $isInPictureInPictureMode")

        if (!isInPictureInPictureMode) {
            // If exiting PiP mode, stop playback and close activity
            playerManager.stopPlayback()
            finish()
        } else {
            // Critical: Restart the stream when entering PiP mode to avoid black screen
            connectionAttempts = 0
            playStream(currentStream)
            Log.d(TAG, "Restarted stream in PiP mode")
        }
    }
}