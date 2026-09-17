package com.example.practicalapp2.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.practicalapp2.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages ORDER BY timestamp DESC")
    fun getAllMessagesFlow(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    fun getMessagesForChatFlow(chatId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE isDeleted = 1 ORDER BY timestamp DESC")
    fun getDeletedMessagesFlow(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE messageType IN (:types) ORDER BY timestamp DESC")
    fun getMediaMessagesFlow(types: List<String>): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE messageType != 'TEXT' AND messageType != 'DELETED' ORDER BY timestamp DESC")
    fun getAllMediaMessagesFlow(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE content LIKE '%' || :query || '%' OR senderName LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchMessagesFlow(query: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE (messageType = :type OR :type = 'ALL') AND (content LIKE '%' || :query || '%' OR senderName LIKE '%' || :query || '%') ORDER BY timestamp DESC")
    fun getFilteredMessagesFlow(type: String, query: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastMessageForChat(chatId: String): MessageEntity?

    @Query("SELECT * FROM messages WHERE senderName = :senderName ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastMessageForSender(senderName: String): MessageEntity?

    @Query("SELECT COUNT(*) FROM messages WHERE senderName = :senderName AND content = :content AND ABS(timestamp - :timestamp) < 5000")
    suspend fun checkDuplicate(senderName: String, content: String, timestamp: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<MessageEntity>)

    @Update
    suspend fun updateMessage(message: MessageEntity)

    @Query("UPDATE messages SET isDeleted = 1 WHERE messageId = :messageId")
    suspend fun markMessageDeleted(messageId: Long)

    @Query("DELETE FROM messages WHERE messageId = :messageId")
    suspend fun deleteMessage(messageId: Long)

    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun deleteMessagesForChat(chatId: String)

    @Query("DELETE FROM messages")
    suspend fun deleteAll()

    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    suspend fun getAllMessagesList(): List<MessageEntity>
}
