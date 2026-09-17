package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.theme.ExpenseRed
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MonthlySpendingTrendBarChart(
    transactions: List<TransactionEntity>,
    monthYear: String,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    // Group expense transactions by day of month
    val expenseTx = remember(transactions) {
        transactions.filter { it.type == TransactionType.EXPENSE }
    }

    val dailySpend = remember(expenseTx, monthYear) {
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val date = try { sdf.parse(monthYear) ?: Date() } catch (e: Exception) { Date() }
        cal.time = date
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val map = (1..daysInMonth).associateWith { 0.0 }.toMutableMap()
        val daySdf = SimpleDateFormat("d", Locale.getDefault())

        expenseTx.forEach { tx ->
            val day = daySdf.format(Date(tx.dateMillis)).toIntOrNull() ?: 1
            if (day in map) {
                map[day] = (map[day] ?: 0.0) + tx.amount
            }
        }
        map
    }

    val maxDailySpend = remember(dailySpend) {
        (dailySpend.values.maxOrNull() ?: 0.0).coerceAtLeast(100.0)
    }

    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(monthYear, transactions) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, animationSpec = tween(800))
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val barTrackColor = MaterialTheme.colorScheme.surfaceVariant
    val alertColor = ExpenseRed

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
        ),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daily Spending Trend",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Peak: $currencySymbol${String.format(Locale.US, "%.0f", maxDailySpend)}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Canvas Bar Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height - 20.dp.toPx()
                    val days = dailySpend.keys.toList().sorted()
                    val totalDays = days.size.coerceAtLeast(1)
                    val barSpacing = canvasWidth / totalDays.toFloat()
                    val barWidth = (barSpacing * 0.65f).coerceIn(4.dp.toPx(), 18.dp.toPx())

                    // Draw subtle grid line for average/max
                    val halfY = canvasHeight / 2f
                    drawLine(
                        color = barTrackColor,
                        start = Offset(0f, halfY),
                        end = Offset(canvasWidth, halfY),
                        strokeWidth = 1.dp.toPx()
                    )

                    days.forEachIndexed { index, day ->
                        val amount = dailySpend[day] ?: 0.0
                        val barHeight = ((amount / maxDailySpend) * canvasHeight * animationProgress.value).toFloat()
                        val x = index * barSpacing + (barSpacing - barWidth) / 2f
                        val y = canvasHeight - barHeight

                        // Draw background slot
                        drawRoundRect(
                            color = barTrackColor.copy(alpha = 0.5f),
                            topLeft = Offset(x, 0f),
                            size = Size(barWidth, canvasHeight),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )

                        if (barHeight > 0) {
                            val isPeak = amount >= maxDailySpend * 0.85 && amount > 500
                            val barBrush = Brush.verticalGradient(
                                colors = if (isPeak) {
                                    listOf(alertColor, alertColor.copy(alpha = 0.7f))
                                } else {
                                    listOf(primaryColor, primaryColor.copy(alpha = 0.6f))
                                }
                            )

                            drawRoundRect(
                                brush = barBrush,
                                topLeft = Offset(x, y),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // X-Axis day markers (Day 1, 5, 10, 15, 20, 25, End)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val maxDay = dailySpend.keys.maxOrNull() ?: 30
                listOf(1, (maxDay * 0.25).toInt(), (maxDay * 0.5).toInt(), (maxDay * 0.75).toInt(), maxDay).forEach { d ->
                    Text(
                        text = "D$d",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
