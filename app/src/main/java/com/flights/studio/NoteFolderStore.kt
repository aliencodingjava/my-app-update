package com.flights.studio

import android.content.Context
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

data class NoteFolder(
    val id: String,
    val name: String,
    val createdAt: Long
)

object NoteFolderStore {
    const val MAIN_FOLDER_ID = "main"

    private const val PREFS_NAME = "note_folders"
    private const val KEY_FOLDERS = "folders"
    private const val KEY_NOTE_FOLDERS = "note_folder_assignments"

    private val gson = Gson()

    fun mainFolder(): NoteFolder = NoteFolder(
        id = MAIN_FOLDER_ID,
        name = "Main",
        createdAt = 0L
    )

    fun loadCustomFolders(context: Context): List<NoteFolder> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val type = object : TypeToken<List<NoteFolder>>() {}.type
        return prefs.getString(KEY_FOLDERS, null)
            ?.let { runCatching { gson.fromJson<List<NoteFolder>>(it, type) }.getOrNull() }
            .orEmpty()
            .filterNot { it.id == MAIN_FOLDER_ID }
            .distinctBy { it.id }
            .sortedBy { it.createdAt }
    }

    fun createFolder(context: Context, rawName: String): NoteFolder? {
        val name = sanitizeFolderName(rawName)
        if (name.isBlank()) return null

        val current = loadCustomFolders(context)
        val existingNames = (current.map { it.name } + mainFolder().name)
            .map { it.lowercase() }
            .toSet()
        val folder = NoteFolder(
            id = UUID.randomUUID().toString(),
            name = uniqueName(name, existingNames),
            createdAt = System.currentTimeMillis()
        )
        saveCustomFolders(context, current + folder)
        return folder
    }

    fun mergeRemoteFolders(context: Context, remoteFolders: List<NoteFolder>) {
        if (remoteFolders.isEmpty()) return
        val current = loadCustomFolders(context)
        val merged = (current + remoteFolders)
            .filterNot { it.id == MAIN_FOLDER_ID }
            .distinctBy { it.id }
            .sortedBy { it.createdAt }
        saveCustomFolders(context, merged)
    }

    fun folderNameForId(context: Context, folderId: String): String {
        if (folderId == MAIN_FOLDER_ID) return mainFolder().name
        return loadCustomFolders(context).firstOrNull { it.id == folderId }?.name ?: "Folder"
    }

    fun folderForNoteKey(context: Context, noteKey: String): String {
        return loadAssignments(context)[noteKey] ?: MAIN_FOLDER_ID
    }

    fun assignNoteToFolder(context: Context, noteKey: String, folderId: String) {
        val validIds = loadCustomFolders(context).map { it.id }.toSet() + MAIN_FOLDER_ID
        val assignments = loadAssignments(context).toMutableMap()
        assignments[noteKey] = if (folderId in validIds) folderId else MAIN_FOLDER_ID
        saveAssignments(context, assignments)
    }

    fun assignRemoteNoteToFolder(context: Context, noteKey: String, folderId: String) {
        val cleanedFolderId = folderId.takeIf { it.isNotBlank() } ?: MAIN_FOLDER_ID
        val validIds = loadCustomFolders(context).map { it.id }.toSet() + MAIN_FOLDER_ID
        val assignments = loadAssignments(context).toMutableMap()
        assignments[noteKey] = if (cleanedFolderId in validIds) cleanedFolderId else MAIN_FOLDER_ID
        saveAssignments(context, assignments)
    }

    fun ensureNoteInMain(context: Context, noteKey: String) {
        val assignments = loadAssignments(context)
        if (noteKey !in assignments) {
            assignNoteToFolder(context, noteKey, MAIN_FOLDER_ID)
        }
    }

    fun removeNote(context: Context, noteKey: String) {
        val assignments = loadAssignments(context).toMutableMap()
        if (assignments.remove(noteKey) != null) {
            saveAssignments(context, assignments)
        }
    }

    fun removeFolders(context: Context, folderIds: Set<String>) {
        val removableIds = folderIds - MAIN_FOLDER_ID
        if (removableIds.isEmpty()) return

        val current = loadCustomFolders(context)
        saveCustomFolders(context, current.filterNot { it.id in removableIds })

        val assignments = loadAssignments(context).filterValues { it !in removableIds }
        saveAssignments(context, assignments)
    }

    fun countByFolder(context: Context, noteKeys: List<String>): Map<String, Int> {
        val assignments = loadAssignments(context)
        return noteKeys
            .groupingBy { key -> assignments[key] ?: MAIN_FOLDER_ID }
            .eachCount()
    }

    private fun saveCustomFolders(context: Context, folders: List<NoteFolder>) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_FOLDERS, gson.toJson(folders.filterNot { it.id == MAIN_FOLDER_ID }.distinctBy { it.id }))
        }
    }

    private fun loadAssignments(context: Context): Map<String, String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val type = object : TypeToken<Map<String, String>>() {}.type
        return prefs.getString(KEY_NOTE_FOLDERS, null)
            ?.let { runCatching { gson.fromJson<Map<String, String>>(it, type) }.getOrNull() }
            .orEmpty()
    }

    private fun saveAssignments(context: Context, assignments: Map<String, String>) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_NOTE_FOLDERS, gson.toJson(assignments))
        }
    }

    private fun uniqueName(name: String, existingNames: Set<String>): String {
        if (name.lowercase() !in existingNames) return name
        var index = 2
        while ("$name $index".lowercase() in existingNames) index++
        return "$name $index"
    }

    private fun sanitizeFolderName(rawName: String): String {
        return rawName
            .replace('\n', ' ')
            .trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .take(3)
            .joinToString(" ") { it.take(24) }
            .take(72)
    }
}
