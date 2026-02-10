package com.flights.studio

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Copies a picked content Uri into app-private storage.
 * Returns the saved File, or null if failed.
 */
fun importImageIntoAppStorage(
    context: Context,
    sourceUri: Uri,
    subDir: String = "notes_photos"
): File? = runCatching {
    val cr = context.contentResolver

    val dir = File(context.filesDir, subDir).apply { mkdirs() }

    // Try to keep original display name, fallback to timestamp.
    val displayName = cr.query(sourceUri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { c ->
            val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (c.moveToFirst() && idx >= 0) c.getString(idx) else null
        }

    val safeName = (displayName?.takeIf { it.isNotBlank() } ?: run {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        "IMG_$stamp.jpg"
    }).replace(Regex("""[^\w\-.]+"""), "_")

    // Avoid overwrite
    val outFile = uniqueFile(dir, safeName)

    cr.openInputStream(sourceUri)?.use { input ->
        FileOutputStream(outFile).use { output ->
            input.copyTo(output, bufferSize = 256 * 1024)
            output.fd.sync()
        }
    } ?: return null

    outFile
}.getOrNull()

private fun uniqueFile(dir: File, fileName: String): File {
    val base = fileName.substringBeforeLast('.', fileName)
    val ext = fileName.substringAfterLast('.', missingDelimiterValue = "")
    var i = 0
    while (true) {
        val name = if (i == 0) fileName else {
            if (ext.isBlank()) "${base}_$i" else "${base}_$i.$ext"
        }
        val f = File(dir, name)
        if (!f.exists()) return f
        i++
    }
}


