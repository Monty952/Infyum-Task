package com.example.practicalapp2.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.practicalapp2.data.local.entity.ChatEntity
import com.example.practicalapp2.data.model.ChatSource
import com.example.practicalapp2.data.repository.ChatRepository
import com.example.practicalapp2.util.BackupManager
import com.example.practicalapp2.util.ChatZipExtractor
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class BackupUiEvent {
    data class Success(val message: String) : BackupUiEvent()
    data class Error(val message: String) : BackupUiEvent()
    data class Info(val message: String) : BackupUiEvent()
}

class ChatBackupViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ChatRepository(application)
    private val zipExtractor = ChatZipExtractor(application)
    private val backupManager = BackupManager(application)

    val importedChats: StateFlow<List<ChatEntity>> = repository.getChatsBySourceFlow(ChatSource.IMPORTED_EXPORT)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _events = MutableSharedFlow<BackupUiEvent>()
    val events: SharedFlow<BackupUiEvent> = _events.asSharedFlow()

    fun importZipFile(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = zipExtractor.extractAndImportZip(uri)
            _isLoading.value = false
            result.onSuccess { chat ->
                _events.emit(BackupUiEvent.Success("Successfully imported chat with ${chat.senderName}"))
            }.onFailure { error ->
                _events.emit(BackupUiEvent.Error("Import failed: ${error.localizedMessage ?: "Unknown error"}"))
            }
        }
    }

    fun importTextFile(uri: Uri, fileName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = zipExtractor.importSingleTextFile(uri, fileName)
            _isLoading.value = false
            result.onSuccess { chat ->
                _events.emit(BackupUiEvent.Success("Successfully imported chat with ${chat.senderName}"))
            }.onFailure { error ->
                _events.emit(BackupUiEvent.Error("Import failed: ${error.localizedMessage ?: "Unknown error"}"))
            }
        }
    }

    fun createBackup(destinationUri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = backupManager.createBackup(destinationUri)
            _isLoading.value = false
            result.onSuccess {
                _events.emit(BackupUiEvent.Success("Backup saved successfully!"))
            }.onFailure { error ->
                _events.emit(BackupUiEvent.Error("Backup failed: ${error.localizedMessage}"))
            }
        }
    }

    fun restoreBackup(sourceUri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = backupManager.restoreBackup(sourceUri)
            _isLoading.value = false
            result.onSuccess {
                _events.emit(BackupUiEvent.Success("Chats restored successfully!"))
            }.onFailure { error ->
                _events.emit(BackupUiEvent.Error("Restore failed: ${error.localizedMessage}"))
            }
        }
    }

    fun deleteChat(chatId: String) {
        viewModelScope.launch {
            repository.deleteChat(chatId)
        }
    }
}
