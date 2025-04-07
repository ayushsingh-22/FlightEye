package com.example.flighteye.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

@Composable
fun StreamInputField(
    url: String,
    onUrlChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        OutlinedTextField(
        value = url,
        onValueChange = { onUrlChange(it) },
        label = { Text("Enter RTSP Stream URL") },
        modifier = Modifier.fillMaxWidth(),
        textStyle = TextStyle(color = Color.Black)
    )

    }
}
