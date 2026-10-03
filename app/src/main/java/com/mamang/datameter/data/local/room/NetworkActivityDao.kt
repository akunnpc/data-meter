package com.mamang.datameter.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NetworkActivityDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: NetworkActivityEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivities(activities: List<NetworkActivityEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSnapshots(snapshots: List<AppSnapshotEntity>)

    @Query("SELECT * FROM app_snapshot")
    suspend fun getAllSnapshots(): List<AppSnapshotEntity>

    @Query("SELECT * FROM network_activity ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentActivities(limit: Int): Flow<List<NetworkActivityEntity>>

    @Query("""
        SELECT * FROM network_activity 
        WHERE timestamp >= :startTime AND timestamp <= :endTime
        AND (:networkType IS NULL OR networkType = :networkType)
        AND (:searchQuery = '' OR appName LIKE '%' || :searchQuery || '%' OR packageName LIKE '%' || :searchQuery || '%')
        ORDER BY 
            CASE WHEN :isAsc = 1 THEN timestamp END ASC,
            CASE WHEN :isAsc = 0 THEN timestamp END DESC
    """)
    fun getActivitiesFiltered(
        startTime: Long,
        endTime: Long,
        networkType: String?,
        searchQuery: String,
        isAsc: Boolean
    ): Flow<List<NetworkActivityEntity>>

    @Query("SELECT * FROM network_activity WHERE timestamp < :cutoffTime AND aggregationLevel = 0")
    suspend fun getRawActivitiesOlderThan(cutoffTime: Long): List<NetworkActivityEntity>

    @Query("SELECT * FROM network_activity WHERE timestamp < :cutoffTime AND aggregationLevel = 1")
    suspend fun getHourlyActivitiesOlderThan(cutoffTime: Long): List<NetworkActivityEntity>

    @Query("DELETE FROM network_activity WHERE id IN (:ids)")
    suspend fun deleteActivitiesByIds(ids: List<Long>)

    @Query("DELETE FROM network_activity WHERE timestamp < :cutoffTime")
    suspend fun deleteActivitiesOlderThan(cutoffTime: Long)

    @Query("DELETE FROM network_activity")
    suspend fun clearAllActivities()

    @Query("DELETE FROM app_snapshot")
    suspend fun clearAllSnapshots()
}
