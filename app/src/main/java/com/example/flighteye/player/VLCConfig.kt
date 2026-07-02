package com.example.flighteye.player

object VLCConfig {

    fun getDefaultOptions(): ArrayList<String> {
        return arrayListOf(
            "--drop-late-frames",
            "--skip-frames",
            "--rtsp-tcp",
            "--network-caching=300",
            "--live-caching=300",
            "--clock-jitter=0",
            "--clock-synchro=0",
            "--avcodec-hw=any",
            "--video-filter=none",
            "--aout=opensles",
            "--audio-time-stretch",
            "--sout-mux-caching=300",
            "--android-display-chroma=RV32",
            "--video-title=FlightEye",
            "--rtsp-timeout=15000",               // 15s timeout for RTSP connection
            "--no-stats",                         // Disable stats collection
            "--verbose=0"                         // Minimal logging in production
        )
    }
}