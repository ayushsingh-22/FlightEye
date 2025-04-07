package com.example.flighteye.player

object VLCConfig {

    fun getDefaultOptions(): ArrayList<String> {
        return arrayListOf(
            "--no-drop-late-frames",              // Keep late frames for smoothness
            "--no-skip-frames",                   // Don't skip frames under pressure
            "--rtsp-tcp",                         // Use TCP instead of UDP for RTSP
            "--network-caching=150",              // Buffer in ms (100-1000 recommended)
            "--avcodec-hw=any",                   // Enable hardware acceleration
            "-vvv"                                // Verbose logging (for debugging)
        )
    }
}
