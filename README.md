# 🎥 FlightEye - RTSP Streaming & Internal Screen Recording App

**FlightEye** is a powerful Android application built with **Kotlin** and **Jetpack Compose** that enables:

- 📡 RTSP video stream playback using **VLC**
- 🎙️ Internal screen recording with system audio using **MediaProjection + AudioPlaybackCapture**
- 🧩 RTSP server testing from IP cameras or local devices
- 🛠️ Full Android 10+ support for secure audio+video recording

---

## 🚀 Features

### 🎬 RTSP Streaming
- Play live RTSP streams (IP Camera, NVR, local RTSP server)
- Uses **VLC for Android (libVLC)** for smooth playback
- Automatic reconnect on stream failure
- Supports both video and audio (if stream provides)

### 📲 Internal Screen Recording
- Record device screen internally with **system audio**
- Uses `MediaProjection` and `AudioPlaybackCapture` (Android 10+)
- Save recordings locally in MP4 format
- Runs recording in a **foreground service** with proper notification
- Works with or without streaming

### 🧪 RTSP Stream Validation
- Built-in UI to test & debug RTSP streams
- Logs and toast messages for invalid or unreachable streams

### ⚙️ System Settings Shortcut
- Quick launcher to open OEM screen recording settings for compatibility

---

## 🧰 Tech Stack

| Layer              | Technology                        |
|--------------------|------------------------------------|
| 🧠 Language         | Kotlin                             |
| 🎨 UI               | Jetpack Compose                    |
| 📹 Streaming        | VLC (libVLC) for Android           |
| 🎥 Screen Capture   | MediaProjection API                |
| 🔊 Audio Capture    | AudioPlaybackCapture API           |
| 📂 File Handling    | MediaStore + Scoped Storage        |
| 🧾 Permissions      | Android runtime permission model   |
| 🧼 Dependency Mgmt  | Gradle + Kotlin DSL                |

---

## 📸 Screenshots
![Untitled design](https://github.com/user-attachments/assets/0f36fa03-653a-449a-9526-be2b137592f6)
---

## 📲 Requirements

- **Android 10+** (API 29+)
- Device must support **AudioPlaybackCapture API**
- Active RTSP stream or IP Camera
- Internet or LAN connection to access stream

---

## 🧩 TODOs / Improvements

- [ ] Save RTSP URLs for quick access  
- [ ] RTSP recording feature (stream to file)  
- [ ] Picture-in-Picture mode support  
- [ ] Export recordings with share intent  

---

## 📄 License

MIT License - feel free to use, contribute, and modify this app as needed.

---

> ✨ Made with ❤️ using Kotlin + Jetpack Compose
