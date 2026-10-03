package com.mamang.datameter.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.mamang.datameter.DataMeterApplication
import java.util.concurrent.TimeUnit

class QuotaCheckWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val appContainer = (context.applicationContext as? DataMeterApplication)?.appContainer
                ?: return Result.success()

            appContainer.checkQuotaAlertsUseCase { threshold, usedBytes, limitBytes ->
                NotificationHelper.showQuotaAlert(context, threshold, usedBytes, limitBytes)
            }
            appContainer.recordNetworkActivitySnapshotUseCase()
            appContainer.performActivityRetentionCleanupUseCase()
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "quota_check_worker"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<QuotaCheckWorker>(1, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
