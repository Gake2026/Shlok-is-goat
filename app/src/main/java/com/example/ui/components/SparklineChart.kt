package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun SparklineChart(
    prices: List<Double>,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (prices.size < 2) return@Canvas

        val width = size.width
        val height = size.height

        val minPrice = prices.minOrNull() ?: 0.0
        val maxPrice = prices.maxOrNull() ?: 0.0
        val priceRange = maxPrice - minPrice

        val points = prices.mapIndexed { index, price ->
            val x = index * (width / (prices.size - 1))
            val y = if (priceRange > 0) {
                height - ((price - minPrice) / priceRange * height).toFloat()
            } else {
                height / 2f
            }
            // Add padding so line doesn't get clipped on edges
            val paddedY = y.coerceIn(4f, height - 4f)
            Pair(x, paddedY)
        }

        // 1. Draw area gradient under sparkline
        val fillPath = Path().apply {
            moveTo(0f, height)
            points.forEach { (x, y) ->
                lineTo(x, y)
            }
            lineTo(width, height)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    color.copy(alpha = 0.25f),
                    color.copy(alpha = 0.0f)
                ),
                startY = 0f,
                endY = height
            )
        )

        // 2. Draw glowing sparkline stroke
        val strokePath = Path().apply {
            points.forEachIndexed { index, (x, y) ->
                if (index == 0) moveTo(x, y) else lineTo(x, y)
            }
        }

        drawPath(
            path = strokePath,
            color = color,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}
