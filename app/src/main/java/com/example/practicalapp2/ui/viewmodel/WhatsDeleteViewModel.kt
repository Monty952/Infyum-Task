package com.example.practicalapp2.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.practicalapp2.data.local.entity.ChatEntity
import com.example.practicalapp2.data.local.entity.MessageEntity
import com.example.practicalapp2.data.model.MessageType
import com.example.practicalapp2.data.repository.ChatRepository
import com.example.practicalapp2.service.WhatsAppNotificationListener
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class WhatsDeleteViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ChatRepository(application)

    val isListenerConnected = WhatsAppNotificationListener.isServiceConnected

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterType = MutableStateFlow("ALL")
    val filterType: StateFlow<String> = _filterType.asStateFlow()

    private val _selectedMediaType = MutableStateFlow(MessageType.IMAGE)
    val selectedMediaType: StateFlow<MessageType> = _selectedMediaType.asStateFlow()

    // Captured messages flow filtered by both filterType and searchQuery with UI-level deduplication
    val capturedMessages: StateFlow<List<MessageEntity>> = combine(_filterType, _searchQuery) { type, query ->
        Pair(type, query)
    }.flatMapLatest { (type, query) ->
        repository.getFilteredMessagesFlow(type, query)
    }.map { list ->
        deduplicateMessages(list)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Chats flow filtered by search query
    val chats: StateFlow<List<ChatEntity>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                repository.getAllChatsFlow()
            } else {
                repository.searchChatsFlow(query)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered media messages based on selected chip with UI-level deduplication
    val mediaMessages: StateFlow<List<MessageEntity>> = _selectedMediaType
        .flatMapLatest { type ->
            repository.getMediaMessagesFlow(listOf(type))
        }.map { list ->
            deduplicateMessages(list)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun deduplicateMessages(messages: List<MessageEntity>): List<MessageEntity> {
        val result = ArrayList<MessageEntity>(messages.size)
        for (msg in messages) {
            val isDuplicate = result.any { existing ->
                val sameSender = existing.senderName.trim().equals(msg.senderName.trim(), ignoreCase = true)
                val sameContent = existing.content.trim() == msg.content.trim()
                val closeTime = kotlin.math.abs(existing.timestamp - msg.timestamp) < 30_000
                (sameSender && sameContent && closeTime) || (sameSender && existing.timestamp == msg.timestamp)
            }
            if (!isDuplicate) {
                result.add(msg)
            }
        }
        return result
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterType(type: String) {
        _filterType.value = type
    }

    fun setSelectedMediaType(type: MessageType) {
        _selectedMediaType.value = type
    }

    fun deleteChat(chatId: String) {
        viewModelScope.launch {
            repository.deleteChat(chatId)
        }
    }

    fun deleteMessage(messageId: Long) {
        viewModelScope.launch {
            repository.deleteMessage(messageId)
        }
    }

    /**
     * Allows immediate testing & demonstration of all required WhatsApp notification types
     */
    fun simulateSampleNotification(type: MessageType) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            when (type) {
                MessageType.TEXT -> {
                    repository.processNotificationMessage(
                        senderName = "Rahul Sharma",
                        content = "Hey bro, are we meeting today?",
                        timestamp = now,
                        packageName = "com.whatsapp",
                        detectedType = MessageType.TEXT
                    )
                }
                MessageType.IMAGE -> {
                    repository.processNotificationMessage(
                        senderName = "Priya Patel",
                        content = "📷 Photo",
                        timestamp = now,
                        packageName = "com.whatsapp",
                        detectedType = MessageType.IMAGE
                    )
                }
                MessageType.VIDEO -> {
                    repository.processNotificationMessage(
                        senderName = "Amit Kumar",
                        content = "🎥 Video (0:45)",
                        timestamp = now,
                        packageName = "com.whatsapp",
                        detectedType = MessageType.VIDEO
                    )
                }
                MessageType.VOICE_NOTE -> {
                    repository.processNotificationMessage(
                        senderName = "Hiren Bhau",
                        content = "🎤 Voice message (0:12)",
                        timestamp = now,
                        packageName = "com.whatsapp",
                        detectedType = MessageType.VOICE_NOTE
                    )
                }
                MessageType.VIDEO_NOTE -> {
                    repository.processNotificationMessage(
                        senderName = "Suresh",
                        content = "🎥 Video note (0:08)",
                        timestamp = now,
                        packageName = "com.whatsapp",
                        detectedType = MessageType.VIDEO_NOTE
                    )
                }
                MessageType.DOCUMENT -> {
                    repository.processNotificationMessage(
                        senderName = "Office Group",
                        content = "📄 Project_Specs.pdf",
                        timestamp = now,
                        packageName = "com.whatsapp",
                        detectedType = MessageType.DOCUMENT
                    )
                }
                MessageType.GIF -> {
                    repository.processNotificationMessage(
                        senderName = "Anjali",
                        content = "GIF",
                        timestamp = now,
                        packageName = "com.whatsapp",
                        detectedType = MessageType.GIF
                    )
                }
                MessageType.STICKER -> {
                    repository.processNotificationMessage(
                        senderName = "Rohan",
                        content = "Sticker",
                        timestamp = now,
                        packageName = "com.whatsapp",
                        detectedType = MessageType.STICKER
                    )
                }
                MessageType.DELETED -> {
                    // First create a message
                    repository.processNotificationMessage(
                        senderName = "Sneha",
                        content = "I made a secret plan for your surprise party!",
                        timestamp = now - 60000,
                        packageName = "com.whatsapp",
                        detectedType = MessageType.TEXT
                    )
                    // Then simulate deletion notification
                    repository.processNotificationMessage(
                        senderName = "Sneha",
                        content = "This message was deleted",
                        timestamp = now,
                        packageName = "com.whatsapp",
                        detectedType = MessageType.DELETED
                    )
                }
                else -> {}
            }
        }
    }
}
