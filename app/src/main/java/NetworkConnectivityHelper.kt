package com.flights.studio

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first

/**
 * Emits `true` whenever the system reports a VALIDATED Internet connection,
 * and `false` when it’s lost.
 *
 * Also provides a convenience suspending function for a single connectivity check.
 */
class NetworkConnectivityHelper(context: Context) {
    private val cm = context
        .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    // Only match networks Android has both INTERNET + VALIDATED capabilities
    private val validatedRequest = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        .build()

    /**
     * A Flow that immediately emits the current connectivity state and
     * updates whenever it changes.
     */
    fun isOnline(): Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }
            override fun onLost(network: Network) {
                trySend(false)
            }
        }

        // Register and send initial state
        cm.registerNetworkCallback(validatedRequest, callback)
        val caps = cm.getNetworkCapabilities(cm.activeNetwork)
        trySend(caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true)

        // Clean up when nobody's collecting
        awaitClose { cm.unregisterNetworkCallback(callback) }
    }

    /**
     * Convenience: suspend until the first value of isOnline() is available.
     */
    suspend fun isInternetAvailableFast(): Boolean =
        isOnline().first()
}
