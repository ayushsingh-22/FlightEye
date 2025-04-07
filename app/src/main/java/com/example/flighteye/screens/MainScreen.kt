package com.example.flighteye.screens

import android.annotation.SuppressLint
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.example.flighteye.component.StreamInputField
import com.example.flighteye.player.VLCPlayerManager
import com.example.flighteye.utils.FileUtils
import com.example.flighteye.utils.PermissionsUtils
import org.videolan.libvlc.util.VLCVideoLayout

@SuppressLint("AuthLeak")
@Composable
fun MainScreen(navController: NavController) {
    val context = LocalContext.current

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

    val videoLayout = remember { VLCVideoLayout(context) }
    val playerManager = remember { VLCPlayerManager(context) }

    var rtspUrl by rememberSaveable { mutableStateOf("") }
    var isPlaying by rememberSaveable { mutableStateOf(false) }
    var isRecording by rememberSaveable { mutableStateOf(false) }

    // Attach surface once
    LaunchedEffect(Unit) {
        playerManager.attachSurface(videoLayout)
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

                    Toast.makeText(context, "Streaming Start", Toast.LENGTH_SHORT).show()
                    playerManager.stopPlayback()

                    if (isRecording) {
                        val filePath = FileUtils.createRecordingFile(context)
                        playerManager.playStreamWithRecording(rtspUrl, filePath.toString())
                        Toast.makeText(context, "Recording to: $filePath", Toast.LENGTH_LONG).show()
                        println("Recording to: $filePath")
                    } else {
                        playerManager.playStream(rtspUrl)
                    }

                    isPlaying = true // ✅ Mark that streaming started
                }) {
                    Text(if (isRecording) "Record & Play" else "Play")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(onClick = {
                    isRecording = !isRecording
                }) {
                    Text(if (isRecording) "Recording: ON" else "Recording: OFF")
                }

                Spacer(modifier = Modifier.width(8.dp))

                // ✅ STOP BUTTON (Visible only when streaming)
                if (isPlaying) {
                    Button(onClick = {
                        playerManager.stopPlayback()
                        Toast.makeText(context, "Streaming Stopped", Toast.LENGTH_SHORT).show()
                        isPlaying = false
                        isRecording = false // Optionally turn off recording too
                    }) {
                        Text("Stop")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

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

