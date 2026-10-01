package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.text.format.DateFormat
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.DEFAULT_SOLANA_TOKENS
import com.example.data.repository.ROBINHOOD_TOKENS
import com.example.data.local.TradeJournalEntry
import com.example.data.local.TradeTransaction
import com.example.ui.components.TraderPnLCard
import com.example.ui.components.TraderPnLDialog
import com.example.ui.theme.PriceDown
import com.example.ui.theme.PriceUp
import com.example.ui.theme.SolanaCyan
import com.example.ui.theme.SolanaGreen
import com.example.ui.theme.SolanaPurple
import com.example.ui.theme.SolanaSurface
import com.example.ui.viewmodel.TradingViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradeHistoryScreen(
    viewModel: TradingViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val transactions by viewModel.transactions.collectAsState()
    val journalEntries by viewModel.journalEntries.collectAsState()
    val coins by viewModel.coins.collectAsState()
    val coinMap = remember(coins) {
        (DEFAULT_SOLANA_TOKENS + ROBINHOOD_TOKENS + coins).associateBy { it.symbol.uppercase() }
    }

    var showExportDialog by remember { mutableStateOf(false) }
    var journalTargetTx by remember { mutableStateOf<TradeTransaction?>(null) }
    var pnlTargetTx by remember { mutableStateOf<TradeTransaction?>(null) }
    var selectedTab by remember { mutableStateOf(0) } // 0: PnL Cards, 1: Fills Log

    pnlTargetTx?.let { tx ->
        TraderPnLDialog(
            transaction = tx,
            coin = coinMap[tx.coinSymbol.uppercase()],
            onDismiss = { pnlTargetTx = null }
        )
    }

    // Dialog for Export CSV
    if (showExportDialog) {
        val csvData = remember { viewModel.exportTradeHistoryCsv() }
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Export Trade History (CSV)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Copy your executed trades log to clipboard in standard CSV format:", style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = csvData,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        textStyle = MaterialTheme.typography.labelSmall
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("TradeHistoryCSV", csvData)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Trade history CSV copied!", Toast.LENGTH_SHORT).show()
                        showExportDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SolanaPurple)
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Copy CSV")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Close")
                }
            },
            containerColor = SolanaSurface
        )
    }

    // Modal Sheet for Trade Journal Entry
    journalTargetTx?.let { tx ->
        val existingJournal = journalEntries.find { it.tradeId == tx.tradeId || it.tradeId == tx.txHash }
        var noteText by remember { mutableStateOf(existingJournal?.note ?: "") }
        var selectedEmotion by remember { mutableStateOf(existingJournal?.emotionTag ?: "DISCIPLINED") }
        var selectedStrategy by remember { mutableStateOf(existingJournal?.strategyTag ?: "SWING") }
        var rating by remember { mutableStateOf(existingJournal?.ratingStars ?: 5) }

        ModalBottomSheet(
            onDismissRequest = { journalTargetTx = null },
            containerColor = SolanaSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Trade Journal — ${tx.coinSymbol}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Trade Rationale & Reflection Note") },
                    placeholder = { Text("e.g. Bought 15m breakout after retest of support. Clean execution.") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp)
                )

                // Emotion Tag Selection
                Text("Emotional State:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("CALM", "FOMO", "GREEDY", "DISCIPLINED", "PANIC").forEach { tag ->
                        val isSel = selectedEmotion == tag
                        FilterChip(
                            selected = isSel,
                            onClick = { selectedEmotion = tag },
                            label = { Text(tag, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SolanaPurple,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                // Strategy Tag Selection
                Text("Trading Strategy:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("BREAKOUT", "DIP_BUY", "MEME_TREND", "SWING", "SCALP").forEach { tag ->
                        val isSel = selectedStrategy == tag
                        FilterChip(
                            selected = isSel,
                            onClick = { selectedStrategy = tag },
                            label = { Text(tag, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SolanaCyan,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }

                // Rating Stars
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Execution Rating:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    (1..5).forEach { star ->
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = if (star <= rating) SolanaGreen else Color(0x33FFFFFF),
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { rating = star }
                        )
                    }
                }

                Button(
                    onClick = {
                        viewModel.saveJournalEntry(
                            tradeId = tx.tradeId,
                            coinSymbol = tx.coinSymbol,
                            note = noteText,
                            emotionTag = selectedEmotion,
                            strategyTag = selectedStrategy,
                            ratingStars = rating
                        )
                        journalTargetTx = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SolanaPurple)
                ) {
                    Text("Save Trade Journal Note", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. TITLE & EXPORT HEADER ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Transaction History",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Exact execution prices, fees, and trade journals.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = { showExportDialog = true },
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(SolanaPurple.copy(alpha = 0.2f))
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Export Trade History",
                    tint = SolanaCyan
                )
            }
        }

        // --- 2. TAB SELECTOR FOR PNL CARDS VS EXECUTION LOG ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                label = { Text("PnL Cards 🚀", fontWeight = FontWeight.Bold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SolanaPurple,
                    selectedLabelColor = Color.White,
                    containerColor = SolanaSurface
                )
            )
            FilterChip(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                label = { Text("Execution Log 📋", fontWeight = FontWeight.Bold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SolanaPurple,
                    selectedLabelColor = Color.White,
                    containerColor = SolanaSurface
                )
            )
        }

        // --- 3. TIMELINE LIST ---
        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "No trades executed yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Buy or sell tokens from any detail page, and your exchange fills will populate here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        } else if (selectedTab == 0) {
            val soldTransactions = transactions.filter { it.type.equals("SELL", ignoreCase = true) }
            if (soldTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "📊", style = MaterialTheme.typography.displaySmall)
                        Text(
                            text = "No Closed Trades Yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "PnL cards are generated after selling tokens.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            } else {
                // PnL Cards View (matching token banner)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(soldTransactions) { tx ->
                        TraderPnLCard(
                            transaction = tx,
                            coin = coinMap[tx.coinSymbol.uppercase()],
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        } else {
            // Execution Log View
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(transactions) { tx ->
                    val journal = journalEntries.find { it.tradeId == tx.tradeId || it.tradeId == tx.txHash }
                    TransactionHistoryRow(
                        transaction = tx,
                        journalEntry = journal,
                        onOpenJournal = { journalTargetTx = tx },
                        onOpenPnLCard = { pnlTargetTx = tx }
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionHistoryRow(
    transaction: TradeTransaction,
    journalEntry: TradeJournalEntry? = null,
    onOpenJournal: () -> Unit = {},
    onOpenPnLCard: () -> Unit = {}
) {
    val context = LocalContext.current
    val isBuy = transaction.type.equals("BUY", ignoreCase = true)
    val colorAccent = if (isBuy) PriceUp else PriceDown

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
            // Header Row: Type, Coin name, Timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 56.dp, height = 30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colorAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = transaction.type,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = colorAccent
                        )
                    }

                    Column {
                        Text(
                            text = "${transaction.coinName} (${transaction.coinSymbol})",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val date = Date(transaction.timestamp)
                        val dateString = DateFormat.format("MMM dd, yyyy · hh:mm:ss a", date).toString()
                        Text(
                            text = dateString,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = transaction.tradeId,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = SolanaCyan
                    )

                    if (!isBuy) {
                        IconButton(
                            onClick = onOpenPnLCard,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Receipt,
                                contentDescription = "View PnL Card",
                                tint = SolanaCyan
                            )
                        }
                    }

                    IconButton(
                        onClick = onOpenJournal,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EditNote,
                            contentDescription = "Trade Journal",
                            tint = if (journalEntry != null) SolanaGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

            // Body Row: Fill breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Token Quantity", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = String.format("%,.4f %s", transaction.tokenAmount, transaction.coinSymbol),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (transaction.avgEntryPriceSol > 0) {
                        Text(
                            text = String.format("Avg Entry: %.6f SOL", transaction.avgEntryPriceSol),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Execution Price", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = if (transaction.executionPriceUsd >= 1.0) String.format("$%,.2f USD", transaction.executionPriceUsd)
                               else String.format("$%,.6f USD", transaction.executionPriceUsd),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = SolanaCyan
                    )
                    Text(
                        text = String.format("%.6f SOL", transaction.executionPriceSol),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Fee details & Net SOL / Realized PnL
            val platformFee = if (transaction.platformFeeSol > 0) transaction.platformFeeSol else transaction.tradingFeeSol
            val totalFee = if (transaction.totalFeeSol > 0) transaction.totalFeeSol else (transaction.gasFeeSol + platformFee)

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = String.format("Gas Fee: %.6f SOL", transaction.gasFeeSol),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format("Platform Fee: %.6f SOL", platformFee),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format("Total Fee: %.6f SOL", totalFee),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = SolanaCyan
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isBuy) String.format("Total Wallet Deduction: %.6f SOL", transaction.netSol)
                               else String.format("Net Wallet Credit: +%.6f SOL", transaction.netSol),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = colorAccent
                    )

                    if (!isBuy && transaction.realizedProfitSol != null) {
                        val pnl = transaction.realizedProfitSol
                        val pnlColor = if (pnl >= 0) PriceUp else PriceDown
                        val pnlSign = if (pnl >= 0) "+" else ""
                        val roiText = if (transaction.roiPercent != null) String.format(" (%s%.1f%%)", pnlSign, transaction.roiPercent) else ""
                        Text(
                            text = String.format("P&L: %s%.4f SOL%s", pnlSign, pnl, roiText),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = pnlColor
                        )
                    }
                }
            }

            // Trade Journal Note display box if note exists
            if (journalEntry != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SolanaPurple.copy(alpha = 0.12f))
                        .border(1.dp, SolanaPurple.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(shape = RoundedCornerShape(4.dp), color = SolanaPurple) {
                                Text(
                                    text = journalEntry.emotionTag,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                            Surface(shape = RoundedCornerShape(4.dp), color = SolanaCyan) {
                                Text(
                                    text = journalEntry.strategyTag,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Row {
                            (1..journalEntry.ratingStars).forEach { _ ->
                                Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = SolanaGreen, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                    Text(
                        text = journalEntry.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Simulated Tx Signature with Copy
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Tx: ${transaction.txHash.take(12)}...${transaction.txHash.takeLast(12)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("TxHash", transaction.txHash)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Copied Tx Signature!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Tx Signature",
                        tint = SolanaPurple,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

