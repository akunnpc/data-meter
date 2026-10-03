package com.mamang.datameter.presentation.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.mamang.datameter.presentation.apps.AppsScreen
import com.mamang.datameter.presentation.apps.AppsViewModel
import com.mamang.datameter.presentation.dashboard.DashboardScreen
import com.mamang.datameter.presentation.dashboard.DashboardViewModel
import com.mamang.datameter.presentation.quota.QuotaScreen
import com.mamang.datameter.presentation.quota.QuotaViewModel
import com.mamang.datameter.presentation.settings.SettingsScreen
import com.mamang.datameter.presentation.settings.SettingsViewModel

@Composable
fun MainScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar(modifier = Modifier.testTag("bottom_nav_bar")) {
                Screen.bottomNavItems.forEach { screen ->
                    val isSelected = currentRoute == screen.route
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                contentDescription = stringResource(screen.titleRes)
                            )
                        },
                        label = { Text(stringResource(screen.titleRes)) },
                        modifier = Modifier.testTag("nav_item_${screen.route}")
                    )
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                val viewModel: DashboardViewModel = viewModel()
                DashboardScreen(viewModel = viewModel)
            }

            composable(Screen.Applications.route) {
                val viewModel: AppsViewModel = viewModel()
                AppsScreen(viewModel = viewModel)
            }

            composable(Screen.Quota.route) {
                val viewModel: QuotaViewModel = viewModel()
                QuotaScreen(viewModel = viewModel)
            }

            composable(Screen.Settings.route) {
                val viewModel: SettingsViewModel = viewModel()
                SettingsScreen(viewModel = viewModel)
            }
        }
    }
}
