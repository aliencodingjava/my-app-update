package com.flights.studio

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.LifecycleCoroutineScope
import com.bumptech.glide.Glide
import com.bumptech.glide.signature.ObjectKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object AvatarLoader {

    fun loadInto(
        scope: LifecycleCoroutineScope,
        userPrefs: UserPreferencesManager,
        authTokenProvider: () -> String?,   // session.accessToken
        imageView: ImageView,
        initialsView: TextView,
        initialsText: String
    ) {
        val raw = userPrefs.userPhotoUriString?.trim().orEmpty()

        // 1) No photo saved
        if (raw.isBlank() || raw.equals("null", true)) {
            initialsView.text = initialsText
            initialsView.visibility = View.VISIBLE
            imageView.setImageDrawable(null)
            imageView.visibility = View.GONE
            return
        }

        // 2) If it's already a real URL or local uri, load directly
        if (raw.startsWith("http", true) || raw.startsWith("content", true) || raw.startsWith("file", true)) {
            initialsView.visibility = View.GONE
            imageView.visibility = View.VISIBLE

            Glide.with(imageView)
                .load(raw)
                .circleCrop() // ✅ fill circular card
                .into(imageView)

            return
        }

        // 3) STORAGE PATH like "profiles/<uid>/avatar.jpg" -> use cache + sign

        // ✅ If we have cached signed url, show it immediately (NO initials flash)
        SignedUrlCache.getValid(raw)?.let { cached ->
            initialsView.visibility = View.GONE
            imageView.visibility = View.VISIBLE

            Glide.with(imageView)
                .load(cached)
                .signature(ObjectKey(raw)) // stable key by PATH
                .circleCrop()
                .into(imageView)

            return // ✅ IMPORTANT: stop here, do NOT re-sign / reload
        } ?: run {
            // No cache yet: show placeholder in the circle (NOT initials)
            initialsView.visibility = View.GONE
            imageView.visibility = View.VISIBLE

            Glide.with(imageView)
                .load(R.drawable.contact_logo_topbar)
                .circleCrop()
                .into(imageView)
        }

        val token = authTokenProvider()
        if (token.isNullOrBlank()) {
            // Can't sign without session:
            // IMPORTANT: keep placeholder / cached image instead of flashing initials
            // If you want to show initials only when absolutely necessary, do nothing here.
            return
        }

        scope.launch(Dispatchers.IO) {
            val signed = SupabaseStorageUploader.createSignedUrl(
                objectPath = raw,
                authToken = token,
                bucket = "profile-photos"
            )

            withContext(Dispatchers.Main) {
                if (!signed.isNullOrBlank()) {
                    SignedUrlCache.put(raw, signed, 60 * 60)

                    initialsView.visibility = View.GONE
                    imageView.visibility = View.VISIBLE

                    Glide.with(imageView)
                        .load(signed)
                        .signature(ObjectKey(raw))
                        .circleCrop()
                        .into(imageView)
                } else {
                    // signing failed -> now fallback
                    SignedUrlCache.invalidate(raw)

                    initialsView.text = initialsText
                    initialsView.visibility = View.VISIBLE
                    imageView.setImageDrawable(null)
                    imageView.visibility = View.GONE
                }
            }
        }
    }
}
