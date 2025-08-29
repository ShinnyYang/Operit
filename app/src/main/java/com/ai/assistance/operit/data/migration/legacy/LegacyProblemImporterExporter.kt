package com.ai.assistance.operit.data.legacy

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.ai.assistance.operit.api.chat.library.ProblemLibraryTool
import com.ai.assistance.operit.data.db.AppDatabase
import com.ai.assistance.operit.ui.features.settings.screens.ImportResult
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Handles the import, export, and deletion of legacy ProblemEntity records.
 * This class interacts directly with the ProblemDao.
 */
class LegacyProblemImporterExporter(context: Context) {

    private val problemDao = AppDatabase.getDatabase(context).problemDao()

    suspend fun getProblemCount(): Int = withContext(Dispatchers.IO) {
        problemDao.getProblemCount()
    }

    suspend fun deleteAllProblems(): Int = withContext(Dispatchers.IO) {
        val count = problemDao.getProblemCount()
        if (count > 0) {
            problemDao.clearAll()
        }
        count
    }

    suspend fun exportProblems(context: Context): String? = withContext(Dispatchers.IO) {
        try {
            val problems = problemDao.getAllProblems().map { it.toProblemRecord() }
            if (problems.isEmpty()) {
                return@withContext null // Or return a message indicating no problems to export
            }

            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val exportDir = File(downloadDir, "Operit")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }

            val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
            val timestamp = dateFormat.format(Date())
            val exportFile = File(exportDir, "problem_library_backup_$timestamp.json")

            val gson = GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
                .setPrettyPrinting()
                .create()

            val jsonString = gson.toJson(problems)
            exportFile.writeText(jsonString)

            exportFile.absolutePath
        } catch (e: Exception) {
            Log.e("LegacyProblemExporter", "Export failed", e)
            null
        }
    }

    suspend fun importProblems(context: Context, uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext ImportResult(0, 0, 0)
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            inputStream.close()

            if (jsonString.isBlank()) {
                throw Exception("Imported file is empty.")
            }

            val problems = try {
                val gson = GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss").create()
                val type = object : TypeToken<List<ProblemLibraryTool.ProblemRecord>>() {}.type
                gson.fromJson<List<ProblemLibraryTool.ProblemRecord>>(jsonString, type)
            } catch (e: Exception) {
                Log.e("LegacyProblemImporter", "GSON parsing failed", e)
                throw Exception("Failed to parse the backup file: ${e.message}. The file may be corrupt or in an incompatible format.")
            }

            if (problems.isEmpty()) {
                return@withContext ImportResult(0, 0, 0)
            }

            val existingIds = problemDao.getAllProblems().map { it.uuid }.toSet()
            var newCount = 0
            var updatedCount = 0
            var skippedCount = 0

            for (problem in problems) {
                if (problem.query.isBlank() || problem.solution.isBlank()) {
                    skippedCount++
                    continue
                }

                if (existingIds.contains(problem.uuid)) {
                    updatedCount++
                } else {
                    newCount++
                }

                problemDao.insertProblem(com.ai.assistance.operit.data.db.ProblemEntity.fromProblemRecord(problem))
            }

            ImportResult(newCount, updatedCount, skippedCount)
        } catch (e: Exception) {
            Log.e("LegacyProblemImporter", "Import failed", e)
            throw e
        }
    }
} 