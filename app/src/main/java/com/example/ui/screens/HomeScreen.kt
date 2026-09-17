package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BudgetEntity
import com.example.data.model.CategoryRegistry
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.components.TransactionItemCard
import com.example.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    selectedMonth: String,
    transactions: List<TransactionEntity>,
    budgets: List<BudgetEntity>,
    selectedTypeFilter: TransactionType?,
    selectedCategoryFilter: String?,
    searchQuery: String,
    currencySymbol: String,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onTypeFilterSelected: (TransactionType?) -> Unit,
    onCategoryFilterSelected: (String?) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onAddTransactionClick: (TransactionType) -> Unit,
    onEditTransactionClick: (TransactionEntity) -> Unit,
    onDeleteTransactionClick: (TransactionEntity) -> Unit,
    onExportPdfClick: () -> Unit,
    onSetBudgetClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalIncome = remember(transactions) {
        transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    }
    val totalExpense = remember(transactions) {
        transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    }
    val netBalance = totalIncome - totalExpense

    val overallBudget = remember(budgets) {
        budgets.find { it.category == "ALL" }
    }

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

    val numFormat = remember {
        NumberFormat.getNumberInstance(Locale.US).apply {
            maximumFractionDigits = 2
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 14.dp, bottom = 100.dp)
    ) {
        // Month Navigation Bento Capsule
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
                            text = displayMonth,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${transactions.size} records • লেনদেনের তালিকা",
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

        // Hero Bento Balance Card (Rich Terracotta Gradient)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = BentoTerracottaPrimary
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = BentoTerracottaLight.copy(alpha = 0.5f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    BentoTerracottaPrimary,
                                    BentoTerracottaDark
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Net Balance (মাসিক অবশিষ্ট)",
                            style = MaterialTheme.typography.labelMedium,
                            color = BentoPeachContainer.copy(alpha = 0.9f)
                        )
                        if (overallBudget != null) {
                            val spent = totalExpense
                            val limit = overallBudget.limitAmount
                            val pct = if (limit > 0) ((spent / limit) * 100).toInt() else 0
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (spent > limit) ExpenseRed else BentoOliveSecondary
                            ) {
                                Text(
                                    text = "Budget: $pct%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "$currencySymbol ${numFormat.format(netBalance)}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Bento Sub-cells: Split Income & Expense Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Income Bento Sub-cell
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.18f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(IncomeGreenContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDownward,
                                        contentDescription = "Income",
                                        tint = IncomeGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Income (আয়)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BentoPeachContainer.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = "+$currencySymbol${numFormat.format(totalIncome)}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        // Expense Bento Sub-cell
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.18f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(ExpenseRedContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = "Expense",
                                        tint = ExpenseRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Expense (খরচ)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BentoPeachContainer.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = "-$currencySymbol${numFormat.format(totalExpense)}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bento Quick Action Tiles Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Add Expense Bento Tile
                Surface(
                    modifier = Modifier
                        .weight(1.1f)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onAddTransactionClick(TransactionType.EXPENSE) },
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ExpenseRedContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Expense", tint = ExpenseRed, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(text = "Expense", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Text(text = "+ খরচ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Add Income Bento Tile
                Surface(
                    modifier = Modifier
                        .weight(1.1f)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onAddTransactionClick(TransactionType.INCOME) },
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(IncomeGreenContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Income", tint = IncomeGreen, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(text = "Income", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Text(text = "+ আয়", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // PDF Export Bento Tile
                Surface(
                    modifier = Modifier
                        .weight(0.9f)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable(onClick = onExportPdfClick),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = "PDF Report", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PDF", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
        }

        // Search & Filter Bento Section
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChanged,
                    placeholder = { Text("Search transactions, notes, categories...", style = MaterialTheme.typography.bodyMedium) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChanged("") }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                    )
                )

                // Bento Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedTypeFilter == null && selectedCategoryFilter == null,
                            onClick = {
                                onTypeFilterSelected(null)
                                onCategoryFilterSelected(null)
                            },
                            label = { Text("All (${transactions.size})") },
                            shape = RoundedCornerShape(12.dp),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedTypeFilter == null && selectedCategoryFilter == null,
                                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                                selectedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    item {
                        FilterChip(
                            selected = selectedTypeFilter == TransactionType.EXPENSE,
                            onClick = {
                                onTypeFilterSelected(if (selectedTypeFilter == TransactionType.EXPENSE) null else TransactionType.EXPENSE)
                            },
                            label = { Text("Expenses (খরচ)") },
                            shape = RoundedCornerShape(12.dp),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedTypeFilter == TransactionType.EXPENSE,
                                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                                selectedBorderColor = ExpenseRed
                            )
                        )
                    }

                    item {
                        FilterChip(
                            selected = selectedTypeFilter == TransactionType.INCOME,
                            onClick = {
                                onTypeFilterSelected(if (selectedTypeFilter == TransactionType.INCOME) null else TransactionType.INCOME)
                            },
                            label = { Text("Income (আয়)") },
                            shape = RoundedCornerShape(12.dp),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedTypeFilter == TransactionType.INCOME,
                                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                                selectedBorderColor = IncomeGreen
                            )
                        )
                    }

                    // Category chips
                    items(CategoryRegistry.allCategories.take(6)) { cat ->
                        FilterChip(
                            selected = selectedCategoryFilter == cat.nameEn,
                            onClick = {
                                onCategoryFilterSelected(if (selectedCategoryFilter == cat.nameEn) null else cat.nameEn)
                            },
                            label = { Text(cat.nameEn) },
                            shape = RoundedCornerShape(12.dp),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedCategoryFilter == cat.nameEn,
                                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                                selectedBorderColor = cat.color
                            )
                        )
                    }
                }
            }
        }

        // Section Title Bento
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Transactions (দৈনন্দিন হিসাব)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "${transactions.size} items",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // Transactions List
        if (transactions.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 44.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = "No transactions",
                                    modifier = Modifier.size(30.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No transactions found\n(এই ক্যাটাগরিতে কোনো হিসাব নেই)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        } else {
            items(transactions, key = { it.id }) { tx ->
                TransactionItemCard(
                    transaction = tx,
                    currencySymbol = currencySymbol,
                    onClick = { onEditTransactionClick(tx) },
                    onDelete = { onDeleteTransactionClick(tx) }
                )
            }
        }
    }
}

