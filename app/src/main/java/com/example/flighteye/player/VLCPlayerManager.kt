package com.example.flighteye.player

import android.content.Context
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

class VLCPlayerManager(private val context: Context) {
    companion object {
        private const val TAG = "VLCPlayerManager"
        private const val FOLDER_NAME = "Flight Eye"
    }

    var libVLC: LibVLC = LibVLC(context, VLCConfig.getDefaultOptions())
    var mediaPlayer: MediaPlayer = MediaPlayer(libVLC)

    private var isRecording = false
    private var currentRecordingPath: String? = null
    var onRecordingStopped: ((String) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    init {
        mediaPlayer.setEventListener { event ->
            when (event.type) {
                MediaPlayer.Event.EncounteredError -> {
                    // Change from event.lengthChanged to a more descriptive message
                    val errorMsg = "VLC playback error: Connection failed"
                    Log.e(TAG, errorMsg)
                    onError?.invoke(errorMsg)
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
                MediaPlayer.Event.Opening -> {
                    Log.d(TAG, "Media opening...")
                }
                MediaPlayer.Event.Playing -> {
                    Log.d(TAG, "Media playing...")
                }
                MediaPlayer.Event.Buffering -> {
                    Log.d(TAG, "Media buffering: ${event.buffering}%")
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

    fun playStream(rtspUrl: String) {
        stopPlayback()
        isRecording = false

        try {
            Log.d(TAG, "Starting stream from: $rtspUrl")
            val media = Media(libVLC, rtspUrl.toUri())

            // Set hardware acceleration
            media.setHWDecoderEnabled(true, false)

            // Add more robust connection options
            media.addOption(":network-caching=3000")  // Increased from 1500
            media.addOption(":live-caching=3000")     // Increased from 1500
            media.addOption(":rtsp-tcp")              // Force TCP mode
            media.addOption(":rtsp-timeout=10000")    // Longer timeout
            media.addOption(":rtsp-frame-buffer-size=600000") // Larger buffer
            media.addOption(":avcodec-hw=any")        // Hardware acceleration
            media.addOption(":file-caching=3000")     // File caching

            // Set the media and play
            mediaPlayer.media = media
            media.release()
            mediaPlayer.play()
            Log.d(TAG, "Attempting to connect to stream: $rtspUrl")
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

            // Use Downloads/Flight Eye directory
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val recordingDir = File(downloadsDir, FOLDER_NAME)
            if (!recordingDir.exists()) {
                recordingDir.mkdirs()
            }

            val recordingFile = File(recordingDir, fileName)
            val recordingPath = recordingFile.absolutePath

            Log.d(TAG, "Will save recording to: $recordingPath")

            val media = Media(libVLC, rtspUrl.toUri())
            media.setHWDecoderEnabled(true, false)

            // Connection settings
            media.addOption(":rtsp-tcp")  // Force RTSP over TCP
            media.addOption(":rtsp-timeout=5000")
            media.addOption(":rtsp-frame-buffer-size=500000")

            // Recording settings with Windows path fix
            val formattedPath = recordingPath.replace("\\", "/")
            media.addOption(":sout=#duplicate{dst=display,dst=file{dst='$formattedPath',mux=mp4,access=file}}")
            media.addOption(":sout-all")
            media.addOption(":sout-keep")

            // Buffering and sync settings
            media.addOption(":network-caching=1500")
            media.addOption(":clock-jitter=0")
            media.addOption(":live-caching=1500")
            media.addOption(":clock-synchro=0")

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