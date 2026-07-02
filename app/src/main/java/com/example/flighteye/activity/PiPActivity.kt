package com.example.flighteye.activity

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Rational
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.example.flighteye.player.VLCPlayerManager
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

class PiPActivity : AppCompatActivity() {
    private val TAG = "PiPActivity"
    private lateinit var videoLayout: VLCVideoLayout
    private lateinit var playerManager: VLCPlayerManager

    private val rtspUrl by lazy {
        intent.getStringExtra("rtsp_url") ?: ""
    }

    private var connectionAttempts = 0
    private val MAX_ATTEMPTS = 5
    private var currentStream = ""

    private var isTransitioningToPiP = false
    private var streamStarted = false
    private var retryPending = false
    private var hasEnteredPiP = false

    private val retryHandler = Handler(Looper.getMainLooper())
    private val retryRunnable = Runnable {
        retryPending = false
        playStream(currentStream)
    }

    // Auto-enter PiP after stream starts playing
    private val autoPiPRunnable = Runnable {
        if (!isInPictureInPictureMode && !isFinishing) {
            enterPiPMode()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        videoLayout = VLCVideoLayout(this)
        setContentView(videoLayout)

        playerManager = VLCPlayerManager(this)
        currentStream = rtspUrl

        // Set up auto-PiP for Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val pipParams = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .setAutoEnterEnabled(true)
                .build()
            setPictureInPictureParams(pipParams)
        }

        playerManager.onError = { error ->
            Log.e(TAG, "Player error: $error")
            connectionAttempts++

            when {
                connectionAttempts < MAX_ATTEMPTS -> {
                    val delay = (connectionAttempts * 2000L).coerceAtMost(5000L)
                    Log.d(TAG, "Reconnecting in ${delay}ms ($connectionAttempts/$MAX_ATTEMPTS)")
                    if (!isInPictureInPictureMode) {
                        Toast.makeText(
                            this,
                            "Reconnecting… ($connectionAttempts/$MAX_ATTEMPTS)",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    retryHandler.removeCallbacks(retryRunnable)
                    retryPending = true
                    retryHandler.postDelayed(retryRunnable, delay)
                }
                else -> {
                    if (!isInPictureInPictureMode) {
                        Toast.makeText(this, "Could not connect to stream", Toast.LENGTH_LONG).show()
                    }
                    Log.w(TAG, "Max retry attempts reached")
                }
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                enterPiPMode()
            }
        })

        playerManager.attachSurface(videoLayout)
        startPlayback()

        // Auto-enter PiP after a brief delay to allow surface initialization
        retryHandler.postDelayed(autoPiPRunnable, 1500)
    }

    /**
     * Extracts credentials from RTSP URL (same logic as VLCPlayerManager).
     */
    private fun extractCredentials(url: String): Pair<String, Pair<String, String>?> {
        val regex = Regex("^(rtsp://)([^:/@]+):([^@/]+)@(.+)$")
        val match = regex.matchEntire(url) ?: return url to null
        val (scheme, user, pass, host) = match.destructured
        return "$scheme$host" to (user to pass)
    }

    private fun playStream(streamUrl: String) {
        if (streamUrl.isBlank()) {
            Log.w(TAG, "No stream URL provided")
            return
        }

        retryHandler.removeCallbacks(retryRunnable)
        retryPending = false

        try {
            val (cleanUrl, creds) = extractCredentials(streamUrl)
            Log.d(TAG, "Starting stream from: $cleanUrl")
            playerManager.stopPlayback()

            val media = Media(playerManager.libVLC, cleanUrl.toUri())
            media.setHWDecoderEnabled(true, false)
            media.addOption(":network-caching=300")
            media.addOption(":live-caching=300")
            media.addOption(":rtsp-tcp")
            media.addOption(":rtsp-timeout=15000")
            media.addOption(":rtsp-frame-buffer-size=600000")
            media.addOption(":avcodec-hw=any")
            media.addOption(":clock-jitter=0")
            media.addOption(":clock-synchro=0")
            media.addOption(":no-audio")

            creds?.let { (u, p) ->
                media.addOption(":rtsp-user=$u")
                media.addOption(":rtsp-pwd=$p")
            }

            playerManager.mediaPlayer.media = media
            media.release()
            playerManager.mediaPlayer.play()
            streamStarted = true
            Log.d(TAG, "Playback started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play stream: ${e.message}")
            playerManager.onError?.invoke("Failed to play stream: ${e.message}")
        }
    }

    private fun startPlayback() {
        connectionAttempts = 0
        playStream(currentStream)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        enterPiPMode()
    }

    private fun enterPiPMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                isTransitioningToPiP = true
                val pipParams = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            setAutoEnterEnabled(true)
                        }
                    }
                    .build()
                enterPictureInPictureMode(pipParams)
                hasEnteredPiP = true
                Log.d(TAG, "Entered PiP mode")
            } catch (e: Exception) {
                isTransitioningToPiP = false
                Log.e(TAG, "Failed to enter PiP mode: ${e.message}", e)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!playerManager.isPlaying() && !retryPending && streamStarted) {
            connectionAttempts = 0
            playStream(currentStream)
            Log.d(TAG, "Resuming playback")
        }
    }

    override fun onPause() {
        super.onPause()
        if (!isInPictureInPictureMode && !isTransitioningToPiP) {
            retryHandler.removeCallbacks(retryRunnable)
            retryHandler.removeCallbacks(autoPiPRunnable)
            playerManager.stopPlayback()
            Log.d(TAG, "Activity paused, playback stopped")
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isInPictureInPictureMode && !isTransitioningToPiP) {
            playerManager.stopPlayback()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        retryHandler.removeCallbacks(retryRunnable)
        retryHandler.removeCallbacks(autoPiPRunnable)
        playerManager.release()
        Log.d(TAG, "PiPActivity destroyed")
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isTransitioningToPiP = false
        Log.d(TAG, "PiP mode changed: $isInPictureInPictureMode")

        if (!isInPictureInPictureMode) {
            // User dismissed PiP or tapped to expand → close
            retryHandler.removeCallbacks(retryRunnable)
            playerManager.stopPlayback()
            finish()
        } else {
            // Entered PiP — restart stream only if it dropped
            connectionAttempts = 0
            if (!playerManager.isPlaying()) {
                Log.d(TAG, "Stream not active in PiP, restarting")
                playStream(currentStream)
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            enterPiPMode()
        } else {
            super.onBackPressed()
        }
    }
}