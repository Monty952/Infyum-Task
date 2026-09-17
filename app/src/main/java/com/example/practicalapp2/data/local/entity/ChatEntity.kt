package com.example.practicalapp2.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey
    val chatId: String,
    val senderName: String,
    val packageName: String,
    val lastMessage: String,
    val lastTimestamp: Long,
    val unreadCount: Int = 0,
    val chatSource: String, // ChatSource enum string
    val avatarColor: Int = 0xFF008069.toInt()
)
