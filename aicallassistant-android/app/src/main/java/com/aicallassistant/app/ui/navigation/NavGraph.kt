package com.aicallassistant.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aicallassistant.app.data.remote.SupabaseManager
import com.aicallassistant.app.ui.screens.actionitems.ActionItemsScreen
import com.aicallassistant.app.ui.screens.auth.ForgotPasswordScreen
import com.aicallassistant.app.ui.screens.auth.LoginScreen
import com.aicallassistant.app.ui.screens.auth.SignUpScreen
import com.aicallassistant.app.ui.screens.auth.WelcomeScreen
import com.aicallassistant.app.ui.screens.detail.CallDetailScreen
import com.aicallassistant.app.ui.screens.history.CallHistoryScreen
import com.aicallassistant.app.ui.screens.home.HomeScreen
import com.aicallassistant.app.ui.screens.settings.SettingsScreen
import com.aicallassistant.app.ui.screens.simulate.SimulateCallScreen
import com.aicallassistant.app.ui.viewmodel.ActionItemsViewModel
import com.aicallassistant.app.ui.viewmodel.AuthViewModel
import com.aicallassistant.app.ui.screens.splash.SplashScreen
import com.aicallassistant.app.ui.viewmodel.CallDetailViewModel
import com.aicallassistant.app.ui.viewmodel.CallHistoryViewModel
import com.aicallassistant.app.ui.viewmodel.HomeViewModel
import com.aicallassistant.app.ui.viewmodel.SettingsViewModel
import com.aicallassistant.app.ui.viewmodel.SimulateCallViewModel

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Welcome : Screen("welcome")
    data object Login : Screen("login")
    data object SignUp : Screen("signup")
    data object ForgotPassword : Screen("forgot_password")
    data object Main : Screen("main")
    data object CallDetail : Screen("call_detail/{callId}") {
        fun createRoute(callId: String) = "call_detail/$callId"
    }
    data object SimulateCall : Screen("simulate_call")
}

sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    data object Home : BottomNavItem("home", "Home", Icons.Default.Home)
    data object History : BottomNavItem("history", "History", Icons.Default.History)
    data object ActionItems : BottomNavItem("action_items", "Tasks", Icons.Default.CheckCircle)
    data object Settings : BottomNavItem("settings", "Settings", Icons.Default.Settings)
}

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToMain = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToWelcome = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onNavigateToLogin = { navController.navigate(Screen.Login.route) },
                onNavigateToSignUp = { navController.navigate(Screen.SignUp.route) }
            )
        }

        composable(Screen.Login.route) {
            val authViewModel: AuthViewModel = viewModel()
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToSignUp = { navController.navigate(Screen.SignUp.route) },
                onNavigateToForgotPassword = { navController.navigate(Screen.ForgotPassword.route) },
                onAuthenticated = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.SignUp.route) {
            val authViewModel: AuthViewModel = viewModel()
            SignUpScreen(
                viewModel = authViewModel,
                onNavigateToLogin = { navController.navigate(Screen.Login.route) },
                onAuthenticated = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.ForgotPassword.route) {
            val authViewModel: AuthViewModel = viewModel()
            ForgotPasswordScreen(
                viewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Main.route) {
            MainContainerScreen(
                onNavigateToSimulateCall = { navController.navigate(Screen.SimulateCall.route) },
                onNavigateToCallDetail = { callId -> navController.navigate(Screen.CallDetail.createRoute(callId)) },
                onSignedOut = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.CallDetail.route,
            arguments = listOf(navArgument("callId") { type = NavType.StringType })
        ) { backStackEntry ->
            val callId = backStackEntry.arguments?.getString("callId") ?: ""
            val detailViewModel: CallDetailViewModel = viewModel(
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        return CallDetailViewModel(callId = callId) as T
                    }
                }
            )
            CallDetailScreen(
                viewModel = detailViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.SimulateCall.route) {
            val simulateViewModel: SimulateCallViewModel = viewModel()
            SimulateCallScreen(
                viewModel = simulateViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCallDetail = { callId ->
                    navController.navigate(Screen.CallDetail.createRoute(callId)) {
                        popUpTo(Screen.SimulateCall.route) { inclusive = true }
                    }
                }
            )
        }
    }
}

@Composable
fun MainContainerScreen(
    onNavigateToSimulateCall: () -> Unit,
    onNavigateToCallDetail: (String) -> Unit,
    onSignedOut: () -> Unit
) {
    val bottomNavController = rememberNavController()
    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.History,
        BottomNavItem.ActionItems,
        BottomNavItem.Settings
    )

    Scaffold(
        containerColor = com.aicallassistant.app.ui.theme.CallMateNavyBg,
        bottomBar = {
            NavigationBar(
                containerColor = com.aicallassistant.app.ui.theme.CallMateNavySurface,
                contentColor = androidx.compose.ui.graphics.Color.White
            ) {
                items.forEach { item ->
                    val isSelected = currentRoute == item.route
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title, fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal) },
                        selected = isSelected,
                        colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                            selectedIconColor = com.aicallassistant.app.ui.theme.CallMateCyan,
                            selectedTextColor = com.aicallassistant.app.ui.theme.CallMateCyan,
                            indicatorColor = com.aicallassistant.app.ui.theme.CallMateCyan.copy(alpha = 0.18f),
                            unselectedIconColor = androidx.compose.ui.graphics.Color(0xFF64748B),
                            unselectedTextColor = androidx.compose.ui.graphics.Color(0xFF64748B)
                        ),
                        onClick = {
                            bottomNavController.navigate(item.route) {
                                popUpTo(bottomNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = bottomNavController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Home.route) {
                val homeViewModel: HomeViewModel = viewModel()
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToSimulateCall = onNavigateToSimulateCall,
                    onNavigateToCallDetail = onNavigateToCallDetail,
                    onNavigateToHistory = {
                        bottomNavController.navigate(BottomNavItem.History.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(BottomNavItem.History.route) {
                val historyViewModel: CallHistoryViewModel = viewModel()
                CallHistoryScreen(
                    viewModel = historyViewModel,
                    onNavigateToCallDetail = onNavigateToCallDetail
                )
            }

            composable(BottomNavItem.ActionItems.route) {
                val actionItemsViewModel: ActionItemsViewModel = viewModel()
                ActionItemsScreen(viewModel = actionItemsViewModel)
            }

            composable(BottomNavItem.Settings.route) {
                val settingsViewModel: SettingsViewModel = viewModel()
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onSignedOut = onSignedOut
                )
            }
        }
    }
}
