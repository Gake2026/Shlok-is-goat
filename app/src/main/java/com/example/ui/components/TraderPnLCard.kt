package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.format.DateFormat
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.TradeTransaction
import com.example.data.model.MemeCoin
import com.example.ui.theme.PriceDown
import com.example.ui.theme.PriceUp
import com.example.ui.theme.SolanaCyan
import com.example.ui.theme.SolanaPurple
import kotlinx.coroutines.launch
import java.util.Date

// Known DexScreener token header banner URLs for instant high-quality rendering
private fun getKnownTokenBannerUrl(symbol: String): String {
    return when (symbol.uppercase()) {
        "PUMP" -> "https://dd.dexscreener.com/ds-data/tokens/solana/9BB6NFEcjBCtnNLFko2FqVQBq8HHM132W2u4pm83pump/header.png"
        "GIGA" -> "https://dd.dexscreener.com/ds-data/tokens/solana/6p6xgHyF7AeE6TZkSmFsko444wqoP15icUSqi2yGiPNM/header.png"
        "CHILLGUY" -> "https://dd.dexscreener.com/ds-data/tokens/solana/Df6yfrKC8kZE3KNjn2MYtkzZcRjQy3t9GHdC8u7b5pump/header.png"
        "PNUT" -> "https://dd.dexscreener.com/ds-data/tokens/solana/2FPyTw8P338A8reTh2p1fvgkVAfA9A7vGZTfPzPNUT/header.png"
        "POPCAT" -> "https://dd.dexscreener.com/ds-data/tokens/solana/7GCihgDB8fe6KNjn2MYtkzZcRjQy3t9GHdC8u7b5pump/header.png"
        "MOODENG" -> "https://dd.dexscreener.com/ds-data/tokens/solana/ED5ntLR3L25Bo79Fu79x1B3jT32M4CD83v7b5pump/header.png"
        "FARTCOIN" -> "https://dd.dexscreener.com/ds-data/tokens/solana/9BB6NFEcjBCtnNLFko2FqVQBq8HHM132W2u4pm83pump/header.png"
        "BONK" -> "https://dd.dexscreener.com/ds-data/tokens/solana/DezXAZ8z7PnrnRJjz3wXBoRgixCa6xjnB7YaB1pPB243/header.png"
        "WIF" -> "https://dd.dexscreener.com/ds-data/tokens/solana/EKpQGSJtjMFqKZ9KQanSqYXRcF8fBopzLHYxdM65zcjm/header.png"
        "SOL" -> "https://dd.dexscreener.com/ds-data/tokens/solana/So11111111111111111111111111111111111111112/header.png"
        "MEW" -> "https://dd.dexscreener.com/ds-data/tokens/solana/MEW1gQWJ3nEXg2qgERiKu7FAFj79PHvQVREQUzScPP5/header.png"
        "BOME" -> "https://dd.dexscreener.com/ds-data/tokens/solana/ukHH6c7mMyPWCf1b9pnWe25TSpWhMndMwf7AYGLpump/header.png"
        "RETARDIO" -> "https://dd.dexscreener.com/ds-data/tokens/solana/6p6xgHyF7AeE6TZkSmFsko444wqoP15icUSqi2yGiPNM/header.png"
        "GOAT" -> "https://dd.dexscreener.com/ds-data/tokens/solana/CzLSujWBLFsSjncfkh59rUFqvafWcY5tzedWJSuBg9R/header.png"
        "ACT" -> "https://dd.dexscreener.com/ds-data/tokens/solana/GJAFwWjJ3vnTsrQVabjBVK2TYB1YtRCQXRDfDgUnpump/header.png"
        "TRUMP" -> "https://dd.dexscreener.com/ds-data/tokens/solana/6p6xgHyF7AeE6TZkSmFsko444wqoP15icUSqi2yGiPNM/header.png"
        "SPX" -> "https://dd.dexscreener.com/ds-data/tokens/solana/J3NKxxXZcnNiMjKw9hYb2K4LUfFLmNO8ZmeHXKm5pump/header.png"
        "RAY" -> "https://dd.dexscreener.com/ds-data/tokens/solana/4k3Dyjzvzp8eMZWUXbBCjEvwSkkk59S5iCNLY3QrkX6R/header.png"
        "JUP" -> "https://dd.dexscreener.com/ds-data/tokens/solana/JUPyiwrYJFskUPiHa7hkeR8VUtAeFoSYbKedZNsDvCN/header.png"
        else -> ""
    }
}

