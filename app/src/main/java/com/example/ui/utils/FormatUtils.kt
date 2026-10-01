package com.example.ui.utils

import java.util.Locale

fun formatPrice(price: Double): String {
    return when {
        price <= 0.0 -> "$0.00"
        price < 0.00000001 -> String.format(Locale.US, "$%.10f", price)
        price < 0.000001 -> String.format(Locale.US, "$%.8f", price)
        price < 0.0001 -> String.format(Locale.US, "$%.6f", price)
        price < 1.0 -> String.format(Locale.US, "$%.4f", price)
        else -> String.format(Locale.US, "$%,.2f", price)
    }
}

fun formatUsd(amount: Double): String {
    return when {
        amount <= 0.0 -> "$0"
        amount >= 1_000_000_000.0 -> String.format(Locale.US, "$%.2fB", amount / 1_000_000_000.0)
        amount >= 1_000_000.0 -> String.format(Locale.US, "$%.2fM", amount / 1_000_000.0)
        amount >= 1_000.0 -> String.format(Locale.US, "$%.1fK", amount / 1_000.0)
        else -> String.format(Locale.US, "$%,.2f", amount)
    }
}

fun truncateAddress(address: String): String {
    if (address.length <= 8) return address
    return "${address.take(4)}...${address.takeLast(4)}"
}
