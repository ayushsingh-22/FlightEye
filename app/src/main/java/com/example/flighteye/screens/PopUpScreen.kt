package com.example.flighteye.screens

import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.example.flighteye.activity.PiPActivity
import com.example.flighteye.player.VLCPlayerManager
import org.videolan.libvlc.util.VLCVideoLayout

private const val TAG = "PopUpScreen"

@Composable
fun PopupScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val videoLayout = remember { VLCVideoLayout(context) }
    val playerManager = remember { VLCPlayerManager(context) }

    val rtspUrl by remember { mutableStateOf("rtsp://admin:admin@192.168.31.244:1935") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Set up callbacks
    LaunchedEffect(playerManager) {
        playerManager.onError = { error ->
            Log.e(TAG, "Player error: $error")
            errorMessage = error
            Toast.makeText(context, "Error: $error", Toast.LENGTH_SHORT).show()
        }
    }

    // Handle lifecycle
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    try {
                        playerManager.attachSurface(videoLayout)
                        playerManager.playStream(rtspUrl)
                        Log.d(TAG, "Stream started in PopUpScreen")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to start stream: ${e.message}")
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    try {
                        playerManager.stopPlayback()
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
            playerManager.stopPlayback()
            playerManager.release()
        }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {

        Text("Pop-Up Stream", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))

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

        Button(onClick = {
            try {
                // Stop current playback before launching PiP
                playerManager.stopPlayback()

                // Create and start PiP activity
                val pipIntent = Intent(context, PiPActivity::class.java)
                pipIntent.putExtra("rtsp_url", rtspUrl)
                context.startActivity(pipIntent)

                Log.d(TAG, "Starting PiP Activity with URL: $rtspUrl")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting PiP: ${e.message}")
                Toast.makeText(context, "Failed to start PiP mode: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }) {
            Text("Start PiP View")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            playerManager.stopPlayback()
            navController.popBackStack()
            Log.d(TAG, "Returning to main screen")
        }) {
            Text("Back to Main")
        }
    }
}