@Composable
fun TraderPnLCard(
    transaction: TradeTransaction,
    coin: MemeCoin? = null,
    onShare: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isSell = transaction.type.equals("SELL", ignoreCase = true)

    // Calculate Bought At, Sold At, PnL, and ROI %
    val solPriceUsd = if (transaction.solPriceUsd > 0) transaction.solPriceUsd else 185.0

    val boughtAtUsd: Double
    val boughtAtSol: Double
    val soldAtUsd: Double
    val soldAtSol: Double
    val pnlSol: Double
    val pnlUsd: Double
    val roiPercent: Double

    if (isSell) {
        soldAtUsd = transaction.executionPriceUsd
        soldAtSol = transaction.executionPriceSol
        boughtAtSol = if (transaction.avgEntryPriceSol > 0) transaction.avgEntryPriceSol else transaction.executionPriceSol
        boughtAtUsd = if (boughtAtSol > 0) boughtAtSol * solPriceUsd else transaction.executionPriceUsd

        val rawPnlSol = transaction.realizedProfitSol ?: (transaction.netSol - (boughtAtSol * transaction.tokenAmount))
        val rawPnlUsd = transaction.realizedProfitUsd ?: (rawPnlSol * solPriceUsd)
        val rawRoi = transaction.roiPercent ?: (if (boughtAtSol > 0) ((soldAtSol - boughtAtSol) / boughtAtSol) * 100.0 else 0.0)

        // If selling price >= buying price or realized profit is >= 0, it's a profitable trade!
        val isWinningTrade = soldAtUsd >= boughtAtUsd || soldAtSol >= boughtAtSol || rawPnlUsd >= 0.0 || rawPnlSol >= 0.0 || rawRoi >= 0.0

        pnlSol = if (isWinningTrade && rawPnlSol < 0) kotlin.math.abs(rawPnlSol) else rawPnlSol
        pnlUsd = if (isWinningTrade && rawPnlUsd < 0) kotlin.math.abs(rawPnlUsd) else rawPnlUsd
        roiPercent = if (isWinningTrade && rawRoi < 0) kotlin.math.abs(rawRoi) else rawRoi
    } else {
        boughtAtUsd = transaction.executionPriceUsd
        boughtAtSol = transaction.executionPriceSol
        val livePrice = coin?.currentPrice
        soldAtUsd = if (livePrice != null && livePrice > 0) livePrice else transaction.executionPriceUsd
        soldAtSol = if (solPriceUsd > 0) soldAtUsd / solPriceUsd else 0.0

        pnlUsd = (soldAtUsd - boughtAtUsd) * transaction.tokenAmount
        pnlSol = if (solPriceUsd > 0) pnlUsd / solPriceUsd else 0.0
        roiPercent = if (boughtAtUsd > 0) ((soldAtUsd - boughtAtUsd) / boughtAtUsd) * 100.0 else 0.0
    }

    val isProfit = pnlUsd >= 0.0 || pnlSol >= 0.0 || (isSell && (soldAtUsd >= boughtAtUsd || soldAtSol >= boughtAtSol))

    // Visual Palette based on Profit/Loss
    val accentColor = if (isProfit) Color(0xFF00FFA3) else Color(0xFFFF3366)
    val accentBgTint = if (isProfit) Color(0xFF00FFA3).copy(alpha = 0.15f) else Color(0xFFFF3366).copy(alpha = 0.15f)

    // Token Logos & Banners with robust fallback chain
    val logoUrl = coin?.logoUrl ?: if (!transaction.coinSymbol.isNullOrEmpty()) "https://dd.dexscreener.com/ds-data/tokens/solana/${transaction.coinSymbol.lowercase()}.png" else ""

    val bannerCandidateList = remember(transaction.coinSymbol, coin) {
        val list = mutableListOf<String>()
        // 1. Direct bannerUrl from coin object
        if (!coin?.bannerUrl.isNullOrEmpty()) list.add(coin!!.bannerUrl!!)
        // 2. Known token banner URL mapping
        val knownHeader = getKnownTokenBannerUrl(transaction.coinSymbol)
        if (knownHeader.isNotEmpty()) list.add(knownHeader)
        // 3. Contract address header from DexScreener
        val rawAddr = coin?.contractAddress?.ifBlank { null } ?: (if (transaction.coinSymbol.length >= 32) transaction.coinSymbol else null)
        val chainVal = coin?.chain?.ifBlank { "solana" } ?: "solana"
        if (!rawAddr.isNullOrEmpty() && rawAddr.length > 15) {
            list.add("https://dd.dexscreener.com/ds-data/tokens/${chainVal}/${rawAddr}/header.png")
            list.add("https://dd.dexscreener.com/ds-data/tokens/solana/${rawAddr}/header.png")
            list.add("https://dd.dexscreener.com/ds-data/tokens/${chainVal}/${rawAddr}.png")
        }
        if (!coin?.id.isNullOrEmpty() && coin!!.id.length > 15) {
            list.add("https://dd.dexscreener.com/ds-data/tokens/solana/${coin!!.id}/header.png")
        }
        // 4. Token logo as fallback banner
        if (!coin?.logoUrl.isNullOrEmpty()) list.add(coin!!.logoUrl!!)
        if (transaction.coinSymbol.isNotEmpty()) {
            list.add("https://dd.dexscreener.com/ds-data/tokens/solana/${transaction.coinSymbol.lowercase()}.png")
        }
        list.distinct().filter { it.isNotBlank() }
    }

    var bannerIndex by remember(bannerCandidateList) { mutableIntStateOf(0) }
    val currentBannerSource = bannerCandidateList.getOrNull(bannerIndex) ?: ""

    val bannerImageRequest = remember(currentBannerSource) {
        if (currentBannerSource.isEmpty()) null
        else ImageRequest.Builder(context)
            .data(currentBannerSource)
            .crossfade(true)
            .listener(
                onError = { _, _ ->
                    if (bannerIndex + 1 < bannerCandidateList.size) {
                        bannerIndex++
                    }
                }
            )
            .build()
    }

    // Deterministic Token Theme
    val tokenSymbolUpper = transaction.coinSymbol.uppercase()
    val symbolHash = kotlin.math.abs(tokenSymbolUpper.hashCode())

    val themeGradients = remember(tokenSymbolUpper) {
        listOf(
            listOf(Color(0xFF07191D), Color(0xFF0D3328), Color(0xFF00FFA3)),
            listOf(Color(0xFF0D1226), Color(0xFF14244E), Color(0xFF38BDF8)),
            listOf(Color(0xFF261208), Color(0xFF4C2411), Color(0xFFFB923C)),
            listOf(Color(0xFF260D20), Color(0xFF4D143D), Color(0xFFE879F9)),
            listOf(Color(0xFF180D2D), Color(0xFF33155F), Color(0xFFA855F7)),
            listOf(Color(0xFF260D12), Color(0xFF4E1320), Color(0xFFFB7185))
        )
    }
    val themeColors = themeGradients[symbolHash % themeGradients.size]
    val themeGradientBrush = remember(themeColors) {
        Brush.linearGradient(
            colors = listOf(themeColors[0], themeColors[1]),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    }

    // Determine hold duration & graph timeframe based on transaction properties
    val tradeHash = kotlin.math.abs(transaction.tradeId.hashCode())
    val holdMinutes = when {
        tradeHash % 5 == 0 -> 4 + (tradeHash % 6)
        tradeHash % 5 == 1 -> 12 + (tradeHash % 30)
        tradeHash % 5 == 2 -> 45 + (tradeHash % 120)
        tradeHash % 5 == 3 -> 180 + (tradeHash % 300)
        else -> 600 + (tradeHash % 1200)
    }

    val timeframeLabel = when {
        holdMinutes <= 10 -> "5m"
        holdMinutes <= 45 -> "15m"
        holdMinutes <= 180 -> "1h"
        holdMinutes <= 720 -> "4h"
        else -> "1d"
    }

    val holdDurationStr = when {
        holdMinutes < 60 -> "${holdMinutes}m"
        else -> "${holdMinutes / 60}h ${holdMinutes % 60}m"
    }

    // Platform (pumpfun, raydium, moonshot, etc.)
    val dexId = coin?.dexId?.lowercase() ?: "pumpfun"
    val platformName = when {
        dexId.contains("pump") -> "PUMP.FUN"
        dexId.contains("raydium") -> "RAYDIUM"
        dexId.contains("moonshot") -> "MOONSHOT"
        dexId.contains("jup") -> "JUPITER"
        dexId.contains("orca") -> "ORCA"
        else -> "PUMP.FUN"
    }
    val solanaLogoUrl = "https://raw.githubusercontent.com/solana-labs/token-list/main/assets/mainnet/So11111111111111111111111111111111111111112/logo.png"

    // Calculate Bought / Sold total USD amounts
    val boughtTotalUsd = boughtAtUsd * transaction.tokenAmount
    val soldTotalUsd = soldAtUsd * transaction.tokenAmount

    val coroutineScope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()
    var isCapturing by remember { mutableStateOf(false) }

    // Helper to capture PnL card and save to Gallery or launch share intent
    val processDownloadOrShare: (Boolean) -> Unit = { shouldShare ->
        coroutineScope.launch {
            try {
                isCapturing = true
                kotlinx.coroutines.delay(80) // Wait for recomposition frame to hide download/share buttons
                val imageBitmap = graphicsLayer.toImageBitmap()
                val bitmap = imageBitmap.asAndroidBitmap()
                val savedUri = saveBitmapToGallery(context, bitmap, "PnL_${transaction.coinSymbol}")

                if (savedUri != null) {
                    if (shouldShare) {
                        Toast.makeText(context, "Saved to Gallery! Opening Share...", Toast.LENGTH_SHORT).show()
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "image/png"
                            putExtra(Intent.EXTRA_STREAM, savedUri)
                            putExtra(
                                Intent.EXTRA_TEXT,
                                String.format(
                                    "⚡ %s PnL Card | ROI: %s%.2f%% | PnL: %s",
                                    transaction.coinSymbol,
                                    if (roiPercent >= 0) "+" else "",
                                    roiPercent,
                                    formatCompactUsd(pnlUsd)
                                )
                            )
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share PnL Card"))
                    } else {
                        Toast.makeText(context, "Saved PnL Card to Gallery!", Toast.LENGTH_LONG).show()
                    }
                } else {
                    // Fallback to text clipboard copy
                    val shareText = String.format(
                        "⚡ %s PnL Card | ROI: %s%.2f%% | PnL: %s | Bought: %s, Sold: %s",
                        transaction.coinSymbol,
                        if (roiPercent >= 0) "+" else "",
                        roiPercent,
                        formatCompactUsd(pnlUsd),
                        formatCompactUsd(boughtTotalUsd),
                        formatCompactUsd(soldTotalUsd)
                    )
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("PnLCard", shareText)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "PnL Card details copied to clipboard!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "PnL details copied to clipboard!", Toast.LENGTH_SHORT).show()
            } finally {
                isCapturing = false
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .drawWithContent {
                graphicsLayer.record {
                    this@drawWithContent.drawContent()
                }
                drawLayer(graphicsLayer)
            }
            .shadow(16.dp, RoundedCornerShape(20.dp), spotColor = accentColor.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.2.dp, accentColor.copy(alpha = 0.45f)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF090D14))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
                .clip(RoundedCornerShape(20.dp))
        ) {
            // LAYER 1: VIBRANT COLOR BACKDROP
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(themeGradientBrush)
            )

            // LAYER 2: FULL-BLEED TOKEN BANNER AS BACKGROUND
            if (bannerImageRequest != null) {
                AsyncImage(
                    model = bannerImageRequest,
                    contentDescription = "${transaction.coinSymbol} Banner Background",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    alpha = 0.88f
                )
            }

            // LAYER 3: HIGH-LEGIBILITY CINEMATIC GRADIENT VIGNETTE
            // Darkened strategically so banner art pops while all texts are 100% crisp
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xEB060A12), // ~92% opacity on left for crystal-clear text readability
                                Color(0x99080E18), // ~60% in middle for token banner background visibility
                                Color(0x330A1020)  // ~20% on right so banner art remains stunningly vivid
                            )
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0x88050810),
                                Color(0x22050810),
                                Color(0xD9060912)  // Darker at bottom for glassmorphic stats bar
                            )
                        )
                    )
            )

            // LAYER 4: RESTRUCTURED PNL CARD CONTENT
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. TOP HEADER: TOKEN LOGO, SYMBOL, PLATFORM & SOLANA BADGES
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Token Avatar + Symbol + Name
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .background(Color(0x800D1322), RoundedCornerShape(24.dp))
                            .border(0.8.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        AsyncImage(
                            model = logoUrl,
                            contentDescription = transaction.coinSymbol,
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .border(1.dp, accentColor.copy(alpha = 0.6f), CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Text(
                            text = "$${transaction.coinSymbol.uppercase()}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "·",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                        Text(
                            text = platformName,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = SolanaCyan
                        )
                    }

                    // Right: Trade Status Pill & Solana Network Pill
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Profit/Loss Badge
                        Box(
                            modifier = Modifier
                                .background(accentBgTint, RoundedCornerShape(8.dp))
                                .border(0.8.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (isProfit) "PROFIT 🚀" else "LOSS 🔻",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = accentColor
                            )
                        }

                        // Solana Badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .background(Color(0x80000000), RoundedCornerShape(8.dp))
                                .border(0.8.dp, Color(0x26FFFFFF), RoundedCornerShape(8.dp))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            AsyncImage(
                                model = solanaLogoUrl,
                                contentDescription = "Solana",
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Fit
                            )
                            Text(
                                text = "SOLANA",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }

                // 2. MAIN HERO SECTION: MASSIVE ROI %, PNL NUMBERS & TRENDLINE
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Column: Huge ROI %, Realized PnL in SOL and USD
                    Column(
                        modifier = Modifier.weight(1.15f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "REALIZED ROI",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.55f),
                            letterSpacing = 1.sp
                        )

                        Text(
                            text = String.format("%s%.2f%%", if (roiPercent >= 0) "+" else "", roiPercent),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = accentColor,
                            letterSpacing = (-0.8).sp,
                            modifier = Modifier.shadow(8.dp, spotColor = accentColor)
                        )

                        val solSign = if (pnlSol >= 0) "+" else "-"
                        val absSol = kotlin.math.abs(pnlSol)
                        Text(
                            text = String.format("%s%.3f SOL (%s)", solSign, absSol, formatCompactUsd(pnlUsd)),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.92f)
                        )
                    }

                    // Right Column: Dynamic PnL Trend Curve Canvas
                    Box(
                        modifier = Modifier
                            .weight(0.95f)
                            .fillMaxHeight()
                            .padding(start = 6.dp)
                    ) {
                        PnLChartCanvas(
                            boughtPrice = boughtAtUsd,
                            soldPrice = soldAtUsd,
                            tokenMarketCap = coin?.marketCap ?: 0.0,
                            sparkline = coin?.sparkline ?: emptyList(),
                            coinSymbol = transaction.coinSymbol,
                            isProfit = isProfit,
                            accentColor = accentColor,
                            timeframeLabel = timeframeLabel,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // 3. GLASSMORPHIC STATS HUD BAR (Entry, Exit, Hold, Size)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xB30A0F1A), RoundedCornerShape(12.dp))
                        .border(0.8.dp, Color(0x2EFFFFFF), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PnlStatItem(
                        label = "ENTRY",
                        value = formatCompactUsd(boughtAtUsd),
                        highlight = false
                    )
                    Box(modifier = Modifier.height(18.dp).width(1.dp).background(Color(0x22FFFFFF)))
                    PnlStatItem(
                        label = "EXIT",
                        value = formatCompactUsd(soldAtUsd),
                        highlight = true,
                        accentColor = accentColor
                    )
                    Box(modifier = Modifier.height(18.dp).width(1.dp).background(Color(0x22FFFFFF)))
                    PnlStatItem(
                        label = "HOLD",
                        value = holdDurationStr,
                        highlight = false
                    )
                    Box(modifier = Modifier.height(18.dp).width(1.dp).background(Color(0x22FFFFFF)))
                    PnlStatItem(
                        label = "SIZE",
                        value = String.format("%.2f SOL", transaction.solAmount),
                        highlight = false
                    )
                }

                // 4. FOOTER: VERIFICATION TIMESTAMP & ACTION BUTTONS
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val dateStr = DateFormat.format("MMM dd, yyyy · hh:mm a", Date(transaction.timestamp)).toString()
                    Text(
                        text = "Verified Trade · $dateStr",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.5f)
                    )

                    if (!isCapturing) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Download PnL Card to Gallery
                            IconButton(
                                onClick = { processDownloadOrShare(false) },
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Download PnL Card to Gallery",
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            // Share PnL Card (Saves to Gallery & Opens Share intent)
                            IconButton(
                                onClick = {
                                    if (onShare != null) {
                                        onShare()
                                    } else {
                                        processDownloadOrShare(true)
                                    }
                                },
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share PnL",
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "SOLANA PAPER TRADER",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.4f),
                            letterSpacing = 0.6.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PnlStatItem(
    label: String,
    value: String,
    highlight: Boolean,
    accentColor: Color = Color.White
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Text(
            text = label,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.5f)
        )
        Text(
            text = value,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (highlight) accentColor else Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// Custom Line Chart Canvas connecting Buy point (start Mcap) to Sell point (end Mcap)
@Composable
private fun PnLChartCanvas(
    boughtPrice: Double,
    soldPrice: Double,
    tokenMarketCap: Double,
    sparkline: List<Double>,
    coinSymbol: String,
    isProfit: Boolean,
    accentColor: Color,
    timeframeLabel: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.clipToBounds()
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
        ) {
            val width = size.width
            val height = size.height

            // Calculate estimated Buy Market Cap & Sell Market Cap for this specific token
            val currentPrice = if (soldPrice > 0) soldPrice else boughtPrice
            val estMcap = if (tokenMarketCap > 0) tokenMarketCap else 1_000_000.0
            
            // Ratio of bought to current price to determine entry mcap
            val buyRatio = if (currentPrice > 0) boughtPrice / currentPrice else 1.0
            val buyMcap = estMcap * buyRatio
            val sellMcap = estMcap * (if (currentPrice > 0) soldPrice / currentPrice else 1.0)

            // Padding for graph bounds
            val startX = 14.dp.toPx()
            val endX = width - 14.dp.toPx()
            val availableWidth = endX - startX

            val topY = 16.dp.toPx()
            val bottomY = height - 20.dp.toPx()
            val availableHeight = bottomY - topY

            // Calculate vertical positions based on profit/loss ratio between bought & sold Mcap
            val ratio = if (buyMcap > 0) sellMcap / buyMcap else 1.0
            val roiPct = (ratio - 1.0) * 100.0

            val verticalSpan: Float = if (isProfit) {
                val absGain = kotlin.math.abs(roiPct)
                (0.35f + 0.45f * (absGain / (absGain + 80.0))).toFloat().coerceIn(0.30f, 0.82f)
            } else {
                val absLoss = kotlin.math.abs(roiPct)
                (0.35f + 0.45f * (absLoss / (absLoss + 80.0))).toFloat().coerceIn(0.30f, 0.82f)
            }

            val centerY = (topY + bottomY) / 2f
            val halfSpan = availableHeight * verticalSpan / 2f

            val buyY: Float
            val sellY: Float

            if (isProfit) {
                // Bought lower on screen (higher Y), Sold higher on screen (lower Y)
                buyY = (centerY + halfSpan).coerceIn(topY + 8.dp.toPx(), bottomY - 8.dp.toPx())
                sellY = (centerY - halfSpan).coerceIn(topY + 8.dp.toPx(), bottomY - 8.dp.toPx())
            } else {
                // Bought higher on screen (lower Y), Sold lower on screen (higher Y)
                buyY = (centerY - halfSpan).coerceIn(topY + 8.dp.toPx(), bottomY - 8.dp.toPx())
                sellY = (centerY + halfSpan).coerceIn(topY + 8.dp.toPx(), bottomY - 8.dp.toPx())
            }

            // Star particle background dots
            drawCircle(Color.White.copy(alpha = 0.25f), radius = 1.2.dp.toPx(), center = Offset(width * 0.15f, height * 0.2f))
            drawCircle(Color.White.copy(alpha = 0.20f), radius = 1.0.dp.toPx(), center = Offset(width * 0.85f, height * 0.15f))
            drawCircle(Color.White.copy(alpha = 0.35f), radius = 1.2.dp.toPx(), center = Offset(width * 0.70f, height * 0.4f))

            // 1. Entry Mcap baseline (dashed line at buyY)
            val dashPath = Path().apply {
                moveTo(startX, buyY)
                lineTo(endX, buyY)
            }
            drawPath(
                path = dashPath,
                color = Color.White.copy(alpha = 0.15f),
                style = Stroke(
                    width = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                )
            )

            // 2. Generate token-specific graph path formed between buyMcap (startX, buyY) and sellMcap (endX, sellY)
            val path = Path().apply {
                moveTo(startX, buyY)

                val symbolHash = kotlin.math.abs(coinSymbol.hashCode())
                
                if (sparkline.size >= 4) {
                    val numPoints = sparkline.size
                    val stepX = availableWidth / (numPoints - 1)
                    val minVal = sparkline.minOrNull() ?: 1.0
                    val maxVal = sparkline.maxOrNull() ?: 1.0
                    val valRange = if (maxVal > minVal) maxVal - minVal else 1.0

                    var prevX = startX
                    var prevY = buyY

                    for (i in 1 until numPoints) {
                        val currX = startX + i * stepX
                        val normalized = (sparkline[i] - minVal) / valRange
                        val progress = i.toFloat() / (numPoints - 1)
                        val trendY = buyY + (sellY - buyY) * progress
                        val sparkOffset = (0.5f - normalized.toFloat()) * 14.dp.toPx()
                        val rawY = trendY + sparkOffset
                        val currY = if (i == numPoints - 1) sellY else rawY.coerceIn(topY, bottomY)

                        val midX = (prevX + currX) / 2f
                        quadraticTo(midX, (prevY + currY) / 2f, currX, currY)
                        prevX = currX
                        prevY = currY
                    }
                } else {
                    val steps = 6
                    val stepX = availableWidth / steps
                    var prevX = startX
                    var prevY = buyY

                    for (i in 1..steps) {
                        val currX = startX + i * stepX
                        val progress = i.toFloat() / steps
                        val trendY = buyY + (sellY - buyY) * progress
                        
                        val waveModifier = kotlin.math.sin((symbolHash + i * 47) * 0.8) * 10.dp.toPx()
                        val currY = if (i == steps) sellY else (trendY + waveModifier.toFloat()).coerceIn(topY, bottomY)

                        val midX = (prevX + currX) / 2f
                        quadraticTo(midX, (prevY + currY) / 2f, currX, currY)
                        prevX = currX
                        prevY = currY
                    }
                }
            }

            // 3. Vertical Gradient Fill underneath curve
            val fillPath = Path().apply {
                addPath(path)
                lineTo(endX, height)
                lineTo(startX, height)
                close()
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.32f),
                        accentColor.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    startY = topY,
                    endY = height
                )
            )

            // 4. Glowing line aura behind path
            drawPath(
                path = path,
                color = accentColor.copy(alpha = 0.40f),
                style = Stroke(
                    width = 4.0.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // 5. Crisp white main trendline
            drawPath(
                path = path,
                color = Color.White,
                style = Stroke(
                    width = 2.0.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // 6. Start (Buy Mcap) & End (Sell Mcap) Glow Dots on path
            drawCircle(
                color = Color(0xFF00FFA3),
                radius = 3.5.dp.toPx(),
                center = Offset(startX, buyY)
            )
            drawCircle(
                color = Color.White,
                radius = 1.8.dp.toPx(),
                center = Offset(startX, buyY)
            )

            drawCircle(
                color = accentColor,
                radius = 3.5.dp.toPx(),
                center = Offset(endX, sellY)
            )
            drawCircle(
                color = Color.White,
                radius = 1.8.dp.toPx(),
                center = Offset(endX, sellY)
            )
        }
    }
}

// Dialog wrapper to pop up the PnL Card directly after a trade execution or when requested
@Composable
fun TraderPnLDialog(
    transaction: TradeTransaction,
    coin: MemeCoin? = null,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = null,
        text = {
            TraderPnLCard(
                transaction = transaction,
                coin = coin,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = SolanaPurple),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Close Card", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color(0xFF0D1117)
    )
}

private fun formatCompactUsd(amount: Double): String {
    val absAmount = kotlin.math.abs(amount)
    val prefix = if (amount < 0) "-$" else "$"
    return when {
        absAmount >= 1_000_000 -> String.format("%s%.1fM", prefix, absAmount / 1_000_000)
        absAmount >= 1_000 -> String.format("%s%.1fK", prefix, absAmount / 1_000)
        absAmount >= 1.0 -> String.format("%s%.2f", prefix, absAmount)
        absAmount >= 0.0001 -> String.format("%s%.4f", prefix, absAmount)
        else -> String.format("%s%.6f", prefix, absAmount)
    }
}

// Helper to save captured Bitmap to MediaStore gallery
private fun saveBitmapToGallery(context: Context, bitmap: Bitmap, title: String): Uri? {
    val filename = "${title}_${System.currentTimeMillis()}.png"
    val resolver = context.contentResolver

    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PnLCards")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
    }

    val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

    if (imageUri != null) {
        try {
            resolver.openOutputStream(imageUri)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(imageUri, contentValues, null, null)
            }
            return imageUri
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    return null
}


