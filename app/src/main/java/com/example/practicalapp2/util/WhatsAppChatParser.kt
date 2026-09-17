package com.example.practicalapp2.util

import com.example.practicalapp2.data.local.entity.MessageEntity
import com.example.practicalapp2.data.model.MessageType
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Locale

data class ParsedChatResult(
    val contactName: String,
    val messages: List<MessageEntity>,
    val lastTimestamp: Long
)

class WhatsAppChatParser {

    companion object {
        // Common WhatsApp export line regex patterns
        // Pattern 1: [17/09/26, 12:40:15] Sender: Message or [17/09/2026, 12:40:15 PM] Sender: Message
        private val BRACKET_PATTERN = Regex("^\\[(\\d{1,2}[/.-]\\d{1,2}[/.-]\\d{2,4},\\s+\\d{1,2}:\\d{2}(?::\\d{2})?(?:\\s+[AaPp][Mm])?)\\]\\s+([^:]+):\\s+(.*)$")

        // Pattern 2: 17/09/2026, 12:40 - Sender: Message or 17/09/26, 12:40 PM - Sender: Message
        private val DASH_PATTERN = Regex("^(\\d{1,2}[/.-]\\d{1,2}[/.-]\\d{2,4},\\s+\\d{1,2}:\\d{2}(?::\\d{2})?(?:\\s+[AaPp][Mm])?)\\s+-\\s+([^:]+):\\s+(.*)$")

        // System message pattern: 17/09/2026, 12:40 - Messages and calls are end-to-end encrypted...
        private val SYSTEM_PATTERN = Regex("^(\\d{1,2}[/.-]\\d{1,2}[/.-]\\d{2,4},\\s+\\d{1,2}:\\d{2}(?::\\d{2})?(?:\\s+[AaPp][Mm])?)(?:\\s+-\\s+|\\]\\s+)(.*)$")

        // Media attachment patterns in WhatsApp export
        private val ATTACHMENT_PATTERN = Regex("([\\w-]+\\.(?:jpg|jpeg|png|mp4|opus|m4a|mp3|pdf|doc|docx|zip|webp))(?:\\s+\\(file attached\\)|<attached:\\s+[^>]+>)?", RegexOption.IGNORE_CASE)

        private val DATE_FORMATS = arrayOf(
            "dd/MM/yyyy, HH:mm:ss",
            "dd/MM/yy, HH:mm:ss",
            "dd/MM/yyyy, hh:mm:ss a",
            "dd/MM/yy, hh:mm:ss a",
            "dd/MM/yyyy, HH:mm",
            "dd/MM/yy, HH:mm",
            "dd/MM/yyyy, hh:mm a",
            "dd/MM/yy, hh:mm a",
            "MM/dd/yyyy, hh:mm a",
            "MM/dd/yy, hh:mm a",
            "yyyy-MM-dd, HH:mm:ss",
            "yyyy/MM/dd, HH:mm"
        )

        fun parseChatStream(
            inputStream: InputStream,
            chatId: String,
            fallbackSenderName: String,
            mediaFilesMap: Map<String, String> // filename -> local absolute path
        ): ParsedChatResult {
            val messages = mutableListOf<MessageEntity>()
            var primaryContactName = fallbackSenderName
            var lastTimestamp = System.currentTimeMillis()

            val reader = BufferedReader(InputStreamReader(inputStream))
            var currentMessage: MessageEntity? = null

            reader.forEachLine { rawLine ->
                val line = rawLine.trim()
                if (line.isEmpty()) return@forEachLine

                val match = BRACKET_PATTERN.matchEntire(line) ?: DASH_PATTERN.matchEntire(line)

                if (match != null) {
                    // Save previous multiline message
                    currentMessage?.let { messages.add(it) }

                    val dateStr = match.groupValues[1]
                    val sender = match.groupValues[2].trim()
                    val body = match.groupValues[3].trim()

                    val timestamp = parseTimestamp(dateStr)
                    lastTimestamp = timestamp

                    if (primaryContactName == fallbackSenderName && sender.isNotBlank() && !sender.equals("You", ignoreCase = true)) {
                        primaryContactName = sender
                    }

                    val isDeleted = isDeletionText(body)
                    val (type, mediaPath) = resolveMediaTypeAndFile(body, mediaFilesMap)

                    currentMessage = MessageEntity(
                        chatId = chatId,
                        senderName = sender,
                        content = body,
                        timestamp = timestamp,
                        messageType = if (isDeleted) MessageType.DELETED.name else type.name,
                        mediaUri = mediaPath,
                        isDeleted = isDeleted,
                        originalContent = body,
                        isFromMe = sender.equals("You", ignoreCase = true)
                    )
                } else {
                    // Check if it's a continuation line of the current message
                    if (currentMessage != null && !SYSTEM_PATTERN.matches(line)) {
                        val updatedContent = currentMessage!!.content + "\n" + line
                        currentMessage = currentMessage!!.copy(content = updatedContent)
                    }
                }
            }

            // Add final pending message
            currentMessage?.let { messages.add(it) }

            return ParsedChatResult(
                contactName = primaryContactName,
                messages = messages,
                lastTimestamp = lastTimestamp
            )
        }

        private fun parseTimestamp(dateString: String): Long {
            val cleaned = dateString.replace("\u202F", " ").trim()
            for (format in DATE_FORMATS) {
                try {
                    val sdf = SimpleDateFormat(format, Locale.getDefault())
                    sdf.isLenient = true
                    val date = sdf.parse(cleaned)
                    if (date != null) return date.time
                } catch (_: Exception) {}
            }
            return System.currentTimeMillis()
        }

        private fun isDeletionText(text: String): Boolean {
            val lower = text.lowercase()
            return lower.contains("this message was deleted") ||
                    lower.contains("you deleted this message")
        }

        private fun resolveMediaTypeAndFile(
            body: String,
            mediaMap: Map<String, String>
        ): Pair<MessageType, String?> {
            val match = ATTACHMENT_PATTERN.find(body)
            if (match != null) {
                val fileName = match.groupValues[1]
                val localPath = mediaMap[fileName] ?: mediaMap.entries.firstOrNull {
                    it.key.equals(fileName, ignoreCase = true)
                }?.value

                val lower = fileName.lowercase()
                val type = when {
                    lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") -> MessageType.IMAGE
                    lower.endsWith(".mp4") || lower.endsWith(".m4v") -> MessageType.VIDEO
                    lower.endsWith(".opus") -> MessageType.VOICE_NOTE
                    lower.endsWith(".mp3") || lower.endsWith(".m4a") -> MessageType.AUDIO
                    lower.endsWith(".webp") -> MessageType.STICKER
                    lower.endsWith(".gif") -> MessageType.GIF
                    else -> MessageType.DOCUMENT
                }
                return Pair(type, localPath)
            }

            val lower = body.lowercase()
            return when {
                lower.contains("<media omitted>") -> Pair(MessageType.IMAGE, null)
                else -> Pair(MessageType.TEXT, null)
            }
        }
    }
}
