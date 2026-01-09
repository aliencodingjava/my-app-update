package com.flights.studio

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfileUpsert(
    val id: String,
    val email: String? = null,
    @SerialName("full_name") val fullName: String? = null,
    val phone: String? = null,
    val bio: String? = null,
    val birthday: String? = null
)

object SupabaseProfilesRepo {

    suspend fun upsertMyProfile(
        fullName: String,
        phone: String,
        email: String,
        bio: String? = null,
        birthday: String? = null
    ) {
        val session = SupabaseManager.client.auth.currentSessionOrNull()
            ?: throw IllegalStateException("No session")
        val uid = session.user?.id ?: throw IllegalStateException("No user id")

        SupabaseManager.client.postgrest["user_profiles"].upsert(
            UserProfileUpsert(
                id = uid,
                email = email,
                fullName = fullName,
                phone = phone,
                bio = bio?.trim()?.ifBlank { null },
                birthday = birthday?.trim()?.ifBlank { null }
            )
        )
    }

    suspend fun isPhoneAvailable(phone: String): Boolean {
        val normalized = normalizePhone(phone)

        // empty phone -> treat as not allowed
        if (normalized.isBlank()) return false

        // ✅ call your SQL function: public.is_phone_available(p_phone text) returns boolean
        return SupabaseManager.client
            .postgrest
            .rpc(
                function = "is_phone_available",
                parameters = mapOf("p_phone" to normalized)
            )
            .decodeAs<Boolean>()
    }

    fun normalizePhone(raw: String): String =
        raw.filter { it.isDigit() }


    // ✅ Minimal payload: only the fields you want to touch (plus id)
    @Serializable
    private data class BioBirthdayUpsert(
        val id: String,
        val bio: String? = null,
        val birthday: String? = null
    )

    suspend fun updateMyBioBirthday(
        bio: String?,
        birthday: String?
    ) {
        val session = SupabaseManager.client.auth.currentSessionOrNull()
            ?: throw IllegalStateException("No session")
        val uid = session.user?.id ?: throw IllegalStateException("No user id")

        val bioClean = bio?.trim()?.ifBlank { null }
        val birthdayClean = birthday?.trim()?.ifBlank { null }

        // if both empty -> don’t spam DB
        if (bioClean == null && birthdayClean == null) return

        // ✅ Use UPSERT so it works even if the row was never created on signup
        SupabaseManager.client.postgrest["user_profiles"].upsert(
            BioBirthdayUpsert(
                id = uid,
                bio = bioClean,
                birthday = birthdayClean
            )
        )
    }
}
