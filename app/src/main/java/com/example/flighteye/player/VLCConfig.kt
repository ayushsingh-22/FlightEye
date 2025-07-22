package com.example.flighteye.player

object VLCConfig {

    fun getDefaultOptions(): ArrayList<String> {
        return arrayListOf(
            "--no-drop-late-frames",              // Keep late frames for smoothness
            "--no-skip-frames",                   // Don't skip frames under pressure
            "--rtsp-tcp",                         // Use TCP instead of UDP for RTSP
            "--network-caching=1500",             // Increased buffer (ms)
            "--avcodec-hw=any",                   // Enable hardware acceleration
            "--video-filter=none",                // Disable video filters
            "--aout=opensles",                    // Use OpenSL ES audio output
            "--audio-time-stretch",               // Time stretching for audio
            "--sout-mux-caching=1500",            // Muxer caching value (ms)
            "--android-display-chroma=RV32",      // Force RV32 chroma for compatibility
            "--video-title=FlightEye",            // Set video title
            "-vvv"                                // Verbose logging (for debugging)
        )
    }
}