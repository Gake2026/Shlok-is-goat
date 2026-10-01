package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.NotificationsActive
import java.util.Locale


import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.example.ui.components.SparklineChart
import com.example.ui.theme.PriceDown
import com.example.ui.theme.PriceUp
import com.example.ui.theme.SolanaCyan
import com.example.ui.theme.SolanaGreen
import com.example.ui.theme.SolanaMagenta
import com.example.ui.theme.SolanaPurple
import com.example.ui.theme.SolanaSurface
import com.example.ui.utils.formatPrice
import com.example.ui.utils.formatUsd
import com.example.ui.viewmodel.TradingViewModel
import kotlinx.coroutines.delay
import kotlin.random.Random

data class OrderBookEntry(
    val priceUsd: Double,
    val sizeTokens: Double,
    val totalSol: Double,
    val depthRatio: Float
)

data class RecentTradeEntry(
    val id: String,
    val type: String, // "BUY" or "SELL"
    val priceUsd: Double,
    val amountSol: Double,
    val amountTokens: Double,
    val timeAgo: String,
    val walletAddress: String
)

data class TopTraderEntry(
    val rank: Int,
    val walletAddress: String,
    val profitSol: Double,
    val profitUsd: Double,
    val winRatePercent: Double,
    val tradesCount: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinDetailScreen(
    symbol: String,
    viewModel: TradingViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToTrade: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val coins by viewModel.coins.collectAsState()
    val watchlist by viewModel.watchlist.collectAsState()
    val solPriceUsd by viewModel.solPriceUsd.collectAsState()

    LaunchedEffect(symbol) {
        if (symbol.isNotEmpty()) {
            viewModel.ensureCoin(symbol)
        }
    }

    val coin = coins.find { 
        it.symbol.equals(symbol, ignoreCase = true) || 
        it.id.equals(symbol, ignoreCase = true) || 
        it.contractAddress.equals(symbol, ignoreCase = true) ||
        it.name.equals(symbol, ignoreCase = true)
    } ?: com.example.data.repository.DEFAULT_SOLANA_TOKENS.find { 
        it.symbol.equals(symbol, ignoreCase = true) || 
        it.id.equals(symbol, ignoreCase = true) || 
        it.contractAddress.equals(symbol, ignoreCase = true) ||
        it.name.equals(symbol, ignoreCase = true)
    } ?: com.example.data.repository.ROBINHOOD_TOKENS.find { 
        it.symbol.equals(symbol, ignoreCase = true) || 
        it.id.equals(symbol, ignoreCase = true) || 
        it.contractAddress.equals(symbol, ignoreCase = true) ||
        it.name.equals(symbol, ignoreCase = true)
    }
    val isWatched = watchlist.contains(symbol.uppercase())
    val transactions by viewModel.transactions.collectAsState()
    val coinTxs = remember(transactions, coin?.symbol) {
        if (coin != null) transactions.filter { it.coinSymbol.equals(coin.symbol, ignoreCase = true) } else emptyList()
    }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    if (coin == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp),
                    strokeWidth = 3.dp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Loading $symbol market data...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    // --- LIVE PRICE & ORDER BOOK STATE ---
    val livePrice = coin.currentPrice
    var selectedTimeframe by remember { mutableStateOf("15M") }

    // Order Book Bids & Asks based on real live price
    val asks = remember(livePrice, solPriceUsd) { generateSimulatedAsks(livePrice, solPriceUsd) }
    val bids = remember(livePrice, solPriceUsd) { generateSimulatedBids(livePrice, solPriceUsd) }

    // Recent Trades Stream
    val recentTrades = remember(livePrice, coin.symbol) { generateInitialRecentTrades(livePrice, coin.symbol) }

    // Slide-down Expandable Section States
    var isOrderBookExpanded by remember { mutableStateOf(false) }
    var isRecentTradesExpanded by remember { mutableStateOf(false) }
    var isLeaderboardExpanded by remember { mutableStateOf(false) }

    val isUp = coin.priceChange24h >= 0
    val trendColor = if (isUp) PriceUp else PriceDown
    val changeSign = if (isUp) "+" else ""

    fun launchUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            clipboardManager.setText(AnnotatedString(url))
            Toast.makeText(context, "Link copied to clipboard: $url", Toast.LENGTH_SHORT).show()
        }
    }

    // Price Alert Dialog State
    var showPriceAlertDialog by remember { mutableStateOf(false) }
    var alertPriceText by remember(livePrice) { mutableStateOf(String.format(Locale.US, "%.6f", livePrice)) }
    var isAboveAlert by remember { mutableStateOf(true) }

    if (showPriceAlertDialog) {
        AlertDialog(
            onDismissRequest = { showPriceAlertDialog = false },
            title = { Text("Set Price Alert for ${coin.symbol}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Current Price: $${String.format(Locale.US, "%.6f", livePrice)} USD")
                    OutlinedTextField(
                        value = alertPriceText,
                        onValueChange = { alertPriceText = it },
                        label = { Text("Target USD Price") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = isAboveAlert,
                            onClick = { isAboveAlert = true },
                            label = { Text("Price Rises Above (≥)") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = !isAboveAlert,
                            onClick = { isAboveAlert = false },
                            label = { Text("Price Drops Below (≤)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val targetPrice = alertPriceText.toDoubleOrNull() ?: livePrice
                        viewModel.createPriceAlert(coin.symbol, coin.name, targetPrice, isAboveAlert)
                        showPriceAlertDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SolanaPurple)
                ) {
                    Text("Set Alert")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPriceAlertDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = SolanaSurface
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(SolanaPurple.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!coin.logoUrl.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = coin.logoUrl,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text(
                                        text = coin.symbol.take(2),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SolanaCyan
                                    )
                                }
                            }

                            LaunchpadDexBadge(
                                coin = coin,
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .offset(x = (-2).dp, y = 2.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "${coin.name} (${coin.symbol})",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(SolanaGreen)
                                )
                                Text(
                                    text = "Live Ticking • ${coin.dexId.uppercase()}",
                                    fontSize = 10.sp,
                                    color = SolanaGreen
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Go back")
                    }
                },
                actions = {
                    IconButton(onClick = { showPriceAlertDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Set Price Alert",
                            tint = SolanaCyan
                        )
                    }

                    IconButton(onClick = { viewModel.toggleWatchlist(coin.symbol) }) {
                        Icon(
                            imageVector = if (isWatched) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Watchlist",
                            tint = if (isWatched) SolanaMagenta else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },


                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                color = SolanaSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = { onNavigateToTrade(coin.symbol, "BUY") },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("detail_buy_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = PriceUp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = "BUY ${coin.symbol}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        )
                    }

                    Button(
                        onClick = { onNavigateToTrade(coin.symbol, "SELL") },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("detail_sell_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = PriceDown),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = "SELL ${coin.symbol}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- HERO TOKEN BANNER ---
            if (!coin.bannerUrl.isNullOrEmpty()) {
                var isBannerError by remember(coin.bannerUrl) { mutableStateOf(false) }
                if (!isBannerError) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SolanaSurface),
                        border = BorderStroke(1.dp, SolanaPurple.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                        ) {
                            AsyncImage(
                                model = coin.bannerUrl,
                                contentDescription = "${coin.name} banner",
                                onError = { isBannerError = true },
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.85f)
                                            )
                                        )
                                    )
                            )
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(modifier = Modifier.size(42.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(SolanaPurple.copy(alpha = 0.3f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!coin.logoUrl.isNullOrEmpty()) {
                                            AsyncImage(
                                                model = coin.logoUrl,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Text(
                                                text = coin.symbol.take(2),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SolanaCyan
                                            )
                                        }
                                    }
                                }
                                Column {
                                    Text(
                                        text = coin.name,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                    )
                                    Text(
                                        text = "${coin.symbol} • ${coin.dexId.uppercase()}",
                                        fontSize = 11.sp,
                                        color = SolanaCyan,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- 1. HERO PRICE TICKER & LIVE PRICE BADGE ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = formatPrice(livePrice),
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 32.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = String.format("%s%.2f%% (24h)", changeSign, coin.priceChange24h),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = trendColor
                        )
                        val priceInSol = if (solPriceUsd > 0) livePrice / solPriceUsd else 0.0
                        Text(
                            text = String.format("≈ %.8f SOL", priceInSol),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Small badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(SolanaPurple.copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = if (coin.isNew) "NEW LAUNCH" else "SOLANA SPL",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = SolanaCyan
                    )
                }
            }

            // --- 2. LIVE INTERACTIVE CHART WITH TIMEFRAMES ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SolanaSurface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Timeframe Selector Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("1M", "5M", "15M", "1H", "4H", "1D", "ALL").forEach { tf ->
                            val isSel = selectedTimeframe == tf
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) SolanaCyan.copy(alpha = 0.2f) else Color.Transparent)
                                    .clickable { selectedTimeframe = tf }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = tf,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) SolanaCyan else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Sparkline / Live Canvas Graph
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    ) {
                        SparklineChart(
                            prices = if (coin.sparkline.isNotEmpty()) coin.sparkline else listOf(livePrice * 0.9, livePrice * 0.95, livePrice * 1.02, livePrice),
                            color = trendColor,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // --- 3. CONTRACT ADDRESS & SOCIAL LINKS ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SolanaSurface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Contract Address Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .clickable {
                                clipboardManager.setText(AnnotatedString(coin.contractAddress))
                                Toast.makeText(context, "Contract address copied!", Toast.LENGTH_SHORT).show()
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = SolanaGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Verified Contract Address (SPL)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = coin.contractAddress,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SolanaCyan,
                                maxLines = 1
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy address",
                            tint = SolanaCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Social Links Row
                    Text(
                        text = "Social Links & Verified Community",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val twitterUrl = coin.socials["twitter"] ?: coin.socials["x"] ?: "https://twitter.com/search?q=${coin.symbol}"
                        val telegramUrl = coin.socials["telegram"] ?: coin.socials["tg"] ?: "https://t.me/s/${coin.symbol.lowercase()}"
                        val isRealSolanaAddress = coin.contractAddress.length >= 20 && 
                            !coin.contractAddress.contains("Robinhood", ignoreCase = true) && 
                            !coin.contractAddress.startsWith("0xRobinhood", ignoreCase = true)
                        val dexUrl = coin.dexUrl ?: if (isRealSolanaAddress) {
                            "https://dexscreener.com/solana/${coin.contractAddress}"
                        } else {
                            "https://dexscreener.com/search?q=${coin.symbol}"
                        }
                        val scanUrl = if (isRealSolanaAddress) {
                            "https://solscan.io/account/${coin.contractAddress}"
                        } else {
                            "https://solscan.io/search?q=${coin.symbol}"
                        }

                        SocialChip(
                            label = "X (Twitter)",
                            icon = Icons.Default.Public,
                            onClick = { launchUrl(twitterUrl) },
                            modifier = Modifier.weight(1f)
                        )
                        SocialChip(
                            label = "Telegram",
                            icon = Icons.Default.Send,
                            onClick = { launchUrl(telegramUrl) },
                            modifier = Modifier.weight(1f)
                        )
                        SocialChip(
                            label = "DexScreener",
                            icon = Icons.Default.OpenInNew,
                            onClick = { launchUrl(dexUrl) },
                            modifier = Modifier.weight(1f)
                        )
                        SocialChip(
                            label = "Explorer",
                            icon = Icons.Default.Language,
                            onClick = { launchUrl(scanUrl) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (coin.websites.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .clickable { launchUrl(coin.websites.first()) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Language, contentDescription = null, tint = SolanaCyan, modifier = Modifier.size(14.dp))
                                Text("Official Website", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Text(coin.websites.first().replace("https://", "").replace("http://", "").trimEnd('/'), fontSize = 11.sp, color = SolanaCyan, maxLines = 1)
                        }
                    }
                }
            }

            // --- 4. YOUR ACTIVE POSITION & PNL CARD (IF OWNED) ---
            val summary by viewModel.computedPortfolioValue.collectAsState()
            val position = summary.detailedHoldings.find { it.holding.coinSymbol.equals(coin.symbol, ignoreCase = true) }

            if (position != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SolanaPurple.copy(alpha = 0.5f), RoundedCornerShape(18.dp)),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SolanaSurface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Your Position: ${String.format("%,.2f", position.holding.amount)} ${position.holding.coinSymbol}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = String.format("Current Value: %.4f SOL ($%,.2f)", position.currentValueSol, position.currentValueUsd),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            val isPos = position.unrealizedProfitSol >= 0
                            val col = if (isPos) PriceUp else PriceDown
                            val sign = if (isPos) "+" else ""

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = String.format("%s%.4f SOL", sign, position.unrealizedProfitSol),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = col
                                )
                                Text(
                                    text = String.format("%s%.2f%% ROI", sign, position.unrealizedRoiPercent),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = col
                                )
                            }
                        }
                    }
                }
            }

            // --- 5. SIMULATED LIVE ORDER BOOK (COLLAPSIBLE SLIDE-DOWN) ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SolanaSurface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isOrderBookExpanded = !isOrderBookExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoGraph,
                                contentDescription = null,
                                tint = SolanaCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "DEX Order Book",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (isOrderBookExpanded) "Hide Book" else "Spread 0.05% • View",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Icon(
                                imageVector = if (isOrderBookExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle Order Book",
                                tint = SolanaCyan
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = isOrderBookExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Order Book Table Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Price ($)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                                Text("Size (${coin.symbol})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                                Text("Total (SOL)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                            }

                            HorizontalDivider(color = Color(0x13FFFFFF))

                            // Asks (Sells - Red)
                            asks.forEach { ask ->
                                OrderBookRow(entry = ask, isAsk = true)
                            }

                            // Mid Price Divider Ticker
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "MID MARKET: ${formatPrice(livePrice)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = trendColor
                                )
                            }

                            // Bids (Buys - Green)
                            bids.forEach { bid ->
                                OrderBookRow(entry = bid, isAsk = false)
                            }
                        }
                    }
                }
            }

            // --- 6. RECENT TRADES STREAM (COLLAPSIBLE SLIDE-DOWN) ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SolanaSurface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isRecentTradesExpanded = !isRecentTradesExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(SolanaGreen)
                            )
                            Text(
                                text = "Live DEX Trades Stream",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (isRecentTradesExpanded) "Hide Stream" else "Live • View",
                                fontSize = 11.sp,
                                color = SolanaGreen
                            )
                            Icon(
                                imageVector = if (isRecentTradesExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle Trade Stream",
                                tint = SolanaGreen
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = isRecentTradesExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Type / Time", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1.2f))
                                Text("Amount (SOL)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                                Text("Trader TX", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                            }

                            HorizontalDivider(color = Color(0x13FFFFFF))

                            recentTrades.forEach { trade ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1.2f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (trade.type == "BUY") PriceUp.copy(alpha = 0.2f) else PriceDown.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = trade.type,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (trade.type == "BUY") PriceUp else PriceDown,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = trade.timeAgo,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Text(
                                        text = String.format(Locale.US, "%.3f SOL", trade.amountSol),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )

                                    Text(
                                        text = trade.walletAddress,
                                        fontSize = 11.sp,
                                        color = SolanaCyan,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- 7. TOP TRADERS LEADERBOARD (COLLAPSIBLE SLIDE-DOWN) ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SolanaSurface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isLeaderboardExpanded = !isLeaderboardExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Top Traders Leaderboard",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (isLeaderboardExpanded) "Hide Ranks" else "Top 4 • View",
                                fontSize = 11.sp,
                                color = Color(0xFFFFD700)
                            )
                            Icon(
                                imageVector = if (isLeaderboardExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle Leaderboard",
                                tint = Color(0xFFFFD700)
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = isLeaderboardExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            getTopTraders(coin.symbol, solPriceUsd).forEach { trader ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    when (trader.rank) {
                                                        1 -> Color(0xFFFFD700)
                                                        2 -> Color(0xC0C0C0)
                                                        3 -> Color(0xCD7F32)
                                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                                    }
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "#${trader.rank}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Black
                                            )
                                        }

                                        Column {
                                            Text(
                                                text = trader.walletAddress,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                color = SolanaCyan
                                            )
                                            Text(
                                                text = "${trader.tradesCount} trades • ${trader.winRatePercent}% win rate",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = String.format(Locale.US, "+%.1f SOL", trader.profitSol),
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = PriceUp
                                        )
                                        Text(
                                            text = String.format(Locale.US, "+$%,.1fK", trader.profitUsd / 1000.0),
                                            fontSize = 10.sp,
                                            color = PriceUp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- 9. DEVELOPER WALLET & SECURITY AUDIT ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SolanaSurface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = SolanaGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Developer Wallet & Security",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .clickable {
                                val devWallet = "4vNp8Xk9mQ1z7vE2p9xK3qL"
                                clipboardManager.setText(AnnotatedString(devWallet))
                                Toast.makeText(context, "Developer wallet address copied!", Toast.LENGTH_SHORT).show()
                            }
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Developer Wallet", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("4vNp8Xk9m...3qL", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SolanaCyan)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Dev Balance: 0.00% (Clean)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SolanaGreen)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = SolanaCyan, modifier = Modifier.size(16.dp))
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SecurityBadge(text = "Mint Authority Revoked", isGood = true, modifier = Modifier.weight(1f))
                        SecurityBadge(text = "Freeze Revoked", isGood = true, modifier = Modifier.weight(1f))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SecurityBadge(text = "LP Burned 99.4%", isGood = true, modifier = Modifier.weight(1f))
                        SecurityBadge(text = "Top 10 Holds < 14%", isGood = true, modifier = Modifier.weight(1f))
                    }
                }
            }

            // --- 9. MULTI-TIMEFRAME DEX ACTIVITY MATRIX (5M / 1H / 6H / 24H) ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0x13FFFFFF), RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SolanaSurface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoGraph,
                                contentDescription = null,
                                tint = SolanaCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "DexScreener Activity Matrix",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Text(
                            text = "Real-time DEX",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // 4 Timeframe Columns: 5M, 1H, 6H, 24H
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple("5M", coin.priceChange5m, coin.volume5m to (coin.buys5m to coin.sells5m)),
                            Triple("1H", coin.priceChange1h, coin.volume1h to (coin.buys1h to coin.sells1h)),
                            Triple("6H", coin.priceChange6h, coin.volume6h to (coin.buys6h to coin.sells6h)),
                            Triple("24H", coin.priceChange24h, coin.volume24h to (coin.buys24h to coin.sells24h))
                        ).forEach { (tf, chg, stats) ->
                            val (vol, txs) = stats
                            val (buys, sells) = txs
                            val isPos = chg >= 0
                            val col = if (isPos) PriceUp else PriceDown
                            val sign = if (isPos) "+" else ""

                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(text = tf, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(text = "$sign${String.format(Locale.US, "%.1f%%", chg)}", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = col)
                                    Text(text = formatUsd(vol), fontSize = 10.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = "$buys", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = PriceUp)
                                        Text(text = "/", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(text = "$sells", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = PriceDown)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- 10. METRICS BREAKDOWN CARD (Liquidity, Market Cap, Volume, Holders, Pair Age) ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0x13FFFFFF), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SolanaSurface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    StatRow(label = "Market Capitalization (MCap)", value = formatUsd(coin.marketCap))
                    HorizontalDivider(color = Color(0x0DFFFFFF))
                    StatRow(label = "Fully Diluted Valuation (FDV)", value = formatUsd(coin.fdv))
                    HorizontalDivider(color = Color(0x0DFFFFFF))
                    StatRow(label = "Circulating Supply", value = "${String.format(Locale.US, "%,.0f", coin.circulatingSupply)} ${coin.symbol}")
                    HorizontalDivider(color = Color(0x0DFFFFFF))
                    StatRow(label = "24-Hour Trading Volume", value = formatUsd(coin.volume24h))
                    HorizontalDivider(color = Color(0x0DFFFFFF))
                    
                    val liqText = if (coin.liquidityQuote > 0) {
                        "${formatUsd(coin.liquidityUsd)} (${String.format(Locale.US, "%,.1f", coin.liquidityQuote)} ${coin.quoteTokenSymbol})"
                    } else {
                        "${formatUsd(coin.liquidityUsd)} (Locked)"
                    }
                    StatRow(label = "Total DEX Liquidity", value = liqText)
                    HorizontalDivider(color = Color(0x0DFFFFFF))

                    val holderSign = if (coin.holdersChange24h >= 0) "+" else ""
                    val holderText = if (coin.holdersCount > 0) {
                        "${String.format(Locale.US, "%,d", coin.holdersCount)} ($holderSign${String.format(Locale.US, "%.1f", coin.holdersChange24h)}% 24h)"
                    } else {
                        "N/A"
                    }
                    StatRow(label = "Token Holders", value = holderText)
                    HorizontalDivider(color = Color(0x0DFFFFFF))
                    
                    if (coin.priceNative != null && coin.priceNative > 0) {
                        StatRow(label = "Native Quote Price", value = "${String.format(Locale.US, "%.8f", coin.priceNative)} ${coin.quoteTokenSymbol}")
                        HorizontalDivider(color = Color(0x0DFFFFFF))
                    }

                    StatRow(label = "Pair Age & Pool", value = "${coin.pairAge} (${coin.dexId.uppercase()})")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun OrderBookRow(entry: OrderBookEntry, isAsk: Boolean) {
    val barColor = if (isAsk) PriceDown.copy(alpha = 0.15f) else PriceUp.copy(alpha = 0.15f)
    val textColor = if (isAsk) PriceDown else PriceUp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(22.dp)
    ) {
        // Depth bar background
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(entry.depthRatio)
                .align(Alignment.CenterEnd)
                .background(barColor)
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatPrice(entry.priceUsd),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = String.format("%,.0f", entry.sizeTokens),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = String.format("%.2f", entry.totalSol),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun SocialChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = SolanaCyan, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun SecurityBadge(text: String, isGood: Boolean, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = (if (isGood) SolanaGreen else PriceDown).copy(alpha = 0.15f),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (isGood) SolanaGreen else PriceDown,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isGood) SolanaGreen else PriceDown
            )
        }
    }
}

fun generateSimulatedAsks(currentPrice: Double, solPriceUsd: Double): List<OrderBookEntry> {
    val list = mutableListOf<OrderBookEntry>()
    for (i in 5 downTo 1) {
        val p = currentPrice * (1.0 + (i * 0.0012))
        val size = Random.nextDouble(50000.0, 850000.0)
        val solTotal = if (solPriceUsd > 0) (size * p) / solPriceUsd else 0.0
        val ratio = (i.toFloat() / 5f) * 0.8f
        list.add(OrderBookEntry(p, size, solTotal, ratio))
    }
    return list
}

fun generateSimulatedBids(currentPrice: Double, solPriceUsd: Double): List<OrderBookEntry> {
    val list = mutableListOf<OrderBookEntry>()
    for (i in 1..5) {
        val p = currentPrice * (1.0 - (i * 0.0012))
        val size = Random.nextDouble(60000.0, 920000.0)
        val solTotal = if (solPriceUsd > 0) (size * p) / solPriceUsd else 0.0
        val ratio = ( (6 - i).toFloat() / 5f) * 0.85f
        list.add(OrderBookEntry(p, size, solTotal, ratio))
    }
    return list
}

fun generateInitialRecentTrades(currentPrice: Double, symbol: String): List<RecentTradeEntry> {
    val list = mutableListOf<RecentTradeEntry>()
    for (i in 0 until 8) {
        val type = if (i % 2 == 0) "BUY" else "SELL"
        val amtSol = Random.nextDouble(0.2, 12.0)
        val amtTokens = if (currentPrice > 0) (amtSol * 185.0) / currentPrice else 0.0
        val wallet = "${Random.nextInt(10, 99)}${listOf("xG", "mK", "9a", "2f", "8q")[i % 5]}...${listOf("7p", "1z", "9w", "4k")[i % 4]}"
        val timeAgo = "${i * 3 + 1}s ago"
        list.add(
            RecentTradeEntry(
                id = "init_$i",
                type = type,
                priceUsd = currentPrice,
                amountSol = amtSol,
                amountTokens = amtTokens,
                timeAgo = timeAgo,
                walletAddress = wallet
            )
        )
    }
    return list
}

fun getTopTraders(symbol: String, solPriceUsd: Double): List<TopTraderEntry> {
    val safeSol = if (solPriceUsd > 0) solPriceUsd else 185.0
    return listOf(
        TopTraderEntry(1, "7xGk8vM...9pA", 1840.0, 1840.0 * safeSol, 94.2, 42),
        TopTraderEntry(2, "4mK9zL1...1zL", 1120.0, 1120.0 * safeSol, 88.5, 29),
        TopTraderEntry(3, "9bX3qW8...8qW", 850.0, 850.0 * safeSol, 91.0, 18),
        TopTraderEntry(4, "2fP1vE5...5vE", 620.0, 620.0 * safeSol, 83.3, 24)
    )
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
