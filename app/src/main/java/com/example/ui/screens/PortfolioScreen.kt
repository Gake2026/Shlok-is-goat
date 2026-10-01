package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PriceDown
import com.example.ui.theme.PriceUp
import com.example.ui.theme.SolanaCyan
import com.example.ui.theme.SolanaGreen
import com.example.ui.theme.SolanaPurple
import com.example.ui.theme.SolanaSurface
import com.example.ui.utils.formatPrice
import com.example.ui.viewmodel.DetailedHolding
import com.example.ui.viewmodel.TradingViewModel

@Composable
fun PortfolioScreen(
    viewModel: TradingViewModel,
    onNavigateToMarkets: () -> Unit,
    onNavigateToCoin: (String) -> Unit,
    onNavigateToTrade: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val summary by viewModel.computedPortfolioValue.collectAsState()
    val activeOrders by viewModel.activePendingOrders.collectAsState()
    val coins by viewModel.coins.collectAsState()
    val transactions by viewModel.transactions.collectAsState()

    val risk = androidx.compose.runtime.remember(summary) { viewModel.calculatePortfolioRisk(summary) }
    val calendarDays = androidx.compose.runtime.remember(transactions) { viewModel.calculateProfitCalendar(transactions) }

    var showResetDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = PriceDown
                )
            },
            title = {
                Text(
                    text = "Reset Solana Portfolio Balance?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This action will reset your paper wallet back to the default starting capital of 10.0 SOL, cancel all active limit orders, and clear your coin holdings and trade history.\n\nAre you sure you want to proceed?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetPaperTrading()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PriceDown)
                ) {
                    Text("Confirm Reset", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showResetDialog = false }
                ) {
                    Text("Cancel")
                }
            },
            containerColor = SolanaSurface
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // --- 1. HERO NET WORTH BANNER WITH LIVE CHART ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SolanaSurface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    SolanaPurple.copy(alpha = 0.45f),
                                    SolanaSurface
                                )
                            )
                        )
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Row: Label & Reset Wallet Action
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(SolanaGreen)
                            )
                            Text(
                                text = "SOLANA PORTFOLIO DASHBOARD",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = { showResetDialog = true },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset Wallet",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Main Portfolio Value Display
                    Column {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = String.format("$%,.2f", summary.totalValueUsd),
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 34.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "USD",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = SolanaCyan
                                ),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }

                        Row(
                            modifier = Modifier.padding(top = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = String.format("≈ %.4f SOL", summary.totalValueSol),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Overall ROI Pill
                            val isPos = summary.netProfitLossSol >= 0
                            val colorAccent = if (isPos) PriceUp else PriceDown
                            val sign = if (isPos) "+" else ""

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = colorAccent.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPos) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                        contentDescription = null,
                                        tint = colorAccent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = String.format("%s%.2f%% Total ROI (%s$%,.2f)", sign, summary.roiPercent, sign, summary.totalProfitUsd),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = colorAccent
                                    )
                                }
                            }
                        }
                    }

                    // Live Interactive Line Chart
                    LivePortfolioChart(
                        pointsUsd = summary.chartPointsUsd,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                    )
                }
            }
        }

        // --- 2. CORE FINANCIAL METRICS (Wallet Balance, Available SOL, Today Profit, Total Profit) ---
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "CORE FINANCIAL SUMMARY",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.Bold
                )

                // Row 1: Wallet Balance & Available SOL
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricBox(
                        title = "Wallet Balance",
                        primaryValue = String.format("$%,.2f", summary.walletValueUsd),
                        secondaryValue = String.format("%.4f SOL", summary.solBalance),
                        icon = Icons.Default.AccountBalanceWallet,
                        iconTint = SolanaGreen,
                        modifier = Modifier.weight(1f)
                    )

                    MetricBox(
                        title = "Available SOL",
                        primaryValue = String.format("%.4f SOL", summary.availableSol),
                        secondaryValue = String.format("$%,.2f USD", summary.availableSol * summary.solPriceUsd),
                        icon = Icons.Default.AccountBalanceWallet,
                        iconTint = SolanaCyan,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 2: Today's Profit & Total Profit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val isTodayPos = summary.todayProfitSol >= 0
                    val todaySign = if (isTodayPos) "+" else ""
                    MetricBox(
                        title = "Today's Profit",
                        primaryValue = String.format("%s$%,.2f", todaySign, summary.todayProfitUsd),
                        secondaryValue = String.format("%s%.2f%% (%s%.4f SOL)", todaySign, summary.todayProfitPercent, todaySign, summary.todayProfitSol),
                        icon = Icons.Default.AutoGraph,
                        iconTint = if (isTodayPos) PriceUp else PriceDown,
                        primaryColor = if (isTodayPos) PriceUp else PriceDown,
                        modifier = Modifier.weight(1f)
                    )

                    val isTotalPos = summary.totalProfitSol >= 0
                    val totalSign = if (isTotalPos) "+" else ""
                    MetricBox(
                        title = "Total Profit",
                        primaryValue = String.format("%s$%,.2f", totalSign, summary.totalProfitUsd),
                        secondaryValue = String.format("%s%.4f SOL", totalSign, summary.totalProfitSol),
                        icon = Icons.Default.TrendingUp,
                        iconTint = if (isTotalPos) PriceUp else PriceDown,
                        primaryColor = if (isTotalPos) PriceUp else PriceDown,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // --- 3. PnL & TRADING STATS (Unrealized Profit, Realized Profit, ROI, Win Rate) ---
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "PnL & TRADING PERFORMANCE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.Bold
                )

                // Row 1: Unrealized & Realized Profit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val isUnrealizedPos = summary.totalUnrealizedProfitSol >= 0
                    val unSign = if (isUnrealizedPos) "+" else ""
                    MetricBox(
                        title = "Unrealized Profit",
                        primaryValue = String.format("%s$%,.2f", unSign, summary.totalUnrealizedProfitUsd),
                        secondaryValue = String.format("%s%.4f SOL", unSign, summary.totalUnrealizedProfitSol),
                        icon = Icons.Default.QueryStats,
                        iconTint = if (isUnrealizedPos) PriceUp else PriceDown,
                        primaryColor = if (isUnrealizedPos) PriceUp else PriceDown,
                        modifier = Modifier.weight(1f)
                    )

                    val isRealizedPos = summary.totalRealizedProfitSol >= 0
                    val reSign = if (isRealizedPos) "+" else ""
                    MetricBox(
                        title = "Realized Profit",
                        primaryValue = String.format("%s$%,.2f", reSign, summary.totalRealizedProfitUsd),
                        secondaryValue = String.format("%s%.4f SOL", reSign, summary.totalRealizedProfitSol),
                        icon = Icons.Default.AccountBalanceWallet,
                        iconTint = if (isRealizedPos) PriceUp else PriceDown,
                        primaryColor = if (isRealizedPos) PriceUp else PriceDown,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 2: ROI & Win Rate
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val isRoiPos = summary.roiPercent >= 0
                    val roiSign = if (isRoiPos) "+" else ""
                    MetricBox(
                        title = "ROI (Return on Investment)",
                        primaryValue = String.format("%s%.2f%%", roiSign, summary.roiPercent),
                        secondaryValue = if (isRoiPos) "Profitable Strategy" else "Capital Drawdown",
                        icon = Icons.Default.Speed,
                        iconTint = if (isRoiPos) PriceUp else PriceDown,
                        primaryColor = if (isRoiPos) PriceUp else PriceDown,
                        modifier = Modifier.weight(1f)
                    )

                    MetricBox(
                        title = "Win Rate",
                        primaryValue = String.format("%.1f%%", summary.winRatePercent),
                        secondaryValue = if (summary.closedTradesCount > 0) "${summary.winningTradesCount}/${summary.closedTradesCount} profitable sells" else "No closed trades",
                        icon = Icons.Default.EmojiEvents,
                        iconTint = SolanaCyan,
                        primaryColor = SolanaCyan,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // --- 4. ADVANCED INSIGHTS (Total Trades, Avg Hold Time, Biggest Winner, Biggest Loser) ---
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "INSIGHTS & RECORD HIGHLIGHTS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricBox(
                        title = "Total Trades",
                        primaryValue = "${summary.totalTradesCount} Executions",
                        secondaryValue = "Includes Limit & Market",
                        icon = Icons.Default.SwapHoriz,
                        iconTint = SolanaPurple,
                        modifier = Modifier.weight(1f)
                    )

                    MetricBox(
                        title = "Average Hold Time",
                        primaryValue = summary.avgHoldTimeFormatted,
                        secondaryValue = "Per token position",
                        icon = Icons.Default.Timer,
                        iconTint = SolanaGreen,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val winner = summary.biggestWinner
                    MetricBox(
                        title = "Biggest Winner",
                        primaryValue = if (winner != null) "$${winner.symbol}" else "N/A",
                        secondaryValue = if (winner != null) String.format("+$%,.2f (+%.1f%%)", winner.profitUsd, winner.roiPercent) else "No winning position",
                        icon = Icons.Default.TrendingUp,
                        iconTint = PriceUp,
                        primaryColor = PriceUp,
                        modifier = Modifier.weight(1f)
                    )

                    val loser = summary.biggestLoser
                    MetricBox(
                        title = "Biggest Loser",
                        primaryValue = if (loser != null) "${loser.symbol}" else "N/A",
                        secondaryValue = if (loser != null) String.format("-$%,.2f (%.1f%%)", kotlin.math.abs(loser.profitUsd), loser.roiPercent) else "No losing position",
                        icon = Icons.Default.TrendingDown,
                        iconTint = PriceDown,
                        primaryColor = PriceDown,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // --- 5. ASSET ALLOCATION BREAKDOWN CHART ---
        item {
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
                                imageVector = Icons.Default.PieChart,
                                contentDescription = null,
                                tint = SolanaCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Asset Allocation",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Text(
                            text = String.format("$%,.2f Total", summary.totalValueUsd),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    val solPercent = if (summary.totalValueUsd > 0) (summary.walletValueUsd / summary.totalValueUsd * 100).toFloat() else 100f
                    val tokenPercent = (100f - solPercent).coerceAtLeast(0f)

                    // Allocation Segmented Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        if (solPercent > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(solPercent.coerceAtLeast(0.01f))
                                    .background(SolanaGreen)
                            )
                        }
                        if (tokenPercent > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(tokenPercent.coerceAtLeast(0.01f))
                                    .background(SolanaPurple)
                            )
                        }
                    }

                    // Allocation Legend
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(SolanaGreen))
                            Text(
                                text = String.format("SOL Balance: %.1f%% ($%,.2f)", solPercent, summary.walletValueUsd),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(SolanaPurple))
                            Text(
                                text = String.format("Meme Coins: %.1f%% ($%,.2f)", tokenPercent, summary.holdingsValueUsd),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // --- PORTFOLIO HEATMAP ---
        item {
            PortfolioHeatmapCard(
                holdings = summary.detailedHoldings,
                coins = coins,
                onNavigateToCoin = onNavigateToCoin
            )
        }

        // --- PORTFOLIO RISK METER ---
        item {
            RiskMeterCard(risk = risk)
        }

        // --- PROFIT CALENDAR ---
        item {
            ProfitCalendarCard(calendarDays = calendarDays)
        }


        // --- 6. SECTION HEADER: ACTIVE POSITIONS ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active Token Positions",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (summary.detailedHoldings.isNotEmpty()) {
                    Text(
                        text = "${summary.detailedHoldings.size} position(s)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // --- 7. LIST OF HOLDINGS ---
        if (summary.detailedHoldings.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "No active positions",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Buy top Solana meme coins with live simulated DEX execution!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = onNavigateToMarkets,
                            colors = ButtonDefaults.buttonColors(containerColor = SolanaPurple)
                        ) {
                            Text("Explore Meme Coin Markets")
                        }
                    }
                }
            }
        } else {
            items(summary.detailedHoldings) { detailed ->
                HoldingItemRow(
                    detailed = detailed,
                    onClick = { onNavigateToCoin(detailed.holding.coinSymbol) },
                    onTradeClick = { onNavigateToTrade(detailed.holding.coinSymbol, "SELL") }
                )
            }
        }

        // --- 8. ACTIVE PENDING ORDERS (Limit, Stop Loss, Take Profit) ---
        if (activeOrders.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Active Pending Orders",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${activeOrders.size} order(s)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SolanaCyan
                    )
                }
            }

            items(activeOrders) { order ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SolanaSurface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = order.coinName,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "(${order.coinSymbol})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            val (typeLabel, typeColor) = when (order.orderType) {
                                "LIMIT_BUY" -> "Limit Buy" to PriceUp
                                "LIMIT_SELL" -> "Limit Sell" to PriceDown
                                "STOP_LOSS" -> "Stop Loss" to PriceDown
                                "TAKE_PROFIT" -> "Take Profit" to SolanaGreen
                                else -> order.orderType to MaterialTheme.colorScheme.onSurface
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = typeLabel,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = typeColor
                                )
                                Text(
                                    text = "Target: $${String.format("%.6f", order.targetPriceUsd)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SolanaCyan
                                )
                            }

                            val amountText = if (order.solAmount > 0) String.format("%.4f SOL", order.solAmount) else String.format("%,.2f %s", order.tokenAmount, order.coinSymbol)
                            Text(
                                text = "Amount: $amountText",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = { viewModel.cancelPendingOrder(order.orderId) },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(PriceDown.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel Order",
                                tint = PriceDown,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricBox(
    title: String,
    primaryValue: String,
    secondaryValue: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    primaryColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(102.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SolanaSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconTint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Column {
                Text(
                    text = primaryValue,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = primaryColor,
                    fontSize = 15.sp
                )
                Text(
                    text = secondaryValue,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun LivePortfolioChart(
    pointsUsd: List<Float>,
    modifier: Modifier = Modifier
) {
    if (pointsUsd.size < 2) return

    val minVal = pointsUsd.minOrNull() ?: 0f
    val maxVal = pointsUsd.maxOrNull() ?: 1f
    val firstVal = pointsUsd.first()
    val lastVal = pointsUsd.last()

    val isFlat = (maxVal - minVal) < 0.05f || pointsUsd.all { kotlin.math.abs(it - firstVal) < 0.05f }

    val lineColor = when {
        isFlat -> SolanaCyan
        lastVal >= firstVal -> PriceUp
        else -> PriceDown
    }
    val gradientTop = lineColor.copy(alpha = 0.35f)
    val gradientBottom = Color.Transparent

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val stepX = width / (pointsUsd.size - 1)

        val strokePath = Path()
        val fillPath = Path()

        if (isFlat) {
            val y = height / 2f
            strokePath.moveTo(0f, y)
            strokePath.lineTo(width, y)

            fillPath.moveTo(0f, height)
            fillPath.lineTo(0f, y)
            fillPath.lineTo(width, y)
            fillPath.lineTo(width, height)
            fillPath.close()
        } else {
            val range = (maxVal - minVal).coerceAtLeast(0.01f)
            pointsUsd.forEachIndexed { index, valUsd ->
                val x = index * stepX
                val normalizedY = (valUsd - minVal) / range
                val y = height - (normalizedY * (height * 0.7f) + height * 0.15f)

                if (index == 0) {
                    strokePath.moveTo(x, y)
                    fillPath.moveTo(x, height)
                    fillPath.lineTo(x, y)
                } else {
                    val prevX = (index - 1) * stepX
                    val prevValUsd = pointsUsd[index - 1]
                    val prevNormalizedY = (prevValUsd - minVal) / range
                    val prevY = height - (prevNormalizedY * (height * 0.7f) + height * 0.15f)

                    val controlX1 = prevX + (x - prevX) / 2f
                    val controlY1 = prevY
                    val controlX2 = prevX + (x - prevX) / 2f
                    val controlY2 = y

                    strokePath.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
                    fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
                }
            }
            fillPath.lineTo(width, height)
            fillPath.close()
        }

        // 1. Fill gradient under curve
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(gradientTop, gradientBottom),
                startY = 0f,
                endY = height
            ),
            style = Fill
        )

        // 2. Draw glowing line
        drawPath(
            path = strokePath,
            color = lineColor,
            style = Stroke(width = 3.dp.toPx())
        )

        // 3. Draw live endpoint pulse dot
        val lastX = width
        val lastY = if (isFlat) height / 2f else {
            val range = (maxVal - minVal).coerceAtLeast(0.01f)
            val normalizedY = (lastVal - minVal) / range
            height - (normalizedY * (height * 0.7f) + height * 0.15f)
        }

        drawCircle(
            color = lineColor.copy(alpha = 0.3f),
            radius = 8.dp.toPx(),
            center = androidx.compose.ui.geometry.Offset(lastX, lastY)
        )
        drawCircle(
            color = lineColor,
            radius = 4.dp.toPx(),
            center = androidx.compose.ui.geometry.Offset(lastX, lastY)
        )
    }
}

@Composable
fun HoldingItemRow(
    detailed: DetailedHolding,
    onClick: () -> Unit,
    onTradeClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SolanaSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: Avatar, Name, Current Value, Trade Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SolanaPurple.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = detailed.holding.coinSymbol.take(3),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = SolanaCyan
                        )
                    }

                    Column {
                        Text(
                            text = detailed.holding.coinName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${detailed.holding.coinSymbol} • ${String.format("%,.2f", detailed.holding.amount)} owned",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = String.format("%.4f SOL", detailed.currentValueSol),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = String.format("$%,.2f", detailed.currentValueUsd),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onTradeClick,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SolanaPurple.copy(alpha = 0.25f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Trade",
                            tint = SolanaCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

            // Sub-metrics Grid: Market Cap, Live Price, Unrealized PnL (ROI)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Market Cap", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = formatMarketCapUsd(detailed.marketCapUsd),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = formatMarketCapSol(detailed.marketCapUsd, detailed.currentPriceUsd, detailed.currentPriceSol),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Live Price", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = formatPrice(detailed.currentPriceUsd),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = String.format("%.6f SOL", detailed.currentPriceSol),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Unrealized PnL (ROI)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val isPos = detailed.unrealizedProfitSol >= 0
                    val col = if (isPos) PriceUp else PriceDown
                    val sign = if (isPos) "+" else ""
                    Text(
                        text = String.format("%s%.4f SOL", sign, detailed.unrealizedProfitSol),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = col
                    )
                    Text(
                        text = String.format("%s%.2f%%", sign, detailed.unrealizedRoiPercent),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = col
                    )
                }
            }
        }
    }
}

