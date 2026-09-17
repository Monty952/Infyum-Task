package com.example.practicalapp2

import com.example.practicalapp2.data.local.entity.MessageEntity
import com.example.practicalapp2.data.repository.ChatRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageDeduplicationTest {

    @Test
    fun testNormalizeTextStripsUnicodeMarksAndTrims() {
        val raw = "\u200E\u200F  Hello  \u202AWorld\u202C  \u00A0"
        val normalized = ChatRepository.normalizeText(raw)
        assertEquals("Hello  World", normalized)

        val nullResult = ChatRepository.normalizeText(null)
        assertEquals("", nullResult)

        val arabicWithDirectional = "\u2066مرحبا\u2069"
        val cleanedArabic = ChatRepository.normalizeText(arabicWithDirectional)
        assertEquals("مرحبا", cleanedArabic)
    }

    @Test
    fun testDeduplicateMessagesRemovesDuplicatesWithin30Seconds() {
        val now = 1726567200000L

        val list = listOf(
            MessageEntity(
                messageId = 1,
                chatId = "com.whatsapp_rahul",
                senderName = "Rahul",
                content = "Hey bro, are we meeting today?",
                timestamp = now,
                messageType = "TEXT"
            ),
            // Duplicate notification 2 seconds later (race condition / notification update)
            MessageEntity(
                messageId = 2,
                chatId = "com.whatsapp_rahul",
                senderName = "Rahul ",
                content = "Hey bro, are we meeting today?",
                timestamp = now + 2000,
                messageType = "TEXT"
            ),
            // Different message from Rahul
            MessageEntity(
                messageId = 3,
                chatId = "com.whatsapp_rahul",
                senderName = "Rahul",
                content = "Let me know soon!",
                timestamp = now + 4000,
                messageType = "TEXT"
            ),
            // Same content from a different sender (Priya)
            MessageEntity(
                messageId = 4,
                chatId = "com.whatsapp_priya",
                senderName = "Priya",
                content = "Hey bro, are we meeting today?",
                timestamp = now + 1000,
                messageType = "TEXT"
            )
        )

        val deduplicated = deduplicateList(list)

        // Should keep messageId 1, messageId 3, and messageId 4 (total 3 messages, duplicate #2 removed)
        assertEquals(3, deduplicated.size)
        assertEquals(1L, deduplicated[0].messageId)
        assertEquals(3L, deduplicated[1].messageId)
        assertEquals(4L, deduplicated[2].messageId)
    }

    @Test
    fun testDeduplicateMessagesAllowsSameContentAfterTimeWindow() {
        val now = 1726567200000L

        val list = listOf(
            MessageEntity(
                messageId = 1,
                chatId = "com.whatsapp_rahul",
                senderName = "Rahul",
                content = "Ok",
                timestamp = now,
                messageType = "TEXT"
            ),
            // Same sender says "Ok" 5 minutes later (legitimate repeat message)
            MessageEntity(
                messageId = 2,
                chatId = "com.whatsapp_rahul",
                senderName = "Rahul",
                content = "Ok",
                timestamp = now + 300_000, // 5 minutes later
                messageType = "TEXT"
            )
        )

        val deduplicated = deduplicateList(list)
        assertEquals(2, deduplicated.size)
    }

    private fun deduplicateList(messages: List<MessageEntity>): List<MessageEntity> {
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
}
