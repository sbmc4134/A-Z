package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.BudgetEntity
import com.example.data.model.CategoryRegistry
import com.example.ui.theme.BudgetAmber
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetBudgetDialog(
    budgetToEdit: BudgetEntity? = null,
    monthYear: String,
    currencySymbol: String = "৳",
    onDismiss: () -> Unit,
    onSave: (category: String, limitAmount: Double, threshold: Int) -> Unit
) {
    var isOverallBudget by remember {
        mutableStateOf(budgetToEdit?.category == "ALL" || budgetToEdit == null)
    }
    var selectedCategory by remember {
        mutableStateOf(
            if (budgetToEdit?.category != "ALL" && budgetToEdit?.category != null) budgetToEdit.category
            else CategoryRegistry.expenseCategories.first().nameEn
        )
    }
    var limitText by remember {
        mutableStateOf(
            budgetToEdit?.let { String.format(Locale.US, "%.0f", it.limitAmount) } ?: "30000"
        )
    }
    var alertThreshold by remember {
        mutableStateOf(budgetToEdit?.alertThresholdPercent?.toFloat() ?: 80f)
    }

    var expandedDropdown by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(22.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (budgetToEdit == null) "Set Budget (বাজেট নির্ধারণ)" else "Edit Budget (বাজেট সংশোধন)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Target Budget Type (Overall vs Category)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = isOverallBudget,
                        onClick = { isOverallBudget = true },
                        label = { Text("Overall Monthly (মোট বাজেট)") },
                        shape = RoundedCornerShape(10.dp)
                    )
                    FilterChip(
                        selected = !isOverallBudget,
                        onClick = { isOverallBudget = false },
                        label = { Text("Per Category (ক্যাটাগরি)") },
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                if (!isOverallBudget) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Category (ক্যাটাগরি):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    ExposedDropdownMenuBox(
                        expanded = expandedDropdown,
                        onExpandedChange = { expandedDropdown = !expandedDropdown }
                    ) {
                        OutlinedTextField(
                            value = selectedCategory,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedDropdown,
                            onDismissRequest = { expandedDropdown = false }
                        ) {
                            CategoryRegistry.expenseCategories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text("${cat.nameEn} (${cat.nameBn})") },
                                    onClick = {
                                        selectedCategory = cat.nameEn
                                        expandedDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Budget Limit Input
                OutlinedTextField(
                    value = limitText,
                    onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) limitText = it },
                    label = { Text("Monthly Limit Amount ($currencySymbol)") },
                    prefix = { Text("$currencySymbol ", fontWeight = FontWeight.Bold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                // Quick presets
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(5000, 15000, 30000, 50000).forEach { pVal ->
                        SuggestionChip(
                            onClick = { limitText = pVal.toString() },
                            label = { Text("$currencySymbol$pVal", fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Alert Threshold Slider
                Text(
                    text = "Alert Warning Threshold: ${alertThreshold.toInt()}% of budget",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Slider(
                    value = alertThreshold,
                    onValueChange = { alertThreshold = it },
                    valueRange = 50f..95f,
                    steps = 8,
                    colors = SliderDefaults.colors(
                        thumbColor = BudgetAmber,
                        activeTrackColor = BudgetAmber
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                val limitAmount = limitText.toDoubleOrNull() ?: 0.0
                Button(
                    onClick = {
                        if (limitAmount > 0) {
                            val cat = if (isOverallBudget) "ALL" else selectedCategory
                            onSave(cat, limitAmount, alertThreshold.toInt())
                            onDismiss()
                        }
                    },
                    enabled = limitAmount > 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = "Save")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Budget (সংরক্ষণ করুন)", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
