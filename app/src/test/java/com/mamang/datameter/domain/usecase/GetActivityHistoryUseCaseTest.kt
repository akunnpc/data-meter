package com.mamang.datameter.domain.usecase

import com.mamang.datameter.core.utils.PeriodType
import com.mamang.datameter.domain.model.NetworkActivity
import com.mamang.datameter.domain.model.NetworkActivityType
import com.mamang.datameter.domain.repository.ActivityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class GetActivityHistoryUseCaseTest {

    private class FakeActivityRepository : ActivityRepository {
        val activities = listOf(
            NetworkActivity(
                id = 1,
                timestamp = System.currentTimeMillis() - 1000,
                uid = 10158,
                packageName = "com.zhiliaoapp.musically",
                appName = "TikTok",
                networkType = NetworkActivityType.WIFI,
                rxBytes = 18_400_000L,
                txBytes = 1_200_000L,
                totalBytes = 19_600_000L,
                formattedTime = "14:32:18",
                formattedDate = "03 Okt 2026"
            ),
            NetworkActivity(
                id = 2,
                timestamp = System.currentTimeMillis() - 500,
                uid = 10257,
                packageName = "com.google.android.youtube",
                appName = "YouTube",
                networkType = NetworkActivityType.WIFI,
                rxBytes = 86_200_000L,
                txBytes = 2_100_000L,
                totalBytes = 88_300_000L,
                formattedTime = "14:41:52",
                formattedDate = "03 Okt 2026"
            ),
            NetworkActivity(
                id = 3,
                timestamp = System.currentTimeMillis() - 100,
                uid = 10354,
                packageName = "com.android.chrome",
                appName = "Chrome",
                networkType = NetworkActivityType.CELLULAR,
                rxBytes = 7_200_000L,
                txBytes = 400_000L,
                totalBytes = 7_600_000L,
                formattedTime = "15:02:44",
                formattedDate = "03 Okt 2026"
            )
        )

        override fun getRecentActivities(limit: Int): Flow<List<NetworkActivity>> {
            return flowOf(activities.take(limit))
        }

        override fun getActivities(
            startTime: Long,
            endTime: Long,
            networkType: NetworkActivityType?,
            searchQuery: String,
            isAscending: Boolean
        ): Flow<List<NetworkActivity>> {
            var filtered = activities.filter { it.timestamp in startTime..endTime }
            if (networkType != null) {
                filtered = filtered.filter { it.networkType == networkType }
            }
            if (searchQuery.isNotBlank()) {
                filtered = filtered.filter {
                    it.appName.contains(searchQuery, ignoreCase = true) ||
                            it.packageName.contains(searchQuery, ignoreCase = true)
                }
            }
            if (isAscending) {
                filtered = filtered.sortedBy { it.timestamp }
            } else {
                filtered = filtered.sortedByDescending { it.timestamp }
            }
            return flowOf(filtered)
        }

        override suspend fun recordSnapshot(): Int = 0
        override suspend fun runRetentionCleanup() {}
        override suspend fun clearAllHistory() {}
    }

    @Test
    fun getActivities_filtersBySearchQuery() = runBlocking {
        val repo = FakeActivityRepository()
        val useCase = GetActivityHistoryUseCase(repo)

        val results = useCase(
            periodType = PeriodType.TODAY,
            searchQuery = "TikTok"
        ).first()

        assertEquals(1, results.size)
        assertEquals("TikTok", results[0].appName)
    }

    @Test
    fun getActivities_filtersByNetworkType() = runBlocking {
        val repo = FakeActivityRepository()
        val useCase = GetActivityHistoryUseCase(repo)

        val results = useCase(
            periodType = PeriodType.TODAY,
            networkType = NetworkActivityType.CELLULAR
        ).first()

        assertEquals(1, results.size)
        assertEquals("Chrome", results[0].appName)
    }

    @Test
    fun getRecentActivities_returnsExpectedLimit() = runBlocking {
        val repo = FakeActivityRepository()
        val useCase = GetRecentActivitiesUseCase(repo)

        val results = useCase(limit = 2).first()

        assertEquals(2, results.size)
    }
}
