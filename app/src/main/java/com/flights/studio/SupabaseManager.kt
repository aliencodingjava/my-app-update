package com.flights.studio


import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseManager {

    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = "https://gdvhiudodnqdqhkyghsk.supabase.co",
            supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImdkdmhpdWRvZG5xZHFoa3lnaHNrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDkwMTI2MTksImV4cCI6MjA2NDU4ODYxOX0.p9P-Hv4r-C9eZU3Dz-kX9U2iv8PUAKaUU5qZGPMJ844",
        ) {
            install(Auth)      // for auth.currentSessionOrNull()
            install(Postgrest) // for postgrest queries
            install(Storage)

        }
    }
}