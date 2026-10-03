package com.mamang.datameter.domain.model

enum class NetworkActivityType {
    WIFI,
    CELLULAR;

    companion object {
        fun fromString(value: String): NetworkActivityType {
            return if (value.equals("WIFI", ignoreCase = true)) WIFI else CELLULAR
        }
    }
}

data class NetworkActivity(
    val id: Long = 0,
    val timestamp: Long,
    val uid: Int,
    val packageName: String,
    val appName: String,
    val networkType: NetworkActivityType,
    val rxBytes: Long,
    val txBytes: Long,
    val totalBytes: Long,
    val formattedTime: String = "",
    val formattedDate: String = ""
)
