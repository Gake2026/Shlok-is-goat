package com.example.data.api

import retrofit2.http.GET
import retrofit2.http.Path

// API Architecture
// This is the Retrofit interface that would connect to a real DEX or market aggregator (like DexScreener, Jupiter, or CoinGecko).
// By separating the API definition, we ensure the project is ready for full production deployment.
interface SolanaMemeApi {

    @GET("v1/solana/meme-coins")
    suspend fun getMemeCoins(): List<ApiCoinResponse>

    @GET("v1/solana/meme-coins/{id}/history")
    suspend fun getCoinHistory(@Path("id") id: String): ApiCoinHistoryResponse
}

data class ApiCoinResponse(
    val id: String,
    val name: String,
    val symbol: String,
    val priceUsd: Double,
    val percentChange24h: Double,
    val volume24h: Double,
    val marketCap: Double,
    val description: String
)

data class ApiCoinHistoryResponse(
    val id: String,
    val prices: List<Double>
)
