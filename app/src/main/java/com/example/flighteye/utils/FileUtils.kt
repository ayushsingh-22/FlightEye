package com.example.flighteye.utils

import android.content.Context
import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileUtils {

    private const val FOLDER_NAME = "Flight Eye"


    private fun getDownloadDirectory(): File {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val recordingDir = File(downloadsDir, FOLDER_NAME)
        if (!recordingDir.exists()) {
            recordingDir.mkdirs()
        }
        return recordingDir
    }

    fun createRecordingFile(context: Context): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "recording_$timeStamp.mp4"
        return File(getDownloadDirectory(), fileName)
    }
}
