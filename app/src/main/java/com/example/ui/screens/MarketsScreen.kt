package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.example.data.model.MemeCoin
import com.example.ui.theme.PriceDown
import com.example.ui.theme.PriceUp
import com.example.ui.theme.SolanaCyan
import com.example.ui.theme.SolanaGreen
import com.example.ui.theme.SolanaMagenta
import com.example.ui.theme.SolanaPurple
import com.example.ui.theme.SolanaSurface
import com.example.ui.utils.formatPrice
import com.example.ui.utils.formatUsd
import com.example.ui.utils.truncateAddress
import com.example.ui.viewmodel.TradingViewModel
import java.util.Locale

// Dex Screener Dark Colors
private val DexCardBg = Color(0xFF131722)
private val DexHeaderBg = Color(0xFF1B202E)
private val DexBadgeBg = Color(0xFF1E2433)
private val DexBorderColor = Color(0x1FFFFFFF)
private val DexOrangeSymbol = Color(0xFFFFA000)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketsScreen(
    viewModel: TradingViewModel,
    onNavigateToCoin: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val coins by viewModel.coins.collectAsState()
    val watchlist by viewModel.watchlist.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isError by viewModel.isError.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Top Category Tab: "trending", "new_trending", "top"
    var topCategoryTab by remember { mutableStateOf("trending") }

    // New Trending Timeframe: "5M", "1H", "6H", "24H"
    var newTrendingTimeframe by remember { mutableStateOf("6H") }
    var showNewTrendingMenu by remember { mutableStateOf(false) }

    // Timeframe Filter: "5M", "1H", "6H", "24H"
    var selectedTimeframe by remember { mutableStateOf("24H") }

    // Chain Filter state
    var selectedChain by remember { mutableStateOf("Solana") }
    var showChainMenu by remember { mutableStateOf(false) }

    // Sorting state: "trending", "newest", "volume", "liquidity", "gainers", "losers", "mcap"
    var sortOption by remember { mutableStateOf("trending") }
    var showSortMenu by remember { mutableStateOf(false) }

    val processedCoins = remember(coins, sortOption, topCategoryTab, selectedTimeframe, newTrendingTimeframe, selectedChain, searchQuery) {
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase()
            val matches = coins.filter { coin ->
                coin.name.lowercase().contains(q) ||
                coin.symbol.lowercase().contains(q) ||
                coin.contractAddress.lowercase().contains(q) ||
                coin.id.lowercase().contains(q)
            }
            matches.sortedByDescending { it.volume24h }
        } else {
            val filteredByChain = when (selectedChain) {
            "Solana" -> coins.filter { 
                (it.chain.equals("solana", ignoreCase = true) || it.chain.isBlank()) && it.dexId != "ROBINHOOD" 
            }.ifEmpty {
                if (searchQuery.isBlank()) com.example.data.repository.DEFAULT_SOLANA_TOKENS else emptyList()
            }
            "Robinhood" -> coins.filter { 
                it.dexId == "ROBINHOOD" || it.chain.equals("robinhood", ignoreCase = true) 
            }.ifEmpty {
                if (searchQuery.isBlank()) com.example.data.repository.ROBINHOOD_TOKENS else emptyList()
            }
            "Ethereum" -> coins.filter { 
                it.chain.equals("ethereum", ignoreCase = true) 
            }.ifEmpty {
                if (searchQuery.isBlank()) com.example.data.repository.DEFAULT_ETHEREUM_TOKENS else emptyList()
            }
            "BNB Chain" -> coins.filter { 
                it.chain.equals("bsc", ignoreCase = true) || it.chain.equals("bnb", ignoreCase = true) 
            }.ifEmpty {
                if (searchQuery.isBlank()) com.example.data.repository.DEFAULT_BSC_TOKENS else emptyList()
            }
            "Base" -> coins.filter { 
                it.chain.equals("base", ignoreCase = true) 
            }.ifEmpty {
                if (searchQuery.isBlank()) com.example.data.repository.DEFAULT_BASE_TOKENS else emptyList()
            }
            "Arbitrum" -> coins.filter { 
                it.chain.equals("arbitrum", ignoreCase = true) 
            }.ifEmpty {
                if (searchQuery.isBlank()) com.example.data.repository.DEFAULT_ARBITRUM_TOKENS else emptyList()
            }
            "Polygon" -> coins.filter { 
                it.chain.equals("polygon", ignoreCase = true) || it.chain.equals("matic", ignoreCase = true) 
            }.ifEmpty {
                if (searchQuery.isBlank()) com.example.data.repository.DEFAULT_POLYGON_TOKENS else emptyList()
            }
            "All Chains" -> coins
            else -> coins
        }

        val filteredCategory = when (topCategoryTab) {
            "new_trending" -> {
                val now = System.currentTimeMillis()
                val maxAgeMs = when (newTrendingTimeframe) {
                    "5M" -> 30 * 60 * 1000L      // up to 30 minutes
                    "1H" -> 60 * 60 * 1000L      // up to 1 hour
                    "6H" -> 6 * 3600 * 1000L     // up to 6 hours
                    "24H" -> 24 * 3600 * 1000L   // up to 24 hours
                    else -> 6 * 3600 * 1000L
                }

                val ageFiltered = filteredByChain.filter { coin ->
                    if (coin.pairCreatedAt > 0L) {
                        val age = now - coin.pairCreatedAt
                        age in 0L..maxAgeMs
                    } else {
                        val ageStr = coin.pairAge.lowercase().trim()
                        if (ageStr.endsWith("m")) {
                            val m = ageStr.dropLast(1).filter { it.isDigit() }.toLongOrNull() ?: 0L
                            m * 60 * 1000L <= maxAgeMs
                        } else if (ageStr.contains("h")) {
                            val h = ageStr.takeWhile { it.isDigit() }.toLongOrNull() ?: 0L
                            h * 3600 * 1000L <= maxAgeMs
                        } else {
                            coin.isNew && maxAgeMs >= 24 * 3600 * 1000L
                        }
                    }
                }

                if (ageFiltered.isNotEmpty()) {
                    ageFiltered.sortedByDescending { it.pairCreatedAt }
                } else {
                    filteredByChain.sortedByDescending { it.volume24h }
                }
            }
            "top" -> filteredByChain.sortedByDescending { it.volume24h }
            else -> filteredByChain
        }

        val activeTimeframe = if (topCategoryTab == "new_trending") newTrendingTimeframe else selectedTimeframe

        val sorted = when (sortOption) {
            "newest" -> filteredCategory.sortedByDescending { it.pairCreatedAt }
            "volume" -> filteredCategory.sortedByDescending { it.volume24h }
            "liquidity" -> filteredCategory.sortedByDescending { it.liquidityUsd }
            "gainers" -> filteredCategory.sortedByDescending {
                when (activeTimeframe) {
                    "5M" -> it.priceChange5m
                    "1H" -> it.priceChange1h
                    "6H" -> it.priceChange6h
                    else -> it.priceChange24h
                }
            }
            "losers" -> filteredCategory.sortedBy {
                when (activeTimeframe) {
                    "5M" -> it.priceChange5m
                    "1H" -> it.priceChange1h
                    "6H" -> it.priceChange6h
                    else -> it.priceChange24h
                }
            }
            "mcap" -> filteredCategory.sortedByDescending { it.marketCap }
            else -> filteredCategory // DexScreener rank default
        }

        // --- DEXSCREENER ANTI-SCAM & ANTI-RUGPULL RANKING ENGINE ---
        if (sortOption == "trending") {
            val (legitCoins, lowLiqCoins) = sorted.partition { coin ->
                (coin.liquidityUsd >= 1000.0 && coin.volume24h >= 100.0) || coin.dexId == "ROBINHOOD"
            }
            legitCoins + lowLiqCoins
        } else {
            sorted
        }
        }
    }

    // Dynamic Volume and Txns Calculations
    val totalVolume24h = remember(coins) {
        val sum = coins.sumOf { it.volume24h }
        if (sum > 0) sum else 6_390_000_000.0
    }
    val totalTxns24h = remember(coins) {
        val sum = coins.sumOf { (it.buys24h + it.sells24h).toLong() }
        if (sum > 0) sum else 19_470_612L
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // --- 1. SEARCH BAR (KEPT AT THE EXACT SAME TOP POSITION) ---
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            placeholder = { Text("Search by name, symbol or contract...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("markets_search_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SolanaCyan,
                unfocusedBorderColor = DexBorderColor,
                focusedContainerColor = DexCardBg,
                unfocusedContainerColor = DexCardBg
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        // --- 2. DEX SCREENER TOP CATEGORIES (Trending | New Trending | Top) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DexCategoryChip(
                label = "Trending",
                isSelected = topCategoryTab == "trending",
                onClick = { topCategoryTab = "trending" },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Whatshot,
                        contentDescription = null,
                        tint = if (topCategoryTab == "trending") Color.White else Color(0xFFFF7043),
                        modifier = Modifier.size(15.dp)
                    )
                },
                modifier = Modifier.weight(1f)
            )
            Box(modifier = Modifier.weight(1.35f)) {
                DexCategoryChip(
                    label = "New Trending $newTrendingTimeframe",
                    isSelected = topCategoryTab == "new_trending",
                    onClick = {
                        topCategoryTab = "new_trending"
                        showNewTrendingMenu = !showNewTrendingMenu
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Eco,
                            contentDescription = null,
                            tint = SolanaGreen,
                            modifier = Modifier.size(15.dp)
                        )
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                DropdownMenu(
                    expanded = showNewTrendingMenu,
                    onDismissRequest = { showNewTrendingMenu = false },
                    modifier = Modifier.background(DexHeaderBg)
                ) {
                    listOf("5M", "1H", "6H", "24H").forEach { tf ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Eco,
                                        contentDescription = null,
                                        tint = SolanaGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "New Trending $tf",
                                        color = if (newTrendingTimeframe == tf && topCategoryTab == "new_trending") SolanaCyan else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    if (newTrendingTimeframe == tf && topCategoryTab == "new_trending") {
                                        Text("✓", color = SolanaCyan, fontWeight = FontWeight.Bold)
                                    }
                                }
                            },
                            onClick = {
                                newTrendingTimeframe = tf
                                selectedTimeframe = tf
                                topCategoryTab = "new_trending"
                                showNewTrendingMenu = false
                            }
                        )
                    }
                }
            }
            DexCategoryChip(
                label = "Top",
                isSelected = topCategoryTab == "top",
                onClick = { topCategoryTab = "top" },
                icon = {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                },
                modifier = Modifier.weight(1f)
            )
        }

        // --- SLIDE DOWN ANIMATED TIMEFRAME SELECTOR FOR NEW TRENDING ---
        AnimatedVisibility(
            visible = topCategoryTab == "new_trending",
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                color = DexCardBg,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, DexBorderColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = SolanaCyan,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "New Trending Age:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("5M", "1H", "6H", "24H").forEach { tf ->
                            val isSel = newTrendingTimeframe == tf
                            Surface(
                                onClick = {
                                    newTrendingTimeframe = tf
                                    selectedTimeframe = tf
                                },
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSel) SolanaCyan.copy(alpha = 0.2f) else Color.Transparent,
                                border = BorderStroke(1.dp, if (isSel) SolanaCyan else Color.DarkGray)
                            ) {
                                Text(
                                    text = tf,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) SolanaCyan else Color.Gray,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 3. METAS TICKER HORIZONTAL SCROLL BAR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(imageVector = Icons.Default.Hub, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(14.dp))
                Text(text = "Metas", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            DexMetaTag(
                name = "Cat",
                cap = "$16.97B",
                isUp = true,
                icon = { Icon(Icons.Default.Pets, contentDescription = null, tint = SolanaCyan, modifier = Modifier.size(14.dp)) }
            )
            DexMetaTag(
                name = "Internet Animals",
                cap = "$95.8M",
                isUp = true,
                icon = { Icon(Icons.Default.Pets, contentDescription = null, tint = SolanaGreen, modifier = Modifier.size(14.dp)) }
            )
            DexMetaTag(
                name = "Memes",
                cap = "$4.2B",
                isUp = true,
                icon = { Icon(Icons.Default.EmojiEmotions, contentDescription = null, tint = Color(0xFFFFD54F), modifier = Modifier.size(14.dp)) }
            )
            DexMetaTag(
                name = "AI Tokens",
                cap = "$1.8B",
                isUp = true,
                icon = { Icon(Icons.Default.Psychology, contentDescription = null, tint = SolanaMagenta, modifier = Modifier.size(14.dp)) }
            )
        }

        // --- 4. 24H VOLUME & 24H TXNS SUMMARY CARDS ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DexSummaryStatCard(
                label = "24H VOLUME",
                value = formatCompactUsd(totalVolume24h),
                modifier = Modifier.weight(1f)
            )
            DexSummaryStatCard(
                label = "24H TXNS",
                value = String.format(Locale.US, "%,d", totalTxns24h),
                modifier = Modifier.weight(1f)
            )
        }

        // --- 5. DEX FILTER PILLS & TIME FILTERS (5M, 1H, 6H, 24H) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Timeframe Selector Pill
            Box {
                var showTimeMenu by remember { mutableStateOf(false) }
                DexFilterPill(
                    label = selectedTimeframe,
                    isSelected = true,
                    onClick = { showTimeMenu = true },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = SolanaCyan,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                )
                DropdownMenu(
                    expanded = showTimeMenu,
                    onDismissRequest = { showTimeMenu = false },
                    modifier = Modifier.background(DexHeaderBg)
                ) {
                    listOf("5M", "1H", "6H", "24H").forEach { tf ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = SolanaCyan,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(tf, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            },
                            onClick = {
                                selectedTimeframe = tf
                                showTimeMenu = false
                            }
                        )
                    }
                }
            }

            // Chain Filter Pill
            val currentChain = CHAIN_OPTIONS.find { it.id == selectedChain } ?: CHAIN_OPTIONS.first()
            Box {
                DexFilterPill(
                    label = currentChain.name,
                    isSelected = selectedChain != "Solana",
                    onClick = { showChainMenu = true },
                    imageUrl = if (currentChain.id != "All Chains") currentChain.logoUrl else null,
                    icon = if (currentChain.id == "All Chains") {
                        {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = SolanaCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else null
                )
                DropdownMenu(
                    expanded = showChainMenu,
                    onDismissRequest = { showChainMenu = false },
                    modifier = Modifier.background(DexHeaderBg)
                ) {
                    CHAIN_OPTIONS.forEach { chain ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF2A2E3D)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (chain.id == "All Chains") {
                                            Icon(
                                                imageVector = Icons.Default.Language,
                                                contentDescription = null,
                                                tint = SolanaCyan,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        } else {
                                            AsyncImage(
                                                model = chain.logoUrl,
                                                contentDescription = chain.name,
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(1.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = chain.name,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            },
                            onClick = {
                                selectedChain = chain.id
                                showChainMenu = false
                            }
                        )
                    }
                }
            }

            // Sort Selector Pill
            Box {
                DexFilterPill(
                    label = "Sort (${sortOption.uppercase()})",
                    isSelected = sortOption != "trending",
                    onClick = { showSortMenu = true },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = null,
                            tint = SolanaCyan,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                )
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false },
                    modifier = Modifier.background(DexHeaderBg)
                ) {
                    val sortItems = listOf(
                        Triple("trending", "Default Trending", Icons.Default.Whatshot),
                        Triple("newest", "Newest Pairs", Icons.Default.Eco),
                        Triple("volume", "24h Volume", Icons.Default.BarChart),
                        Triple("liquidity", "Liquidity", Icons.Default.WaterDrop),
                        Triple("gainers", "Top Gainers", Icons.Default.TrendingUp),
                        Triple("losers", "Top Losers", Icons.Default.TrendingDown),
                        Triple("mcap", "Market Cap", Icons.Default.Diamond)
                    )
                    sortItems.forEach { (key, title, iconVec) ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = iconVec,
                                        contentDescription = null,
                                        tint = if (sortOption == key) SolanaCyan else Color.Gray,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(title, color = Color.White, fontSize = 13.sp)
                                }
                            },
                            onClick = {
                                sortOption = key
                                showSortMenu = false
                            }
                        )
                    }
                }
            }
        }

        // --- 6. COIN LIST VIEW ENGINE ---
        if (isError && coins.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .border(1.dp, Color(0x33FF0055), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = DexCardBg),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "API Error",
                            tint = PriceDown,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "DexScreener API is currently unavailable.\nRetrying automatically every 10 seconds...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { viewModel.retryFetch() },
                            colors = ButtonDefaults.buttonColors(containerColor = SolanaPurple),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Retry Now", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else if (isLoading && coins.isEmpty()) {
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
                    CircularProgressIndicator(color = SolanaCyan)
                    Text(
                        text = "Loading DexScreener Live Pairs...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (processedCoins.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No live Solana tokens matched your search.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(processedCoins, key = { it.id }) { coin ->
                    val isFavorite = watchlist.contains(coin.symbol.uppercase())

                    DexScreenerCoinRow(
                        coin = coin,
                        selectedTimeframe = selectedTimeframe,
                        isFavorite = isFavorite,
                        onClick = { onNavigateToCoin(coin.symbol) },
                        onFavoriteToggle = { viewModel.toggleWatchlist(coin.symbol) }
                    )
                }
            }
        }
    }
}

