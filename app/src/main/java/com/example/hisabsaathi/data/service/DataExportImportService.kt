package com.example.hisabsaathi.data.service

import android.content.Context
import android.net.Uri
import com.example.hisabsaathi.data.db.HisabDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * DataExportImportService provides secure business data export and import capabilities:
 * - Exports business data to a structured, SHA-256 checksum-verified JSON payload.
 * - Streams JSON files directly to user-chosen device storage locations (SAF / Downloads).
 * - Restores data from JSON files or strings.
 * - Strictly enforces all relational foreign key constraints (Customer, Employee, Transaction relations).
 * - Runs atomically inside database transactions with rollback on constraint violation.
 */
class DataExportImportService(
    private val database: HisabDatabase,
    private val backupService: BusinessBackupService = BusinessBackupService(database)
) {

    /**
     * Exports full business entity hierarchy into formatted JSON.
     */
    suspend fun exportBusinessDataToJson(
        requestingUserId: Long,
        targetBusinessId: Long
    ): ExportResult = withContext(Dispatchers.IO) {
        backupService.exportBusinessDataToJson(requestingUserId, targetBusinessId)
    }

    /**
     * Imports and restores business data from JSON, validating foreign key constraints atomically.
     */
    suspend fun importBusinessDataFromJson(
        requestingUserId: Long,
        targetBusinessId: Long,
        jsonString: String,
        clearExistingData: Boolean = true
    ): ImportResult = withContext(Dispatchers.IO) {
        backupService.importBusinessDataFromJson(requestingUserId, targetBusinessId, jsonString, clearExistingData)
    }

    /**
     * Writes the given JSON string to a file Uri via Storage Access Framework (SAF).
     */
    suspend fun writeJsonToStorageUri(
        context: Context,
        uri: Uri,
        jsonContent: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(jsonContent)
                    writer.flush()
                }
            } ?: return@withContext Result.failure(Exception("Could not open destination file for writing."))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reads the JSON string from a user-selected file Uri via Storage Access Framework (SAF).
     */
    suspend fun readJsonFromStorageUri(
        context: Context,
        uri: Uri
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val content = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            } ?: return@withContext Result.failure(Exception("Could not open file for reading."))
            Result.success(content)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Generates a sanitized default filename for exporting business backups.
     */
    fun generateDefaultExportFileName(businessName: String): String {
        val cleanName = businessName.lowercase(Locale.ROOT)
            .replace("[^a-z0-9]".toRegex(), "_")
            .take(20)
            .ifBlank { "business" }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return "hisab_saathi_${cleanName}_backup_$timestamp.json"
    }
}
