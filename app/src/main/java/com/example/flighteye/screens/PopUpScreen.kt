package com.example.flighteye.screens

import android.content.Intent
import android.view.SurfaceView
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.example.flighteye.activity.PiPActivity
import com.example.flighteye.player.VLCPlayerManager
import com.example.flighteye.player.VLCPlayerManagerSingleton
import org.videolan.libvlc.util.VLCVideoLayout

@Composable
fun PopUpScreen(navController: NavController) {
    val context = LocalContext.current
    val videoLayout = remember { VLCVideoLayout(context) }
    val playerManager = remember { VLCPlayerManagerSingleton.getInstance(context) }

    val rtspUrl by remember { mutableStateOf("rtsp://192.168.1.2:5540/ch0") }

    LaunchedEffect(Unit) {
        playerManager.attachSurface(videoLayout)
        playerManager.playStream(rtspUrl)
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

        // ✅ Modified Button with detachSurface() call
        Button(onClick = {
            playerManager.detachSurface() // 🔄 Detach before new view
            val pipIntent = Intent(context, PiPActivity::class.java)
            pipIntent.putExtra("rtsp_url", rtspUrl)
            context.startActivity(pipIntent)
        }) {
            Text("Start PiP View")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            playerManager.stopPlayback()
            navController.popBackStack()
        }) {
            Text("Back to Main")
        }
    }
}
