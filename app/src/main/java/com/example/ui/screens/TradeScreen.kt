package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PriceDown
import com.example.ui.theme.PriceUp
import com.example.ui.theme.SolanaCyan
import com.example.ui.theme.SolanaGreen
import com.example.ui.theme.SolanaPurple
import com.example.ui.theme.SolanaSurface
import com.example.ui.components.TraderPnLDialog
import com.example.data.local.TradeTransaction
import com.example.ui.viewmodel.TradeResultEvent
import com.example.ui.viewmodel.TradingViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradeScreen(
    symbol: String,
    viewModel: TradingViewModel,
    onNavigateBack: () -> Unit,
    initialMode: String = "BUY",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coins by viewModel.coins.collectAsState()
    val holdings by viewModel.holdings.collectAsState()
    val summary by viewModel.computedPortfolioValue.collectAsState()
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
    } ?: com.example.data.repository.ROBINHOOD_TOKENS.find { 
        it.symbol.equals(symbol, ignoreCase = true) || 
        it.id.equals(symbol, ignoreCase = true) || 
        it.contractAddress.equals(symbol, ignoreCase = true) ||
        it.name.equals(symbol, ignoreCase = true)
    }
    val currentHolding = holdings.find { it.coinSymbol.equals(symbol, ignoreCase = true) }

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
                    text = "Preparing $symbol trading order...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    val activeOrders by viewModel.activePendingOrders.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val coinPendingOrders = activeOrders.filter { it.coinSymbol.equals(symbol, ignoreCase = true) }

    var isBuyMode by remember(initialMode) { mutableStateOf(initialMode != "SELL") }
    var selectedOrderType by remember { mutableStateOf("MARKET") } // "MARKET", "LIMIT", "STOP_LOSS", "TAKE_PROFIT"
    var inputAmountStr by remember { mutableStateOf("") }
    var targetPriceStr by remember { mutableStateOf("") }
    
    // Trade confirmation popup state
    var completedTradeEvent by remember { mutableStateOf<TradeResultEvent.Success?>(null) }

    val safeSolPriceUsd = if (solPriceUsd > 0) solPriceUsd else 185.0
    val liveTokenPriceUsd = coin.currentPrice
    val liveTokenPriceSol = if (safeSolPriceUsd > 0) liveTokenPriceUsd / safeSolPriceUsd else 0.0

    // Auto update target price when order type or coin changes
    LaunchedEffect(key1 = coin.currentPrice, key2 = selectedOrderType) {
        if (targetPriceStr.isEmpty() || targetPriceStr.toDoubleOrNull() == null) {
            targetPriceStr = String.format("%.6f", coin.currentPrice)
        }
    }

    val selectedCongestion by viewModel.selectedCongestion.collectAsState()

    val inputAmount = inputAmountStr.toDoubleOrNull() ?: 0.0
    val targetPriceUsd = targetPriceStr.toDoubleOrNull() ?: liveTokenPriceUsd
    val executionPriceUsdForEstimate = if (selectedOrderType == "MARKET") liveTokenPriceUsd else targetPriceUsd

    // Calculations for SELL gross SOL value
    val sellGrossUsd = inputAmount * executionPriceUsdForEstimate
    val sellGrossSol = if (safeSolPriceUsd > 0) sellGrossUsd / safeSolPriceUsd else 0.0

    // Realistic Solana Fee Simulation via Fee Calculator
    val feeDetails = com.example.data.util.SolanaFeeCalculator.calculateFees(
        solAmount = if (isBuyMode) inputAmount else sellGrossSol,
        congestion = selectedCongestion
    )

    val networkFeeSol = feeDetails.networkFeeSol      // Fixed 0.000005 SOL
    val priorityFeeSol = feeDetails.priorityFeeSol    // Compute unit priority fee
    val gasFeeSol = feeDetails.gasFeeSol              // Network Fee + Priority Fee
    val platformFeeSol = feeDetails.platformFeeSol    // Platform Fee (0.25%)
    val totalFeeSol = feeDetails.totalFeeSol          // Gas Fee + Platform Fee

    // Calculations for BUY
    val buyEstimatedTokens = if (executionPriceUsdForEstimate > 0) (inputAmount * safeSolPriceUsd) / executionPriceUsdForEstimate else 0.0
    val buyTotalDeductedSol = inputAmount + totalFeeSol

    // Calculations for SELL
    val sellNetSolProceeds = maxOf(0.0, sellGrossSol - totalFeeSol)
    
    val currentOwnedTokens = currentHolding?.amount ?: 0.0
    val soldRatio = if (currentOwnedTokens > 0) (inputAmount / currentOwnedTokens).coerceIn(0.0, 1.0) else 0.0
    val estimatedCostBasisSol = (currentHolding?.totalInvestedSol ?: 0.0) * soldRatio
    val estimatedRealizedPnlSol = sellNetSolProceeds - estimatedCostBasisSol
    val estimatedRealizedPnlUsd = estimatedRealizedPnlSol * safeSolPriceUsd

    // Listen to transaction results for feedback
    LaunchedEffect(key1 = viewModel) {
        viewModel.tradeEventFlow.collectLatest { event ->
            when (event) {
                is TradeResultEvent.Success -> {
                    completedTradeEvent = event
                    inputAmountStr = "" // clear inputs on success
                }
                is TradeResultEvent.Error -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Trade Completion Modal with PnL Card (ONLY shown when token is SOLD)
    completedTradeEvent?.let { tradeSuccess ->
        val matchingTx = transactions.find { it.tradeId == tradeSuccess.tradeId }
            ?: transactions.firstOrNull { it.coinSymbol.equals(coin.symbol, ignoreCase = true) }
            ?: TradeTransaction(
                tradeId = tradeSuccess.tradeId,
                coinSymbol = coin.symbol,
                coinName = coin.name,
                type = if (isBuyMode) "BUY" else "SELL",
                solAmount = inputAmount,
                tokenAmount = if (liveTokenPriceSol > 0) inputAmount / liveTokenPriceSol else 0.0,
                executionPriceUsd = liveTokenPriceUsd,
                executionPriceSol = liveTokenPriceSol,
                solPriceUsd = safeSolPriceUsd,
                avgEntryPriceSol = liveTokenPriceSol,
                txHash = tradeSuccess.txHash,
                timestamp = System.currentTimeMillis()
            )

        val isSellTrade = matchingTx.type.equals("SELL", ignoreCase = true)

        if (isSellTrade) {
            TraderPnLDialog(
                transaction = matchingTx,
                coin = coin,
                onDismiss = { completedTradeEvent = null }
            )
        } else {
            // For BUY trades, show confirmation toast without PnL card
            LaunchedEffect(tradeSuccess) {
                Toast.makeText(
                    context,
                    "Order Filled: Bought ${matchingTx.coinSymbol.uppercase()} successfully!",
                    Toast.LENGTH_SHORT
                ).show()
                completedTradeEvent = null
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Exchange Fill: ${coin.symbol}") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
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
            // --- 1. BUY vs SELL SELECTOR ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SolanaSurface)
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isBuyMode) PriceUp else Color.Transparent)
                        .clickable {
                            isBuyMode = true
                            inputAmountStr = ""
                            selectedOrderType = "MARKET"
                        }
                        .testTag("trade_buy_tab_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "BUY WITH SOL",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isBuyMode) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (!isBuyMode) PriceDown else Color.Transparent)
                        .clickable {
                            isBuyMode = false
                            inputAmountStr = ""
                            selectedOrderType = "MARKET"
                        }
                        .testTag("trade_sell_tab_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "SELL TOKEN",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (!isBuyMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // --- 1B. ORDER TYPE SELECTOR CHIPS ---
            Text(
                text = "Order Type",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val availableTypes = if (isBuyMode) {
                    listOf("MARKET" to "Market Buy", "LIMIT" to "Limit Buy")
                } else {
                    listOf(
                        "MARKET" to "Market Sell",
                        "LIMIT" to "Limit Sell",
                        "STOP_LOSS" to "Stop Loss",
                        "TAKE_PROFIT" to "Take Profit"
                    )
                }

                availableTypes.forEach { (typeKey, typeLabel) ->
                    val isSelected = selectedOrderType == typeKey
                    val activeColor = if (isBuyMode) PriceUp else PriceDown
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedOrderType = typeKey
                            targetPriceStr = String.format("%.6f", coin.currentPrice)
                        },
                        label = {
                            Text(
                                text = typeLabel,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = activeColor.copy(alpha = 0.25f),
                            selectedLabelColor = activeColor
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // --- 2. REAL-TIME MARKET & WALLET STATS CARD ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SolanaSurface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Live Market Price", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (coin.currentPrice >= 1.0) String.format("$%,.2f USD", coin.currentPrice)
                                       else if (coin.currentPrice >= 0.01) String.format("$%,.4f USD", coin.currentPrice)
                                       else if (coin.currentPrice >= 0.0001) String.format("$%,.6f USD", coin.currentPrice)
                                       else String.format("$%,.9f USD", coin.currentPrice),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = SolanaCyan
                            )
                            Text(
                                text = String.format("%.8f SOL", liveTokenPriceSol),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = SolanaGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Text("Available Wallet SOL", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        }
                        Text(
                            text = String.format("%.4f SOL", summary.solBalance),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = SolanaGreen
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Your Token Position", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        Text(
                            text = String.format("%,.4f %s", currentOwnedTokens, coin.symbol),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // --- 2B. TARGET EXECUTION PRICE CARD (For Limit / Stop Loss / Take Profit) ---
            if (selectedOrderType != "MARKET") {
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
                        val priceTitle = when (selectedOrderType) {
                            "LIMIT" -> if (isBuyMode) "Target Buy Price (USD)" else "Target Sell Price (USD)"
                            "STOP_LOSS" -> "Stop Loss Trigger Price (USD)"
                            "TAKE_PROFIT" -> "Take Profit Target Price (USD)"
                            else -> "Target Price (USD)"
                        }

                        Text(
                            text = priceTitle,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        OutlinedTextField(
                            value = targetPriceStr,
                            onValueChange = { newValue ->
                                if (newValue.isEmpty() || newValue.toDoubleOrNull() != null || newValue == ".") {
                                    targetPriceStr = newValue
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            placeholder = { Text(String.format("%.6f", coin.currentPrice), fontSize = 20.sp) },
                            textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SolanaCyan,
                                unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                                focusedContainerColor = MaterialTheme.colorScheme.background,
                                unfocusedContainerColor = MaterialTheme.colorScheme.background
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            prefix = { Text("$ ", fontWeight = FontWeight.Bold, color = SolanaCyan) }
                        )

                        // Target Price Percentage Adjustments
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val shortcuts = when (selectedOrderType) {
                                "LIMIT" -> if (isBuyMode) listOf(-5.0, -10.0, -20.0, -50.0) else listOf(5.0, 10.0, 25.0, 50.0)
                                "STOP_LOSS" -> listOf(-5.0, -10.0, -15.0, -25.0)
                                "TAKE_PROFIT" -> listOf(15.0, 25.0, 50.0, 100.0)
                                else -> listOf(5.0, 10.0, 25.0, 50.0)
                            }

                            shortcuts.forEach { pct ->
                                val sign = if (pct > 0) "+" else ""
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        val newTarget = coin.currentPrice * (1.0 + pct / 100.0)
                                        targetPriceStr = if (newTarget >= 1.0) String.format("%.2f", newTarget)
                                                         else if (newTarget >= 0.01) String.format("%.4f", newTarget)
                                                         else String.format("%.7f", newTarget)
                                    },
                                    label = { Text("$sign${pct.toInt()}%", fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Helpful explanation of order condition
                        val conditionExplanation = when {
                            isBuyMode && selectedOrderType == "LIMIT" ->
                                "Order will execute automatically at live market price when ${coin.symbol} drops to or below $${String.format("%.6f", targetPriceUsd)}."
                            !isBuyMode && selectedOrderType == "LIMIT" ->
                                "Order will execute automatically at live market price when ${coin.symbol} rises to or above $${String.format("%.6f", targetPriceUsd)}."
                            selectedOrderType == "STOP_LOSS" ->
                                "Stop Loss triggers automatically at live market price if ${coin.symbol} drops to or below $${String.format("%.6f", targetPriceUsd)}."
                            selectedOrderType == "TAKE_PROFIT" ->
                                "Take Profit triggers automatically at live market price if ${coin.symbol} rises to or above $${String.format("%.6f", targetPriceUsd)}."
                            else -> ""
                        }

                        Text(
                            text = conditionExplanation,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // --- 3. INPUT CARD & PRESET SHORTCUTS ---
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
                    Text(
                        text = if (isBuyMode) "Enter SOL Amount to Spend" else "Enter ${coin.symbol} Quantity to Sell",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    OutlinedTextField(
                        value = inputAmountStr,
                        onValueChange = { newValue ->
                            if (newValue.isEmpty() || newValue.toDoubleOrNull() != null || newValue == ".") {
                                inputAmountStr = newValue
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        placeholder = { Text("0.00", fontSize = 24.sp) },
                        textStyle = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isBuyMode) PriceUp else PriceDown,
                            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.background,
                            unfocusedContainerColor = MaterialTheme.colorScheme.background
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("trade_amount_input"),
                        suffix = {
                            Text(
                                text = if (isBuyMode) "SOL" else coin.symbol,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = SolanaPurple
                            )
                        }
                    )

                    // Quick Preset Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isBuyMode) {
                            listOf(0.1, 0.5, 1.0, 2.5).forEach { preset ->
                                FilterChip(
                                    selected = false,
                                    onClick = { inputAmountStr = preset.toString() },
                                    label = { Text("${preset} SOL", fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            FilterChip(
                                selected = false,
                                onClick = {
                                    val maxSol = maxOf(0.0, summary.solBalance - totalFeeSol)
                                    inputAmountStr = String.format("%.4f", maxSol)
                                },
                                label = { Text("MAX", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            listOf(25, 50, 75, 100).forEach { pct ->
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        if (pct == 100) {
                                            inputAmountStr = if (currentOwnedTokens % 1.0 == 0.0) {
                                                String.format(java.util.Locale.US, "%.0f", currentOwnedTokens)
                                            } else {
                                                String.format(java.util.Locale.US, "%.8f", currentOwnedTokens).trimEnd('0').trimEnd('.')
                                            }
                                        } else {
                                            val amt = currentOwnedTokens * (pct / 100.0)
                                            inputAmountStr = String.format(java.util.Locale.US, "%.4f", amt)
                                        }
                                    },
                                    label = { Text("$pct%", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // --- 4. SOLANA NETWORK CONDITIONS CARD ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SolanaSurface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                imageVector = Icons.Default.LocalGasStation,
                                contentDescription = "Network Fee",
                                tint = SolanaCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Solana Network Conditions",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Text(
                            text = "${selectedCongestion.label} Congestion",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = when (selectedCongestion) {
                                com.example.data.util.SolanaNetworkCongestion.LOW -> SolanaGreen
                                com.example.data.util.SolanaNetworkCongestion.MEDIUM -> SolanaCyan
                                com.example.data.util.SolanaNetworkCongestion.HIGH -> Color(0xFFFFB74D)
                                com.example.data.util.SolanaNetworkCongestion.TURBO -> PriceDown
                            }
                        )
                    }

                    Text(
                        text = selectedCongestion.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        com.example.data.util.SolanaNetworkCongestion.values().forEach { option ->
                            val isSelected = selectedCongestion == option
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setCongestion(option) },
                                label = { Text(option.label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SolanaCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = SolanaCyan
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // --- 5. EXECUTION BREAKDOWN & REALISTIC FEE SIMULATION CARD ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SolanaSurface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (selectedOrderType == "MARKET") "Order Execution & Fee Details" else "Estimated Execution & Fee Details",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (isBuyMode) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Received Quantity", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            Text(
                                text = String.format("%,.4f %s", buyEstimatedTokens, coin.symbol),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = SolanaCyan
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Gross SOL Value", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            Text(
                                text = String.format("%.4f SOL", sellGrossSol),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                    // Mandatory Fee Breakdown Rows: Gas Fee, Platform Fee, Total Fee
                    Text(
                        text = "SOLANA FEE BREAKDOWN",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                        color = SolanaPurple
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("• Network Fee (5k lamports base)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        Text(
                            text = String.format("%.6f SOL", networkFeeSol),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("• Priority Fee (${selectedCongestion.label})", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        Text(
                            text = String.format("%.6f SOL", priorityFeeSol),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Gas Fee (Network + Priority)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), fontSize = 13.sp)
                        Text(
                            text = String.format("%.6f SOL", gasFeeSol),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Platform Fee (0.25% DEX)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), fontSize = 13.sp)
                        Text(
                            text = String.format("%.6f SOL", platformFeeSol),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Fee", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold), fontSize = 14.sp)
                        Text(
                            text = String.format("%.6f SOL", totalFeeSol),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = SolanaCyan
                        )
                    }

                    if (!isBuyMode) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Est. Realized P&L", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            val pnlColor = if (estimatedRealizedPnlSol >= 0) PriceUp else PriceDown
                            val pnlSign = if (estimatedRealizedPnlSol >= 0) "+" else ""
                            Text(
                                text = String.format("%s%.4f SOL (%s$%,.2f)", pnlSign, estimatedRealizedPnlSol, pnlSign, estimatedRealizedPnlUsd),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = pnlColor
                            )
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.surfaceVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isBuyMode) "Total SOL Deducted" else "Net SOL Credited",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (isBuyMode) String.format("%.6f SOL", buyTotalDeductedSol) else String.format("%.6f SOL", sellNetSolProceeds),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = if (isBuyMode) PriceUp else PriceDown)
                        )
                    }
                }
            }

            // --- 5. ACTION SUBMIT BUTTON ---
            val isPending = selectedOrderType != "MARKET"
            val canSubmit = inputAmount > 0 && if (isBuyMode) {
                buyTotalDeductedSol <= summary.solBalance + 0.00001
            } else {
                inputAmount <= currentOwnedTokens + 0.0001
            } && (!isPending || targetPriceUsd > 0)

            val actionButtonColor = if (isBuyMode) PriceUp else PriceDown
            val actionTextColor = if (isBuyMode) Color.Black else Color.White

            Button(
                onClick = {
                    if (selectedOrderType == "MARKET") {
                        if (isBuyMode) {
                            viewModel.executeBuy(coin.symbol, inputAmount)
                        } else {
                            val sellAmount = if (inputAmount >= currentOwnedTokens - 0.0001) currentOwnedTokens else inputAmount
                            viewModel.executeSell(coin.symbol, sellAmount)
                        }
                    } else {
                        val actualType = if (isBuyMode) "LIMIT_BUY" else when (selectedOrderType) {
                            "LIMIT" -> "LIMIT_SELL"
                            "STOP_LOSS" -> "STOP_LOSS"
                            "TAKE_PROFIT" -> "TAKE_PROFIT"
                            else -> "LIMIT_SELL"
                        }
                        val sellTokenAmount = if (!isBuyMode && inputAmount >= currentOwnedTokens - 0.0001) currentOwnedTokens else if (!isBuyMode) inputAmount else 0.0
                        viewModel.placePendingOrder(
                            symbol = coin.symbol,
                            orderType = actualType,
                            targetPriceUsd = targetPriceUsd,
                            solAmount = if (isBuyMode) inputAmount else 0.0,
                            tokenAmount = sellTokenAmount
                        )
                    }
                },
                enabled = canSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
                    .testTag("trade_submit_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = actionButtonColor,
                    contentColor = actionTextColor
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                val buttonLabel = when {
                    selectedOrderType == "MARKET" && isBuyMode ->
                        "Confirm Market Buy (${String.format("%.4f SOL", buyTotalDeductedSol)})"
                    selectedOrderType == "MARKET" && !isBuyMode ->
                        "Confirm Market Sell (${String.format("%.2f %s", inputAmount, coin.symbol)})"
                    selectedOrderType == "LIMIT" && isBuyMode ->
                        "Place Limit Buy @ $${String.format("%.6f", targetPriceUsd)}"
                    selectedOrderType == "LIMIT" && !isBuyMode ->
                        "Place Limit Sell @ $${String.format("%.6f", targetPriceUsd)}"
                    selectedOrderType == "STOP_LOSS" ->
                        "Place Stop Loss @ $${String.format("%.6f", targetPriceUsd)}"
                    selectedOrderType == "TAKE_PROFIT" ->
                        "Place Take Profit @ $${String.format("%.6f", targetPriceUsd)}"
                    else -> "Submit Order"
                }

                Text(
                    text = buttonLabel,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            // --- 6. ACTIVE PENDING ORDERS FOR THIS COIN ---
            if (coinPendingOrders.isNotEmpty()) {
                Text(
                    text = "Active Pending Orders (${coinPendingOrders.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SolanaSurface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        coinPendingOrders.forEach { order ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    val (typeLabel, typeColor) = when (order.orderType) {
                                        "LIMIT_BUY" -> "Limit Buy" to PriceUp
                                        "LIMIT_SELL" -> "Limit Sell" to PriceDown
                                        "STOP_LOSS" -> "Stop Loss" to PriceDown
                                        "TAKE_PROFIT" -> "Take Profit" to SolanaGreen
                                        else -> order.orderType to MaterialTheme.colorScheme.onSurface
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = typeLabel,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = typeColor
                                        )
                                        Text(
                                            text = "Target: $${String.format("%.6f", order.targetPriceUsd)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Cancel Order",
                                        tint = PriceDown
                                    )
                                }
                            }
                            if (order != coinPendingOrders.last()) {
                                HorizontalDivider(color = Color(0x13FFFFFF))
                            }
                        }
                    }
                }
            }

            // Disclaimer about real live execution prices
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Live DexScreener spot price execution. Pending orders trigger automatically when live prices reach target.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}
