package com.example.ui.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.*
import com.example.ui.theme.PriceDown
import com.example.ui.theme.PriceUp
import com.example.ui.theme.SolanaCyan
import com.example.ui.theme.SolanaGreen
import com.example.ui.theme.SolanaPurple
import com.example.ui.theme.SolanaSurface
import com.example.ui.viewmodel.TradeResultEvent
import com.example.ui.viewmodel.TradingViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    viewModel: TradingViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Real-time top banner message state
    var currentNotificationMessage by remember { mutableStateOf<String?>(null) }
    var isNotificationSuccess by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        viewModel.tradeEventFlow.collect { event ->
            when (event) {
                is TradeResultEvent.Success -> {
                    currentNotificationMessage = event.message
                    isNotificationSuccess = true
                    delay(4500)
                    if (currentNotificationMessage == event.message) {
                        currentNotificationMessage = null
                    }
                }
                is TradeResultEvent.Error -> {
                    currentNotificationMessage = event.message
                    isNotificationSuccess = false
                    delay(4500)
                    if (currentNotificationMessage == event.message) {
                        currentNotificationMessage = null
                    }
                }
            }
        }
    }

    // Define main bottom bar navigation items
    val navigationItems = listOf(
        Pair(Screen.Portfolio, Icons.Default.AccountBalanceWallet),
        Pair(Screen.Markets, Icons.Default.Storefront),
        Pair(Screen.Watchlist, Icons.Default.Favorite),
        Pair(Screen.TradeHistory, Icons.Default.History),
        Pair(Screen.Settings, Icons.Default.Settings)
    )

    // Check if the current route is a sub-screen that shouldn't show the bottom nav bar
    val showBottomBar = navigationItems.any { it.first.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    modifier = Modifier.testTag("app_bottom_nav_bar"),
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    navigationItems.forEach { (screen, icon) ->
                        val selected = currentRoute == screen.route
                        NavigationBarItem(
                            icon = { Icon(imageVector = icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = selected,
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
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = SolanaCyan,
                                selectedTextColor = SolanaCyan,
                                indicatorColor = SolanaPurple.copy(alpha = 0.3f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Portfolio.route,
                modifier = Modifier.fillMaxSize()
            ) {
                // --- PORTFOLIO SCREEN ---
                composable(Screen.Portfolio.route) {
                    PortfolioScreen(
                        viewModel = viewModel,
                        onNavigateToMarkets = { navController.navigate(Screen.Markets.route) },
                        onNavigateToCoin = { symbol -> navController.navigate(Screen.CoinDetail.createRoute(symbol)) },
                        onNavigateToTrade = { symbol, mode -> navController.navigate(Screen.Trade.createRoute(symbol, mode)) }
                    )
                }

                // --- MARKETS SCREEN ---
                composable(Screen.Markets.route) {
                    MarketsScreen(
                        viewModel = viewModel,
                        onNavigateToCoin = { symbol -> navController.navigate(Screen.CoinDetail.createRoute(symbol)) }
                    )
                }

                // --- WATCHLIST SCREEN ---
                composable(Screen.Watchlist.route) {
                    WatchlistScreen(
                        viewModel = viewModel,
                        onNavigateToCoin = { symbol -> navController.navigate(Screen.CoinDetail.createRoute(symbol)) },
                        onNavigateToMarkets = { navController.navigate(Screen.Markets.route) }
                    )
                }

                // --- TRADE HISTORY SCREEN ---
                composable(Screen.TradeHistory.route) {
                    TradeHistoryScreen(viewModel = viewModel)
                }

                // --- SETTINGS SCREEN ---
                composable(Screen.Settings.route) {
                    SettingsScreen(viewModel = viewModel)
                }

                // --- COIN DETAIL SCREEN ---
                composable(
                    route = Screen.CoinDetail.route,
                    arguments = listOf(navArgument("symbol") { type = NavType.StringType })
                ) { backStackEntry ->
                    val symbol = backStackEntry.arguments?.getString("symbol") ?: ""
                    CoinDetailScreen(
                        symbol = symbol,
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToTrade = { coinSymbol, mode -> navController.navigate(Screen.Trade.createRoute(coinSymbol, mode)) }
                    )
                }

                // --- TRADE SCREEN ---
                composable(
                    route = Screen.Trade.route,
                    arguments = listOf(
                        navArgument("symbol") { type = NavType.StringType },
                        navArgument("initialMode") {
                            type = NavType.StringType
                            defaultValue = "BUY"
                        }
                    )
                ) { backStackEntry ->
                    val symbol = backStackEntry.arguments?.getString("symbol") ?: ""
                    val initialMode = backStackEntry.arguments?.getString("initialMode") ?: "BUY"
                    TradeScreen(
                        symbol = symbol,
                        initialMode = initialMode,
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }

            // Real-Time Animated Floating Banner Notification
            AnimatedVisibility(
                visible = currentNotificationMessage != null,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
            ) {
                currentNotificationMessage?.let { msg ->
                    val bg = if (isNotificationSuccess) SolanaSurface else Color(0xFF2A1518)
                    val borderColor = if (isNotificationSuccess) SolanaCyan else PriceDown

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = bg,
                        tonalElevation = 8.dp,
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = if (isNotificationSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = null,
                                    tint = if (isNotificationSuccess) SolanaGreen else PriceDown,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 13.sp
                                )
                            }
                            IconButton(
                                onClick = { currentNotificationMessage = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
