package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PriceDown
import com.example.ui.theme.PriceUp
import com.example.ui.theme.SolanaCyan
import com.example.ui.theme.SolanaGreen
import com.example.ui.theme.SolanaPurple
import com.example.ui.theme.SolanaSurface
import com.example.ui.viewmodel.TradingViewModel

@Composable
fun SettingsScreen(
    viewModel: TradingViewModel,
    modifier: Modifier = Modifier
) {
    val summary by viewModel.computedPortfolioValue.collectAsState()
    val transactions by viewModel.transactions.collectAsState()

    var showResetDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Simulation") },
            text = { Text("Are you sure you want to reset your wallet back to 10.0 simulated SOL? This will permanently delete all active positions and transaction histories.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetPaperTrading()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = PriceDown)
                ) {
                    Text("Reset Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = SolanaSurface
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. TITLE ---
        Column(modifier = Modifier.padding(top = 16.dp)) {
            Text(
                text = "Simulation Settings",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Manage your Solana paper portfolio settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // --- 2. ACTIVE PRICE ALERTS ---
        Text(
            text = "Active Price Alerts",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        val priceAlerts by viewModel.priceAlerts.collectAsState()
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SolanaSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (priceAlerts.isEmpty()) {
                    Text(
                        text = "No price alerts created yet. Set price targets from any coin detail page.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    priceAlerts.forEach { alert ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${alert.coinSymbol} ${if (alert.isAbove) "≥" else "≤"} $${String.format("%.6f", alert.targetPriceUsd)}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (alert.isTriggered) "Triggered" else if (alert.isEnabled) "Active" else "Disabled",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (alert.isTriggered) SolanaGreen else if (alert.isEnabled) SolanaCyan else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(
                                    checked = alert.isEnabled,
                                    onCheckedChange = { viewModel.togglePriceAlert(alert) }
                                )
                                TextButton(onClick = { viewModel.deletePriceAlert(alert) }) {
                                    Text("Delete", color = PriceDown, fontSize = 11.sp)
                                }
                            }

                        }
                        Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    }
                }
            }
        }

        // --- 3. PAPER STATISTICS SUMMARY ---
        Text(
            text = "Trader Performance",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SolanaSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                StatRow(
                    label = "Total Portfolio Capital",
                    value = String.format("%.4f SOL ($%,.2f)", summary.totalValueSol, summary.totalValueUsd)
                )
                Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                StatRow(
                    label = "Total Trades Placed",
                    value = "${transactions.size}"
                )
                Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                StatRow(
                    label = "Active Positions",
                    value = "${summary.detailedHoldings.size}"
                )
                Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                
                val profit = summary.netProfitLossSol >= 0
                val colorAccent = if (profit) PriceUp else PriceDown
                val sign = if (profit) "+" else ""
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Net Return",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format("%s%.4f SOL (%s%.2f%%)", sign, summary.netProfitLossSol, sign, summary.netProfitLossPercent),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = colorAccent
                    )
                }
            }
        }

        // --- 4. DANGER CONTROLS ---
        Text(
            text = "Risk Management",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SolanaSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "If your meme coins 'went to zero' or you want a fresh start, use the quick reset trigger below.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Button(
                    onClick = { showResetDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("settings_reset_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = PriceDown.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = PriceDown)
                        Text(
                            text = "Reset Simulation Balance",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = PriceDown
                        )
                    }
                }
            }
        }

        // --- 4. DETAILS BANNER ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SolanaPurple.copy(alpha = 0.1f))
                .padding(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = SolanaCyan
                )
                Column {
                    Text(
                        text = "Did you know?",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = SolanaCyan
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Solana is highly famous for its super-fast block times and near-zero transaction fees. This makes it the leading chain for meme coin trading and liquid speculation.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}


