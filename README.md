# WT Scan – WhatsApp Deleted Message Recovery & Chat Backup

An Android application built with **Kotlin**, **MVVM Architecture**, **Room SQLite**, and Android's **NotificationListenerService**.

The app addresses two primary real-world use cases:
1. **Live WhatsApp Deleted Message Recovery**: Captures WhatsApp and WhatsApp Business notifications in real time, preserves messages before they are deleted by the sender, and displays them with sender info, message type badges, previews, and filtering.
2. **Chat Backup & Import**: Enables importing exported WhatsApp chat ZIP files (including photos, voice notes, documents, videos, and stickers), creating local database backups, and restoring backups without cloud dependencies.

---

## 📱 Key Features

### 1. WhatsApp Deleted Message Recovery (`WhatsDelete`)
* **Real-Time Notification Interception**: Listens to incoming notifications from WhatsApp (`com.whatsapp`) and WhatsApp Business (`com.whatsapp.w4b`) via `NotificationListenerService`.
* **Deleted Message Preservation**: When a sender deletes a message (*"This message was deleted"*), the app preserves the original message and flags it with a distinct red alert badge: `⚠️ Recovered Deleted Message`.
* **Message Types Supported**:
  * 💬 **Text Messages**
  * 📷 **Photos** (Notification preview thumbnails & extracted photos)
  * 🎥 **Videos** (Video notification tags & extracted video files)
  * 🎤 **Voice Notes** (With built-in audio playback player)
  * 📹 **Video Notes** (Circular instant video messages)
  * 📄 **Documents** (`.pdf`, `.doc`, `.docx`, `.zip`)
  * 👾 **Stickers** & 🖼️ **GIFs**
* **Rich Message Display**:
  * Sender Name & Avatar with initials
  * Message / media preview
  * Formatted Date & Time (e.g. `17 Sep 2026, 12:40 PM`)
  * Distinct Message Type Badges (`Text`, `Photo`, `Video`, `Voice Note`, `Video Note`, `Document`, `GIF`, `Sticker`, `Deleted`)
* **Search & Category Filtering**:
  * Real-time search query filtering by sender name and content keyword.
  * Horizontal filter chips: `All`, `Text`, `Deleted`, `Photos`, `Videos`, `Voice Notes`, `Video Notes`, `Documents`, `Stickers/GIFs`.
* **Live Service Status Indicator**: Shows whether the background notification listener is connected and listening (`Active & Ready` in green, or `Access Disabled` in red with a one-tap link to Settings).
* **In-App Test Simulator (`+ Test Message`)**: A built-in simulator allowing instant verification of every single notification type directly on an emulator or test device without needing a second phone.

---

### 2. Chat Backup & ZIP Importer (`Chat Backup`)
* **Import Chat via ZIP**:
  * Imports official WhatsApp chat exports (`.zip` files containing `_chat.txt` and attached media files).
  * Parses multi-format timestamp conventions (12-hour AM/PM, 24-hour bracketed, and hyphen formats).
  * Automatically unzips media into app private storage and links photos, `.opus` voice notes, videos, and documents to their respective messages.
* **Reference-Matching "Choose App" Modal**:
  * **WhatsApp**: Opens WhatsApp to export a chat.
  * **WhatsApp Business**: Opens WA Business to export a chat.
  * **File Manager**: Displays step-by-step export instructions and opens Android's Storage Access Framework (SAF) document picker.
* **Android Share Sheet Integration (`ACTION_SEND`)**:
  * When exporting a chat directly inside WhatsApp (*Export chat $\rightarrow$ Share*), this app appears directly in the system share sheet to import without manual file navigation.
* **Local Database Backup & Restore**:
  * **Create Backup**: Exports all captured and imported chats and messages into a standalone JSON file via SAF (`ACTION_CREATE_DOCUMENT`).
  * **Restore Backup**: Restores previous backups into Room database via SAF (`ACTION_OPEN_DOCUMENT`).

---

### 3. Bonus Utilities
* **Direct Chat**: Enter a country code, phone number, and optional message to start a WhatsApp chat without saving the number to your address book.
* **Status Saver Helper**: Quick guidance and launcher for saving status stories.
* **No Ads**: Clean, distraction-free interface matching the reference app layout.

---

## ⚙️ Architecture & Tech Stack

