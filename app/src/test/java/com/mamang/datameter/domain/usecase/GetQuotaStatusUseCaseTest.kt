package com.mamang.datameter.domain.usecase

import com.mamang.datameter.domain.model.AppUsage
import com.mamang.datameter.domain.model.DailyUsagePoint
import com.mamang.datameter.domain.model.NetworkUsage
import com.mamang.datameter.domain.model.QuotaResetPeriod
import com.mamang.datameter.domain.model.QuotaSettings
import com.mamang.datameter.domain.model.UsageSummary
import com.mamang.datameter.domain.repository.NetworkStatsRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GetQuotaStatusUseCaseTest {

    private class FakeNetworkStatsRepository(
        private val mobileRx: Long,
        private val mobileTx: Long
    ) : NetworkStatsRepository {
        override suspend fun getUsageSummary(startTime: Long, endTime: Long): Result<UsageSummary> {
            return Result.success(
                UsageSummary(
                    mobileUsage = NetworkUsage(rxBytes = mobileRx, txBytes = mobileTx),
                    wifiUsage = NetworkUsage.ZERO
                )
            )
        }

        override suspend fun getDailyUsagePoints(startTime: Long, endTime: Long): Result<List<DailyUsagePoint>> {
            return Result.success(emptyList())
        }

        override suspend fun getAppUsageList(startTime: Long, endTime: Long): Result<List<AppUsage>> {
            return Result.success(emptyList())
        }
    }

    @Test
    fun getQuotaStatus_whenDisabled_returnsDisabledStatus() = runBlocking {
        val repo = FakeNetworkStatsRepository(100L, 100L)
        val useCase = GetQuotaStatusUseCase(repo)

        val settings = QuotaSettings(isEnabled = false, limitBytes = 1000L)
        val result = useCase(settings).getOrThrow()

        assertFalse(result.isEnabled)
    }

    @Test
    fun getQuotaStatus_calculatesCorrectPercentageAndRemaining() = runBlocking {
        // 5 GB used out of 10 GB limit
        val fiveGb = 5L * 1024L * 1024L * 1024L
        val tenGb = 10L * 1024L * 1024L * 1024L

        val repo = FakeNetworkStatsRepository(mobileRx = fourGb(), mobileTx = oneGb())
        val useCase = GetQuotaStatusUseCase(repo)

        val settings = QuotaSettings(
            isEnabled = true,
            limitBytes = tenGb,
            resetPeriod = QuotaResetPeriod.MONTHLY,
            resetDay = 1
        )

        val result = useCase(settings).getOrThrow()

        assertTrue(result.isEnabled)
        assertEquals(fiveGb, result.usedBytes)
        assertEquals(fiveGb, result.remainingBytes)
        assertEquals(50.0f, result.percentage, 0.1f)
        assertFalse(result.isExceeded)
    }

    private fun fourGb() = 4L * 1024L * 1024L * 1024L
    private fun oneGb() = 1L * 1024L * 1024L * 1024L
}
