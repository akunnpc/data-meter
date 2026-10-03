package com.mamang.datameter.data.local.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quota_alerts")
data class QuotaAlertEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cycleStart: Long,
    val thresholdPercent: Int,
    val triggeredAt: Long,
    val usedBytes: Long,
    val limitBytes: Long
)
