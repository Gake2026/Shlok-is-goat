package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class DexScreenerResponse(
    @Json(name = "pairs") val pairs: List<DexPair>?
)

@JsonClass(generateAdapter = true)
data class DexPair(
    @Json(name = "chainId") val chainId: String,
    @Json(name = "dexId") val dexId: String,
    @Json(name = "url") val url: String?,
    @Json(name = "pairAddress") val pairAddress: String,
    @Json(name = "baseToken") val baseToken: DexToken,
    @Json(name = "quoteToken") val quoteToken: DexToken?,
    @Json(name = "priceNative") val priceNative: String?,
    @Json(name = "priceUsd") val priceUsd: String?,
    @Json(name = "txns") val txns: DexTxns?,
    @Json(name = "volume") val volume: DexVolume?,
    @Json(name = "priceChange") val priceChange: DexPriceChange?,
    @Json(name = "liquidity") val liquidity: DexLiquidity?,
    @Json(name = "fdv") val fdv: Double?,
    @Json(name = "marketCap") val marketCap: Double?,
    @Json(name = "pairCreatedAt") val pairCreatedAt: Long?,
    @Json(name = "info") val info: DexInfo?
)

@JsonClass(generateAdapter = true)
data class DexToken(
    @Json(name = "address") val address: String,
    @Json(name = "name") val name: String?,
    @Json(name = "symbol") val symbol: String?
)

@JsonClass(generateAdapter = true)
data class DexTxns(
    @Json(name = "m5") val m5: DexTxnDirection?,
    @Json(name = "h1") val h1: DexTxnDirection?,
    @Json(name = "h6") val h6: DexTxnDirection?,
    @Json(name = "h24") val h24: DexTxnDirection?
)

@JsonClass(generateAdapter = true)
data class DexTxnDirection(
    @Json(name = "buys") val buys: Int?,
    @Json(name = "sells") val sells: Int?
)

@JsonClass(generateAdapter = true)
data class DexVolume(
    @Json(name = "m5") val m5: Double?,
    @Json(name = "h1") val h1: Double?,
    @Json(name = "h6") val h6: Double?,
    @Json(name = "h24") val h24: Double?
)

@JsonClass(generateAdapter = true)
data class DexPriceChange(
    @Json(name = "m5") val m5: Double?,
    @Json(name = "h1") val h1: Double?,
    @Json(name = "h6") val h6: Double?,
    @Json(name = "h24") val h24: Double?
)

@JsonClass(generateAdapter = true)
data class DexLiquidity(
    @Json(name = "usd") val usd: Double?,
    @Json(name = "base") val base: Double?,
    @Json(name = "quote") val quote: Double?
)

@JsonClass(generateAdapter = true)
data class DexInfo(
    @Json(name = "imageUrl") val imageUrl: String?,
    @Json(name = "header") val header: String?,
    @Json(name = "websites") val websites: List<DexWebsite>?,
    @Json(name = "socials") val socials: List<DexSocial>?
)

@JsonClass(generateAdapter = true)
data class DexWebsite(
    @Json(name = "label") val label: String?,
    @Json(name = "url") val url: String?
)

@JsonClass(generateAdapter = true)
data class DexSocial(
    @Json(name = "platform") val platform: String?,
    @Json(name = "handle") val handle: String?
)

@JsonClass(generateAdapter = true)
data class DexTokenBoost(
    @Json(name = "url") val url: String?,
    @Json(name = "chainId") val chainId: String?,
    @Json(name = "tokenAddress") val tokenAddress: String?,
    @Json(name = "icon") val icon: String?,
    @Json(name = "description") val description: String?,
    @Json(name = "totalAmount") val totalAmount: Double?,
    @Json(name = "amount") val amount: Double?
)

@JsonClass(generateAdapter = true)
data class DexTokenProfile(
    @Json(name = "url") val url: String?,
    @Json(name = "chainId") val chainId: String?,
    @Json(name = "tokenAddress") val tokenAddress: String?,
    @Json(name = "icon") val icon: String?,
    @Json(name = "header") val header: String?,
    @Json(name = "description") val description: String?
)

interface DexScreenerApi {
    @GET("latest/dex/search")
    suspend fun searchPairs(@Query("q") query: String): DexScreenerResponse

    @GET("token-boosts/latest/v1")
    suspend fun getLatestTokenBoosts(): List<DexTokenBoost>

    @GET("token-boosts/top/v1")
    suspend fun getTopTokenBoosts(): List<DexTokenBoost>

    @GET("token-profiles/latest/v1")
    suspend fun getLatestTokenProfiles(): List<DexTokenProfile>

    @GET("token-profiles/recent-updates/v1")
    suspend fun getRecentTokenProfiles(): List<DexTokenProfile>

    @GET("tokens/v1/{chainId}/{tokenAddresses}")
    suspend fun getTokensByAddresses(
        @Path("chainId") chainId: String,
        @Path("tokenAddresses") tokenAddresses: String
    ): List<DexPair>

    @GET("token-pairs/v1/{chainId}/{tokenAddress}")
    suspend fun getTokenPairsByAddress(
        @Path("chainId") chainId: String,
        @Path("tokenAddress") tokenAddress: String
    ): List<DexPair>

    @GET("latest/dex/pairs/{chainId}/{pairId}")
    suspend fun getPairById(
        @Path("chainId") chainId: String,
        @Path("pairId") pairId: String
    ): DexScreenerResponse
}
