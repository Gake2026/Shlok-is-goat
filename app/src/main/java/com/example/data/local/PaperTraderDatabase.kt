package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PortfolioState::class,
        Holding::class,
        TradeTransaction::class,
        WatchlistItem::class,
        PendingOrder::class,
        PriceAlert::class,
        TradeJournalEntry::class
    ],
    version = 6,
    exportSchema = false
)
abstract class PaperTraderDatabase : RoomDatabase() {

    abstract fun paperTraderDao(): PaperTraderDao

    companion object {
        @Volatile
        private var INSTANCE: PaperTraderDatabase? = null

        fun getDatabase(context: Context): PaperTraderDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PaperTraderDatabase::class.java,
                    "solana_paper_trader_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
