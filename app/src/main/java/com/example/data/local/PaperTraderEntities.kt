package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "portfolio_state")
data class PortfolioState(
    @PrimaryKey val id: Int = 1,
    val solBalance: Double = 10.0, // Every user starts with exactly 10 simulated SOL
    val initialSolBalance: Double = 10.0
)

@Entity(tableName = "holdings")
data class Holding(
    @PrimaryKey val coinSymbol: String,
    val coinName: String,
    val amount: Double,                   // Remaining Quantity owned
    val totalBoughtQuantity: Double = 0.0,// Cumulative total tokens bought in this position
    val avgBuyPriceUsd: Double,           // Average Entry Price in USD
    val avgBuyPriceSol: Double,           // Average Entry Price in SOL
    val totalInvestedSol: Double,         // Active Cost Basis in SOL
    val realizedProfitSol: Double = 0.0,  // Cumulative Realized Profit in SOL for this token
    val realizedProfitUsd: Double = 0.0,  // Cumulative Realized Profit in USD for this token
    val totalTradingFeesSol: Double = 0.0,// Total DEX / platform fees paid
    val totalGasFeesSol: Double = 0.0,    // Total network gas fees paid (Network + Priority)
    val totalFeesSol: Double = 0.0        // Total overall fees paid in SOL
)

@Entity(tableName = "transactions")
data class TradeTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tradeId: String,             // Unique trade identifier (e.g., TRD-8F2A9C1B)
    val txHash: String,              // Simulated Solana base58 transaction signature
    val coinSymbol: String,
    val coinName: String,
    val type: String,                // "BUY" or "SELL"
    val solAmount: Double,           // Gross SOL amount spent (BUY) or proceeds (SELL)
    val tokenAmount: Double,         // Token quantity bought or sold
    val executionPriceUsd: Double,   // Live execution price in USD
    val executionPriceSol: Double = 0.0,// Live execution price in SOL
    val solPriceUsd: Double,         // Live price of SOL in USD at execution time
    val networkFeeSol: Double = 0.000005, // Solana base signature fee (5k lamports)
    val priorityFeeSol: Double = 0.000150,// Solana priority fee (Compute units)
    val gasFeeSol: Double = 0.000155,      // Total Gas fee = Network Fee + Priority Fee
    val platformFeeSol: Double = 0.0, // Platform fee (e.g. 0.25%)
    val tradingFeeSol: Double = 0.0,       // DEX / Platform fee in SOL
    val totalFeeSol: Double = 0.0,   // Total Fee = Gas Fee + Platform Fee
    val netSol: Double = 0.0,              // Net SOL deducted from wallet (BUY) or credited (SELL)
    val avgEntryPriceSol: Double = 0.0,// Average entry price in SOL prior to or after execution
    val realizedProfitSol: Double? = null, // Realized profit/loss in SOL (SELL trades)
    val realizedProfitUsd: Double? = null, // Realized profit/loss in USD (SELL trades)
    val roiPercent: Double? = null,        // Trade ROI % (SELL trades)
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "watchlist")
data class WatchlistItem(
    @PrimaryKey val coinSymbol: String
)

@Entity(tableName = "pending_orders")
data class PendingOrder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: String,             // Unique order ID (e.g. ORD-9F2A1C8B)
    val coinSymbol: String,
    val coinName: String,
    val orderType: String,           // "LIMIT_BUY", "LIMIT_SELL", "STOP_LOSS", "TAKE_PROFIT"
    val targetPriceUsd: Double,      // Specified price threshold in USD
    val targetPriceSol: Double = 0.0,// Specified price threshold in SOL
    val solAmount: Double = 0.0,     // For BUY orders: SOL to spend
    val tokenAmount: Double = 0.0,   // For SELL orders: Token quantity to sell
    val status: String = "PENDING",  // "PENDING", "EXECUTED", "CANCELLED"
    val createdTimestamp: Long = System.currentTimeMillis(),
    val executedTimestamp: Long? = null,
    val executedPriceUsd: Double? = null,
    val executedPriceSol: Double? = null
)

@Entity(tableName = "price_alerts")
data class PriceAlert(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val coinSymbol: String,
    val coinName: String,
    val targetPriceUsd: Double,
    val isAbove: Boolean = true,      // true if alert when price >= target, false if price <= target
    val isEnabled: Boolean = true,
    val isTriggered: Boolean = false,
    val createdTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "trade_journals")
data class TradeJournalEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tradeId: String,             // Matches TradeTransaction.tradeId or coinSymbol
    val coinSymbol: String,
    val note: String,
    val emotionTag: String = "DISCIPLINED", // "CALM", "FOMO", "GREEDY", "DISCIPLINED", "PANIC"
    val strategyTag: String = "SWING",      // "BREAKOUT", "DIP_BUY", "MEME_TREND", "SWING", "SCALP"
    val ratingStars: Int = 5,
    val timestamp: Long = System.currentTimeMillis()
)


