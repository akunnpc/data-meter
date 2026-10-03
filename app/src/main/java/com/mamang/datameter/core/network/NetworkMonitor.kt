package com.mamang.datameter.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

interface NetworkMonitor {
    val connectionState: Flow<NetworkConnectionState>
    fun getCurrentConnectionState(): NetworkConnectionState
}

class NetworkMonitorImpl(private val context: Context) : NetworkMonitor {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    override val connectionState: Flow<NetworkConnectionState> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(getCurrentConnectionState())
            }

            override fun onLost(network: Network) {
                trySend(getCurrentConnectionState())
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                trySend(getCurrentConnectionState())
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        try {
            connectivityManager?.registerNetworkCallback(request, callback)
        } catch (_: Exception) {
            // Some environments may restrict network callback registration
        }

        // Emit initial state
        trySend(getCurrentConnectionState())

        awaitClose {
            try {
                connectivityManager?.unregisterNetworkCallback(callback)
            } catch (_: Exception) {
            }
        }
    }.distinctUntilChanged()

    override fun getCurrentConnectionState(): NetworkConnectionState {
        val cm = connectivityManager ?: return NetworkConnectionState()
        val activeNetwork = cm.activeNetwork ?: return NetworkConnectionState()
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return NetworkConnectionState()

        val isMetered = !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

        val type = when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
            else -> NetworkType.DISCONNECTED
        }

        return NetworkConnectionState(
            type = if (hasInternet) type else NetworkType.DISCONNECTED,
            isMetered = isMetered,
            isConnected = hasInternet && type != NetworkType.DISCONNECTED
        )
    }
}
