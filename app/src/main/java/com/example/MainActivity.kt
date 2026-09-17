package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BudgetEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.pdf.PdfReportExporter
import com.example.ui.dialogs.AddEditTransactionSheet
import com.example.ui.dialogs.PinSetupDialog
import com.example.ui.dialogs.SetBudgetDialog
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.BudgetScreen
import com.example.ui.screens.CloudSyncScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PinLockScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ExpenseViewModel

enum class NavigationTab {
    HOME,
    ANALYTICS,
    BUDGETS,
    CLOUD
}

class MainActivity : ComponentActivity() {

    private val viewModel: ExpenseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                ExpenseApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseApp(viewModel: ExpenseViewModel) {
    val context = LocalContext.current

    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val currentMonthTx by viewModel.currentMonthTransactions.collectAsStateWithLifecycle()
    val filteredTx by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val currentMonthBudgets by viewModel.currentMonthBudgets.collectAsStateWithLifecycle()
    val selectedTypeFilter by viewModel.selectedTypeFilter.collectAsStateWithLifecycle()
    val selectedCatFilter by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val syncMessage by viewModel.syncMessage.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(NavigationTab.HOME) }

    // Dialog & Sheet States
    var showAddEditSheet by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<TransactionEntity?>(null) }
    var initialSheetType by remember { mutableStateOf(TransactionType.EXPENSE) }

    var showBudgetDialog by remember { mutableStateOf(false) }
    var budgetToEdit by remember { mutableStateOf<BudgetEntity?>(null) }

    var showPinSetupDialog by remember { mutableStateOf(false) }

    // Check if app is locked with PIN
    if (userProfile.isAppLocked && userProfile.isPinEnabled) {
        PinLockScreen(
            onUnlockSuccess = {
                Toast.makeText(context, "Welcome back!", Toast.LENGTH_SHORT).show()
            },
            verifyPin = { pin ->
                viewModel.unlockApp(pin)
            }
        )
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = "App Logo",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Expense Tracker",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "দৈনন্দিন খরচের হিসাব ও বাজেট",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // PDF quick export button in header
                    FilledIconButton(
                        onClick = {
                            val file = viewModel.exportPdfReport(context)
                            if (file != null) {
                                PdfReportExporter.openOrSharePdf(context, file)
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Export PDF",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Lock button if PIN is enabled
                    if (userProfile.isPinEnabled) {
                        FilledIconButton(
                            onClick = { viewModel.lockApp() },
                            shape = RoundedCornerShape(10.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Lock App",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Surface(
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                ),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                NavigationBar(
                    containerColor = Color.Transparent,
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        selected = activeTab == NavigationTab.HOME,
                        onClick = { activeTab = NavigationTab.HOME },
                        icon = {
                            Icon(
                                imageVector = if (activeTab == NavigationTab.HOME) Icons.Default.Home else Icons.Default.Home,
                                contentDescription = "Home"
                            )
                        },
                        label = { Text("Expenses", fontWeight = if (activeTab == NavigationTab.HOME) FontWeight.Bold else FontWeight.Normal) }
                    )

                    NavigationBarItem(
                        selected = activeTab == NavigationTab.ANALYTICS,
                        onClick = { activeTab = NavigationTab.ANALYTICS },
                        icon = {
                            Icon(
                                imageVector = if (activeTab == NavigationTab.ANALYTICS) Icons.Default.PieChart else Icons.Default.PieChartOutline,
                                contentDescription = "Analytics"
                            )
                        },
                        label = { Text("Analytics", fontWeight = if (activeTab == NavigationTab.ANALYTICS) FontWeight.Bold else FontWeight.Normal) }
                    )

                    NavigationBarItem(
                        selected = activeTab == NavigationTab.BUDGETS,
                        onClick = { activeTab = NavigationTab.BUDGETS },
                        icon = {
                            Icon(
                                imageVector = if (activeTab == NavigationTab.BUDGETS) Icons.Default.AccountBalance else Icons.Default.AccountBalance,
                                contentDescription = "Budgets"
                            )
                        },
                        label = { Text("Budgets", fontWeight = if (activeTab == NavigationTab.BUDGETS) FontWeight.Bold else FontWeight.Normal) }
                    )

                    NavigationBarItem(
                        selected = activeTab == NavigationTab.CLOUD,
                        onClick = { activeTab = NavigationTab.CLOUD },
                        icon = {
                            Icon(
                                imageVector = if (activeTab == NavigationTab.CLOUD) Icons.Default.CloudSync else Icons.Default.CloudQueue,
                                contentDescription = "Cloud"
                            )
                        },
                        label = { Text("Cloud", fontWeight = if (activeTab == NavigationTab.CLOUD) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }
        },
        floatingActionButton = {
            if (activeTab == NavigationTab.HOME || activeTab == NavigationTab.BUDGETS) {
                ExtendedFloatingActionButton(
                    onClick = {
                        if (activeTab == NavigationTab.BUDGETS) {
                            budgetToEdit = null
                            showBudgetDialog = true
                        } else {
                            transactionToEdit = null
                            initialSheetType = TransactionType.EXPENSE
                            showAddEditSheet = true
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    icon = { Icon(imageVector = Icons.Default.Add, contentDescription = "Add") },
                    text = {
                        Text(if (activeTab == NavigationTab.BUDGETS) "Add Budget" else "Add Entry", fontWeight = FontWeight.Bold)
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeTab) {
                NavigationTab.HOME -> {
                    HomeScreen(
                        selectedMonth = selectedMonth,
                        transactions = filteredTx,
                        budgets = currentMonthBudgets,
                        selectedTypeFilter = selectedTypeFilter,
                        selectedCategoryFilter = selectedCatFilter,
                        searchQuery = searchQuery,
                        currencySymbol = userProfile.currencySymbol,
                        onPreviousMonth = { viewModel.previousMonth() },
                        onNextMonth = { viewModel.nextMonth() },
                        onTypeFilterSelected = { viewModel.setTypeFilter(it) },
                        onCategoryFilterSelected = { viewModel.setCategoryFilter(it) },
                        onSearchQueryChanged = { viewModel.setSearchQuery(it) },
                        onAddTransactionClick = { type ->
                            transactionToEdit = null
                            initialSheetType = type
                            showAddEditSheet = true
                        },
                        onEditTransactionClick = { tx ->
                            transactionToEdit = tx
                            showAddEditSheet = true
                        },
                        onDeleteTransactionClick = { tx ->
                            viewModel.deleteTransaction(tx)
                            Toast.makeText(context, "Deleted: ${tx.title}", Toast.LENGTH_SHORT).show()
                        },
                        onExportPdfClick = {
                            val file = viewModel.exportPdfReport(context)
                            if (file != null) {
                                PdfReportExporter.openOrSharePdf(context, file)
                            }
                        },
                        onSetBudgetClick = {
                            budgetToEdit = null
                            showBudgetDialog = true
                        }
                    )
                }

                NavigationTab.ANALYTICS -> {
                    AnalyticsScreen(
                        selectedMonth = selectedMonth,
                        transactions = currentMonthTx,
                        currencySymbol = userProfile.currencySymbol,
                        onPreviousMonth = { viewModel.previousMonth() },
                        onNextMonth = { viewModel.nextMonth() },
                        onExportPdfClick = {
                            val file = viewModel.exportPdfReport(context)
                            if (file != null) {
                                PdfReportExporter.openOrSharePdf(context, file)
                            }
                        }
                    )
                }

                NavigationTab.BUDGETS -> {
                    BudgetScreen(
                        selectedMonth = selectedMonth,
                        budgets = currentMonthBudgets,
                        transactions = currentMonthTx,
                        currencySymbol = userProfile.currencySymbol,
                        onPreviousMonth = { viewModel.previousMonth() },
                        onNextMonth = { viewModel.nextMonth() },
                        onAddBudgetClick = {
                            budgetToEdit = null
                            showBudgetDialog = true
                        },
                        onEditBudgetClick = { b ->
                            budgetToEdit = b
                            showBudgetDialog = true
                        },
                        onDeleteBudgetClick = { b ->
                            viewModel.deleteBudget(b.id)
                            Toast.makeText(context, "Budget removed", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                NavigationTab.CLOUD -> {
                    CloudSyncScreen(
                        userProfile = userProfile,
                        isSyncing = isSyncing,
                        syncMessage = syncMessage,
                        onSyncClick = { viewModel.syncDataWithCloud() },
                        onPinSetupClick = { showPinSetupDialog = true },
                        onCurrencyChange = { viewModel.setCurrency(it) },
                        onLanguageToggle = { viewModel.toggleLanguage() },
                        onExportPdfClick = {
                            val file = viewModel.exportPdfReport(context)
                            if (file != null) {
                                PdfReportExporter.openOrSharePdf(context, file)
                            }
                        }
                    )
                }
            }
        }
    }

    // Modal Add / Edit Transaction Sheet
    if (showAddEditSheet) {
        AddEditTransactionSheet(
            transactionToEdit = transactionToEdit,
            currencySymbol = userProfile.currencySymbol,
            onDismiss = { showAddEditSheet = false },
            onSave = { title, amount, type, category, paymentMethod, dateMillis, note ->
                if (transactionToEdit == null) {
                    viewModel.addTransaction(
                        title = title,
                        amount = amount,
                        type = type,
                        category = category,
                        paymentMethod = paymentMethod,
                        dateMillis = dateMillis,
                        note = note
                    )
                    Toast.makeText(context, "Transaction saved! (হিসাব সংরক্ষিত)", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.updateTransaction(
                        transactionToEdit!!.copy(
                            title = title,
                            amount = amount,
                            type = type,
                            category = category,
                            paymentMethod = paymentMethod,
                            dateMillis = dateMillis,
                            note = note
                        )
                    )
                    Toast.makeText(context, "Transaction updated! (আপডেট সম্পন্ন)", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // Set Budget Dialog
    if (showBudgetDialog) {
        SetBudgetDialog(
            budgetToEdit = budgetToEdit,
            monthYear = selectedMonth,
            currencySymbol = userProfile.currencySymbol,
            onDismiss = { showBudgetDialog = false },
            onSave = { category, limitAmount, threshold ->
                viewModel.setBudget(category, limitAmount, threshold)
                Toast.makeText(context, "Budget saved for $category!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // PIN Setup Dialog
    if (showPinSetupDialog) {
        PinSetupDialog(
            currentPin = userProfile.pinCode,
            onDismiss = { showPinSetupDialog = false },
            onSavePin = { pin ->
                viewModel.setPin(pin)
                val msg = if (pin.isNotEmpty()) "4-digit PIN enabled successfully!" else "PIN lock disabled."
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        )
    }
}
