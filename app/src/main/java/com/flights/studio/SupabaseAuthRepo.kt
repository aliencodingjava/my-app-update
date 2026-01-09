package com.flights.studio

import android.util.Log
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc

object SupabaseAuthRepo {

    suspend fun signIn(email: String, password: String): Result<Unit> =
        runCatching {
            SupabaseManager.client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }

            Log.d("AUTH", "SignIn session = ${SupabaseManager.client.auth.currentSessionOrNull()}")
        }

    /**
     * ✅ Use this in your signup screen
     *
     * Rules enforced:
     * - If phone already used -> BLOCK signup (even with different email)
     * - If email already registered -> Supabase Auth blocks signup
     *
     * Requires SQL function:
     *   public.reserve_phone(p_phone text)
     */
    suspend fun signUpWithProfileCheck(
        email: String,
        password: String,
        phone: String
    ): Result<Unit> = runCatching {

        // ✅ IMPORTANT: DO NOT use digits-only unless your DB also stores digits-only.
        // Better: store E.164 (ex: +14035551234). But keeping your style for now:
        val cleanPhone = phone.filter { it.isDigit() }
        if (cleanPhone.isBlank()) throw IllegalArgumentException("INVALID_PHONE")

        // ✅ Local weak password check FIRST (so we don't reserve phone then fail)
        if (password.length < 6) throw IllegalArgumentException("WEAK_PASSWORD")

        fun isConflictMessage(m: String): Boolean {
            val msg = m.lowercase()
            return msg.contains("23505") ||
                    msg.contains("duplicate key") ||
                    msg.contains("already registered") ||
                    msg.contains("user already registered") ||
                    msg.contains("email already") ||
                    msg.contains("already exists") ||
                    msg.contains("phone_exists") ||
                    msg.contains("signup_conflict")
        }

        var reserved = false

        try {
            // 1) Reserve phone
            SupabaseManager.client.postgrest.rpc(
                "reserve_phone",
                mapOf("p_phone" to cleanPhone)
            )
            reserved = true

            // 2) Auth signup
            SupabaseManager.client.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }

            Log.d("AUTH", "SignUp user = ${SupabaseManager.client.auth.currentUserOrNull()}")
            Log.d("AUTH", "SignUp session = ${SupabaseManager.client.auth.currentSessionOrNull()}")

        } catch (e: Exception) {

            val m = e.message.orEmpty()

            // ✅ If we reserved phone, but signup failed, rollback reservation
            if (reserved) {
                runCatching {
                    SupabaseManager.client.postgrest.rpc(
                        "release_phone",
                        mapOf("p_phone" to cleanPhone)
                    )
                }
            }

            // ✅ Normalize errors
            val msg = m.lowercase()
            val isWeak =
                msg.contains("weak_password") ||
                        msg.contains("weak password") ||
                        (msg.contains("password") && msg.contains("weak")) ||
                        (msg.contains("password") && msg.contains("at least") && msg.contains("character"))

            if (isWeak) throw IllegalArgumentException("WEAK_PASSWORD")
            if (isConflictMessage(m)) throw IllegalStateException("SIGNUP_CONFLICT")

            throw IllegalStateException("SIGNUP_FAILED")
        }
    }



    suspend fun resetPassword(email: String): Result<Unit> =
        runCatching {
            SupabaseManager.client.auth.resetPasswordForEmail(email)
        }

    fun hasSession(): Boolean =
        SupabaseManager.client.auth.currentSessionOrNull() != null
}
