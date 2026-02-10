package com.flights.studio


import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

fun copyPhotoToClipboard(context: Context, model: String) {
    if (model.isBlank()) return

    CoroutineScope(Dispatchers.Main).launch {
        val uriForOthers = withContext(Dispatchers.IO) {
            exportImageToMediaStore(context, model)
        } ?: return@launch

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newUri(context.contentResolver, "Photo", uriForOthers)
        clipboard.setPrimaryClip(clip)
        // opțional: toast / haptic / snack
    }
}

/**
 * copy imaginea din:
 * - content://
 * - file://
 * - path local (/data/user/0/..)
 * - https:// (prin download în bytes -> MediaStore)
 *
 * în MediaStore => return content:// care poate fi lipit în alte apps.
 */
private fun exportImageToMediaStore(context: Context, model: String): Uri? {
    val cr = context.contentResolver

    val (mime, displayName) = guessMimeAndName(model)

    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
        put(MediaStore.Images.Media.MIME_TYPE, mime)
        if (Build.VERSION.SDK_INT >= 29) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Flights Studio")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }

    val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    val outUri = cr.insert(collection, values) ?: return null

    try {
        cr.openOutputStream(outUri)?.use { out ->
            openModelInputStream(context, model)?.use { input ->
                input.copyTo(out)
            } ?: run {
                // dacă nu avem stream (ex: http fără implementare), ștergem entry-ul
                cr.delete(outUri, null, null)
                return null
            }
        }

        if (Build.VERSION.SDK_INT >= 29) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            cr.update(outUri, values, null, null)
        }

        return outUri
    } catch (_: Throwable) {
        runCatching { cr.delete(outUri, null, null) }
        return null
    }
}

private fun openModelInputStream(context: Context, model: String) = runCatching {
    val cr = context.contentResolver
    when {
        model.startsWith("content://") -> cr.openInputStream(model.toUri())
        model.startsWith("file://") -> File(model.toUri().path ?: return@runCatching null).inputStream()
        model.startsWith("/") -> File(model).inputStream()


        model.startsWith("http://") || model.startsWith("https://") -> {

            null
        }

        else -> null
    }
}.getOrNull()

private fun guessMimeAndName(model: String): Pair<String, String> {
    val nameGuess = runCatching {
        val u = model.toUri()
        u.lastPathSegment ?: model.substringAfterLast('/').ifBlank { "photo" }
    }.getOrDefault("photo")

    val lower = nameGuess.lowercase(Locale.US)
    val mime = when {
        lower.endsWith(".png") -> "image/png"
        lower.endsWith(".webp") -> "image/webp"
        lower.endsWith(".gif") -> "image/gif"
        lower.endsWith(".jpg") || lower.endsWith(".jpeg") -> "image/jpeg"
        else -> "image/jpeg"
    }


    val finalName = if (nameGuess.contains('.')) nameGuess else "$nameGuess.jpg"
    return mime to finalName
}


