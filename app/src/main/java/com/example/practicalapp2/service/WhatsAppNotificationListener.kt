package com.example.practicalapp2.service

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.toBitmap
import com.example.practicalapp2.data.model.MessageType
import com.example.practicalapp2.data.repository.ChatRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class WhatsAppNotificationListener : NotificationListenerService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var repository: ChatRepository

    override fun onCreate() {
        super.onCreate()
        repository = ChatRepository(applicationContext)
        Log.d(TAG, "WhatsAppNotificationListener created")
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceConnected.value = false
        serviceScope.cancel()
        Log.d(TAG, "WhatsAppNotificationListener destroyed")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        isServiceConnected.value = true
        Log.d(TAG, "WhatsAppNotificationListener connected successfully")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isServiceConnected.value = false
        Log.d(TAG, "WhatsAppNotificationListener disconnected, requesting rebind")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            requestRebind(ComponentName(this, WhatsAppNotificationListener::class.java))
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        val packageName = sbn.packageName ?: return
        if (packageName != PACKAGE_WHATSAPP && packageName != PACKAGE_WHATSAPP_BUSINESS) {
            return
        }

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        // Skip ongoing / non-clearable background notifications (e.g. WhatsApp Web active, backup in progress)
        if (notification.flags and Notification.FLAG_ONGOING_EVENT != 0) {
            return
        }

        serviceScope.launch {
            try {
                processNotification(sbn, notification, extras, packageName)
            } catch (e: Exception) {
                Log.e(TAG, "Error processing WhatsApp notification", e)
            }
        }
    }

    private suspend fun processNotification(
        sbn: StatusBarNotification,
        notification: Notification,
        extras: Bundle,
        packageName: String
    ) {
        val postTime = if (sbn.postTime > 0) sbn.postTime else System.currentTimeMillis()

        // Extract title safely via CharSequence
        val rawTitle = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?: extras.getCharSequence("android.title")?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString()
            ?: "WhatsApp User"

        // Extract text safely via CharSequence
        val rawText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras.getCharSequence("android.text")?.toString()
            ?: ""

        // Try extracting via MessagingStyle (standard in modern WhatsApp notifications)
        val messagingStyle = try {
            NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)
        } catch (e: Exception) {
            null
        }

        if (messagingStyle != null && messagingStyle.messages.isNotEmpty()) {
            val conversationTitle = messagingStyle.conversationTitle?.toString()
            val userPerson = messagingStyle.user

            // Process recent messages from the MessagingStyle bundle
            for (msg in messagingStyle.messages) {
                val text = msg.text?.toString() ?: ""
                if (text.isBlank() && msg.dataMimeType == null) continue

                // Check if message is sent by user themselves
                if (userPerson.name != null && msg.person?.name == userPerson.name) {
                    continue
                }

                val sender = msg.person?.name?.toString()
                    ?: conversationTitle
                    ?: rawTitle

                val timestamp = if (msg.timestamp > 0) msg.timestamp else postTime
                val detectedType = detectMessageType(text, msg.dataMimeType)
                val mediaUri = extractAndSaveMediaBitmap(extras, timestamp)

                repository.processNotificationMessage(
                    senderName = sender,
                    content = text,
                    timestamp = timestamp,
                    packageName = packageName,
                    mediaUri = mediaUri,
                    detectedType = detectedType
                )
            }
            return
        }

        // Fallback for standard notifications
        if (rawText.isBlank() || isSummaryMessage(rawText)) {
            return
        }

        // Check for Group Chat format: "Sender: Message" in rawText
        var finalSender = rawTitle
        var finalContent = rawText

        if (rawText.contains(": ")) {
            val parts = rawText.split(Regex(":\\s+"), limit = 2)
            if (parts.size == 2 && !parts[0].contains("\n") && parts[0].length < 35) {
                finalSender = "${parts[0]} ($rawTitle)"
                finalContent = parts[1]
            }
        }

        val detectedType = detectMessageType(finalContent, null)
        val mediaUri = extractAndSaveMediaBitmap(extras, postTime)

        repository.processNotificationMessage(
            senderName = finalSender,
            content = finalContent,
            timestamp = postTime,
            packageName = packageName,
            mediaUri = mediaUri,
            detectedType = detectedType
        )
    }

    private fun detectMessageType(text: String, mimeType: String?): MessageType {
        val lower = text.lowercase().trim()
        return when {
            lower.contains("this message was deleted") ||
                    lower.contains("you deleted this message") ||
                    lower.contains("message was deleted") -> MessageType.DELETED

            lower.contains("video message") ||
                    lower.contains("video note") -> MessageType.VIDEO_NOTE

            mimeType?.startsWith("video") == true ||
                    lower.startsWith("🎥 video") ||
                    lower == "video" ||
                    lower.startsWith("video (") -> MessageType.VIDEO

            mimeType?.startsWith("image") == true ||
                    lower.startsWith("📷 photo") ||
                    lower == "photo" ||
                    lower.startsWith("photo (") -> MessageType.IMAGE

            lower.contains("voice message") ||
                    lower.contains("voice note") ||
                    lower.startsWith("🎤") -> MessageType.VOICE_NOTE

            mimeType?.startsWith("audio") == true ||
                    lower.startsWith("🎵 audio") ||
                    lower == "audio" -> MessageType.AUDIO

            lower == "sticker" ||
                    lower.contains("👾 sticker") -> MessageType.STICKER

            lower == "gif" ||
                    lower.startsWith("gif") -> MessageType.GIF

            lower.startsWith("📄") ||
                    lower == "document" ||
                    lower.endsWith(".pdf") ||
                    lower.endsWith(".doc") ||
                    lower.endsWith(".docx") ||
                    lower.endsWith(".zip") -> MessageType.DOCUMENT

            else -> MessageType.TEXT
        }
    }

    private fun extractAndSaveMediaBitmap(extras: Bundle, timestamp: Long): String? {
        try {
            var bitmap: Bitmap? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                extras.getParcelable(Notification.EXTRA_PICTURE, Bitmap::class.java)
            } else {
                @Suppress("DEPRECATION")
                extras.getParcelable(Notification.EXTRA_PICTURE)
            }

            if (bitmap == null) {
                // Try picture icon
                val icon = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    extras.getParcelable(Notification.EXTRA_PICTURE_ICON, android.graphics.drawable.Icon::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    extras.getParcelable(Notification.EXTRA_PICTURE_ICON)
                }
                if (icon != null) {
                    val drawable = icon.loadDrawable(this)
                    if (drawable is BitmapDrawable) {
                        bitmap = drawable.bitmap
                    } else if (drawable != null) {
                        bitmap = drawable.toBitmap()
                    }
                }
            }

            if (bitmap == null) return null

            val mediaDir = File(filesDir, "notification_media")
            if (!mediaDir.exists()) mediaDir.mkdirs()

            val file = File(mediaDir, "media_${timestamp}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            return file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save preview bitmap", e)
            return null
        }
    }

    private fun isSummaryMessage(text: String): Boolean {
        val lower = text.lowercase()
        return lower.matches(Regex("\\d+\\s+new messages?")) ||
                lower.matches(Regex("(\\d+ messages? from \\d+ chats?)")) ||
                lower.contains("checking for new messages") ||
                lower.contains("whatsapp web is currently active")
    }

    companion object {
        const val TAG = "WANotificationListener"
        const val PACKAGE_WHATSAPP = "com.whatsapp"
        const val PACKAGE_WHATSAPP_BUSINESS = "com.whatsapp.w4b"

        val isServiceConnected = MutableStateFlow(false)

        fun requestRebindIfNecessary(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    val componentName = ComponentName(context, WhatsAppNotificationListener::class.java)
                    NotificationListenerService.requestRebind(componentName)
                } catch (e: Exception) {
                    Log.e(TAG, "requestRebind error", e)
                }
            }
        }
    }
}
