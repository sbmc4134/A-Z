package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryRegistry
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.atan2

data class PieChartSlice(
    val category: String,
    val amount: Double,
    val percentage: Float,
    val color: Color
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryDonutChart(
    categorySpending: Map<String, Double>,
    currencySymbol: String,
    modifier: Modifier = Modifier,
    onCategorySelected: (String?) -> Unit = {}
) {
    val totalExpense = remember(categorySpending) { categorySpending.values.sum() }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    val slices = remember(categorySpending, totalExpense) {
        if (totalExpense <= 0) emptyList()
        else {
            categorySpending.entries
                .sortedByDescending { it.value }
                .map { entry ->
                    val catItem = CategoryRegistry.getCategoryItem(entry.key)
                    val pct = ((entry.value / totalExpense) * 100).toFloat()
                    PieChartSlice(
                        category = entry.key,
                        amount = entry.value,
                        percentage = pct,
                        color = catItem.color
                    )
                }
        }
    }

    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(categorySpending) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, animationSpec = tween(700))
    }

    val numFormat = remember {
        NumberFormat.getNumberInstance(Locale.US).apply {
            maximumFractionDigits = 1
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (slices.isEmpty() || totalExpense == 0.0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No expense data for this month\n(এই মাসে কোনো খরচের হিসাব নেই)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            // Interactive Donut Canvas
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(slices) {
                            detectTapGestures { offset ->
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val touchAngle = (Math.toDegrees(
                                    atan2(
                                        (offset.y - center.y).toDouble(),
                                        (offset.x - center.x).toDouble()
                                    )
                                ) + 360) % 360

                                var currentAngle = 0f
                                var tappedSlice: PieChartSlice? = null
                                for (slice in slices) {
                                    val sweep = (slice.percentage / 100f) * 360f
                                    val normalizedStart = (currentAngle - 90f + 360f) % 360f
                                    val normalizedEnd = (normalizedStart + sweep) % 360f

                                    val isInside = if (normalizedStart < normalizedEnd) {
                                        touchAngle in normalizedStart..normalizedEnd
                                    } else {
                                        touchAngle >= normalizedStart || touchAngle <= normalizedEnd
                                    }

                                    if (isInside) {
                                        tappedSlice = slice
                                        break
                                    }
                                    currentAngle += sweep
                                }

                                if (tappedSlice != null) {
                                    selectedCategory = if (selectedCategory == tappedSlice.category) null else tappedSlice.category
                                    onCategorySelected(selectedCategory)
                                } else {
                                    selectedCategory = null
                                    onCategorySelected(null)
                                }
                            }
                        }
                ) {
                    val strokeWidth = 32.dp.toPx()
                    val diameter = size.minDimension - strokeWidth
                    val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                    val arcSize = Size(diameter, diameter)

                    var startAngle = -90f
                    val totalProgress = animationProgress.value

                    slices.forEach { slice ->
                        val sweepAngle = (slice.percentage / 100f) * 360f * totalProgress
                        val isSelected = selectedCategory == slice.category
                        val currentStroke = if (isSelected) strokeWidth * 1.25f else strokeWidth

                        drawArc(
                            color = slice.color,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle.coerceAtLeast(1f),
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(
                                width = currentStroke,
                                cap = StrokeCap.Round
                            )
                        )
                        startAngle += sweepAngle
                    }
                }

                // Center Info Box
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)
                ) {
                    if (selectedCategory != null) {
                        val activeSlice = slices.find { it.category == selectedCategory }
                        if (activeSlice != null) {
                            Text(
                                text = activeSlice.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "$currencySymbol${numFormat.format(activeSlice.amount)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = activeSlice.color
                            )
                            Text(
                                text = "${String.format(Locale.US, "%.1f", activeSlice.percentage)}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                        Text(
                            text = "Total Spent",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$currencySymbol${numFormat.format(totalExpense)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${slices.size} Categories",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Category Legend Badges
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = 3
            ) {
                slices.forEach { slice ->
                    val isSelected = selectedCategory == slice.category
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) slice.color.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, slice.color) else null,
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                selectedCategory = if (isSelected) null else slice.category
                                onCategorySelected(selectedCategory)
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(slice.color)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${slice.category} (${String.format(Locale.US, "%.0f", slice.percentage)}%)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
