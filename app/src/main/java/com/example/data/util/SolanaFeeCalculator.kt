package com.example.data.util

enum class SolanaNetworkCongestion(
    val label: String,
    val basePriorityFeeSol: Double,
    val computeUnitsMicroLamports: Long,
    val description: String
) {
    LOW("Low", 0.000030, 5_000, "Fast confirmation (~0.00003 SOL)"),
    MEDIUM("Medium", 0.000150, 25_000, "Normal traffic (~0.00015 SOL)"),
    HIGH("High", 0.000450, 75_000, "High traffic (~0.00045 SOL)"),
    TURBO("Turbo / Extreme", 0.001200, 200_000, "Heavy MEV spike (~0.0012 SOL)")
}

data class SolanaTransactionFees(
    val networkFeeSol: Double = 0.000005, // Fixed base signature fee (5,000 lamports)
    val priorityFeeSol: Double,           // Dynamic compute unit priority fee
    val platformFeeSol: Double,           // Platform/DEX fee (0.25%)
    val congestionLevel: SolanaNetworkCongestion
) {
    val gasFeeSol: Double get() = networkFeeSol + priorityFeeSol
    val totalFeeSol: Double get() = gasFeeSol + platformFeeSol
}

object SolanaFeeCalculator {
    const val NETWORK_BASE_FEE_SOL = 0.000005 // 5,000 lamports signature fee

    fun calculateFees(
        solAmount: Double,
        congestion: SolanaNetworkCongestion = SolanaNetworkCongestion.MEDIUM,
        platformFeeRate: Double = 0.0025 // 0.25%
    ): SolanaTransactionFees {
        val platformFee = solAmount * platformFeeRate
        val priorityFee = congestion.basePriorityFeeSol
        return SolanaTransactionFees(
            networkFeeSol = NETWORK_BASE_FEE_SOL,
            priorityFeeSol = priorityFee,
            platformFeeSol = platformFee,
            congestionLevel = congestion
        )
    }
}
