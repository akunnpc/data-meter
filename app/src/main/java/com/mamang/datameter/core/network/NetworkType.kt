package com.mamang.datameter.core.network

enum class NetworkType {
    WIFI,
    CELLULAR,
    ETHERNET,
    DISCONNECTED
}

data class NetworkConnectionState(
    val type: NetworkType = NetworkType.DISCONNECTED,
    val isMetered: Boolean = false,
    val isConnected: Boolean = false
)
