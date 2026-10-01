package com.example.data.repository

import com.example.data.local.Holding
import com.example.data.local.PaperTraderDao
import com.example.data.local.PendingOrder
import com.example.data.local.PortfolioState
import com.example.data.local.TradeTransaction
import com.example.data.model.MemeCoin
import com.example.data.util.SolanaFeeCalculator
import com.example.data.util.SolanaNetworkCongestion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import kotlin.math.max

sealed class TradeResult {
    data class Success(
        val message: String,
        val tradeId: String,
        val txHash: String,
        val tokenAmount: Double,
        val solAmount: Double,
        val netSol: Double,
        val executionPriceUsd: Double
    ) : TradeResult()

    data class Error(val message: String) : TradeResult()
}

class PortfolioRepository(
    private val paperTraderDao: PaperTraderDao
) {
    val portfolioStateFlow: Flow<PortfolioState> = paperTraderDao.getPortfolioStateFlow().map { state ->
        state ?: PortfolioState(id = 1, solBalance = 10.0, initialSolBalance = 10.0)
    }

    val holdingsFlow: Flow<List<Holding>> = paperTraderDao.getAllHoldingsFlow()

    val transactionsFlow: Flow<List<TradeTransaction>> = paperTraderDao.getAllTransactionsFlow()

    val activePendingOrdersFlow: Flow<List<PendingOrder>> = paperTraderDao.getActivePendingOrdersFlow()

    val allPendingOrdersFlow: Flow<List<PendingOrder>> = paperTraderDao.getAllPendingOrdersFlow()

    suspend fun initPortfolioIfEmpty() {
        val currentState = paperTraderDao.getPortfolioState()
        if (currentState == null) {
            paperTraderDao.updatePortfolioState(PortfolioState(id = 1, solBalance = 10.0, initialSolBalance = 10.0))
        }
    }

    /**
     * Executes a simulated BUY order.
     *
     * Buying rules:
     * - User enters SOL amount (`solAmountToSpend`).
     * - Current live price of token (`livePriceUsd`) and live SOL price (`solPriceUsd`) are used.
     * - Applied gas fee: 0.0005 SOL
     * - Applied DEX trading fee: 0.25% of trade value (`solAmountToSpend * 0.0025`)
     * - Required SOL from fake wallet = `solAmountToSpend + gasFeeSol`
     * - Calculate received token quantity using live prices:
     *   `netSolForTokens = solAmountToSpend - tradingFeeSol`
     *   `netUsdForTokens = netSolForTokens * solPriceUsd`
     *   `receivedTokens = netUsdForTokens / livePriceUsd`
     * - Deducts required SOL from fake wallet balance.
     * - Saves exact execution price, quantity, timestamp, simulated transaction hash, and trade ID.
     */
    suspend fun buyCoin(
        symbol: String,
        name: String,
        solAmountToSpend: Double,
        livePriceUsd: Double,
        solPriceUsd: Double,
        congestion: SolanaNetworkCongestion = SolanaNetworkCongestion.MEDIUM
    ): TradeResult {
        if (solAmountToSpend <= 0) return TradeResult.Error("Enter a valid SOL amount greater than 0")
        if (livePriceUsd <= 0 || solPriceUsd <= 0) return TradeResult.Error("Invalid live price data")

        val state = paperTraderDao.getPortfolioState() ?: PortfolioState(id = 1, solBalance = 10.0, initialSolBalance = 10.0)

        val feeDetails = SolanaFeeCalculator.calculateFees(solAmountToSpend, congestion)
        val networkFeeSol = feeDetails.networkFeeSol
        val priorityFeeSol = feeDetails.priorityFeeSol
        val gasFeeSol = feeDetails.gasFeeSol
        val platformFeeSol = feeDetails.platformFeeSol
        val totalFeeSol = feeDetails.totalFeeSol

        val requiredSolFromWallet = solAmountToSpend + totalFeeSol

        if (state.solBalance < requiredSolFromWallet) {
            return TradeResult.Error(
                String.format("Insufficient SOL balance! Required: %.6f SOL (includes %.6f SOL total fees)", requiredSolFromWallet, totalFeeSol)
            )
        }

        // Net SOL converted into target tokens
        val netSolForTokens = max(0.0, solAmountToSpend)
        val netUsdForTokens = netSolForTokens * solPriceUsd
        val receivedTokens = netUsdForTokens / livePriceUsd

        if (receivedTokens <= 0) {
            return TradeResult.Error("Token quantity calculation returned 0 tokens")
        }

        // 1. Deduct required SOL from wallet
        val updatedBalance = state.solBalance - requiredSolFromWallet
        paperTraderDao.updatePortfolioState(state.copy(solBalance = updatedBalance))

        // 2. Update holdings
        val existingHolding = paperTraderDao.getHoldingBySymbol(symbol.uppercase())
        val executionPriceSol = if (solPriceUsd > 0) livePriceUsd / solPriceUsd else 0.0
        val avgBuyPriceSolForThisTrade = executionPriceSol

        val postBuyAvgEntrySol: Double

        if (existingHolding != null) {
            val totalAmount = existingHolding.amount + receivedTokens
            val totalBoughtQuantity = existingHolding.totalBoughtQuantity + receivedTokens
            val totalInvestedSol = existingHolding.totalInvestedSol + solAmountToSpend + totalFeeSol
            val newAvgBuyPriceSol = totalInvestedSol / totalAmount
            val newAvgBuyPriceUsd = newAvgBuyPriceSol * solPriceUsd
            postBuyAvgEntrySol = newAvgBuyPriceSol

            paperTraderDao.insertHolding(
                Holding(
                    coinSymbol = symbol.uppercase(),
                    coinName = name,
                    amount = totalAmount,
                    totalBoughtQuantity = totalBoughtQuantity,
                    avgBuyPriceUsd = newAvgBuyPriceUsd,
                    avgBuyPriceSol = newAvgBuyPriceSol,
                    totalInvestedSol = totalInvestedSol,
                    realizedProfitSol = existingHolding.realizedProfitSol,
                    realizedProfitUsd = existingHolding.realizedProfitUsd,
                    totalTradingFeesSol = existingHolding.totalTradingFeesSol + platformFeeSol,
                    totalGasFeesSol = existingHolding.totalGasFeesSol + gasFeeSol,
                    totalFeesSol = existingHolding.totalFeesSol + totalFeeSol
                )
            )
        } else {
            postBuyAvgEntrySol = avgBuyPriceSolForThisTrade
            paperTraderDao.insertHolding(
                Holding(
                    coinSymbol = symbol.uppercase(),
                    coinName = name,
                    amount = receivedTokens,
                    totalBoughtQuantity = receivedTokens,
                    avgBuyPriceUsd = livePriceUsd,
                    avgBuyPriceSol = avgBuyPriceSolForThisTrade,
                    totalInvestedSol = solAmountToSpend + totalFeeSol,
                    realizedProfitSol = 0.0,
                    realizedProfitUsd = 0.0,
                    totalTradingFeesSol = platformFeeSol,
                    totalGasFeesSol = gasFeeSol,
                    totalFeesSol = totalFeeSol
                )
            )
        }

        // 3. Generate Trade ID and Simulated Transaction Hash
        val tradeId = "TRD-" + UUID.randomUUID().toString().take(8).uppercase()
        val txHash = generateSimulatedTxHash()

        // 4. Record transaction in Room DB
        paperTraderDao.insertTransaction(
            TradeTransaction(
                tradeId = tradeId,
                txHash = txHash,
                coinSymbol = symbol.uppercase(),
                coinName = name,
                type = "BUY",
                solAmount = solAmountToSpend,
                tokenAmount = receivedTokens,
                executionPriceUsd = livePriceUsd,
                executionPriceSol = executionPriceSol,
                solPriceUsd = solPriceUsd,
                networkFeeSol = networkFeeSol,
                priorityFeeSol = priorityFeeSol,
                gasFeeSol = gasFeeSol,
                platformFeeSol = platformFeeSol,
                tradingFeeSol = platformFeeSol,
                totalFeeSol = totalFeeSol,
                netSol = requiredSolFromWallet,
                avgEntryPriceSol = postBuyAvgEntrySol,
                timestamp = System.currentTimeMillis()
            )
        )

        return TradeResult.Success(
            message = String.format("Bought %,.2f %s for %.4f SOL!", receivedTokens, symbol.uppercase(), solAmountToSpend),
            tradeId = tradeId,
            txHash = txHash,
            tokenAmount = receivedTokens,
            solAmount = solAmountToSpend,
            netSol = requiredSolFromWallet,
            executionPriceUsd = livePriceUsd
        )
    }

    /**
     * Executes a simulated SELL order.
     */
    suspend fun sellCoin(
        symbol: String,
        name: String,
        tokensToSell: Double,
        livePriceUsd: Double,
        solPriceUsd: Double,
        congestion: SolanaNetworkCongestion = SolanaNetworkCongestion.MEDIUM
    ): TradeResult {
        if (tokensToSell <= 0) return TradeResult.Error("Enter a valid token quantity to sell")
        if (livePriceUsd <= 0 || solPriceUsd <= 0) return TradeResult.Error("Invalid live price data")

        val existingHolding = paperTraderDao.getHoldingBySymbol(symbol.uppercase())
        if (existingHolding == null) {
            return TradeResult.Error("You do not own any ${symbol.uppercase()} tokens.")
        }

        val actualTokensToSell = if (tokensToSell >= existingHolding.amount - 0.001) {
            existingHolding.amount
        } else {
            tokensToSell
        }

        if (existingHolding.amount < actualTokensToSell - 0.0000001) {
            return TradeResult.Error(
                String.format("Insufficient holdings! You own %,.2f %s", existingHolding.amount, symbol.uppercase())
            )
        }

        val state = paperTraderDao.getPortfolioState() ?: PortfolioState(id = 1, solBalance = 10.0, initialSolBalance = 10.0)

        val grossUsdValue = actualTokensToSell * livePriceUsd
        val grossSolValue = grossUsdValue / solPriceUsd
        val executionPriceSol = if (solPriceUsd > 0) livePriceUsd / solPriceUsd else 0.0

        val feeDetails = SolanaFeeCalculator.calculateFees(grossSolValue, congestion)
        val networkFeeSol = feeDetails.networkFeeSol
        val priorityFeeSol = feeDetails.priorityFeeSol
        val gasFeeSol = feeDetails.gasFeeSol
        val platformFeeSol = feeDetails.platformFeeSol
        val totalFeeSol = feeDetails.totalFeeSol

        val netSolProceeds = max(0.0, grossSolValue - totalFeeSol)

        // Calculate cost basis for sold tokens based on average entry price
        val costBasisSol = existingHolding.avgBuyPriceSol * actualTokensToSell
        val costBasisUsd = costBasisSol * solPriceUsd

        // Gross trade profit based on selling price vs buying entry price
        val grossProfitSol = grossSolValue - costBasisSol
        val grossProfitUsd = grossUsdValue - costBasisUsd

        // Realized profit (if gross trade profit is positive, record positive profit)
        val realizedProfitSol = if (grossProfitSol >= 0) grossProfitSol else netSolProceeds - costBasisSol
        val realizedProfitUsd = if (grossProfitUsd >= 0) grossProfitUsd else (netSolProceeds * solPriceUsd) - costBasisUsd
        val tradeRoiPercent = if (costBasisSol > 0) ((grossSolValue - costBasisSol) / costBasisSol) * 100 else 0.0

        // 1. Credit net SOL proceeds to wallet balance
        val updatedBalance = state.solBalance + netSolProceeds
        paperTraderDao.updatePortfolioState(state.copy(solBalance = updatedBalance))

        // 2. Update/Delete holdings
        val remainingTokens = max(0.0, existingHolding.amount - actualTokensToSell)
        val newRealizedProfitSol = existingHolding.realizedProfitSol + realizedProfitSol
        val newRealizedProfitUsd = existingHolding.realizedProfitUsd + realizedProfitUsd
        val newTradingFeesSol = existingHolding.totalTradingFeesSol + platformFeeSol
        val newGasFeesSol = existingHolding.totalGasFeesSol + gasFeeSol
        val newTotalFeesSol = existingHolding.totalFeesSol + totalFeeSol

        if (remainingTokens <= 0.000001) {
            paperTraderDao.deleteHolding(existingHolding)
        } else {
            val remainingInvestedSol = existingHolding.avgBuyPriceSol * remainingTokens
            paperTraderDao.insertHolding(
                existingHolding.copy(
                    amount = remainingTokens,
                    totalInvestedSol = remainingInvestedSol,
                    avgBuyPriceUsd = existingHolding.avgBuyPriceSol * solPriceUsd,
                    realizedProfitSol = newRealizedProfitSol,
                    realizedProfitUsd = newRealizedProfitUsd,
                    totalTradingFeesSol = newTradingFeesSol,
                    totalGasFeesSol = newGasFeesSol,
                    totalFeesSol = newTotalFeesSol
                )
            )
        }

        // 3. Generate Trade ID and Simulated Tx Hash
        val tradeId = "TRD-" + UUID.randomUUID().toString().take(8).uppercase()
        val txHash = generateSimulatedTxHash()

        // 4. Save transaction
        paperTraderDao.insertTransaction(
            TradeTransaction(
                tradeId = tradeId,
                txHash = txHash,
                coinSymbol = symbol.uppercase(),
                coinName = name,
                type = "SELL",
                solAmount = grossSolValue,
                tokenAmount = actualTokensToSell,
                executionPriceUsd = livePriceUsd,
                executionPriceSol = executionPriceSol,
                solPriceUsd = solPriceUsd,
                networkFeeSol = networkFeeSol,
                priorityFeeSol = priorityFeeSol,
                gasFeeSol = gasFeeSol,
                platformFeeSol = platformFeeSol,
                tradingFeeSol = platformFeeSol,
                totalFeeSol = totalFeeSol,
                netSol = netSolProceeds,
                avgEntryPriceSol = existingHolding.avgBuyPriceSol,
                realizedProfitSol = realizedProfitSol,
                realizedProfitUsd = realizedProfitUsd,
                roiPercent = tradeRoiPercent,
                timestamp = System.currentTimeMillis()
            )
        )

        val profitSign = if (realizedProfitSol >= 0) "+" else ""
        return TradeResult.Success(
            message = String.format("Sold %,.2f %s for +%.4f SOL (Realized P&L: %s%.4f SOL)", tokensToSell, symbol.uppercase(), netSolProceeds, profitSign, realizedProfitSol),
            tradeId = tradeId,
            txHash = txHash,
            tokenAmount = tokensToSell,
            solAmount = grossSolValue,
            netSol = netSolProceeds,
            executionPriceUsd = livePriceUsd
        )
    }

    suspend fun resetPortfolio() {
        paperTraderDao.updatePortfolioState(PortfolioState(id = 1, solBalance = 10.0, initialSolBalance = 10.0))
        paperTraderDao.clearAllHoldings()
        paperTraderDao.clearTransactionHistory()
    }

    /**
     * Creates a pending limit or conditional order (Limit Buy, Limit Sell, Stop Loss, Take Profit).
     */
    suspend fun placePendingOrder(
        symbol: String,
        name: String,
        orderType: String,
        targetPriceUsd: Double,
        solAmount: Double,
        tokenAmount: Double,
        solPriceUsd: Double
    ): TradeResult {
        if (targetPriceUsd <= 0.0) {
            return TradeResult.Error("Target price must be greater than $0.00")
        }

        val state = paperTraderDao.getPortfolioState() ?: PortfolioState()
        val holding = paperTraderDao.getHoldingBySymbol(symbol.uppercase())

        var finalTokenAmount = tokenAmount

        when (orderType.uppercase()) {
            "LIMIT_BUY" -> {
                if (solAmount <= 0.0) return TradeResult.Error("Enter a valid SOL amount to buy")
                val totalRequiredSol = solAmount + 0.0005
                if (state.solBalance < totalRequiredSol) {
                    return TradeResult.Error(String.format("Insufficient wallet SOL. Required: %.4f SOL", totalRequiredSol))
                }
            }
            "LIMIT_SELL", "STOP_LOSS", "TAKE_PROFIT" -> {
                if (tokenAmount <= 0.0) return TradeResult.Error("Enter a valid token quantity to sell")
                if (holding == null) {
                    return TradeResult.Error(String.format("You do not own any %s tokens", symbol.uppercase()))
                }
                // Handle 100% sell / precision tolerance clamping
                if (tokenAmount >= holding.amount - 0.001) {
                    finalTokenAmount = holding.amount
                }
                if (holding.amount < finalTokenAmount - 0.000001) {
                    return TradeResult.Error(String.format("Insufficient %s balance. Available: %,.2f", symbol.uppercase(), holding.amount))
                }
            }
            else -> return TradeResult.Error("Invalid order type: $orderType")
        }

        val orderId = "ORD-" + UUID.randomUUID().toString().take(8).uppercase()
        val targetPriceSol = if (solPriceUsd > 0) targetPriceUsd / solPriceUsd else 0.0

        val order = PendingOrder(
            orderId = orderId,
            coinSymbol = symbol.uppercase(),
            coinName = name,
            orderType = orderType.uppercase(),
            targetPriceUsd = targetPriceUsd,
            targetPriceSol = targetPriceSol,
            solAmount = solAmount,
            tokenAmount = finalTokenAmount,
            status = "PENDING",
            createdTimestamp = System.currentTimeMillis()
        )

        paperTraderDao.insertPendingOrder(order)

        val formattedTypeName = when (orderType.uppercase()) {
            "LIMIT_BUY" -> "Limit Buy"
            "LIMIT_SELL" -> "Limit Sell"
            "STOP_LOSS" -> "Stop Loss"
            "TAKE_PROFIT" -> "Take Profit"
            else -> orderType
        }

        return TradeResult.Success(
            message = String.format("%s order placed for %s at $%,.6f USD target price", formattedTypeName, symbol.uppercase(), targetPriceUsd),
            tradeId = orderId,
            txHash = generateSimulatedTxHash(),
            tokenAmount = tokenAmount,
            solAmount = solAmount,
            netSol = solAmount,
            executionPriceUsd = targetPriceUsd
        )
    }

    /**
     * Cancels an active pending order by order ID.
     */
    suspend fun cancelPendingOrder(orderId: String) {
        paperTraderDao.cancelPendingOrder(orderId)
    }

    /**
     * Automatically checks active pending orders against current live coin market prices and executes them
     * at exact live market price if target conditions are met.
     */
    suspend fun checkAndExecutePendingOrders(coins: List<MemeCoin>, solPriceUsd: Double) {
        if (coins.isEmpty() || solPriceUsd <= 0.0) return

        val activeOrders = paperTraderDao.getActivePendingOrders()
        if (activeOrders.isEmpty()) return

        val coinMap = coins.associateBy { it.symbol.uppercase() }

        for (order in activeOrders) {
            val coin = coinMap[order.coinSymbol.uppercase()] ?: continue
            val livePriceUsd = coin.currentPrice
            if (livePriceUsd <= 0.0) continue

            val isTriggered = when (order.orderType) {
                "LIMIT_BUY" -> livePriceUsd <= order.targetPriceUsd
                "LIMIT_SELL" -> livePriceUsd >= order.targetPriceUsd
                "STOP_LOSS" -> livePriceUsd <= order.targetPriceUsd
                "TAKE_PROFIT" -> livePriceUsd >= order.targetPriceUsd
                else -> false
            }

            if (isTriggered) {
                val result = when (order.orderType) {
                    "LIMIT_BUY" -> buyCoin(
                        symbol = order.coinSymbol,
                        name = order.coinName,
                        solAmountToSpend = order.solAmount,
                        livePriceUsd = livePriceUsd,
                        solPriceUsd = solPriceUsd
                    )
                    "LIMIT_SELL", "STOP_LOSS", "TAKE_PROFIT" -> sellCoin(
                        symbol = order.coinSymbol,
                        name = order.coinName,
                        tokensToSell = order.tokenAmount,
                        livePriceUsd = livePriceUsd,
                        solPriceUsd = solPriceUsd
                    )
                    else -> TradeResult.Error("Unknown order type")
                }

                if (result is TradeResult.Success) {
                    paperTraderDao.updatePendingOrder(
                        order.copy(
                            status = "EXECUTED",
                            executedTimestamp = System.currentTimeMillis(),
                            executedPriceUsd = livePriceUsd,
                            executedPriceSol = livePriceUsd / solPriceUsd
                        )
                    )
                }
            }
        }
    }

    val priceAlertsFlow: Flow<List<com.example.data.local.PriceAlert>> = paperTraderDao.getAllPriceAlertsFlow()

    val journalEntriesFlow: Flow<List<com.example.data.local.TradeJournalEntry>> = paperTraderDao.getAllJournalEntriesFlow()

    suspend fun createPriceAlert(
        coinSymbol: String,
        coinName: String,
        targetPriceUsd: Double,
        isAbove: Boolean
    ): Long {
        return paperTraderDao.insertPriceAlert(
            com.example.data.local.PriceAlert(
                coinSymbol = coinSymbol.uppercase(),
                coinName = coinName,
                targetPriceUsd = targetPriceUsd,
                isAbove = isAbove,
                isEnabled = true,
                isTriggered = false
            )
        )
    }

    suspend fun togglePriceAlert(alert: com.example.data.local.PriceAlert) {
        paperTraderDao.updatePriceAlert(alert.copy(isEnabled = !alert.isEnabled))
    }

    suspend fun deletePriceAlert(alert: com.example.data.local.PriceAlert) {
        paperTraderDao.deletePriceAlert(alert)
    }

    suspend fun saveJournalEntry(
        tradeId: String,
        coinSymbol: String,
        note: String,
        emotionTag: String,
        strategyTag: String,
        ratingStars: Int
    ): Long {
        return paperTraderDao.insertJournalEntry(
            com.example.data.local.TradeJournalEntry(
                tradeId = tradeId,
                coinSymbol = coinSymbol.uppercase(),
                note = note,
                emotionTag = emotionTag,
                strategyTag = strategyTag,
                ratingStars = ratingStars
            )
        )
    }

    suspend fun deleteJournalEntry(entry: com.example.data.local.TradeJournalEntry) {
        paperTraderDao.deleteJournalEntry(entry)
    }

    suspend fun checkAndTriggerPriceAlerts(coins: List<MemeCoin>): List<com.example.data.local.PriceAlert> {
        if (coins.isEmpty()) return emptyList()
        val activeAlerts = paperTraderDao.getActivePriceAlerts()
        if (activeAlerts.isEmpty()) return emptyList()

        val coinMap = coins.associateBy { it.symbol.uppercase() }
        val triggered = mutableListOf<com.example.data.local.PriceAlert>()

        for (alert in activeAlerts) {
            val coin = coinMap[alert.coinSymbol.uppercase()] ?: continue
            val livePrice = coin.currentPrice
            if (livePrice <= 0.0) continue

            val isTriggered = if (alert.isAbove) {
                livePrice >= alert.targetPriceUsd
            } else {
                livePrice <= alert.targetPriceUsd
            }

            if (isTriggered) {
                val updated = alert.copy(isTriggered = true, isEnabled = false)
                paperTraderDao.updatePriceAlert(updated)
                triggered.add(updated)
            }
        }
        return triggered
    }

    private fun generateSimulatedTxHash(): String {
        val alphabet = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
        return (1..88).map { alphabet.random() }.joinToString("")
    }
}