// --- DEX SCREENER UI COMPONENTS ---

@Composable
fun DexCategoryChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(38.dp),
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) Color(0xFF2B52B0) else DexBadgeBg,
        border = if (isSelected) BorderStroke(1.dp, SolanaCyan) else BorderStroke(1.dp, DexBorderColor)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            if (icon != null) {
                icon()
                Spacer(modifier = Modifier.width(5.dp))
            }
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (trailingIcon != null) {
                Spacer(modifier = Modifier.width(3.dp))
                trailingIcon()
            }
        }
    }
}

@Composable
fun DexMetaTag(
    name: String,
    cap: String,
    isUp: Boolean,
    icon: (@Composable () -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DexBadgeBg)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        if (icon != null) {
            icon()
        }
        Text(text = name, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
        Text(text = cap, fontSize = 11.sp, color = Color.LightGray)
        Icon(
            imageVector = if (isUp) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
            contentDescription = null,
            tint = if (isUp) PriceUp else PriceDown,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun DexSummaryStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = DexCardBg),
        border = BorderStroke(1.dp, DexBorderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
        }
    }
}

data class ChainOption(
    val id: String,
    val name: String,
    val logoUrl: String
)

val CHAIN_OPTIONS = listOf(
    ChainOption("Solana", "Solana", "https://dd.dexscreener.com/ds-data/chains/solana.png"),
    ChainOption("Robinhood", "Robinhood", "https://cdn.jsdelivr.net/gh/walkxcode/dashboard-icons/png/robinhood.png"),
    ChainOption("Ethereum", "Ethereum", "https://dd.dexscreener.com/ds-data/chains/ethereum.png"),
    ChainOption("BNB Chain", "BNB Chain", "https://dd.dexscreener.com/ds-data/chains/bsc.png"),
    ChainOption("Base", "Base", "https://dd.dexscreener.com/ds-data/chains/base.png"),
    ChainOption("Arbitrum", "Arbitrum", "https://dd.dexscreener.com/ds-data/chains/arbitrum.png"),
    ChainOption("Polygon", "Polygon", "https://dd.dexscreener.com/ds-data/chains/polygon.png"),
    ChainOption("All Chains", "All Chains", "https://dd.dexscreener.com/ds-data/chains/solana.png")
)



