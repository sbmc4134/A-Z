package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BudgetEntity
import com.example.data.model.CategoryRegistry
import com.example.ui.theme.BudgetAmber
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import java.text.NumberFormat
import java.util.Locale

@Composable
fun BudgetCardItem(
    budget: BudgetEntity,
    currentSpent: Double,
    currencySymbol: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val limit = budget.limitAmount
    val percentage = if (limit > 0) ((currentSpent / limit) * 100).toFloat() else 0f
    val progress = (currentSpent / limit).coerceIn(0.0, 1.0).toFloat()
    val isOverBudget = currentSpent > limit
    val isWarning = percentage >= budget.alertThresholdPercent && !isOverBudget

    val progressAnimated by animateFloatAsState(targetValue = progress, label = "budget_progress")
    val statusColor by animateColorAsState(
        targetValue = when {
            isOverBudget -> ExpenseRed
            isWarning -> BudgetAmber
            else -> IncomeGreen
        },
        label = "budget_status_color"
    )

    val numFormat = NumberFormat.getNumberInstance(Locale.US).apply {
        maximumFractionDigits = 1
    }

    val isOverall = budget.category == "ALL"
    val categoryItem = if (isOverall) null else CategoryRegistry.getCategoryItem(budget.category)

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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isOverall) MaterialTheme.colorScheme.primaryContainer
                                else categoryItem?.color?.copy(alpha = 0.18f) ?: MaterialTheme.colorScheme.primaryContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isOverall) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Overall Budget",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        } else {
                            Icon(
                                imageVector = categoryItem?.icon ?: Icons.Default.CheckCircle,
                                contentDescription = budget.category,
                                tint = categoryItem?.color ?: MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = if (isOverall) "Overall Monthly Budget" else budget.category,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isOverBudget) "Over budget by $currencySymbol${numFormat.format(currentSpent - limit)}!"
                            else "Remaining: $currencySymbol${numFormat.format(limit - currentSpent)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (isOverBudget) FontWeight.Bold else FontWeight.Normal,
                            color = if (isOverBudget) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Status Badge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = statusColor.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = when {
                            isOverBudget -> "Over limit"
                            isWarning -> "Alert ${budget.alertThresholdPercent}%"
                            else -> "Safe"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bento Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = progressAnimated)
                        .clip(RoundedCornerShape(4.dp))
                        .background(statusColor)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Amount labels & Action icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Spent: $currencySymbol${numFormat.format(currentSpent)} / $currencySymbol${numFormat.format(limit)} (${String.format(Locale.US, "%.0f", percentage)}%)",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilledIconButton(
                        onClick = onEdit,
                        shape = RoundedCornerShape(8.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Budget",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    FilledIconButton(
                        onClick = onDelete,
                        shape = RoundedCornerShape(8.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Budget",
                            tint = ExpenseRed,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

