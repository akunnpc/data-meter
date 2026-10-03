package com.mamang.datameter.di

import android.content.Context
import com.mamang.datameter.core.network.NetworkMonitor
import com.mamang.datameter.core.network.NetworkMonitorImpl
import com.mamang.datameter.data.local.datastore.PreferencesManager
import com.mamang.datameter.data.local.room.DataMeterDatabase
import com.mamang.datameter.data.networkstats.NetworkStatsDataSource
import com.mamang.datameter.data.networkstats.NetworkStatsDataSourceImpl
import com.mamang.datameter.data.repository.NetworkStatsRepositoryImpl
import com.mamang.datameter.data.repository.PreferencesRepositoryImpl
import com.mamang.datameter.domain.repository.NetworkStatsRepository
import com.mamang.datameter.domain.repository.PreferencesRepository
import com.mamang.datameter.domain.usecase.CheckQuotaAlertsUseCase
import com.mamang.datameter.domain.usecase.GetAppUsageListUseCase
import com.mamang.datameter.domain.usecase.GetNetworkUsageUseCase
import com.mamang.datameter.domain.usecase.GetQuotaStatusUseCase
import com.mamang.datameter.domain.usecase.GetUsageChartDataUseCase

interface AppContainer {
    val networkMonitor: NetworkMonitor
    val networkStatsRepository: NetworkStatsRepository
    val preferencesRepository: PreferencesRepository
    val getNetworkUsageUseCase: GetNetworkUsageUseCase
    val getAppUsageListUseCase: GetAppUsageListUseCase
    val getUsageChartDataUseCase: GetUsageChartDataUseCase
    val getQuotaStatusUseCase: GetQuotaStatusUseCase
    val checkQuotaAlertsUseCase: CheckQuotaAlertsUseCase
}

class AppContainerImpl(private val context: Context) : AppContainer {

    override val networkMonitor: NetworkMonitor by lazy {
        NetworkMonitorImpl(context)
    }

    private val database: DataMeterDatabase by lazy {
        DataMeterDatabase.getInstance(context)
    }

    private val preferencesManager: PreferencesManager by lazy {
        PreferencesManager(context)
    }

    private val networkStatsDataSource: NetworkStatsDataSource by lazy {
        NetworkStatsDataSourceImpl(context)
    }

    override val networkStatsRepository: NetworkStatsRepository by lazy {
        NetworkStatsRepositoryImpl(networkStatsDataSource)
    }

    override val preferencesRepository: PreferencesRepository by lazy {
        PreferencesRepositoryImpl(preferencesManager, database.quotaAlertDao())
    }

    override val getNetworkUsageUseCase: GetNetworkUsageUseCase by lazy {
        GetNetworkUsageUseCase(networkStatsRepository)
    }

    override val getAppUsageListUseCase: GetAppUsageListUseCase by lazy {
        GetAppUsageListUseCase(networkStatsRepository)
    }

    override val getUsageChartDataUseCase: GetUsageChartDataUseCase by lazy {
        GetUsageChartDataUseCase(networkStatsRepository)
    }

    override val getQuotaStatusUseCase: GetQuotaStatusUseCase by lazy {
        GetQuotaStatusUseCase(networkStatsRepository)
    }

    override val checkQuotaAlertsUseCase: CheckQuotaAlertsUseCase by lazy {
        CheckQuotaAlertsUseCase(getQuotaStatusUseCase, preferencesRepository)
    }
}
