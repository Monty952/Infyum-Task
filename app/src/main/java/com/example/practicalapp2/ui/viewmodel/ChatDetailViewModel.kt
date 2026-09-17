package com.example.practicalapp2.ui.viewmodel

import android.app.Application
import android.media.MediaPlayer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.practicalapp2.data.local.entity.ChatEntity
import com.example.practicalapp2.data.local.entity.MessageEntity
import com.example.practicalapp2.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

data class AudioPlaybackState(
    val isPlaying: Boolean = false,
    val messageId: Long? = null,
    val currentPosition: Int = 0,
    val duration: Int = 0
)

class ChatDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ChatRepository(application)
    private var mediaPlayer: MediaPlayer? = null

    private val _chat = MutableStateFlow<ChatEntity?>(null)
    val chat: StateFlow<ChatEntity?> = _chat.asStateFlow()

    private val _playbackState = MutableStateFlow(AudioPlaybackState())
    val playbackState: StateFlow<AudioPlaybackState> = _playbackState.asStateFlow()

    fun loadChat(chatId: String): StateFlow<List<MessageEntity>> {
        viewModelScope.launch {
            _chat.value = repository.getChatById(chatId)
        }
        return repository.getMessagesForChatFlow(chatId)
            .map { deduplicateMessages(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

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

    fun playAudio(messageId: Long, filePath: String) {
        val file = File(filePath)
        if (!file.exists()) return

        if (_playbackState.value.isPlaying && _playbackState.value.messageId == messageId) {
            stopAudio()
            return
        }

        stopAudio()

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                start()
                _playbackState.value = AudioPlaybackState(
                    isPlaying = true,
                    messageId = messageId,
                    duration = duration
                )
                setOnCompletionListener {
                    stopAudio()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            stopAudio()
        }
    }

    fun stopAudio() {
        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        mediaPlayer = null
        _playbackState.value = AudioPlaybackState(isPlaying = false, messageId = null)
    }

    override fun onCleared() {
        super.onCleared()
        stopAudio()
    }
}
