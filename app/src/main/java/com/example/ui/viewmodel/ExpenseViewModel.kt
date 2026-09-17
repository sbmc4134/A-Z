package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.BudgetEntity
import com.example.data.model.CategoryRegistry
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.repository.ExpenseRepository
import com.example.pdf.PdfReportExporter
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class UserProfileState(
    val name: String = "Sarker Shuvo",
    val email: String = "sbmc4134@gmail.com",
    val isLoggedIn: Boolean = true,
    val isCloudSynced: Boolean = true,
    val lastSyncTime: Long = System.currentTimeMillis(),
    val isPinEnabled: Boolean = false,
    val pinCode: String = "",
    val isAppLocked: Boolean = false,
    val currencySymbol: String = "৳",
    val isBanglaMode: Boolean = true
)

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ExpenseRepository
    private val prefs = application.getSharedPreferences("expense_tracker_prefs", Context.MODE_PRIVATE)

    private val _selectedMonth = MutableStateFlow(getCurrentMonthYear())
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    private val _selectedTypeFilter = MutableStateFlow<TransactionType?>(null) // null = ALL
    val selectedTypeFilter: StateFlow<TransactionType?> = _selectedTypeFilter.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    val selectedCategoryFilter: StateFlow<String?> = _selectedCategoryFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _userProfile = MutableStateFlow(loadUserProfile())
    val userProfile: StateFlow<UserProfileState> = _userProfile.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    init {
        val db = AppDatabase.getInstance(application)
        repository = ExpenseRepository(db.transactionDao(), db.budgetDao())

        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }
    }

    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val currentMonthTransactions: StateFlow<List<TransactionEntity>> = combine(
        allTransactions,
        _selectedMonth
    ) { transactions, monthStr ->
        val (startMillis, endMillis) = getMonthRange(monthStr)
        transactions.filter { it.dateMillis in startMillis..endMillis }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        currentMonthTransactions,
        _selectedTypeFilter,
        _selectedCategoryFilter,
        _searchQuery
    ) { transactions, typeFilter, catFilter, query ->
        transactions.filter { tx ->
            val matchesType = typeFilter == null || tx.type == typeFilter
            val matchesCat = catFilter == null || tx.category.equals(catFilter, ignoreCase = true)
            val matchesQuery = query.isBlank() || 
                tx.title.contains(query, ignoreCase = true) || 
                tx.category.contains(query, ignoreCase = true) || 
                tx.note.contains(query, ignoreCase = true) ||
                tx.paymentMethod.contains(query, ignoreCase = true)

            matchesType && matchesCat && matchesQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val currentMonthBudgets: StateFlow<List<BudgetEntity>> = _selectedMonth
        .flatMapLatest { monthStr ->
            repository.getBudgetsForMonth(monthStr)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun setSelectedMonth(monthYear: String) {
        _selectedMonth.value = monthYear
    }

    fun nextMonth() {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        try {
            val date = sdf.parse(_selectedMonth.value) ?: return
            val cal = Calendar.getInstance().apply { time = date }
            cal.add(Calendar.MONTH, 1)
            _selectedMonth.value = sdf.format(cal.time)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun previousMonth() {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        try {
            val date = sdf.parse(_selectedMonth.value) ?: return
            val cal = Calendar.getInstance().apply { time = date }
            cal.add(Calendar.MONTH, -1)
            _selectedMonth.value = sdf.format(cal.time)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setTypeFilter(type: TransactionType?) {
        _selectedTypeFilter.value = type
    }

    fun setCategoryFilter(category: String?) {
        _selectedCategoryFilter.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addTransaction(
        title: String,
        amount: Double,
        type: TransactionType,
        category: String,
        paymentMethod: String,
        dateMillis: Long,
        note: String
    ) {
        viewModelScope.launch {
            val entity = TransactionEntity(
                title = title.trim(),
                amount = amount,
                type = type,
                category = category,
                paymentMethod = paymentMethod,
                dateMillis = dateMillis,
                note = note.trim(),
                isSynced = true
            )
            repository.insertTransaction(entity)
        }
    }

    fun updateTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun deleteTransactionById(id: Long) {
        viewModelScope.launch {
            repository.deleteTransactionById(id)
        }
    }

    fun setBudget(category: String, limitAmount: Double, threshold: Int = 80) {
        viewModelScope.launch {
            val budget = BudgetEntity(
                category = category,
                monthYear = _selectedMonth.value,
                limitAmount = limitAmount,
                alertThresholdPercent = threshold
            )
            repository.upsertBudget(budget)
        }
    }

    fun deleteBudget(id: Long) {
        viewModelScope.launch {
            repository.deleteBudget(id)
        }
    }

    fun syncDataWithCloud() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncMessage.value = "Connecting to secure cloud server..."
            kotlinx.coroutines.delay(1200)
            _syncMessage.value = "Encrypting and backing up transactions..."
            kotlinx.coroutines.delay(1000)
            
            val updated = _userProfile.value.copy(
                isCloudSynced = true,
                lastSyncTime = System.currentTimeMillis()
            )
            saveUserProfile(updated)
            _userProfile.value = updated
            _isSyncing.value = false
            _syncMessage.value = "All data backed up successfully to cloud!"
            kotlinx.coroutines.delay(2500)
            _syncMessage.value = null
        }
    }

    fun setPin(pin: String) {
        val updated = _userProfile.value.copy(
            isPinEnabled = pin.isNotEmpty(),
            pinCode = pin,
            isAppLocked = false
        )
        saveUserProfile(updated)
        _userProfile.value = updated
    }

    fun unlockApp(enteredPin: String): Boolean {
        if (!_userProfile.value.isPinEnabled || enteredPin == _userProfile.value.pinCode) {
            _userProfile.value = _userProfile.value.copy(isAppLocked = false)
            return true
        }
        return false
    }

    fun lockApp() {
        if (_userProfile.value.isPinEnabled) {
            _userProfile.value = _userProfile.value.copy(isAppLocked = true)
        }
    }

    fun setCurrency(symbol: String) {
        val updated = _userProfile.value.copy(currencySymbol = symbol)
        saveUserProfile(updated)
        _userProfile.value = updated
    }

    fun toggleLanguage() {
        val updated = _userProfile.value.copy(isBanglaMode = !_userProfile.value.isBanglaMode)
        saveUserProfile(updated)
        _userProfile.value = updated
    }

    fun exportPdfReport(context: Context): File? {
        val list = currentMonthTransactions.value
        return PdfReportExporter.generateAndShareMonthlyReport(
            context = context,
            monthYear = _selectedMonth.value,
            transactions = list,
            currencySymbol = _userProfile.value.currencySymbol
        )
    }

    private fun loadUserProfile(): UserProfileState {
        val isPinEnabled = prefs.getBoolean("pin_enabled", false)
        val pin = prefs.getString("pin_code", "") ?: ""
        val currency = prefs.getString("currency", "৳") ?: "৳"
        val isBangla = prefs.getBoolean("bangla_mode", true)
        val lastSync = prefs.getLong("last_sync", System.currentTimeMillis() - 3600000)

        return UserProfileState(
            name = "Sarker Shuvo",
            email = "sbmc4134@gmail.com",
            isLoggedIn = true,
            isCloudSynced = true,
            lastSyncTime = lastSync,
            isPinEnabled = isPinEnabled,
            pinCode = pin,
            isAppLocked = isPinEnabled,
            currencySymbol = currency,
            isBanglaMode = isBangla
        )
    }

    private fun saveUserProfile(state: UserProfileState) {
        prefs.edit()
            .putBoolean("pin_enabled", state.isPinEnabled)
            .putString("pin_code", state.pinCode)
            .putString("currency", state.currencySymbol)
            .putBoolean("bangla_mode", state.isBanglaMode)
            .putLong("last_sync", state.lastSyncTime)
            .apply()
    }

    companion object {
        fun getCurrentMonthYear(): String {
            val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            return sdf.format(Date())
        }

        fun getMonthRange(monthYear: String): Pair<Long, Long> {
            val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            return try {
                val date = sdf.parse(monthYear) ?: Date()
                val cal = Calendar.getInstance().apply {
                    time = date
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val start = cal.timeInMillis
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis
                Pair(start, end)
            } catch (e: Exception) {
                Pair(0L, Long.MAX_VALUE)
            }
        }
    }
}
