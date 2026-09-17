package com.example.practicalapp2.data.repository

import android.content.Context
import com.example.practicalapp2.data.local.AppDatabase
import com.example.practicalapp2.data.local.entity.ChatEntity
import com.example.practicalapp2.data.local.entity.MessageEntity
import com.example.practicalapp2.data.model.ChatSource
import com.example.practicalapp2.data.model.MessageType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class BackupData(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val chats: List<ChatEntity>,
    val messages: List<MessageEntity>
)

class ChatRepository(context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val chatDao = database.chatDao()
    private val messageDao = database.messageDao()
    private val gson = Gson()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                messageDao.purgeDuplicateMessages()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getAllChatsFlow(): Flow<List<ChatEntity>> = chatDao.getAllChatsFlow()

    fun getChatsBySourceFlow(source: ChatSource): Flow<List<ChatEntity>> =
        chatDao.getChatsBySourceFlow(source.name)

    fun searchChatsFlow(query: String): Flow<List<ChatEntity>> =
        chatDao.searchChatsFlow(query)

    fun getAllMessagesFlow(): Flow<List<MessageEntity>> = messageDao.getAllMessagesFlow()

    fun getFilteredMessagesFlow(type: String, query: String): Flow<List<MessageEntity>> =
        messageDao.getFilteredMessagesFlow(type, query)

    fun getMessagesForChatFlow(chatId: String): Flow<List<MessageEntity>> =
        messageDao.getMessagesForChatFlow(chatId)

    fun getDeletedMessagesFlow(): Flow<List<MessageEntity>> =
        messageDao.getDeletedMessagesFlow()

    fun getMediaMessagesFlow(types: List<MessageType>): Flow<List<MessageEntity>> =
        messageDao.getMediaMessagesFlow(types.map { it.name })

    fun getAllMediaMessagesFlow(): Flow<List<MessageEntity>> =
        messageDao.getAllMediaMessagesFlow()

    fun searchMessagesFlow(query: String): Flow<List<MessageEntity>> =
        messageDao.searchMessagesFlow(query)

    suspend fun getChatById(chatId: String): ChatEntity? = withContext(Dispatchers.IO) {
        chatDao.getChatById(chatId)
    }

    suspend fun processNotificationMessage(
        senderName: String,
        content: String,
        timestamp: Long,
        packageName: String,
        mediaUri: String? = null,
        detectedType: MessageType = MessageType.TEXT
    ): Boolean = withContext(Dispatchers.IO) {
        notificationMutex.withLock {
            val cleanSender = normalizeText(senderName).ifBlank { "WhatsApp User" }
            val cleanContent = normalizeText(content)
            val chatId = "${packageName}_${cleanSender.lowercase()}"
            val isDeletionNotice = isDeletionMessage(cleanContent)

            if (isDeletionNotice) {
                // Find last message from this sender and mark as deleted
                val lastMsg = messageDao.getLastMessageForSender(cleanSender)
                if (lastMsg != null) {
                    if (!lastMsg.isDeleted) {
                        messageDao.markMessageDeleted(lastMsg.messageId)
                        val currentChat = chatDao.getChatById(chatId)
                        if (currentChat != null) {
                            chatDao.insertOrUpdate(
                                currentChat.copy(
                                    lastMessage = "⚠️ [Deleted] ${lastMsg.content.ifBlank { lastMsg.messageType }}",
                                    lastTimestamp = timestamp
                                )
                            )
                        }
                    }
                    // Return early: preserve original captured message as deleted without inserting a duplicate card
                    return@withLock true
                }
            }

            // Check for duplicate messages to prevent spam from repeated notifications
            val duplicateCount = messageDao.checkDuplicate(chatId, cleanSender, cleanContent, timestamp)
            if (duplicateCount > 0) {
                return@withLock false
            }

            val avatarColor = getAvatarColor(cleanSender)

            val chatEntity = ChatEntity(
                chatId = chatId,
                senderName = cleanSender,
                packageName = packageName,
                lastMessage = if (cleanContent.isBlank()) "[${detectedType.displayName}]" else cleanContent,
                lastTimestamp = timestamp,
                unreadCount = (chatDao.getChatById(chatId)?.unreadCount ?: 0) + 1,
                chatSource = if (packageName.contains("w4b")) ChatSource.NOTIFICATION_BUSINESS.name else ChatSource.NOTIFICATION_WHATSAPP.name,
                avatarColor = avatarColor
            )
            chatDao.insertOrUpdate(chatEntity)

            val messageEntity = MessageEntity(
                chatId = chatId,
                senderName = cleanSender,
                content = cleanContent,
                timestamp = timestamp,
                messageType = detectedType.name,
                mediaUri = mediaUri,
                isDeleted = isDeletionNotice,
                originalContent = cleanContent
            )
            messageDao.insertMessage(messageEntity)
            true
        }
    }

    suspend fun purgeDuplicates(): Int = withContext(Dispatchers.IO) {
        try {
            messageDao.purgeDuplicateMessages()
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    suspend fun insertImportedChat(
        chat: ChatEntity,
        messages: List<MessageEntity>
    ) = withContext(Dispatchers.IO) {
        chatDao.insertOrUpdate(chat)
        messageDao.insertAll(messages)
    }

    suspend fun deleteChat(chatId: String) = withContext(Dispatchers.IO) {
        messageDao.deleteMessagesForChat(chatId)
        chatDao.deleteChat(chatId)
    }

    suspend fun deleteMessage(messageId: Long) = withContext(Dispatchers.IO) {
        messageDao.deleteMessage(messageId)
    }

    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        val chats = chatDao.getAllChatsList()
        val messages = messageDao.getAllMessagesList()
        val backup = BackupData(
            version = 1,
            timestamp = System.currentTimeMillis(),
            chats = chats,
            messages = messages
        )
        gson.toJson(backup)
    }

    suspend fun restoreBackupJson(jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val type = object : TypeToken<BackupData>() {}.type
            val backup: BackupData = gson.fromJson(jsonString, type)
            if (backup.chats.isNotEmpty()) {
                chatDao.insertAll(backup.chats)
            }
            if (backup.messages.isNotEmpty()) {
                messageDao.insertAll(backup.messages)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun isDeletionMessage(text: String): Boolean {
        val lower = text.lowercase()
        return lower.contains("this message was deleted") ||
                lower.contains("you deleted this message") ||
                lower.contains("message was deleted")
    }

    companion object {
        private val notificationMutex = Mutex()

        fun normalizeText(input: String?): String {
            if (input == null) return ""
            return input
                .replace("\u200E", "") // Left-to-Right Mark
                .replace("\u200F", "") // Right-to-Left Mark
                .replace("\u202A", "") // Left-to-Right Embedding
                .replace("\u202B", "") // Right-to-Left Embedding
                .replace("\u202C", "") // Pop Directional Formatting
                .replace("\u202D", "") // Left-to-Right Override
                .replace("\u202E", "") // Right-to-Left Override
                .replace("\u2066", "") // Left-to-Right Isolate
                .replace("\u2067", "") // Right-to-Left Isolate
                .replace("\u2068", "") // First Strong Isolate
                .replace("\u2069", "") // Pop Directional Isolate
                .replace("\u00A0", " ") // Non-breaking space
                .replace("\u202F", " ") // Narrow no-break space
                .trim()
        }

        fun getAvatarColor(name: String): Int {
            val palette = intArrayOf(
                0xFF008069.toInt(),
                0xFF00A884.toInt(),
                0xFF128C7E.toInt(),
                0xFF25D366.toInt(),
                0xFF075E54.toInt(),
                0xFF34B7F1.toInt(),
                0xFF4A90E2.toInt(),
                0xFF7B1FA2.toInt(),
                0xFFC2185B.toInt(),
                0xFFD32F2F.toInt(),
                0xFFE65100.toInt(),
                0xFF5D4037.toInt()
            )
            val hash = kotlin.math.abs(name.hashCode())
            return palette[hash % palette.size]
        }
    }
}
