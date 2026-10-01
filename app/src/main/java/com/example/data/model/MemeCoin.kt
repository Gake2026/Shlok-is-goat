package com.example.data.model

data class MemeCoin(
    val id: String,                  // This will be the pair address or token address
    val name: String,
    val symbol: String,
    val logoUrl: String?,
    val bannerUrl: String? = null,
    val currentPrice: Double,
    val priceChange24h: Double,      // e.g. +12.4
    val priceChange5m: Double = 0.0,
    val priceChange1h: Double = 0.0,
    val priceChange6h: Double = 0.0,
    val liquidityUsd: Double,
    val fdv: Double,
    val marketCap: Double,
    val volume24h: Double,
    val buys24h: Int = 0,
    val sells24h: Int = 0,
    val pairAge: String,             // Formatted e.g. "12h" or "2d" or "New"
    val dexId: String,               // e.g. "raydium", "pumpfun"
    val contractAddress: String,
    val sparkline: List<Double> = emptyList(),
    val isNew: Boolean = false,      // Auto-detected newly launched indicator
    val pairCreatedAt: Long = 0L,    // Raw timestamp for sorting/filtering
    val chain: String = "solana",    // Blockchain e.g. "solana", "robinhood", "ethereum", "bsc", "base", "arbitrum", "polygon"
    
    // --- Rich DexScreener Derived Fields ---
    val priceNative: Double? = null,           // Price in quote asset (e.g. SOL)
    val quoteTokenSymbol: String = "SOL",      // Quote asset symbol (e.g. "SOL", "USDC")
    val holdersCount: Int = 0,                 // Realistically derived on-chain holders
    val holdersChange24h: Double = 0.0,        // 24h net holders delta percentage
    val liquidityBase: Double = 0.0,           // Base token amount in liquidity pool
    val liquidityQuote: Double = 0.0,          // Quote token amount (SOL/USDC) in pool
    val volume5m: Double = 0.0,
    val volume1h: Double = 0.0,
    val volume6h: Double = 0.0,
    val buys5m: Int = 0,
    val sells5m: Int = 0,
    val buys1h: Int = 0,
    val sells1h: Int = 0,
    val buys6h: Int = 0,
    val sells6h: Int = 0,
    val dexUrl: String? = null,                // Direct DexScreener pair URL
    val websites: List<String> = emptyList(),  // Websites from DexScreener info
    val socials: Map<String, String> = emptyMap(), // Social links (twitter, telegram, discord)
    val circulatingSupply: Double = 1_000_000_000.0 // Circulating token supply
)

