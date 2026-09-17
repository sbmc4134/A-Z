package com.example.data.repository

import android.content.Context
import com.example.data.dao.BudgetDao
import com.example.data.dao.TransactionDao
import com.example.data.model.BudgetEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ExpenseRepository(
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao
) {
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val allBudgets: Flow<List<BudgetEntity>> = budgetDao.getAllBudgets()

    fun getBudgetsForMonth(monthYear: String): Flow<List<BudgetEntity>> {
        return budgetDao.getBudgetsForMonth(monthYear)
    }

    fun getTransactionsBetween(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>> {
        return transactionDao.getTransactionsBetween(startMillis, endMillis)
    }

    suspend fun insertTransaction(transaction: TransactionEntity): Long {
        return transactionDao.insertTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: TransactionEntity) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun deleteTransactionById(id: Long) {
        transactionDao.deleteTransactionById(id)
    }

    suspend fun upsertBudget(budget: BudgetEntity): Long {
        return budgetDao.insertOrUpdate(budget)
    }

    suspend fun deleteBudget(id: Long) {
        budgetDao.deleteById(id)
    }

    // Cloud Backup & Restore Serialization
    suspend fun exportDataAsJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        val txList = mutableListOf<TransactionEntity>()
        // We can get all current transactions
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("device", "Android")
        root.toString(2)
    }

    suspend fun importDataFromJson(jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            if (root.has("transactions")) {
                val txArray = root.getJSONArray("transactions")
                val list = mutableListOf<TransactionEntity>()
                for (i in 0 until txArray.length()) {
                    val obj = txArray.getJSONObject(i)
                    list.add(
                        TransactionEntity(
                            id = 0,
                            title = obj.getString("title"),
                            amount = obj.getDouble("amount"),
                            type = TransactionType.valueOf(obj.getString("type")),
                            category = obj.getString("category"),
                            paymentMethod = obj.optString("paymentMethod", "Cash"),
                            dateMillis = obj.optLong("dateMillis", System.currentTimeMillis()),
                            note = obj.optString("note", "")
                        )
                    )
                }
                transactionDao.insertAll(list)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun seedInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        val count = transactionDao.getCount()
        if (count == 0) {
            val cal = Calendar.getInstance()
            val currentYearMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(cal.time)

            // Seed helpful everyday transactions in BDT / native format
            val sampleTransactions = listOf(
                TransactionEntity(
                    title = "Monthly Salary (মাসিক বেতন)",
                    amount = 55000.0,
                    type = TransactionType.INCOME,
                    category = "Salary & Wages",
                    paymentMethod = "Bank Transfer (ব্যাংক)",
                    dateMillis = System.currentTimeMillis() - (12 * 86400000L),
                    note = "Software engineering salary credited"
                ),
                TransactionEntity(
                    title = "House Rent & Utility (বাড়ি ভাড়া)",
                    amount = 16000.0,
                    type = TransactionType.EXPENSE,
                    category = "Bills & Utilities",
                    paymentMethod = "bKash (বিকাশ)",
                    dateMillis = System.currentTimeMillis() - (10 * 86400000L),
                    note = "Monthly apartment rent"
                ),
                TransactionEntity(
                    title = "Monthly Grocery (কাঁচাবাজার ও মুদি)",
                    amount = 4850.0,
                    type = TransactionType.EXPENSE,
                    category = "Groceries & Market",
                    paymentMethod = "Cash (নগদ)",
                    dateMillis = System.currentTimeMillis() - (7 * 86400000L),
                    note = "Rice, oil, fish, vegetables from local market"
                ),
                TransactionEntity(
                    title = "Electricity & Internet Bill (বিদ্যুৎ ও ওয়াইফাই বিল)",
                    amount = 2200.0,
                    type = TransactionType.EXPENSE,
                    category = "Bills & Utilities",
                    paymentMethod = "bKash (বিকাশ)",
                    dateMillis = System.currentTimeMillis() - (5 * 86400000L),
                    note = "DESCO & ISP bill"
                ),
                TransactionEntity(
                    title = "Dinner with Family (পারিবারিক ডিনার)",
                    amount = 1750.0,
                    type = TransactionType.EXPENSE,
                    category = "Food & Dining",
                    paymentMethod = "Bank Card (কার্ড)",
                    dateMillis = System.currentTimeMillis() - (3 * 86400000L),
                    note = "Restaurant weekend meal"
                ),
                TransactionEntity(
                    title = "Ride & Fuel (উবার ও যাতায়াত)",
                    amount = 450.0,
                    type = TransactionType.EXPENSE,
                    category = "Transport & Fuel",
                    paymentMethod = "Nagad (নগদ অ্যাপ)",
                    dateMillis = System.currentTimeMillis() - (2 * 86400000L),
                    note = "Office commute"
                ),
                TransactionEntity(
                    title = "Freelance Design Gig (ডিজাইন কাজ)",
                    amount = 12000.0,
                    type = TransactionType.INCOME,
                    category = "Freelance / Gig",
                    paymentMethod = "Bank Transfer (ব্যাংক)",
                    dateMillis = System.currentTimeMillis() - (1 * 86400000L),
                    note = "Mobile UI kit design delivery"
                ),
                TransactionEntity(
                    title = "Prescription Medicine (ওষুধ ক্রয়)",
                    amount = 680.0,
                    type = TransactionType.EXPENSE,
                    category = "Health & Medicine",
                    paymentMethod = "Cash (নগদ)",
                    dateMillis = System.currentTimeMillis() - (12 * 3600000L),
                    note = "Pharmacy regular vitamins"
                ),
                TransactionEntity(
                    title = "Coffee & Snacks (চা ও নাস্তা)",
                    amount = 180.0,
                    type = TransactionType.EXPENSE,
                    category = "Food & Dining",
                    paymentMethod = "Cash (নগদ)",
                    dateMillis = System.currentTimeMillis() - (2 * 3600000L),
                    note = "Afternoon refreshments"
                )
            )
            transactionDao.insertAll(sampleTransactions)

            // Seed overall budget and food budget
            budgetDao.insertOrUpdate(
                BudgetEntity(
                    category = "ALL",
                    monthYear = currentYearMonth,
                    limitAmount = 35000.0,
                    alertThresholdPercent = 80
                )
            )
            budgetDao.insertOrUpdate(
                BudgetEntity(
                    category = "Food & Dining",
                    monthYear = currentYearMonth,
                    limitAmount = 6000.0,
                    alertThresholdPercent = 80
                )
            )
            budgetDao.insertOrUpdate(
                BudgetEntity(
                    category = "Bills & Utilities",
                    monthYear = currentYearMonth,
                    limitAmount = 20000.0,
                    alertThresholdPercent = 85
                )
            )
        }
    }
}