@Composable
fun DexFilterPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    imageUrl: String? = null,
    icon: (@Composable () -> Unit)? = null
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) Color(0xFF1E2838) else DexBadgeBg,
        border = BorderStroke(1.dp, if (isSelected) SolanaCyan else DexBorderColor)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            if (!imageUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = label,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                )
            } else if (icon != null) {
                icon()
            }
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) SolanaCyan else Color.LightGray
            )
        }
    }
}

// --- DEX SCREENER TOKEN ITEM ROW (MATCHES USER ATTACHED SCREENSHOT EXACTLY) ---
@Composable
fun DexScreenerCoinRow(
    coin: MemeCoin,
    selectedTimeframe: String,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    val selectedChange = when (selectedTimeframe) {
        "5M" -> coin.priceChange5m
        "1H" -> coin.priceChange1h
        "6H" -> coin.priceChange6h
        else -> coin.priceChange24h
    }

    val change24h = coin.priceChange24h
    val is1hUp = coin.priceChange1h >= 0
    val is24hUp = change24h >= 0

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("coin_row_${coin.symbol.lowercase()}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DexCardBg),
        border = BorderStroke(1.dp, DexBorderColor)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            TokenBannerHeader(bannerUrl = coin.bannerUrl, name = coin.name)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
            // --- TOP PORTION: LOGO, NAME, AGE/BOOST, PRICE & PERCENTAGES ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // LEFT SIDE: Token Logo & Info
                Row(
                    modifier = Modifier.weight(1.4f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Logo Box with DEX badge overlay
                    Box(
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SolanaPurple.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!coin.logoUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = coin.logoUrl,
                                    contentDescription = "${coin.name} logo",
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text(
                                    text = coin.symbol.take(3),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SolanaCyan
                                )
                            }
                        }

                        // DEX / Swap Launchpad Badge Overlay
                        LaunchpadDexBadge(
                            coin = coin,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .offset(x = (-3).dp, y = 3.dp)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        // Symbol line with Age & Boost
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = coin.symbol,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp
                                ),
                                color = DexOrangeSymbol,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            // Age Pill Eco leaf icon + 5h
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Eco,
                                    contentDescription = null,
                                    tint = SolanaGreen,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = coin.pairAge,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SolanaGreen
                                )
                            }

                            // Boost score Bolt icon
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = Color(0xFFFFD54F),
                                    modifier = Modifier.size(11.dp)
                                )
                                val boostVal = if (coin.volume24h > 0) {
                                    (coin.volume24h / 100000.0).toInt().coerceIn(25, 999)
                                } else {
                                    (coin.buys24h + coin.sells24h).coerceIn(10, 999)
                                }
                                Text(
                                    text = "$boostVal",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFD54F)
                                )
                            }
                        }

                        // Subtitle line: Full Token Name
                        Text(
                            text = coin.name,
                            fontSize = 12.sp,
                            color = Color.Gray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // RIGHT SIDE: Price & Timeframe Percentages
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = formatDexPrice(coin.currentPrice),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        ),
                        color = Color.White
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Selected Timeframe percent (e.g. 1H)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "$selectedTimeframe ", fontSize = 10.sp, color = Color.Gray)
                            Text(
                                text = formatCompactPercent(selectedChange),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedChange >= 0) PriceUp else PriceDown
                            )
                        }

                        // 24H percent
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "24H ", fontSize = 10.sp, color = Color.Gray)
                            Text(
                                text = formatCompactPercent(change24h),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (is24hUp) PriceUp else PriceDown
                            )
                        }
                    }
                }
            }

            // --- BOTTOM PORTION: LIQUIDITY, VOLUME, MARKET CAP BADGES ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DexMetricBadge(label = "LIQ", value = formatCompactUsd(coin.liquidityUsd))
                DexMetricBadge(label = "VOL", value = formatCompactUsd(coin.volume24h))
                DexMetricBadge(label = "MCAP", value = formatCompactUsd(coin.marketCap))

                Spacer(modifier = Modifier.weight(1f))

                // Watchlist Favorite Star
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) SolanaMagenta else Color.Gray,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { onFavoriteToggle() }
                )
            }
        }
    }
}
}

