package com.mamang.datameter.data.local.room

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "network_activity",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["packageName"]),
        Index(value = ["networkType"]),
        Index(value = ["uid"])
    ]
)
data class NetworkActivityEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,            // Epoch millis of the detected usage
    val uid: Int,
    val packageName: String,
    val appName: String,
    val networkType: String,         // "WIFI" or "CELLULAR"
    val rxBytes: Long,
    val txBytes: Long,
    val totalBytes: Long,
    val aggregationLevel: Int = 0    // 0 = raw minute, 1 = hourly rollup, 2 = daily rollup
)
