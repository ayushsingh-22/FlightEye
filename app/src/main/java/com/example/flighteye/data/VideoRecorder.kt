package com.example.flighteye.data

import android.content.Context
import java.io.File


private fun getOutputFilePath(context: Context): String {
        val outputDir = File(context.getExternalFilesDir(null), "recordings")
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }

        val fileName = "video_${System.currentTimeMillis()}.mp4"
        return File(outputDir, fileName).absolutePath
    }

