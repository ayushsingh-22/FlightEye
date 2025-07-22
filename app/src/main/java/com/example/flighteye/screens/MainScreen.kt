package com.example.flighteye.screens

import android.annotation.SuppressLint
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.example.flighteye.component.StreamInputField
import com.example.flighteye.player.VLCPlayerManager
import com.example.flighteye.utils.FileUtils
import com.example.flighteye.utils.PermissionsUtils
import org.videolan.libvlc.util.VLCVideoLayout
import java.io.File

private const val TAG = "MainScreen"

@SuppressLint("AuthLeak")
@Composable
fun MainScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val allGranted = result.values.all { it }
        if (!allGranted) {
            Toast.makeText(context, "Storage permissions are required!", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!PermissionsUtils.hasAllPermissions(context)) {
            permissionLauncher.launch(PermissionsUtils.REQUIRED_PERMISSIONS)
        }
    }

    // State variables
    var rtspUrl by rememberSaveable { mutableStateOf("rtsp://admin:admin@192.168.31.244:1935") }
    var isPlaying by rememberSaveable { mutableStateOf(false) }
    var isRecording by rememberSaveable { mutableStateOf(false) }
    var currentRecordingPath by rememberSaveable { mutableStateOf<String?>(null) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    // Create VLC components
    val videoLayout = remember { VLCVideoLayout(context) }
    val playerManager = remember { VLCPlayerManager(context) }

    // Set up callbacks
    LaunchedEffect(playerManager) {
        playerManager.onRecordingStopped = { path ->
            Log.d(TAG, "Recording saved to: $path")
            Toast.makeText(context, "Recording saved to: $path", Toast.LENGTH_LONG).show()
            currentRecordingPath = path
        }

        playerManager.onError = { error ->
            Log.e(TAG, "Player error: $error")
            errorMessage = error
            Toast.makeText(context, "Error: $error", Toast.LENGTH_SHORT).show()
        }
    }

    // Observe lifecycle to properly clean up resources
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    try {
                        playerManager.attachSurface(videoLayout)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to attach surface: ${e.message}")
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    try {
                        if (isPlaying) {
                            playerManager.stopPlayback()
                            isPlaying = false
                            isRecording = false
                        }
                        playerManager.detachSurface()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error during lifecycle handling: ${e.message}")
                    }
                }
                Lifecycle.Event.ON_DESTROY -> {
                    playerManager.release()
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            playerManager.release()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .offset(y = 20.dp)
            .padding(16.dp)
    ) {
        Text(
            text = "Flight Eye Player",
            fontWeight = FontWeight.W500,
            fontSize = 25.sp,
        )

        // RTSP Input
        StreamInputField(
            url = rtspUrl,
            onUrlChange = { rtspUrl = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // VLC Player Surface
        AndroidView(
            factory = { videoLayout },
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Error message display if any
        errorMessage?.let {
            Text(
                text = "Error: $it",
                color = androidx.compose.ui.graphics.Color.Red,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(onClick = {
                    if (rtspUrl.isBlank()) {
                        Toast.makeText(context, "Please enter a valid RTSP URL", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    errorMessage = null
                    playerManager.stopPlayback()

                    if (isRecording) {
                        try {
                            playerManager.playStreamWithRecording(rtspUrl)
                            Toast.makeText(context, "Recording started", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to start recording: ${e.message}")
                            Toast.makeText(context, "Failed to start recording: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        playerManager.playStream(rtspUrl)
                        Toast.makeText(context, "Streaming started", Toast.LENGTH_SHORT).show()
                    }

                    isPlaying = true
                }) {
                    Text(if (isRecording) "Record & Play" else "Play")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(onClick = {
                    isRecording = !isRecording

                    if (isRecording && isPlaying) {
                        // If already playing, restart with recording
                        playerManager.stopPlayback()
                        playerManager.playStreamWithRecording(rtspUrl)
                        Toast.makeText(context, "Started recording", Toast.LENGTH_SHORT).show()
                    } else if (!isRecording && isPlaying) {
                        // If turning off recording while playing, restart without recording
                        playerManager.stopPlayback()
                        playerManager.playStream(rtspUrl)
                        Toast.makeText(context, "Recording stopped", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text(if (isRecording) "Recording: ON" else "Recording: OFF")
                }

                Spacer(modifier = Modifier.width(8.dp))

                // STOP BUTTON (Visible only when streaming)
                if (isPlaying) {
                    Button(onClick = {
                        playerManager.stopPlayback()
                        Toast.makeText(context, "Streaming Stopped", Toast.LENGTH_SHORT).show()
                        isPlaying = false
                        isRecording = false
                    }) {
                        Text("Stop")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Show last recorded file path if available
            currentRecordingPath?.let {
                Text(
                    text = "Last recording: $it",
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    navController.navigate("popup")
                }) {
                Text("Pop-Up View")
            }
        }
    }
}