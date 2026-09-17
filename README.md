# WT Scan – WhatsApp Deleted Message Recovery & Chat Backup

A clean, modern Android application built with **Kotlin**, **MVVM architecture**, **Room SQLite database**, and Android's **NotificationListenerService**. It allows users to read deleted WhatsApp messages, recover media previews, import exported chat backups, and message unsaved numbers directly.

---

## 📱 How the App Works

### 1. Live WhatsApp Message & Deletion Recovery
* **How messages are captured**: When someone sends you a message on WhatsApp or WhatsApp Business, Android generates a status bar notification. The app's `NotificationListenerService` reads this notification in real time and saves the sender's name, message content, timestamp, and any attached preview bitmap directly to a private local Room database.
* **How deleted messages are detected**: When the sender deletes a message for everyone, WhatsApp posts an update notification stating *"This message was deleted"*. The app detects this deletion event, links it to the previously captured message from that sender, and flags it with a red badge: **`⚠️ This message was deleted by the sender (Recovered)`** while preserving the original text and preview.
* **Search & Filters**: You can filter captured messages by category (**All**, **Text**, **Deleted**, **Photos**, **Videos**, **Voice Notes**, **Video Notes**, **Documents**, **Stickers/GIFs**) and search in real time by sender name or keyword.

### 2. Chat Backup & Export ZIP Importer
* **Import Chat**: Allows importing complete WhatsApp chat exports (with full photos, videos, voice notes, documents, and stickers) without third-party servers.
  * Tap **Import Chat** $\rightarrow$ Choose **WhatsApp** (to open WhatsApp and export a chat) or **File Manager** (to pick an exported `.zip` or `.txt` file).
  * The built-in parser automatically extracts messages, links attachments, and renders the entire chat conversation history with media playback.
* **Share Sheet Support**: You can also open WhatsApp, tap **⋮ $\rightarrow$ More $\rightarrow$ Export chat $\rightarrow$ Share**, and pick this app directly from the Android Share Sheet.
* **Local Backup & Restore**: Create JSON backup files of your entire saved database and restore them anytime via the Android Storage Access Framework (SAF).

### 3. Direct Chat (WA Tools)
* Send a message to any phone number on WhatsApp without saving them to your device contacts. Enter the country code, phone number, and optional message, and tap **Open WhatsApp Chat**.

---

## ⚠️ Things to Know Before Using the App

To ensure the app works properly, please keep the following Android OS behaviors and platform boundaries in mind:

### 1. Notification Access Permission is Mandatory
* The app requires **Notification Access** to detect incoming messages.
* When you open the app, look at the status bar at the top:
  * 🟢 **Active & Listening**: The service is running and ready.
  * 🔴 **Access Disabled**: Tap the banner to open Android Settings and enable permission for the app.

### 2. Notifications Must Be Turned ON in WhatsApp
* The app reads messages from Android notifications. Therefore:
  * **Muted chats**: If you have muted a contact or group in WhatsApp, WhatsApp does not post sound or banner notifications, so the app cannot intercept them.
  * **Active chats**: If you are actively chatting with someone with the WhatsApp screen open, WhatsApp does not trigger a notification for incoming messages in that chat.
  * **WhatsApp Notification Settings**: Make sure notifications are enabled in WhatsApp (*Settings $\rightarrow$ Notifications*).

### 3. Scoped Storage & Real-Time Media Limitations (No Hacks / Fake Data)
* **What live notifications provide**: Notifications contain message text, timestamps, sender/group names, and small image/sticker thumbnail previews (`EXTRA_PICTURE`).
* **What live notifications DO NOT provide**: Android notifications never carry raw, full-length video files, high-resolution audio files, or original PDF documents over broadcast streams.
* **Scoped Storage (Android 11+)**: Since Android 11, WhatsApp stores full media inside an isolated private sandbox (`/Android/media/com.whatsapp/`). Third-party apps are prohibited by Android security policies from accessing these private files without root or unsupported exploits.
* **How to recover full media**: To view full videos, voice notes, documents, and photos completely and safely, use the **Import Chat** feature by exporting the chat with media.

### 4. Battery Optimization / Background Restrictions
* Modern Android systems (Xiaomi/MIUI, Samsung OneUI, OnePlus/OxygenOS, etc.) can aggressively kill background services to save battery.
* To keep the notification listener running reliably in the background:
  1. Go to **Settings $\rightarrow$ Apps $\rightarrow$ PracticalApp2 (WT Scan)**.
  2. Under **Battery**, set it to **Unrestricted** (or turn off "Battery Optimization").
  3. Ensure **Auto-start** is allowed if your phone has this setting.

---

## 🧪 How to Test & Verify Without a Second Phone

You don't need a second phone or an active SIM card to test message recovery!
1. Open the app $\rightarrow$ tap **WA Delete**.
2. Tap the **`+ Test Message`** button at the top right of the screen.
3. Select any type to simulate in real time:
   * `💬 Text Message`
   * `📷 Photo`
   * `🎥 Video`
   * `🎤 Voice Note`
   * `📹 Video Note`
   * `📄 Document (.pdf)`
   * `👾 Sticker` / `🖼️ GIF`
   * `⚠️ Deleted Message Notice` (Simulates a message followed by a deletion notice)
   * `🚀 Generate All Types at Once`
4. The simulated messages will immediately appear in the list with their corresponding badge, preview, and category filter!

---

## 🛠️ Tech Stack & Architecture

* **Language**: Kotlin
* **Architecture**: MVVM (Model-View-ViewModel) with Repository pattern
* **Database**: Room SQLite Database (with Coroutines Flow for reactive updates)
* **Background Service**: Android `NotificationListenerService`
* **File & Storage**: Storage Access Framework (SAF), ZIP streaming, Scoped Storage compliant
* **Image Loading**: Coil (Kotlin Coroutines image loader)
* **UI**: Material 3, ViewBinding, ViewPager2, custom vector drawables
