package com.example.ui.navigation

sealed class Screen(val route: String, val title: String) {
    object Portfolio : Screen("portfolio", "Portfolio")
    object Markets : Screen("markets", "Markets")
    object Watchlist : Screen("watchlist", "Watchlist")
    object TradeHistory : Screen("trade_history", "History")
    object Settings : Screen("settings", "Settings")
    
    object Trade : Screen("trade/{symbol}?initialMode={initialMode}", "Trade") {
        fun createRoute(symbol: String, initialMode: String = "BUY") = "trade/$symbol?initialMode=$initialMode"
    }
    
    object CoinDetail : Screen("coin_detail/{symbol}", "Details") {
        fun createRoute(symbol: String) = "coin_detail/$symbol"
    }
}
