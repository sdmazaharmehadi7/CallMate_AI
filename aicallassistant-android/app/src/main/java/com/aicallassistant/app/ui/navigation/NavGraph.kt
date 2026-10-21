package com.aicallassistant.app.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aicallassistant.app.ui.components.UserProfileAvatar
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
import com.aicallassistant.app.ui.screens.splash.SplashScreen
import com.aicallassistant.app.ui.theme.EqualDarkBg
import com.aicallassistant.app.ui.theme.EqualDarkBorder
import com.aicallassistant.app.ui.theme.EqualDarkCard
import com.aicallassistant.app.ui.theme.EqualDarkSurface
import com.aicallassistant.app.ui.theme.EqualGreen
import com.aicallassistant.app.ui.theme.EqualGreenPill
import com.aicallassistant.app.ui.theme.EqualGreenPillSelected
import com.aicallassistant.app.ui.theme.EqualGreenText
import com.aicallassistant.app.ui.theme.EqualTextPrimary
import com.aicallassistant.app.ui.theme.EqualTextSecondary
import com.aicallassistant.app.ui.theme.TealPrimary
import com.aicallassistant.app.ui.viewmodel.ActionItemsViewModel
import com.aicallassistant.app.ui.viewmodel.AuthViewModel
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

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector,
    val isUserTab: Boolean = false
) {
    data object Chats : BottomNavItem(
        route = "chats",
        title = "Chats",
        activeIcon = Icons.Default.Chat,
        inactiveIcon = Icons.Outlined.ChatBubbleOutline
    )
    data object Calls : BottomNavItem(
        route = "calls",
        title = "Calls",
        activeIcon = Icons.Default.Phone,
        inactiveIcon = Icons.Outlined.Phone
    )
    data object Tasks : BottomNavItem(
        route = "tasks",
        title = "Tasks",
        activeIcon = Icons.Default.CheckCircle,
        inactiveIcon = Icons.Outlined.CheckCircle
    )
    data object You : BottomNavItem(
        route = "you",
        title = "You",
        activeIcon = Icons.Default.Person,
        inactiveIcon = Icons.Outlined.Person,
        isUserTab = true
    )

    // Backward-compatible alias for existing tests
    companion object {
        val Home = Chats
        val History = Calls
        val ActionItems = Tasks
        val Settings = You
    }
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
    val currentRoute = navBackStackEntry?.destination?.route ?: BottomNavItem.Chats.route
    var activeTabRoute by remember { mutableStateOf(BottomNavItem.Chats.route) }

    LaunchedEffect(currentRoute) {
        activeTabRoute = currentRoute
    }

    val items = listOf(
        BottomNavItem.Chats,
        BottomNavItem.Calls,
        BottomNavItem.Tasks,
        BottomNavItem.You
    )

    var isDockVisible by remember { mutableStateOf(true) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // When scrolling down, hide the dock
                if (available.y < -10f) {
                    if (isDockVisible) isDockVisible = false
                }
                // When scrolling up, show the dock
                else if (available.y > 10f) {
                    if (!isDockVisible) isDockVisible = true
                }
                return Offset.Zero
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(EqualDarkBg)
            .nestedScroll(nestedScrollConnection)
    ) {
        // Main Tab Content
        NavHost(
            navController = bottomNavController,
            startDestination = BottomNavItem.Chats.route,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(BottomNavItem.Chats.route) {
                val homeViewModel: HomeViewModel = viewModel()
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToSimulateCall = onNavigateToSimulateCall,
                    onNavigateToCallDetail = onNavigateToCallDetail,
                    onNavigateToHistory = {
                        bottomNavController.navigate(BottomNavItem.Calls.route) {
                            popUpTo(bottomNavController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            composable(BottomNavItem.Calls.route) {
                val historyViewModel: CallHistoryViewModel = viewModel()
                CallHistoryScreen(
                    viewModel = historyViewModel,
                    onNavigateToCallDetail = onNavigateToCallDetail
                )
            }

            composable(BottomNavItem.Tasks.route) {
                val actionItemsViewModel: ActionItemsViewModel = viewModel()
                ActionItemsScreen(viewModel = actionItemsViewModel)
            }

            composable(BottomNavItem.You.route) {
                val settingsViewModel: SettingsViewModel = viewModel()
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onSignedOut = onSignedOut
                )
            }
        }

        // Floating Capsule Navigation Bar with Auto-Hide on Scroll
        AnimatedVisibility(
            visible = isDockVisible,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(durationMillis = 220)
            ) + fadeIn(animationSpec = tween(180)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(durationMillis = 220)
            ) + fadeOut(animationSpec = tween(180)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 14.dp, start = 40.dp, end = 40.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = com.aicallassistant.app.ui.theme.CallMateSurface,
                    border = BorderStroke(1.dp, com.aicallassistant.app.ui.theme.CallMateBorder),
                    shadowElevation = 12.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items.forEach { item ->
                            val isSelected = activeTabRoute == item.route
                            val interactionSource = remember { MutableInteractionSource() }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable(
                                        interactionSource = interactionSource,
                                        indication = null
                                    ) {
                                        if (activeTabRoute != item.route) {
                                            activeTabRoute = item.route
                                            bottomNavController.navigate(item.route) {
                                                popUpTo(bottomNavController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 44.dp, height = 36.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (isSelected) Color.White else Color.Transparent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) item.activeIcon else item.inactiveIcon,
                                        contentDescription = item.title,
                                        tint = if (isSelected) Color.Black else com.aicallassistant.app.ui.theme.CallMateSlate,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(3.dp))

                                // Small indicator dot below selected item
                                Box(
                                    modifier = Modifier
                                        .size(3.5.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(if (isSelected) Color.White else Color.Transparent)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
