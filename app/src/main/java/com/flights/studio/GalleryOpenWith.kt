package com.flights.studio

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import java.io.File

/**
 * Converts "file://" (internal app files) into FileProvider "content://".
 * Leaves "content://" as-is. Handles http(s) separately.
 */
private fun toExternalUri(context: Context, model: String): Uri? {
    if (model.isBlank()) return null

    // Online links -> normal VIEW
    if (model.startsWith("http://") || model.startsWith("https://")) {
        return model.toUri()
    }

    val uri = runCatching { model.toUri() }.getOrNull() ?: return null

    return when (uri.scheme) {
        "content" -> uri

        "file" -> {
            val path = uri.path ?: return null
            val file = File(path)
            if (!file.exists()) return null

            // IMPORTANT: authority must match your manifest provider authority.
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        }

        else -> null
    }
}

/**
 * "Open with…" (chooser) for photo.
 * - file:// is converted to content:// via FileProvider
 * - adds GRANT_READ_URI_PERMISSION + ClipData (important for some OEMs)
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun openWith(context: Context, model: String) {
    val uri = toExternalUri(context, model) ?: return

    // If it's a web link, just open normally
    if (model.startsWith("http://") || model.startsWith("https://")) {
        val intent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(Intent.createChooser(intent, "Open with"))
        return
    }

    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "image/*")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        // ✅ This helps the permission actually propagate on some devices
        clipData = ClipData.newUri(context.contentResolver, "photo", uri)
    }

    // Extra safety: grant URI permission to all possible targets
    val resInfoList = context.packageManager.queryIntentActivities(
        viewIntent,
        PackageManager.ResolveInfoFlags.of(0)
    )
    for (ri in resInfoList) {
        val pkg = ri.activityInfo.packageName
        context.grantUriPermission(pkg, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    context.startActivity(Intent.createChooser(viewIntent, "Open with"))
}
