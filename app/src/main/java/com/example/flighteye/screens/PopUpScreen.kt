package com.example.flighteye.screens

import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
private const val MAX_RETRIES = 5

@Composable
fun PopupScreen(navController: NavController, rtspUrl: String) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val videoLayout = remember { VLCVideoLayout(context) }
    val playerManager = remember { VLCPlayerManager(context) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var retryCount by remember { mutableStateOf(0) }

    LaunchedEffect(playerManager, rtspUrl) {
        playerManager.onError = { error ->
            Log.e(TAG, "Player error: $error (attempt ${retryCount + 1}/$MAX_RETRIES)")
            if (retryCount < MAX_RETRIES - 1) {
                retryCount += 1
                errorMessage = "Reconnecting… ($retryCount/$MAX_RETRIES)"
                val delay = (retryCount * 2000L).coerceAtMost(5000L)
                videoLayout.postDelayed({
                    if (rtspUrl.isNotBlank()) playerManager.playStream(rtspUrl)
                }, delay)
            } else {
                errorMessage = "Could not connect. Check the RTSP URL and network."
            }
        }
    }

    DisposableEffect(lifecycleOwner, rtspUrl) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    try {
                        playerManager.attachSurface(videoLayout)
                        if (rtspUrl.isNotBlank()) {
                            retryCount = 0
                            errorMessage = null
                            playerManager.playStream(rtspUrl)
                            Log.d(TAG, "Stream started in PopUpScreen: $rtspUrl")
                        } else {
                            errorMessage = "No RTSP URL provided"
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to start stream: ${e.message}")
                        errorMessage = "Failed to start: ${e.message}"
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Pop-Up Stream", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))

        AndroidView(
            factory = { videoLayout },
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        errorMessage?.let {
            Text(
                text = it,
                color = androidx.compose.ui.graphics.Color.Red,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                if (rtspUrl.isBlank()) {
                    Toast.makeText(context, "No stream URL", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                try {
                    playerManager.stopPlayback()
                    val pipIntent = Intent(context, PiPActivity::class.java).apply {
                        putExtra("rtsp_url", rtspUrl)
                    }
                    context.startActivity(pipIntent)
                    Log.d(TAG, "Starting PiP Activity with URL: $rtspUrl")
                } catch (e: Exception) {
                    Log.e(TAG, "Error starting PiP: ${e.message}")
                    Toast.makeText(context, "Failed to start PiP mode: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }) { Text("Start PiP View") }

            Button(onClick = {
                retryCount = 0
                errorMessage = null
                if (rtspUrl.isNotBlank()) playerManager.playStream(rtspUrl)
            }) { Text("Retry") }
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
