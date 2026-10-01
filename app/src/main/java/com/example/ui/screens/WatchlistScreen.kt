package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SolanaPurple
import com.example.ui.viewmodel.TradingViewModel

@Composable
fun WatchlistScreen(
    viewModel: TradingViewModel,
    onNavigateToCoin: (String) -> Unit,
    onNavigateToMarkets: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coins by viewModel.coins.collectAsState()
    val watchlist by viewModel.watchlist.collectAsState()

    val watchedCoins = androidx.compose.runtime.remember(coins, watchlist) {
        coins
            .filter { coin ->
                watchlist.contains(coin.symbol.uppercase())
            }
            .sortedWith(
                compareByDescending<com.example.data.model.MemeCoin> { it.liquidityUsd }
                    .thenByDescending { it.volume24h }
                    .thenByDescending { it.marketCap }
            )
            .distinctBy { it.symbol.uppercase() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. TITLE ---
        Column(modifier = Modifier.padding(top = 16.dp)) {
            Text(
                text = "Watchlist",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Track your favorite high-octane meme positions.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // --- 2. LIST ---
        if (watchedCoins.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Your watchlist is empty",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Star coins on the markets page to keep track of their live price swings.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onNavigateToMarkets,
                        colors = ButtonDefaults.buttonColors(containerColor = SolanaPurple)
                    ) {
                        Text("Explore Markets")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(watchedCoins) { coin ->
                    DexScreenerCoinRow(
                        coin = coin,
                        selectedTimeframe = "24H",
                        isFavorite = true,
                        onClick = { onNavigateToCoin(coin.symbol) },
                        onFavoriteToggle = { viewModel.toggleWatchlist(coin.symbol) }
                    )
                }
            }
        }
    }
}
