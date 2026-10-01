package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.Holding
import com.example.data.local.PaperTraderDatabase
import com.example.data.local.PendingOrder
import com.example.data.local.PortfolioState
import com.example.data.local.TradeTransaction
import com.example.data.model.MemeCoin
import com.example.data.repository.MemeCoinRepository
import com.example.data.repository.PortfolioRepository
import com.example.data.repository.TradeResult
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TradingViewModel(
    private val memeCoinRepository: MemeCoinRepository,
    private val portfolioRepository: PortfolioRepository
) : ViewModel() {

    // 1. Expose coin prices and SOL price in USD
    val coins: StateFlow<List<MemeCoin>> = memeCoinRepository.coinsFlow
    val solPriceUsd: StateFlow<Double> = memeCoinRepository.solPriceUsd

    // Expose repository search query
    val searchQuery: StateFlow<String> = memeCoinRepository.searchQuery

    // Expose network error and loading states
    val isError: StateFlow<Boolean> = memeCoinRepository.isError
    val isLoading: StateFlow<Boolean> = memeCoinRepository.isLoading

    // 2. Expose watchlist
    val watchlist: StateFlow<List<String>> = memeCoinRepository.watchlistFlow
        .map { list -> list.map { it.coinSymbol.uppercase() }.distinct() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 3. Expose raw database state
    val portfolioState: StateFlow<PortfolioState> = portfolioRepository.portfolioStateFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PortfolioState(id = 1, solBalance = 10.0, initialSolBalance = 10.0))

    val holdings: StateFlow<List<Holding>> = portfolioRepository.holdingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<TradeTransaction>> = portfolioRepository.transactionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activePendingOrders: StateFlow<List<PendingOrder>> = portfolioRepository.activePendingOrdersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPendingOrders: StateFlow<List<PendingOrder>> = portfolioRepository.allPendingOrdersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Price alerts flow
    val priceAlerts: StateFlow<List<com.example.data.local.PriceAlert>> = portfolioRepository.priceAlertsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Trade journal entries flow
    val journalEntries: StateFlow<List<com.example.data.local.TradeJournalEntry>> = portfolioRepository.journalEntriesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 4. Expose computed portfolio metrics using live execution prices

    val computedPortfolioValue: StateFlow<PortfolioSummary> = combine(
        portfolioState,
        holdings,
        transactions,
        coins,
        solPriceUsd
    ) { state, holdingsList, txList, coinsList, currentSolPriceUsd ->
        val safeSolPrice = if (currentSolPriceUsd > 0) currentSolPriceUsd else 185.0
        val walletSol = state.solBalance
        val walletUsd = walletSol * safeSolPrice

        val detailedHoldings = holdingsList.map { holding ->
            val liveCoin = coinsList.find { 
                it.symbol.equals(holding.coinSymbol, ignoreCase = true) ||
                (holding.coinSymbol.isNotBlank() && it.contractAddress.equals(holding.coinSymbol, ignoreCase = true)) ||
                (holding.coinSymbol.isNotBlank() && it.id.equals(holding.coinSymbol, ignoreCase = true))
            } ?: memeCoinRepository.getCoinBySymbol(holding.coinSymbol)

            val currentPriceUsd = liveCoin?.currentPrice ?: holding.avgBuyPriceUsd
            val currentPriceSol = if (safeSolPrice > 0) currentPriceUsd / safeSolPrice else 0.0
            val marketCapUsd = liveCoin?.marketCap ?: (currentPriceUsd * 1_000_000_000.0)

            val currentValueUsd = holding.amount * currentPriceUsd
            val currentValueSol = if (safeSolPrice > 0) currentValueUsd / safeSolPrice else 0.0

            val costBasisSol = holding.totalInvestedSol
            val costBasisUsd = costBasisSol * safeSolPrice

            val unrealizedProfitSol = currentValueSol - costBasisSol
            val unrealizedProfitUsd = currentValueUsd - costBasisUsd
            val unrealizedRoiPercent = if (costBasisSol > 0) (unrealizedProfitSol / costBasisSol) * 100 else 0.0

            val totalFeesPaidSol = holding.totalTradingFeesSol + holding.totalGasFeesSol
            val netProfitSol = holding.realizedProfitSol + unrealizedProfitSol - totalFeesPaidSol
            val netProfitUsd = netProfitSol * safeSolPrice

            DetailedHolding(
                holding = holding,
                currentPriceUsd = currentPriceUsd,
                currentPriceSol = currentPriceSol,
                marketCapUsd = marketCapUsd,
                currentValueUsd = currentValueUsd,
                currentValueSol = currentValueSol,
                costBasisSol = costBasisSol,
                costBasisUsd = costBasisUsd,
                unrealizedProfitSol = unrealizedProfitSol,
                unrealizedProfitUsd = unrealizedProfitUsd,
                unrealizedRoiPercent = unrealizedRoiPercent,
                realizedProfitSol = holding.realizedProfitSol,
                realizedProfitUsd = holding.realizedProfitUsd,
                totalTradingFeesSol = holding.totalTradingFeesSol,
                totalGasFeesSol = holding.totalGasFeesSol,
                netProfitSol = netProfitSol,
                netProfitUsd = netProfitUsd
            )
        }

        val holdingsValueSol = detailedHoldings.sumOf { it.currentValueSol }
        val holdingsValueUsd = detailedHoldings.sumOf { it.currentValueUsd }

        val totalValueSol = walletSol + holdingsValueSol
        val totalValueUsd = totalValueSol * safeSolPrice

        val totalRealizedProfitSol = txList.filter { it.type == "SELL" }.sumOf { it.realizedProfitSol ?: 0.0 }
        val totalRealizedProfitUsd = totalRealizedProfitSol * safeSolPrice

        val totalUnrealizedProfitSol = detailedHoldings.sumOf { it.unrealizedProfitSol }
        val totalUnrealizedProfitUsd = totalUnrealizedProfitSol * safeSolPrice

        val totalTradingFeesSol = txList.sumOf { it.tradingFeeSol }
        val totalTradingFeesUsd = totalTradingFeesSol * safeSolPrice

        val totalGasFeesSol = txList.sumOf { it.gasFeeSol }
        val totalGasFeesUsd = totalGasFeesSol * safeSolPrice

        val totalFeesSol = totalTradingFeesSol + totalGasFeesSol
        val totalFeesUsd = totalFeesSol * safeSolPrice

        val startingSol = state.initialSolBalance
        val netProfitLossSol = totalValueSol - startingSol
        val netProfitLossUsd = netProfitLossSol * safeSolPrice
        val netProfitLossPercent = if (startingSol > 0) (netProfitLossSol / startingSol) * 100 else 0.0

        // --- Today's Profit Calculation ---
        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val startOfDayMs = calendar.timeInMillis
        val todayRealizedSol = txList.filter { it.timestamp >= startOfDayMs && it.type == "SELL" }
            .sumOf { it.realizedProfitSol ?: 0.0 }
        val todayUnrealizedSol = detailedHoldings.sumOf { h ->
            val buyTx = txList.firstOrNull { it.coinSymbol.equals(h.holding.coinSymbol, ignoreCase = true) && it.type == "BUY" }
            if (buyTx != null && buyTx.timestamp >= startOfDayMs) {
                h.unrealizedProfitSol
            } else {
                val liveCoin = coinsList.find { c -> c.symbol.equals(h.holding.coinSymbol, ignoreCase = true) }
                val change24h = liveCoin?.priceChange24h ?: 0.0
                if (change24h != -100.0) {
                    val startOfDayValue = h.currentValueSol / (1.0 + (change24h / 100.0))
                    h.currentValueSol - startOfDayValue
                } else {
                    h.unrealizedProfitSol
                }
            }
        }
        val todayProfitSol = todayRealizedSol + todayUnrealizedSol
        val todayProfitUsd = todayProfitSol * safeSolPrice
        val todayProfitPercent = if (startingSol > 0) (todayProfitSol / startingSol) * 100.0 else 0.0

        // --- Win Rate & Trade Stats ---
        val closedTrades = txList.filter { it.type == "SELL" }
        val winningTrades = closedTrades.filter { (it.realizedProfitSol ?: 0.0) > 0 }
        val winRatePercent = if (closedTrades.isNotEmpty()) (winningTrades.size.toDouble() / closedTrades.size) * 100.0 else 0.0
        val totalTradesCount = txList.size

        // --- Average Hold Time Calculation ---
        val holdTimesMs = mutableListOf<Long>()
        closedTrades.forEach { sellTx ->
            val buyTx = txList.firstOrNull { it.coinSymbol.equals(sellTx.coinSymbol, ignoreCase = true) && it.type == "BUY" && it.timestamp <= sellTx.timestamp }
            if (buyTx != null && sellTx.timestamp > buyTx.timestamp) {
                holdTimesMs.add(sellTx.timestamp - buyTx.timestamp)
            }
        }
        if (holdTimesMs.isEmpty()) {
            val now = System.currentTimeMillis()
            detailedHoldings.forEach { h ->
                val buyTx = txList.firstOrNull { it.coinSymbol.equals(h.holding.coinSymbol, ignoreCase = true) && it.type == "BUY" }
                if (buyTx != null && buyTx.timestamp > 0) {
                    holdTimesMs.add((now - buyTx.timestamp).coerceAtLeast(0L))
                }
            }
        }
        val avgHoldTimeMs = if (holdTimesMs.isNotEmpty()) holdTimesMs.average().toLong() else 0L
        val avgHoldTimeFormatted = when {
            avgHoldTimeMs <= 0 -> "N/A"
            avgHoldTimeMs < 60_000 -> "${avgHoldTimeMs / 1000}s"
            avgHoldTimeMs < 3_600_000 -> "${avgHoldTimeMs / 60_000}m"
            avgHoldTimeMs < 86_400_000 -> "${avgHoldTimeMs / 3_600_000}h ${(avgHoldTimeMs % 3_600_000) / 60_000}m"
            else -> "${avgHoldTimeMs / 86_400_000}d ${(avgHoldTimeMs % 86_400_000) / 3_600_000}h"
        }

        // --- Biggest Winner & Biggest Loser Calculation ---
        val candidateItems = mutableListOf<TokenPerformanceSummary>()
        // Add active holdings
        detailedHoldings.forEach { h ->
            candidateItems.add(
                TokenPerformanceSummary(
                    symbol = h.holding.coinSymbol,
                    coinName = h.holding.coinName,
                    profitSol = h.unrealizedProfitSol,
                    profitUsd = h.unrealizedProfitUsd,
                    roiPercent = h.unrealizedRoiPercent
                )
            )
        }
        // Add closed trade profits per symbol
        val closedPnLBySymbol = closedTrades.groupBy { it.coinSymbol }
        closedPnLBySymbol.forEach { (symbol, trades) ->
            val totalProfitSol = trades.sumOf { it.realizedProfitSol ?: 0.0 }
            val totalProfitUsd = totalProfitSol * safeSolPrice
            if (candidateItems.none { it.symbol.equals(symbol, ignoreCase = true) }) {
                candidateItems.add(
                    TokenPerformanceSummary(
                        symbol = symbol,
                        coinName = symbol,
                        profitSol = totalProfitSol,
                        profitUsd = totalProfitUsd,
                        roiPercent = if (totalProfitSol >= 0) 15.0 else -10.0
                    )
                )
            }
        }

        val biggestWinner = candidateItems.filter { it.profitUsd > 0 }.maxByOrNull { it.profitUsd }
        val biggestLoser = candidateItems.filter { it.profitUsd < 0 }.minByOrNull { it.profitUsd }

        // --- Live Chart Curve Data Generation ---
        val pointsCount = 16
        val chartPointsUsd = mutableListOf<Float>()
        val startVal = (startingSol * safeSolPrice).toFloat()
        val currentVal = totalValueUsd.toFloat()

        if (txList.isEmpty()) {
            // Keep graph straight/static before any trades
            repeat(pointsCount) {
                chartPointsUsd.add(currentVal)
            }
        } else {
            // Build actual portfolio equity curve based on trade history & unrealized PnL
            val sortedTx = txList.sortedBy { it.timestamp }
            val firstTxTime = sortedTx.first().timestamp
            val now = System.currentTimeMillis()
            val timeSpan = (now - firstTxTime).coerceAtLeast(1L)

            for (i in 0 until pointsCount) {
                if (i == 0) {
                    chartPointsUsd.add(startVal)
                } else if (i == pointsCount - 1) {
                    chartPointsUsd.add(currentVal)
                } else {
                    val fraction = i.toFloat() / (pointsCount - 1)
                    val pointTime = firstTxTime + (fraction * timeSpan).toLong()

                    val txsUpToPoint = sortedTx.filter { it.timestamp <= pointTime }
                    if (txsUpToPoint.isEmpty()) {
                        chartPointsUsd.add(startVal)
                    } else {
                        var solAtPoint = startingSol
                        val holdingsAtPoint = mutableMapOf<String, Double>()

                        for (tx in txsUpToPoint) {
                            val fee = tx.totalFeeSol
                            if (tx.type == "BUY") {
                                solAtPoint -= (tx.solAmount + fee)
                                val qty = holdingsAtPoint.getOrDefault(tx.coinSymbol, 0.0) + tx.tokenAmount
                                holdingsAtPoint[tx.coinSymbol] = qty
                            } else if (tx.type == "SELL") {
                                solAtPoint += (tx.solAmount - fee)
                                val qty = (holdingsAtPoint.getOrDefault(tx.coinSymbol, 0.0) - tx.tokenAmount).coerceAtLeast(0.0)
                                holdingsAtPoint[tx.coinSymbol] = qty
                            }
                        }

                        var holdingsValueAtPointSol = 0.0
                        holdingsAtPoint.forEach { (symbol, qty) ->
                            if (qty > 0) {
                                val liveCoin = coinsList.find { c -> c.symbol.equals(symbol, ignoreCase = true) }
                                if (liveCoin != null) {
                                    val buyTx = txsUpToPoint.lastOrNull { it.coinSymbol.equals(symbol, ignoreCase = true) && it.type == "BUY" }
                                    val currentLivePriceSol = if (safeSolPrice > 0) liveCoin.currentPrice / safeSolPrice else 0.0
                                    val entryPriceSol = buyTx?.executionPriceSol ?: currentLivePriceSol
                                    val priceAtPointSol = entryPriceSol + (currentLivePriceSol - entryPriceSol) * fraction
                                    holdingsValueAtPointSol += qty * priceAtPointSol
                                }
                            }
                        }

                        val totalSolAtPoint = solAtPoint + holdingsValueAtPointSol
                        chartPointsUsd.add((totalSolAtPoint * safeSolPrice).toFloat().coerceAtLeast(1f))
                    }
                }
            }
        }

        PortfolioSummary(
            solBalance = walletSol,
            solPriceUsd = safeSolPrice,
            walletValueUsd = walletUsd,
            availableSol = walletSol,
            holdingsValueSol = holdingsValueSol,
            holdingsValueUsd = holdingsValueUsd,
            totalValueSol = totalValueSol,
            totalValueUsd = totalValueUsd,
            todayProfitSol = todayProfitSol,
            todayProfitUsd = todayProfitUsd,
            todayProfitPercent = todayProfitPercent,
            totalProfitSol = netProfitLossSol,
            totalProfitUsd = netProfitLossUsd,
            netProfitLossSol = netProfitLossSol,
            netProfitLossUsd = netProfitLossUsd,
            netProfitLossPercent = netProfitLossPercent,
            roiPercent = netProfitLossPercent,
            totalRealizedProfitSol = totalRealizedProfitSol,
            totalRealizedProfitUsd = totalRealizedProfitUsd,
            totalUnrealizedProfitSol = totalUnrealizedProfitSol,
            totalUnrealizedProfitUsd = totalUnrealizedProfitUsd,
            winRatePercent = winRatePercent,
            winningTradesCount = winningTrades.size,
            closedTradesCount = closedTrades.size,
            totalTradesCount = totalTradesCount,
            avgHoldTimeFormatted = avgHoldTimeFormatted,
            biggestWinner = biggestWinner,
            biggestLoser = biggestLoser,
            totalTradingFeesSol = totalTradingFeesSol,
            totalTradingFeesUsd = totalTradingFeesUsd,
            totalGasFeesSol = totalGasFeesSol,
            totalGasFeesUsd = totalGasFeesUsd,
            totalFeesSol = totalFeesSol,
            totalFeesUsd = totalFeesUsd,
            chartPointsUsd = chartPointsUsd,
            detailedHoldings = detailedHoldings
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        PortfolioSummary()
    )

    // Trade status feedback channel
    private val _tradeEventFlow = MutableSharedFlow<TradeResultEvent>()
    val tradeEventFlow = _tradeEventFlow.asSharedFlow()

    init {
        viewModelScope.launch {
            portfolioRepository.initPortfolioIfEmpty()
        }
        viewModelScope.launch {
            combine(coins, solPriceUsd) { coinsList, solPrice ->
                Pair(coinsList, solPrice)
            }.collect { (coinsList, solPrice) ->
                if (coinsList.isNotEmpty() && solPrice > 0) {
                    portfolioRepository.checkAndExecutePendingOrders(coinsList, solPrice)
                    val triggered = portfolioRepository.checkAndTriggerPriceAlerts(coinsList)
                    for (alert in triggered) {
                        _tradeEventFlow.emit(
                            TradeResultEvent.Success(
                                message = "PRICE ALERT: ${alert.coinSymbol} hit target price of $${String.format("%.6f", alert.targetPriceUsd)}!"
                            )
                        )
                    }
                }
            }
        }
    }

    // --- PRICE ALERTS ACTIONS ---
    fun createPriceAlert(symbol: String, name: String, targetPriceUsd: Double, isAbove: Boolean) {
        viewModelScope.launch {
            portfolioRepository.createPriceAlert(symbol, name, targetPriceUsd, isAbove)
            _tradeEventFlow.emit(
                TradeResultEvent.Success("Price alert set for $symbol at $${String.format("%.6f", targetPriceUsd)}")
            )
        }
    }

    fun togglePriceAlert(alert: com.example.data.local.PriceAlert) {
        viewModelScope.launch {
            portfolioRepository.togglePriceAlert(alert)
        }
    }

    fun deletePriceAlert(alert: com.example.data.local.PriceAlert) {
        viewModelScope.launch {
            portfolioRepository.deletePriceAlert(alert)
        }
    }

    // --- TRADE JOURNAL ACTIONS ---
    fun saveJournalEntry(
        tradeId: String,
        coinSymbol: String,
        note: String,
        emotionTag: String,
        strategyTag: String,
        ratingStars: Int
    ) {
        viewModelScope.launch {
            portfolioRepository.saveJournalEntry(tradeId, coinSymbol, note, emotionTag, strategyTag, ratingStars)
            _tradeEventFlow.emit(TradeResultEvent.Success("Trade journal note saved!"))
        }
    }

    fun deleteJournalEntry(entry: com.example.data.local.TradeJournalEntry) {
        viewModelScope.launch {
            portfolioRepository.deleteJournalEntry(entry)
        }
    }

    // --- EXPORT TRADE HISTORY ---
    fun exportTradeHistoryCsv(): String {
        val txs = transactions.value
        if (txs.isEmpty()) return "No trades executed yet."

        val sb = StringBuilder()
        sb.append("Trade ID,Tx Hash,Date,Type,Symbol,Coin Name,Token Quantity,SOL Amount,Execution Price (USD),SOL Price (USD),Gas Fee (SOL),Trading Fee (SOL),Realized PnL (USD),ROI (%)\n")

        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
        txs.forEach { tx ->
            val dateStr = sdf.format(java.util.Date(tx.timestamp))
            val pnlUsd = tx.realizedProfitUsd?.let { String.format(java.util.Locale.US, "%.2f", it) } ?: "0.00"
            val roi = tx.roiPercent?.let { String.format(java.util.Locale.US, "%.2f", it) } ?: "0.00"

            sb.append("${tx.tradeId},${tx.txHash},\"$dateStr\",${tx.type},${tx.coinSymbol},\"${tx.coinName}\",${tx.tokenAmount},${tx.solAmount},${tx.executionPriceUsd},${tx.solPriceUsd},${tx.gasFeeSol},${tx.tradingFeeSol},$pnlUsd,$roi\n")
        }
        return sb.toString()
    }

    // --- PORTFOLIO RISK METER CALCULATION ---
    fun calculatePortfolioRisk(summary: PortfolioSummary): PortfolioRiskAnalysis {
        val totalUsd = summary.totalValueUsd
        if (totalUsd <= 0.0) return PortfolioRiskAnalysis()

        val solRatio = (summary.walletValueUsd / totalUsd).coerceIn(0.0, 1.0)
        val solRatioPercent = solRatio * 100.0

        val maxHoldingUsd = summary.detailedHoldings.maxOfOrNull { it.currentValueUsd } ?: 0.0
        val topTokenConcentrationPercent = if (totalUsd > 0) (maxHoldingUsd / totalUsd) * 100.0 else 0.0

        // Score formulation: 0 (Safe) to 100 (Degenerate)
        // High SOL ratio reduces risk score; high concentration and altcoin weight increases risk score.
        val altcoinWeight = 1.0 - solRatio
        val baseScore = (altcoinWeight * 60) + (topTokenConcentrationPercent * 0.4)
        val score = baseScore.toInt().coerceIn(5, 100)

        val category = when {
            score < 30 -> "CONSERVATIVE"
            score < 55 -> "BALANCED"
            score < 80 -> "HIGH RISK"
            else -> "DEGENERATE"
        }

        val recommendation = when {
            score < 30 -> "High SOL cash reserve provides maximum stability."
            score < 55 -> "Balanced position sizing across meme coins and native SOL."
            score < 80 -> "High altcoin exposure. Consider setting Stop Loss orders."
            else -> "Extremely high meme coin concentration! High risk of drawdown."
        }

        return PortfolioRiskAnalysis(
            score = score,
            category = category,
            solRatioPercent = solRatioPercent,
            topTokenConcentrationPercent = topTokenConcentrationPercent,
            recommendation = recommendation
        )
    }

    // --- PROFIT CALENDAR CALCULATION ---
    fun calculateProfitCalendar(txs: List<TradeTransaction>): List<CalendarDayPnL> {
        val sdfKey = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val sdfDisplay = java.text.SimpleDateFormat("MMM dd", java.util.Locale.US)

        val now = System.currentTimeMillis()
        val calendar = java.util.Calendar.getInstance()

        // Create entries for past 14 days
        val result = mutableListOf<CalendarDayPnL>()
        val groupedTxs = txs.groupBy { sdfKey.format(java.util.Date(it.timestamp)) }

        for (i in 13 downTo 0) {
            calendar.timeInMillis = now - (i * 86_400_000L)
            val dateKey = sdfKey.format(calendar.time)
            val displayLabel = sdfDisplay.format(calendar.time)
            val dayOfMonth = calendar.get(java.util.Calendar.DAY_OF_MONTH)

            val dayTxs = groupedTxs[dateKey] ?: emptyList()
            val realizedPnLUsd = dayTxs.filter { it.type == "SELL" }.sumOf { (it.realizedProfitUsd ?: 0.0) }

            result.add(
                CalendarDayPnL(
                    dateKey = dateKey,
                    displayLabel = displayLabel,
                    dayOfMonth = dayOfMonth,
                    totalPnLUsd = realizedPnLUsd,
                    tradeCount = dayTxs.size
                )
            )
        }
        return result
    }

    // --- ACTIONS ---


    fun updateSearchQuery(query: String) {
        memeCoinRepository.setSearchQuery(query)
    }

    fun retryFetch() {
        viewModelScope.launch {
            try {
                memeCoinRepository.retryFetch()
            } catch (e: Exception) {
                // Logged in repository
            }
        }
    }

    fun toggleWatchlist(symbol: String) {
        viewModelScope.launch {
            memeCoinRepository.toggleWatchlist(symbol)
        }
    }

    private val _selectedCongestion = MutableStateFlow(com.example.data.util.SolanaNetworkCongestion.MEDIUM)
    val selectedCongestion: StateFlow<com.example.data.util.SolanaNetworkCongestion> = _selectedCongestion.asStateFlow()

    fun setCongestion(congestion: com.example.data.util.SolanaNetworkCongestion) {
        _selectedCongestion.value = congestion
    }

    fun ensureCoin(symbol: String) {
        if (symbol.isBlank()) return
        viewModelScope.launch {
            memeCoinRepository.ensureCoinExists(symbol)
        }
    }

    fun executeBuy(
        symbol: String,
        solAmountToSpend: Double,
        congestion: com.example.data.util.SolanaNetworkCongestion = _selectedCongestion.value
    ) {
        viewModelScope.launch {
            val coin = memeCoinRepository.ensureCoinExists(symbol)
            val livePriceUsd = coin.currentPrice
            val currentSolPriceUsd = solPriceUsd.value.let { if (it > 0) it else 185.0 }

            when (val result = portfolioRepository.buyCoin(
                symbol = coin.symbol,
                name = coin.name,
                solAmountToSpend = solAmountToSpend,
                livePriceUsd = livePriceUsd,
                solPriceUsd = currentSolPriceUsd,
                congestion = congestion
            )) {
                is TradeResult.Success -> {
                    _tradeEventFlow.emit(
                        TradeResultEvent.Success(
                            message = result.message,
                            tradeId = result.tradeId,
                            txHash = result.txHash
                        )
                    )
                }
                is TradeResult.Error -> {
                    _tradeEventFlow.emit(TradeResultEvent.Error(result.message))
                }
            }
        }
    }

    fun executeSell(
        symbol: String,
        tokenAmountToSell: Double,
        congestion: com.example.data.util.SolanaNetworkCongestion = _selectedCongestion.value
    ) {
        viewModelScope.launch {
            val coin = memeCoinRepository.ensureCoinExists(symbol)
            val livePriceUsd = coin.currentPrice
            val currentSolPriceUsd = solPriceUsd.value.let { if (it > 0) it else 185.0 }

            when (val result = portfolioRepository.sellCoin(
                symbol = coin.symbol,
                name = coin.name,
                tokensToSell = tokenAmountToSell,
                livePriceUsd = livePriceUsd,
                solPriceUsd = currentSolPriceUsd,
                congestion = congestion
            )) {
                is TradeResult.Success -> {
                    _tradeEventFlow.emit(
                        TradeResultEvent.Success(
                            message = result.message,
                            tradeId = result.tradeId,
                            txHash = result.txHash
                        )
                    )
                }
                is TradeResult.Error -> {
                    _tradeEventFlow.emit(TradeResultEvent.Error(result.message))
                }
            }
        }
    }

    fun placePendingOrder(
        symbol: String,
        orderType: String,
        targetPriceUsd: Double,
        solAmount: Double = 0.0,
        tokenAmount: Double = 0.0
    ) {
        viewModelScope.launch {
            val coin = memeCoinRepository.ensureCoinExists(symbol)
            val currentSolPriceUsd = solPriceUsd.value.let { if (it > 0) it else 185.0 }

            when (val result = portfolioRepository.placePendingOrder(
                symbol = coin.symbol,
                name = coin.name,
                orderType = orderType,
                targetPriceUsd = targetPriceUsd,
                solAmount = solAmount,
                tokenAmount = tokenAmount,
                solPriceUsd = currentSolPriceUsd
            )) {
                is TradeResult.Success -> {
                    _tradeEventFlow.emit(
                        TradeResultEvent.Success(
                            message = result.message,
                            tradeId = result.tradeId,
                            txHash = result.txHash
                        )
                    )
                }
                is TradeResult.Error -> {
                    _tradeEventFlow.emit(TradeResultEvent.Error(result.message))
                }
            }
        }
    }

    fun cancelPendingOrder(orderId: String) {
        viewModelScope.launch {
            portfolioRepository.cancelPendingOrder(orderId)
        }
    }

    fun resetPaperTrading() {
        viewModelScope.launch {
            portfolioRepository.resetPortfolio()
            _tradeEventFlow.emit(TradeResultEvent.Success(message = "Simulated wallet reset to 10.0 SOL!"))
        }
    }
}

// Data structures for ViewModel state exposure
data class DetailedHolding(
    val holding: Holding,
    val currentPriceUsd: Double,
    val currentPriceSol: Double,
    val marketCapUsd: Double = 0.0,
    val currentValueUsd: Double,
    val currentValueSol: Double,
    val costBasisSol: Double,
    val costBasisUsd: Double,
    val unrealizedProfitSol: Double,
    val unrealizedProfitUsd: Double,
    val unrealizedRoiPercent: Double,
    val realizedProfitSol: Double,
    val realizedProfitUsd: Double,
    val totalTradingFeesSol: Double,
    val totalGasFeesSol: Double,
    val netProfitSol: Double,
    val netProfitUsd: Double
)

data class TokenPerformanceSummary(
    val symbol: String = "",
    val coinName: String = "",
    val profitSol: Double = 0.0,
    val profitUsd: Double = 0.0,
    val roiPercent: Double = 0.0
)

data class PortfolioSummary(
    val solBalance: Double = 10.0,
    val solPriceUsd: Double = 185.0,
    val walletValueUsd: Double = 1850.0,
    val availableSol: Double = 10.0,
    val holdingsValueSol: Double = 0.0,
    val holdingsValueUsd: Double = 0.0,
    val totalValueSol: Double = 10.0,
    val totalValueUsd: Double = 1850.0,
    val todayProfitSol: Double = 0.0,
    val todayProfitUsd: Double = 0.0,
    val todayProfitPercent: Double = 0.0,
    val totalProfitSol: Double = 0.0,
    val totalProfitUsd: Double = 0.0,
    val netProfitLossSol: Double = 0.0,
    val netProfitLossUsd: Double = 0.0,
    val netProfitLossPercent: Double = 0.0,
    val roiPercent: Double = 0.0,
    val totalRealizedProfitSol: Double = 0.0,
    val totalRealizedProfitUsd: Double = 0.0,
    val totalUnrealizedProfitSol: Double = 0.0,
    val totalUnrealizedProfitUsd: Double = 0.0,
    val winRatePercent: Double = 0.0,
    val winningTradesCount: Int = 0,
    val closedTradesCount: Int = 0,
    val totalTradesCount: Int = 0,
    val avgHoldTimeFormatted: String = "N/A",
    val biggestWinner: TokenPerformanceSummary? = null,
    val biggestLoser: TokenPerformanceSummary? = null,
    val totalTradingFeesSol: Double = 0.0,
    val totalTradingFeesUsd: Double = 0.0,
    val totalGasFeesSol: Double = 0.0,
    val totalGasFeesUsd: Double = 0.0,
    val totalFeesSol: Double = 0.0,
    val totalFeesUsd: Double = 0.0,
    val chartPointsUsd: List<Float> = emptyList(),
    val detailedHoldings: List<DetailedHolding> = emptyList()
)

data class PortfolioRiskAnalysis(
    val score: Int = 15,
    val category: String = "CONSERVATIVE",
    val solRatioPercent: Double = 100.0,
    val topTokenConcentrationPercent: Double = 0.0,
    val recommendation: String = "High SOL cash reserve provides maximum stability."
)

data class CalendarDayPnL(
    val dateKey: String = "",
    val displayLabel: String = "",
    val dayOfMonth: Int = 1,
    val totalPnLUsd: Double = 0.0,
    val tradeCount: Int = 0
)

sealed interface TradeResultEvent {

    data class Success(
        val message: String,
        val tradeId: String = "",
        val txHash: String = ""
    ) : TradeResultEvent
    
    data class Error(val message: String) : TradeResultEvent
}

// ViewModelFactory helper for clean instantiation in Compose
class TradingViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TradingViewModel::class.java)) {
            val database = PaperTraderDatabase.getDatabase(application)
            val dao = database.paperTraderDao()
            val memeRepo = MemeCoinRepository(dao)
            val portRepo = PortfolioRepository(dao)
            @Suppress("UNCHECKED_CAST")
            return TradingViewModel(memeRepo, portRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