@Composable
fun PortfolioHeatmapCard(
    holdings: List<DetailedHolding>,
    coins: List<com.example.data.model.MemeCoin>,
    onNavigateToCoin: (String) -> Unit
) {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.QueryStats, contentDescription = null, tint = SolanaCyan, modifier = Modifier.size(20.dp))
                    Text(
                        text = "Portfolio Heatmap",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Text(
                    text = "${holdings.size} Active Positions",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (holdings.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x0AFFFFFF)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No active holdings. Buy tokens to populate heatmap.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val totalValue = holdings.sumOf { it.currentValueUsd }.coerceAtLeast(0.01)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    holdings.forEach { holding ->
                        val priceChange = holding.unrealizedRoiPercent
                        val isUp = priceChange >= 0
                        val tileBg = if (isUp) PriceUp.copy(alpha = 0.25f) else PriceDown.copy(alpha = 0.25f)
                        val tileBorder = if (isUp) PriceUp else PriceDown
                        val weight = (holding.currentValueUsd / totalValue).toFloat().coerceAtLeast(0.1f)

                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(weight)
                                .clip(RoundedCornerShape(8.dp))
                                .background(tileBg)
                                .border(1.dp, tileBorder.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .clickable { onNavigateToCoin(holding.holding.coinSymbol) }
                                .padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = holding.holding.coinSymbol,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = String.format("%s%.1f%%", if (isUp) "+" else "", priceChange),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = tileBorder
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RiskMeterCard(
    risk: com.example.ui.viewmodel.PortfolioRiskAnalysis
) {
    val categoryColor = when (risk.category) {
        "CONSERVATIVE" -> SolanaGreen
        "BALANCED" -> SolanaCyan
        "HIGH RISK" -> SolanaPurple
        else -> PriceDown
    }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Speed, contentDescription = null, tint = SolanaPurple, modifier = Modifier.size(20.dp))
                    Text(
                        text = "Portfolio Risk Meter",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = categoryColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = risk.category,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = categoryColor
                    )
                }
            }

            // Score gauge bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Risk Score", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "${risk.score} / 100", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = categoryColor)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color(0x1AFFFFFF))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth((risk.score / 100f).coerceIn(0.05f, 1f))
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(SolanaGreen, SolanaCyan, SolanaPurple, PriceDown)
                                )
                            )
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Cash (SOL) Reserve", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = String.format("%.1f%%", risk.solRatioPercent), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Max Token Weight", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = String.format("%.1f%%", risk.topTokenConcentrationPercent), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
            }

            Text(
                text = risk.recommendation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ProfitCalendarCard(
    calendarDays: List<com.example.ui.viewmodel.CalendarDayPnL>
) {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Schedule, contentDescription = null, tint = SolanaGreen, modifier = Modifier.size(20.dp))
                    Text(
                        text = "14-Day Profit Calendar",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                val totalCalendarPnL = calendarDays.sumOf { it.totalPnLUsd }
                val isPos = totalCalendarPnL >= 0
                Text(
                    text = String.format("%s$%,.2f 14d PnL", if (isPos) "+" else "", totalCalendarPnL),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isPos) PriceUp else PriceDown
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val rows = calendarDays.chunked(7)
                rows.forEach { rowDays ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        rowDays.forEach { day ->
                            val hasTrades = day.tradeCount > 0
                            val isProfitable = day.totalPnLUsd > 0
                            val isLoss = day.totalPnLUsd < 0
                            val bg = when {
                                isProfitable -> PriceUp.copy(alpha = 0.25f)
                                isLoss -> PriceDown.copy(alpha = 0.25f)
                                else -> Color(0x0DFFFFFF)
                            }
                            val border = when {
                                isProfitable -> PriceUp.copy(alpha = 0.6f)
                                isLoss -> PriceDown.copy(alpha = 0.6f)
                                else -> Color(0x1AFFFFFF)
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(bg)
                                    .border(1.dp, border, RoundedCornerShape(8.dp))
                                    .padding(2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "${day.dayOfMonth}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (hasTrades) {
                                        Text(
                                            text = if (isProfitable) "+$${day.totalPnLUsd.toInt()}" else "-$${kotlin.math.abs(day.totalPnLUsd.toInt())}",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isProfitable) PriceUp else PriceDown
                                        )
                                    } else {
                                        Text(text = "—", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatMarketCapUsd(mc: Double): String {
    return when {
        mc >= 1_000_000_000.0 -> String.format(java.util.Locale.US, "$%.2fB", mc / 1_000_000_000.0)
        mc >= 1_000_000.0 -> String.format(java.util.Locale.US, "$%.2fM", mc / 1_000_000.0)
        mc >= 1_000.0 -> String.format(java.util.Locale.US, "$%.1fK", mc / 1_000.0)
        mc > 0 -> String.format(java.util.Locale.US, "$%,.2f", mc)
        else -> "$0.00"
    }
}

private fun formatMarketCapSol(mcUsd: Double, currentPriceUsd: Double, currentPriceSol: Double): String {
    val solPrice = if (currentPriceSol > 0) currentPriceUsd / currentPriceSol else 185.0
    val mcSol = if (solPrice > 0) mcUsd / solPrice else 0.0
    return when {
        mcSol >= 1_000_000.0 -> String.format(java.util.Locale.US, "%,.1fM SOL", mcSol / 1_000_000.0)
        mcSol >= 1_000.0 -> String.format(java.util.Locale.US, "%,.1fK SOL", mcSol / 1_000.0)
        mcSol > 0 -> String.format(java.util.Locale.US, "%,.2f SOL", mcSol)
        else -> "0 SOL"
    }
}