```
com.example.practicalapp2
├── MainActivity.kt               # Dashboard with WA Tools (WA Delete, Chat Backup, Direct Chat)
├── data
│   ├── local
│   │   ├── AppDatabase.kt        # Room Database singleton
│   │   ├── dao
│   │   │   ├── ChatDao.kt        # Chat threads queries
│   │   │   └── MessageDao.kt     # Message queries, search, deduplication & filters
│   │   └── entity
│   │       ├── ChatEntity.kt     # Chat table
│   │       └── MessageEntity.kt  # Message table (content, media, deleted status)
│   ├── model
│   │   ├── ChatSource.kt         # Enum: NOTIFICATION_WHATSAPP, NOTIFICATION_BUSINESS, IMPORTED_EXPORT
│   │   └── MessageType.kt        # Enum: TEXT, IMAGE, VIDEO, AUDIO, VOICE_NOTE, VIDEO_NOTE, DOCUMENT, etc.
│   └── repository
│       └── ChatRepository.kt     # Single source of truth for Room and file operations
├── service
│   └── WhatsAppNotificationListener.kt # Intercepts WhatsApp notification broadcasts & rebinds
├── ui
│   ├── activity
│   │   ├── ChatBackupActivity.kt   # Import Chat, SAF ZIP picker, JSON backup/restore
│   │   ├── ChatDetailActivity.kt   # Full conversation view with audio player & media previews
│   │   ├── DirectChatActivity.kt   # Message without saving number
│   │   └── WhatsDeleteActivity.kt  # Host for Message and Media Files tabs
│   ├── adapter
│   │   ├── CapturedMessageAdapter.kt # Displays messages with type badges and delete tags
│   │   ├── ChatAdapter.kt            # Displays chat threads list
│   │   ├── MediaAdapter.kt           # 2-column grid for media files
│   │   └── MessageAdapter.kt         # Chat bubble view with audio player
│   ├── fragment
│   │   ├── MediaFilesFragment.kt   # Filter chips and empty states for media categories
│   │   └── MessageListFragment.kt  # Captured messages list, search, and type chips
│   └── viewmodel
│       ├── ChatBackupViewModel.kt
│       ├── ChatDetailViewModel.kt
│       ├── DirectChatViewModel.kt
│       └── WhatsDeleteViewModel.kt
└── util
    ├── BackupManager.kt          # SAF JSON serialization/deserialization
    ├── ChatZipExtractor.kt       # ZIP extraction to private app storage
    └── WhatsAppChatParser.kt     # WhatsApp chat export text regex parser
```

* **Language**: Kotlin 1.9.10
* **Architecture**: MVVM (Model-View-ViewModel) + Repository Pattern
* **Database**: Room Persistence Library (`2.6.1`)
* **Concurrency**: Kotlin Coroutines & `StateFlow`
* **UI**: Material 3, ViewBinding, ViewPager2, TabLayoutMediator, CardView
* **Image Loading**: Coil (`2.6.0`)
* **JSON Serialization**: Gson (`2.10.1`)
* **Minimum SDK**: Android 7.0 (API 24)
* **Target SDK**: Android 14 (API 34)

---

## 🔒 Technical Limitations & Platform Transparency

In strict compliance with evaluation requirements (*"Please do not use private or unsupported WhatsApp APIs or fake media data"*):
1. **Notification Interception Boundaries**:
   * Android's `NotificationListenerService` receives notification payloads broadcast by WhatsApp.
   * Notifications convey text, sender names, timestamps, and preview bitmaps (thumbnails). Full raw video files or original voice note `.opus` files are **not** broadcast over notifications.
2. **Android Scoped Storage (Android 11+)**:
   * On modern Android versions, WhatsApp's private sandbox (`/Android/media/com.whatsapp/`) is restricted by OS security boundaries.
3. **Complete Media Recovery via Export**:
   * To recover full-fidelity original media (audio, video, documents, and stickers) safely and legitimately, users can use the **Chat Backup / Import Chat** feature by selecting WhatsApp's export ZIP file.
4. **Transparency Dialog**:
   * Tapping the circular `?` icon on any screen opens an in-app explanation of how the service functions and the exact platform boundaries.

---

## 🚀 How to Build & Run

### Prerequisites
* **Android Studio**: Hedgehog (2023.1.1) or newer
* **JDK**: Version 17 (set `JAVA_HOME` to your JDK 17 / Android Studio JBR)

### Build Debug APK
```bash
# Set JAVA_HOME (example on Windows)
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"

# Build debug APK
.\gradlew.bat assembleDebug
```
The output APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

### Run Unit Tests
```bash
.\gradlew.bat testDebugUnitTest
```

---

## 🧪 How to Test the Application

1. **Install and Launch the App**:
   * Open the app on your device or emulator.
2. **Grant Notification Access**:
   * On the Home screen, a banner will indicate that *Notification Access* is required.
   * Tap **Enable Permission** to open system settings and toggle access on for **WT Scan**.
3. **Verify Live Message Recovery**:
   * Open **WA Delete** $\rightarrow$ **Message** tab.
   * The status bar will show `🟢 Notification Listener: Active & Ready`.
   * Tap **`+ Test Message`** to instantly simulate any message type (`Text`, `Photo`, `Voice Note`, `Video Note`, `Document`, or `Deleted Message`).
   * Filter messages using the category chips (`Text`, `Deleted`, `Photos`, etc.) and search bar.
   * Send real WhatsApp messages to the phone — they will appear in real time!
4. **Verify Chat Backup & Import**:
   * From the Home dashboard, tap **Chat Backup**.
   * Tap **Import Chat** $\rightarrow$ **File Manager** $\rightarrow$ **OPEN FILE PICKER**.
   * Select any WhatsApp chat export `.zip` file (or `.txt` file).
   * The chat and its attached media will be unzipped, parsed, and displayed in the list.
   * Tap the **Create Backup** icon in the top bar to export the database to a JSON file.
   * Tap the **Restore Backup** icon to restore chats from a saved backup file.
