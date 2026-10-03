package com.mamang.datameter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.rememberNavController
import com.mamang.datameter.core.ui.theme.DataMeterTheme
import com.mamang.datameter.domain.model.AppTheme
import com.mamang.datameter.presentation.navigation.MainScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as DataMeterApplication).appContainer
        val preferencesRepository = appContainer.preferencesRepository

        setContent {
            val userSettings by preferencesRepository.userSettingsFlow.collectAsState(initial = null)
            val currentTheme = userSettings?.theme ?: AppTheme.SYSTEM

            DataMeterTheme(appTheme = currentTheme) {
                val navController = rememberNavController()
                MainScreen(navController = navController)
            }
        }
    }
}
