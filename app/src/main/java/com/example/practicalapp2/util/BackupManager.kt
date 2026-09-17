package com.example.practicalapp2.util

import android.content.Context
import android.net.Uri
import com.example.practicalapp2.data.repository.ChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class BackupManager(private val context: Context) {
    private val repository = ChatRepository(context)

    suspend fun createBackup(destinationUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val json = repository.exportBackupJson()
            context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(json)
                }
            } ?: return@withContext Result.failure(Exception("Cannot open destination for backup."))
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun restoreBackup(sourceUri: Uri): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val stringBuilder = java.lang.StringBuilder()
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String? = reader.readLine()
                    while (line != null) {
                        stringBuilder.append(line)
                        line = reader.readLine()
                    }
                }
            } ?: return@withContext Result.failure(Exception("Cannot open backup source file."))

            val success = repository.restoreBackupJson(stringBuilder.toString())
            if (success) {
                Result.success(true)
            } else {
                Result.failure(Exception("Invalid or corrupted backup JSON file format."))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
