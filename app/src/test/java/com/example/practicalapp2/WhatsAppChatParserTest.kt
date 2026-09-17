package com.example.practicalapp2

import com.example.practicalapp2.data.local.entity.ChatEntity
import com.example.practicalapp2.data.local.entity.MessageEntity
import com.example.practicalapp2.data.model.MessageType
import com.example.practicalapp2.data.repository.BackupData
import com.example.practicalapp2.util.WhatsAppChatParser
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class WhatsAppChatParserTest {

    @Test
    fun testParseStandardDashChatFormat() {
        val chatLog = """
            17/09/2026, 12:35 - Messages and calls are end-to-end encrypted. No one outside of this chat, not even WhatsApp, can read or listen to them. Tap to learn more.
            17/09/2026, 12:40 - Hiren Bhau: Hello bro!
            17/09/2026, 12:41 - You: Hey Hiren!
            17/09/2026, 12:42 - Hiren Bhau: Check this photo IMG-20260917-WA0001.jpg (file attached)
            17/09/2026, 12:43 - Hiren Bhau: This message was deleted
        """.trimIndent()

        val mediaMap = mapOf("IMG-20260917-WA0001.jpg" to "/data/user/0/app/files/imported/IMG-20260917-WA0001.jpg")

        val result = WhatsAppChatParser.parseChatStream(
            inputStream = ByteArrayInputStream(chatLog.toByteArray()),
            chatId = "test_chat_1",
            fallbackSenderName = "Hiren Bhau",
            mediaFilesMap = mediaMap
        )

        assertEquals("Hiren Bhau", result.contactName)
        assertEquals(4, result.messages.size)

        // First message: Text
        assertEquals("Hello bro!", result.messages[0].content)
        assertEquals("Hiren Bhau", result.messages[0].senderName)
        assertEquals(MessageType.TEXT.name, result.messages[0].messageType)

        // Second message: from You
        assertTrue(result.messages[1].isFromMe)

        // Third message: Image
        assertEquals(MessageType.IMAGE.name, result.messages[2].messageType)
        assertNotNull(result.messages[2].mediaUri)

        // Fourth message: Deleted message
        assertTrue(result.messages[3].isDeleted)
        assertEquals(MessageType.DELETED.name, result.messages[3].messageType)
    }

    @Test
    fun testParseBracketChatFormat() {
        val chatLog = """
            [17/09/2026, 12:40:15 PM] Alex: Hey, are you free?
            [17/09/2026, 12:41:00 PM] Alex: Listen to this PTT-20260917-WA0002.opus (file attached)
        """.trimIndent()

        val mediaMap = mapOf("PTT-20260917-WA0002.opus" to "/data/user/0/app/files/imported/PTT-20260917-WA0002.opus")

        val result = WhatsAppChatParser.parseChatStream(
            inputStream = ByteArrayInputStream(chatLog.toByteArray()),
            chatId = "test_chat_2",
            fallbackSenderName = "Alex",
            mediaFilesMap = mediaMap
        )

        assertEquals("Alex", result.contactName)
        assertEquals(2, result.messages.size)
        assertEquals(MessageType.VOICE_NOTE.name, result.messages[1].messageType)
    }

    @Test
    fun testBackupDataSerialization() {
        val gson = Gson()
        val chats = listOf(
            ChatEntity(
                chatId = "chat_1",
                senderName = "Hiren Bhau",
                packageName = "com.whatsapp",
                lastMessage = "Ohkk bhai",
                lastTimestamp = 1726567200000L,
                unreadCount = 1,
                chatSource = "NOTIFICATION_WHATSAPP"
            )
        )
        val messages = listOf(
            MessageEntity(
                messageId = 1,
                chatId = "chat_1",
                senderName = "Hiren Bhau",
                content = "Ohkk bhai",
                timestamp = 1726567200000L,
                messageType = "TEXT"
            )
        )

        val backup = BackupData(
            version = 1,
            timestamp = System.currentTimeMillis(),
            chats = chats,
            messages = messages
        )

        val json = gson.toJson(backup)
        val restored = gson.fromJson(json, BackupData::class.java)

        assertEquals(1, restored.chats.size)
        assertEquals("Hiren Bhau", restored.chats[0].senderName)
        assertEquals(1, restored.messages.size)
        assertEquals("Ohkk bhai", restored.messages[0].content)
    }
}