@Composable
fun DexMetricBadge(
    label: String,
    value: String
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(DexBadgeBg)
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = label, fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            Text(text = value, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.ExtraBold)
        }
    }
}

// --- HELPER COMPACT FORMATTERS ---
private fun formatCompactUsd(amount: Double): String {
    return when {
        amount <= 0.0 -> "$0"
        amount >= 1_000_000_000.0 -> String.format(Locale.US, "$%.2fB", amount / 1_000_000_000.0)
        amount >= 1_000_000.0 -> {
            val valM = amount / 1_000_000.0
            if (valM >= 100.0) String.format(Locale.US, "$%.0fM", valM)
            else String.format(Locale.US, "$%.2fM", valM)
        }
        amount >= 1_000.0 -> {
            val valK = amount / 1_000.0
            if (valK >= 100.0) String.format(Locale.US, "$%.0fK", valK)
            else String.format(Locale.US, "$%.1fK", valK)
        }
        else -> String.format(Locale.US, "$%.2f", amount)
    }
}

private fun formatCompactPercent(percent: Double): String {
    val absVal = kotlin.math.abs(percent)
    val sign = if (percent >= 0) "" else "-"
    return when {
        absVal >= 1000 -> String.format(Locale.US, "%s%.0fK%%", sign, absVal / 1000.0)
        absVal >= 10 -> String.format(Locale.US, "%s%.0f%%", sign, absVal)
        else -> String.format(Locale.US, "%s%.1f%%", sign, absVal)
    }
}

