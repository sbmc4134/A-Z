package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BudgetEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.components.BudgetCardItem
import com.example.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BudgetScreen(
    selectedMonth: String,
    budgets: List<BudgetEntity>,
    transactions: List<TransactionEntity>,
    currencySymbol: String,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onAddBudgetClick: () -> Unit,
    onEditBudgetClick: (BudgetEntity) -> Unit,
    onDeleteBudgetClick: (BudgetEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val displayMonth = remember(selectedMonth) {
        val sdfIn = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val sdfOut = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        try {
            val d = sdfIn.parse(selectedMonth)
            sdfOut.format(d ?: Date())
        } catch (e: Exception) {
            selectedMonth
        }
    }

    val expenseTx = remember(transactions) {
        transactions.filter { it.type == TransactionType.EXPENSE }
    }
    val totalSpent = remember(expenseTx) { expenseTx.sumOf { it.amount } }

    val overallBudget = remember(budgets) {
        budgets.find { it.category == "ALL" }
    }
    val categoryBudgets = remember(budgets) {
        budgets.filter { it.category != "ALL" }
    }

    val categorySpendingMap = remember(expenseTx) {
        expenseTx.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }

    val daysRemainingInMonth = remember(selectedMonth) {
        val cal = Calendar.getInstance()
        val today = cal.get(Calendar.DAY_OF_MONTH)
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        (maxDays - today + 1).coerceAtLeast(1)
    }

    val dailyBudgetRemaining = remember(overallBudget, totalSpent, daysRemainingInMonth) {
        if (overallBudget != null) {
            val remaining = (overallBudget.limitAmount - totalSpent).coerceAtLeast(0.0)
            remaining / daysRemainingInMonth
        } else {
            0.0
        }
    }

    val numFormat = remember {
        NumberFormat.getNumberInstance(Locale.US).apply {
            maximumFractionDigits = 0
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 14.dp, bottom = 100.dp)
    ) {
        // Month Selector Bento Capsule
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                ),
                tonalElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledIconButton(
                        onClick = onPreviousMonth,
                        shape = RoundedCornerShape(12.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous Month",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Budgeting ($displayMonth)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Monthly Spending Limits & Alerts",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    FilledIconButton(
                        onClick = onNextMonth,
                        shape = RoundedCornerShape(12.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Month",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Daily Allowance Insight Bento Card
        if (overallBudget != null) {
            item {
                val isExceeded = totalSpent > overallBudget.limitAmount
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = if (isExceeded) ExpenseRedContainer else BentoPeachContainer,
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (isExceeded) ExpenseRed.copy(alpha = 0.4f) else BentoTerracottaPrimary.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isExceeded) ExpenseRed else BentoTerracottaPrimary
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isExceeded) Icons.Default.Warning else Icons.Default.Savings,
                                contentDescription = "Daily Allowance",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = if (isExceeded) "Budget Limit Exceeded! (বাজেট অতিরিক্ত)"
                                else "Daily Safe Spend Allowance (দৈনিক খরচ সীমা)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isExceeded) ExpenseRed else BentoTerracottaPrimary
                            )
                            Text(
                                text = if (isExceeded) "Please review high-expense categories"
                                else "$currencySymbol${numFormat.format(dailyBudgetRemaining)} / day (for $daysRemainingInMonth days left)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Section: Overall Budget
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Overall Monthly Budget (সর্বমোট বাজেট)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (overallBudget == null) {
                    FilledTonalButton(
                        onClick = onAddBudgetClick,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Set Budget", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Set Budget")
                    }
                }
            }
        }

        if (overallBudget != null) {
            item {
                BudgetCardItem(
                    budget = overallBudget,
                    currentSpent = totalSpent,
                    currencySymbol = currencySymbol,
                    onEdit = { onEditBudgetClick(overallBudget) },
                    onDelete = { onDeleteBudgetClick(overallBudget) }
                )
            }
        } else {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No overall budget set for $displayMonth",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onAddBudgetClick,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Set Monthly Budget")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Set Monthly Budget (বাজেট সেট করুন)")
                        }
                    }
                }
            }
        }

        // Section: Category Budgets
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Category Budgets (ক্যাটাগরি বাজেট)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                FilledTonalButton(
                    onClick = onAddBudgetClick,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Category Budget", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Category")
                }
            }
        }

        if (categoryBudgets.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = "Category Budgets",
                            modifier = Modifier.size(44.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Track budgets per category (e.g. Food, Bills, Shopping)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedButton(
                            onClick = onAddBudgetClick,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("+ Add Category Budget")
                        }
                    }
                }
            }
        } else {
            items(categoryBudgets, key = { it.id }) { budget ->
                val spent = categorySpendingMap[budget.category] ?: 0.0
                BudgetCardItem(
                    budget = budget,
                    currentSpent = spent,
                    currencySymbol = currencySymbol,
                    onEdit = { onEditBudgetClick(budget) },
                    onDelete = { onDeleteBudgetClick(budget) }
                )
            }
        }
    }
}

