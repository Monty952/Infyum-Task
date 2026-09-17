package com.example.practicalapp2.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["chatId"]),
        Index(value = ["timestamp"]),
        Index(value = ["messageType"])
    ]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val messageId: Long = 0,
    val chatId: String,
    val senderName: String,
    val content: String,
    val timestamp: Long,
    val messageType: String,
    val mediaUri: String? = null,
    val mediaMimeType: String? = null,
    val isDeleted: Boolean = false,
    val originalContent: String? = null,
    val isFromMe: Boolean = false
)
