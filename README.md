# 🎧 Audio Player — Android App

Universal audio player that scans your device for all audio files (MP3, WEBM, OGG, WAV, FLAC, M4A, etc.) and plays them with a beautiful dark-themed interface.

## Features

- ✅ **Auto-scan** — finds ALL audio files on your device via MediaStore
- ✅ **Search** — filter by title or artist
- ✅ **Mini player** — always visible at the bottom
- ✅ **SeekBar** — drag to any position
- ✅ **Speed control** — 0.5x to 2.0x
- ✅ **Shuffle** — random playback
- ✅ **Repeat** — off / repeat one / repeat all
- ✅ **Sleep timer** — 15m / 30m / 1h auto-pause
- ✅ **Foreground service** — plays in background with notification controls
- ✅ **Dark theme** — Material 3, #0d1117 background
- ✅ **All formats** — MP3, WEBM, OGG, WAV, FLAC, M4A, AAC, OPUS

## How to Build

### Prerequisites
- Android Studio (latest)
- Android SDK 34
- JDK 17

### Steps
1. Open Android Studio
2. File → Open → select the `audio-player` folder
3. Wait for Gradle sync
4. Build → Build APK
5. Transfer APK to your phone and install

### Or via command line
```bash
cd audio-player
./gradlew assembleDebug
# APK at: app/build/outputs/apk/debug/app-debug.apk
```

## Permissions
- `READ_MEDIA_AUDIO` — to scan audio files
- `POST_NOTIFICATIONS` — for playback notification
- `FOREGROUND_SERVICE` — to keep playing in background

## Project Structure
```
audio-player/
├── app/src/main/java/com/mateus/audioplayer/
│   ├── AudioFile.java          — Data model
│   ├── AudioLoader.java        — MediaStore scanner
│   ├── AudioAdapter.java       — RecyclerView adapter
│   ├── MusicService.java       — Foreground service + MediaPlayer
│   ├── MainActivity.java       — UI + controls
│   └── NotificationReceiver.java
├── app/src/main/res/
│   ├── layout/
│   │   ├── activity_main.xml   — Main screen
│   │   └── item_audio.xml      — List item
│   ├── drawable/               — Backgrounds
│   └── values/                 — Theme, strings
└── README.md
```

## Notes
- On Android 13+, grant notification permission when prompted
- The app scans MediaStore — make sure your audio files are indexed (most file managers do this automatically)
- For audiobooks, place files in any folder — the app finds them by format, not location
