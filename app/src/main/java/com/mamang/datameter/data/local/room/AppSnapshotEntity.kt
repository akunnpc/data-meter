package com.mamang.datameter.data.local.room

import androidx.room.Entity

@Entity(
    tableName = "app_snapshot",
    primaryKeys = ["uid", "networkType"]
)
data class AppSnapshotEntity(
    val uid: Int,
    val networkType: String,         // "WIFI" or "CELLULAR"
    val lastRxBytes: Long,
    val lastTxBytes: Long,
    val lastSnapshotTime: Long
)
