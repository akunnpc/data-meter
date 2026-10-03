package com.mamang.datameter.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface QuotaAlertDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: QuotaAlertEntity): Long

    @Query("SELECT * FROM quota_alerts WHERE cycleStart = :cycleStart AND thresholdPercent = :threshold LIMIT 1")
    suspend fun getAlert(cycleStart: Long, threshold: Int): QuotaAlertEntity?

    @Query("SELECT * FROM quota_alerts ORDER BY triggeredAt DESC")
    suspend fun getAllAlerts(): List<QuotaAlertEntity>

    @Query("DELETE FROM quota_alerts")
    suspend fun clearAll()
}