private fun formatDexPrice(price: Double): String {
    if (price <= 0.0) return "$0.00"
    if (price >= 10.0) {
        return String.format(Locale.US, "$%,.2f", price)
    }
    if (price >= 1.0) {
        return String.format(Locale.US, "$%.3f", price)
    }
    if (price >= 0.01) {
        return String.format(Locale.US, "$%.4f", price)
    }
    // Subscript notation format e.g. $0.0₃1172 for tiny meme prices
    val priceStr = String.format(Locale.US, "%.10f", price)
    val afterDot = priceStr.substringAfter(".")
    var zeroCount = 0
    for (char in afterDot) {
        if (char == '0') zeroCount++ else break
    }
    if (zeroCount >= 2) {
        val digits = afterDot.drop(zeroCount).take(4)
        val subscriptDigit = when (zeroCount) {
            2 -> "₂"
            3 -> "₃"
            4 -> "₄"
            5 -> "₅"
            6 -> "₆"
            7 -> "₇"
            8 -> "₈"
            else -> "₉"
        }
        return "$0.0$subscriptDigit$digits"
    }
    return String.format(Locale.US, "$%.6f", price)
}

@Composable
fun LaunchpadDexBadge(
    coin: MemeCoin,
    modifier: Modifier = Modifier
) {
    val chainLower = coin.chain.lowercase()
    val dexIdLower = coin.dexId.lowercase()
    val contractLower = coin.contractAddress.lowercase()

    val isRobinhood = chainLower == "robinhood" || dexIdLower == "robinhood"

    if (isRobinhood) {
        Box(
            modifier = modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(Color(0xFF00C805))
                .border(1.dp, Color(0xFF0D111C), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Eco,
                contentDescription = "Robinhood",
                tint = Color.White,
                modifier = Modifier.size(10.dp)
            )
        }
        return
    }

    val (logoUrl, fallbackBg, fallbackText) = when {
        // Pump.fun on Solana
        dexIdLower.contains("pump") || (chainLower == "solana" && contractLower.endsWith("pump")) ->
            Triple("https://dd.dexscreener.com/ds-data/dexes/pumpfun.png", Color(0xFF00C853), "P")

        // Raydium
        dexIdLower.contains("raydium") || dexIdLower == "ray" ->
            Triple("https://dd.dexscreener.com/ds-data/dexes/raydium.png", Color(0xFF2B52B0), "R")

        // Meteora
        dexIdLower.contains("meteora") ->
            Triple("https://dd.dexscreener.com/ds-data/dexes/meteora.png", Color(0xFFE2553B), "M")

        // Moonshot
        dexIdLower.contains("moonshot") ->
            Triple("https://dd.dexscreener.com/ds-data/dexes/moonshot.png", Color(0xFF7000FF), "M")

        // Orca
        dexIdLower.contains("orca") ->
            Triple("https://dd.dexscreener.com/ds-data/dexes/orca.png", Color(0xFFFFD54F), "O")

        // Aerodrome (Base)
        dexIdLower.contains("aerodrome") ->
            Triple("https://dd.dexscreener.com/ds-data/dexes/aerodrome.png", Color(0xFF0052FF), "A")

        // PancakeSwap (BSC)
        dexIdLower.contains("pancakeswap") || dexIdLower.contains("cake") ->
            Triple("https://dd.dexscreener.com/ds-data/dexes/pancakeswap.png", Color(0xFF00C49F), "P")

        // Uniswap
        dexIdLower.contains("uniswap") ->
            Triple("https://dd.dexscreener.com/ds-data/dexes/uniswap.png", Color(0xFF882255), "U")

        // QuickSwap
        dexIdLower.contains("quickswap") ->
            Triple("https://dd.dexscreener.com/ds-data/dexes/quickswap.png", Color(0xFF2799F9), "Q")

        // Chain fallback icons
        chainLower == "ethereum" -> Triple("https://dd.dexscreener.com/ds-data/chains/ethereum.png", Color(0xFF627EEA), "E")
        chainLower == "base" -> Triple("https://dd.dexscreener.com/ds-data/chains/base.png", Color(0xFF0052FF), "B")
        chainLower == "bsc" || chainLower == "bnb" -> Triple("https://dd.dexscreener.com/ds-data/chains/bsc.png", Color(0xFFF3BA2F), "B")
        chainLower == "arbitrum" -> Triple("https://dd.dexscreener.com/ds-data/chains/arbitrum.png", Color(0xFF28A0F0), "A")
        chainLower == "polygon" -> Triple("https://dd.dexscreener.com/ds-data/chains/polygon.png", Color(0xFF8247E5), "P")
        chainLower == "solana" -> Triple("https://dd.dexscreener.com/ds-data/chains/solana.png", Color(0xFF14F195), "S")

        else -> Triple("https://dd.dexscreener.com/ds-data/chains/solana.png", Color(0xFF14F195), "S")
    }

    Box(
        modifier = modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(Color(0xFF0D111C))
            .border(1.dp, Color(0xFF222B3D), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        var isError by remember(logoUrl) { mutableStateOf(false) }

        if (!isError) {
            AsyncImage(
                model = logoUrl,
                contentDescription = "${coin.dexId} badge",
                contentScale = ContentScale.Crop,
                onError = { isError = true },
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(fallbackBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = fallbackText,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun TokenBannerHeader(
    bannerUrl: String?,
    name: String,
    modifier: Modifier = Modifier
) {
    var isError by remember(bannerUrl) { mutableStateOf(false) }
    if (!bannerUrl.isNullOrEmpty() && !isError) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
        ) {
            AsyncImage(
                model = bannerUrl,
                contentDescription = "$name banner",
                onError = { isError = true },
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
                                DexCardBg.copy(alpha = 0.95f)
                            )
                        )
                    )
            )
        }
    }
}

