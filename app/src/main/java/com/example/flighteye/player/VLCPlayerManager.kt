package com.example.flighteye.player

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.net.toUri
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout
import java.io.File
import android.os.Environment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VLCPlayerManager(context: Context) {
    // Use applicationContext to avoid holding a reference to any Activity.
    private val appContext = context.applicationContext

    companion object {
        private const val TAG = "VLCPlayerManager"
        private const val FOLDER_NAME = "FlightEye"
    }

    // libVLC is heavy to construct — defer initialization until first use on a background call site.
    // Public accessors trigger lazy creation; the underlying Media/MediaPlayer are always accessed
    // via these getters so callers never touch the field before it's ready.
    val libVLC: LibVLC by lazy { LibVLC(appContext, VLCConfig.getDefaultOptions()) }
    val mediaPlayer: MediaPlayer by lazy {
        MediaPlayer(libVLC).also { attachEventListener(it) }
    }

    private var isRecording = false
    private var currentRecordingPath: String? = null
    var onRecordingStopped: ((String) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    // All event callbacks must run on the main thread (Toast, UI state, etc.)
    private val mainHandler = Handler(Looper.getMainLooper())

    private fun attachEventListener(player: MediaPlayer) {
        player.setEventListener { event ->
            mainHandler.post {
                when (event.type) {
                    MediaPlayer.Event.EncounteredError -> {
                        val errorMsg = "VLC playback error: Connection failed"
                        Log.e(TAG, errorMsg)
                        if (isRecording && currentRecordingPath != null) {
                            Log.e(TAG, "Recording failed for: $currentRecordingPath")
                        }
                        isRecording = false
                        onError?.invoke(errorMsg)
                    }
                    MediaPlayer.Event.EndReached, MediaPlayer.Event.Stopped -> {
                        if (isRecording && currentRecordingPath != null) {
                            Log.d(TAG, "Recording completed and saved to: $currentRecordingPath")
                            onRecordingStopped?.invoke(currentRecordingPath!!)
                        }
                        isRecording = false
                    }
                    MediaPlayer.Event.Opening -> Log.d(TAG, "Media opening...")
                    MediaPlayer.Event.Playing -> Log.d(TAG, "Media playing...")
                    MediaPlayer.Event.Buffering -> Log.d(TAG, "Media buffering: ${event.buffering}%")
                }
            }
        }
    }

    fun attachSurface(videoLayout: VLCVideoLayout) {
        try {
            mediaPlayer.attachViews(videoLayout, null, false, false)
            Log.d(TAG, "Surface attached successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach surface: ${e.message}")
            onError?.invoke("Failed to attach video surface: ${e.message}")
        }
    }

    fun detachSurface() {
        try {
            mediaPlayer.detachViews()
            Log.d(TAG, "Surface detached")
        } catch (e: Exception) {
            Log.e(TAG, "Error detaching surface: ${e.message}")
        }
    }

    /**
     * Strips `user:password@` from RTSP URL and returns a Pair of (cleanUrl, credentials?).
     * VLC deprecated in-URL credentials; use --rtsp-user / --rtsp-pwd options instead.
     */
    private fun extractCredentials(url: String): Pair<String, Pair<String, String>?> {
        val regex = Regex("^(rtsp://)([^:/@]+):([^@/]+)@(.+)$")
        val match = regex.matchEntire(url) ?: return url to null
        val (scheme, user, pass, host) = match.destructured
        return "$scheme$host" to (user to pass)
    }

    fun playStream(rtspUrl: String) {
        stopPlayback()
        isRecording = false

        try {
            val (cleanUrl, creds) = extractCredentials(rtspUrl)
            Log.d(TAG, "Starting stream from: $cleanUrl")
            val media = Media(libVLC, cleanUrl.toUri())

            media.setHWDecoderEnabled(true, false)

            // Low-latency options for live streams
            media.addOption(":network-caching=300")
            media.addOption(":live-caching=300")
            media.addOption(":rtsp-tcp")
            media.addOption(":rtsp-timeout=15000")
            media.addOption(":rtsp-frame-buffer-size=600000")
            media.addOption(":avcodec-hw=any")
            media.addOption(":file-caching=300")
            media.addOption(":clock-jitter=0")
            media.addOption(":clock-synchro=0")

            creds?.let { (u, p) ->
                media.addOption(":rtsp-user=$u")
                media.addOption(":rtsp-pwd=$p")
            }

            mediaPlayer.media = media
            media.release()
            mediaPlayer.play()
            Log.d(TAG, "Attempting to connect to stream")
        } catch (e: Exception) {
            Log.e(TAG, "Error playing stream: ${e.message}")
            onError?.invoke("Connection failed: ${e.message}")
        }
    }

    fun playStreamWithRecording(rtspUrl: String, filePath: String? = null) {
        stopPlayback()

        try {
            // Create file in Downloads directory
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "FlightEye_$timeStamp.mp4"

            // Use Downloads/FlightEye directory
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val recordingDir = File(downloadsDir, FOLDER_NAME)
            if (!recordingDir.exists()) {
                recordingDir.mkdirs()
            }

            val recordingFile = File(recordingDir, fileName)
            val recordingPath = recordingFile.absolutePath

            Log.d(TAG, "Will save recording to: $recordingPath")

            val formattedPath = recordingPath.replace(" ", "%20")

            val (cleanUrl, creds) = extractCredentials(rtspUrl)
            val media = Media(libVLC, cleanUrl.toUri())
            media.setHWDecoderEnabled(true, false)

            // Connection settings
            media.addOption(":rtsp-tcp")
            media.addOption(":rtsp-timeout=15000")
            media.addOption(":rtsp-frame-buffer-size=500000")
            media.addOption(":network-caching=300")
            media.addOption(":live-caching=300")
            media.addOption(":clock-jitter=0")
            media.addOption(":clock-synchro=0")

            creds?.let { (u, p) ->
                media.addOption(":rtsp-user=$u")
                media.addOption(":rtsp-pwd=$p")
            }

            // Recording sout: duplicate stream to display and file
            media.addOption(":sout=#duplicate{dst=display,dst=std{access=file,mux=mp4,dst=$formattedPath}}")
            media.addOption(":sout-all")
            media.addOption(":sout-keep")

            // Set the media and play
            mediaPlayer.media = media
            media.release()

            mediaPlayer.play()
            isRecording = true
            currentRecordingPath = recordingPath
            Log.d(TAG, "Recording started to $recordingPath")
        } catch (e: Exception) {
            isRecording = false
            Log.e(TAG, "Error starting recording: ${e.message}")
            onError?.invoke("Failed to start recording: ${e.message}")
        }
    }

    fun stopPlayback() {
        try {
            if (mediaPlayer.isPlaying) {
                if (isRecording && currentRecordingPath != null) {
                    val savedPath = currentRecordingPath!!
                    mediaPlayer.stop()
                    Log.d(TAG, "Recording stopped and saved to: $savedPath")
                    onRecordingStopped?.invoke(savedPath)
                } else {
                    mediaPlayer.stop()
                    Log.d(TAG, "Playback stopped")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping playback: ${e.message}")
        } finally {
            isRecording = false
            currentRecordingPath = null
        }
    }

    fun isPlaying(): Boolean {
        return mediaPlayer.isPlaying
    }

    fun isRecording(): Boolean {
        return isRecording
    }

    fun getCurrentRecordingPath(): String? {
        return currentRecordingPath
    }

    fun release() {
        try {
            stopPlayback()
            mediaPlayer.release()
            libVLC.release()
            Log.d(TAG, "VLC resources released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing resources: ${e.message}")
        }
    }
}