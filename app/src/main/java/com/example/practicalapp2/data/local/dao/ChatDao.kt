package com.example.practicalapp2.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.practicalapp2.data.local.entity.ChatEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats ORDER BY lastTimestamp DESC")
    fun getAllChatsFlow(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE chatSource = :source ORDER BY lastTimestamp DESC")
    fun getChatsBySourceFlow(source: String): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE senderName LIKE '%' || :query || '%' OR lastMessage LIKE '%' || :query || '%' ORDER BY lastTimestamp DESC")
    fun searchChatsFlow(query: String): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE chatId = :chatId LIMIT 1")
    suspend fun getChatById(chatId: String): ChatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(chat: ChatEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(chats: List<ChatEntity>)

    @Query("DELETE FROM chats WHERE chatId = :chatId")
    suspend fun deleteChat(chatId: String)

    @Query("DELETE FROM chats")
    suspend fun deleteAll()

    @Query("SELECT * FROM chats ORDER BY lastTimestamp DESC")
    suspend fun getAllChatsList(): List<ChatEntity>
}
