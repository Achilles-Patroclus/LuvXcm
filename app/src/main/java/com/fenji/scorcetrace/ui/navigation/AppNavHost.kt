package com.fenji.scorcetrace.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fenji.scorcetrace.R
import com.fenji.scorcetrace.ui.screen.home.HomeScreen
import com.fenji.scorcetrace.ui.screen.plan.PlanScreen
import com.fenji.scorcetrace.ui.screen.score.ScoreScreen
import com.fenji.scorcetrace.ui.screen.settings.SettingsScreen

private data class BottomNavItem(
    val screen: Screen,
    val labelRes: Int,
    val icon: ImageVector,
)

private val bottomNavItems = listOf(
    BottomNavItem(Screen.Home, R.string.tab_home, Icons.Filled.Home),
    BottomNavItem(Screen.Plan, R.string.tab_plan, Icons.Filled.DateRange),
    BottomNavItem(Screen.Score, R.string.tab_score, Icons.Filled.Star),
    BottomNavItem(Screen.Settings, R.string.tab_settings, Icons.Filled.Settings),
)

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { item ->
                    val label = stringResource(item.labelRes)
                    NavigationBarItem(
                        selected = currentRoute == item.screen.route,
                        onClick = { navController.navigateToTab(item.screen) },
                        icon = { Icon(item.icon, contentDescription = label) },
                        label = { Text(label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onOpenPlan = { navController.navigateToTab(Screen.Plan) },
                    onOpenScore = { navController.navigateToTab(Screen.Score) },
                )
            }
            composable(Screen.Plan.route) { PlanScreen() }
            composable(Screen.Score.route) { ScoreScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}

/** 底部 Tab 统一的切换逻辑：单栈、保留各 Tab 的滚动位置。 */
private fun NavHostController.navigateToTab(screen: Screen) {
    navigate(screen.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
