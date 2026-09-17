package com.example.practicalapp2.util

import android.content.Context
import android.net.Uri
import com.example.practicalapp2.data.local.entity.ChatEntity
import com.example.practicalapp2.data.model.ChatSource
import com.example.practicalapp2.data.repository.ChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

class ChatZipExtractor(private val context: Context) {

    suspend fun extractAndImportZip(zipUri: Uri, fallbackChatTitle: String? = null): Result<ChatEntity> =
        withContext(Dispatchers.IO) {
            try {
                val timestamp = System.currentTimeMillis()
                val targetDir = File(context.filesDir, "imported_chats/$timestamp")
                if (!targetDir.exists()) targetDir.mkdirs()

                val mediaFilesMap = mutableMapOf<String, String>()
                var chatTextFile: File? = null

                context.contentResolver.openInputStream(zipUri)?.use { inputStream ->
                    ZipInputStream(inputStream).use { zipIn ->
                        var entry = zipIn.nextEntry
                        while (entry != null) {
                            if (!entry.isDirectory) {
                                val fileName = File(entry.name).name
                                val outputFile = File(targetDir, fileName)

                                FileOutputStream(outputFile).use { out ->
                                    zipIn.copyTo(out)
                                }

                                if (fileName.endsWith(".txt", ignoreCase = true)) {
                                    chatTextFile = outputFile
                                } else {
                                    mediaFilesMap[fileName] = outputFile.absolutePath
                                }
                            }
                            zipIn.closeEntry()
                            entry = zipIn.nextEntry
                        }
                    }
                }

                if (chatTextFile == null || !chatTextFile!!.exists()) {
                    return@withContext Result.failure(Exception("No WhatsApp chat .txt log found in the selected ZIP file."))
                }

                val chatId = "imported_${timestamp}"
                val defaultName = fallbackChatTitle ?: chatTextFile!!.nameWithoutExtension.replace("WhatsApp Chat with ", "")

                val parsed = FileInputStream(chatTextFile!!).use { fileStream ->
                    WhatsAppChatParser.parseChatStream(
                        inputStream = fileStream,
                        chatId = chatId,
                        fallbackSenderName = defaultName,
                        mediaFilesMap = mediaFilesMap
                    )
                }

                val chatEntity = ChatEntity(
                    chatId = chatId,
                    senderName = parsed.contactName,
                    packageName = "com.whatsapp",
                    lastMessage = parsed.messages.lastOrNull()?.content ?: "Imported chat",
                    lastTimestamp = parsed.lastTimestamp,
                    unreadCount = 0,
                    chatSource = ChatSource.IMPORTED_EXPORT.name,
                    avatarColor = ChatRepository.getAvatarColor(parsed.contactName)
                )

                val repository = ChatRepository(context)
                repository.insertImportedChat(chatEntity, parsed.messages)

                Result.success(chatEntity)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }

    suspend fun importSingleTextFile(textUri: Uri, fileName: String): Result<ChatEntity> =
        withContext(Dispatchers.IO) {
            try {
                val timestamp = System.currentTimeMillis()
                val chatId = "imported_${timestamp}"
                val defaultName = fileName.replace("WhatsApp Chat with ", "").removeSuffix(".txt")

                val parsed = context.contentResolver.openInputStream(textUri)?.use { fileStream ->
                    WhatsAppChatParser.parseChatStream(
                        inputStream = fileStream,
                        chatId = chatId,
                        fallbackSenderName = defaultName,
                        mediaFilesMap = emptyMap()
                    )
                } ?: return@withContext Result.failure(Exception("Cannot read chat text file."))

                val chatEntity = ChatEntity(
                    chatId = chatId,
                    senderName = parsed.contactName,
                    packageName = "com.whatsapp",
                    lastMessage = parsed.messages.lastOrNull()?.content ?: "Imported chat",
                    lastTimestamp = parsed.lastTimestamp,
                    unreadCount = 0,
                    chatSource = ChatSource.IMPORTED_EXPORT.name,
                    avatarColor = ChatRepository.getAvatarColor(parsed.contactName)
                )

                val repository = ChatRepository(context)
                repository.insertImportedChat(chatEntity, parsed.messages)

                Result.success(chatEntity)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
}
