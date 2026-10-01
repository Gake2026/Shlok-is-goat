package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PaperTraderDao {

    // --- PORTFOLIO STATE ---
    @Query("SELECT * FROM portfolio_state WHERE id = 1")
    fun getPortfolioStateFlow(): Flow<PortfolioState?>

    @Query("SELECT * FROM portfolio_state WHERE id = 1")
    suspend fun getPortfolioState(): PortfolioState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updatePortfolioState(state: PortfolioState)

    // --- HOLDINGS ---
    @Query("SELECT * FROM holdings WHERE amount > 0")
    fun getAllHoldingsFlow(): Flow<List<Holding>>

    @Query("SELECT * FROM holdings WHERE coinSymbol = :symbol")
    suspend fun getHoldingBySymbol(symbol: String): Holding?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHolding(holding: Holding)

    @Delete
    suspend fun deleteHolding(holding: Holding)

    @Query("DELETE FROM holdings")
    suspend fun clearAllHoldings()

    // --- TRANSACTIONS ---
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactionsFlow(): Flow<List<TradeTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TradeTransaction)

    @Query("DELETE FROM transactions")
    suspend fun clearTransactionHistory()

    // --- WATCHLIST ---
    @Query("SELECT * FROM watchlist")
    fun getWatchlistFlow(): Flow<List<WatchlistItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToWatchlist(item: WatchlistItem)

    @Delete
    suspend fun removeFromWatchlist(item: WatchlistItem)

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE coinSymbol = :symbol)")
    fun isCoinWatchedFlow(symbol: String): Flow<Boolean>

    // --- PENDING ORDERS ---
    @Query("SELECT * FROM pending_orders WHERE status = 'PENDING' ORDER BY createdTimestamp DESC")
    fun getActivePendingOrdersFlow(): Flow<List<PendingOrder>>

    @Query("SELECT * FROM pending_orders ORDER BY createdTimestamp DESC")
    fun getAllPendingOrdersFlow(): Flow<List<PendingOrder>>

    @Query("SELECT * FROM pending_orders WHERE status = 'PENDING'")
    suspend fun getActivePendingOrders(): List<PendingOrder>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingOrder(order: PendingOrder): Long

    @Update
    suspend fun updatePendingOrder(order: PendingOrder)

    @Delete
    suspend fun deletePendingOrder(order: PendingOrder)

    @Query("UPDATE pending_orders SET status = 'CANCELLED' WHERE orderId = :orderId")
    suspend fun cancelPendingOrder(orderId: String)

    // --- PRICE ALERTS ---
    @Query("SELECT * FROM price_alerts ORDER BY createdTimestamp DESC")
    fun getAllPriceAlertsFlow(): Flow<List<PriceAlert>>

    @Query("SELECT * FROM price_alerts WHERE isEnabled = 1 AND isTriggered = 0")
    suspend fun getActivePriceAlerts(): List<PriceAlert>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPriceAlert(alert: PriceAlert): Long

    @Update
    suspend fun updatePriceAlert(alert: PriceAlert)

    @Delete
    suspend fun deletePriceAlert(alert: PriceAlert)

    // --- TRADE JOURNALS ---
    @Query("SELECT * FROM trade_journals ORDER BY timestamp DESC")
    fun getAllJournalEntriesFlow(): Flow<List<TradeJournalEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournalEntry(entry: TradeJournalEntry): Long

    @Delete
    suspend fun deleteJournalEntry(entry: TradeJournalEntry)
}

