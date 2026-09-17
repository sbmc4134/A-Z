package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.ui.theme.*

enum class TransactionType {
    EXPENSE,
    INCOME
}

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val category: String,
    val paymentMethod: String,
    val dateMillis: Long = System.currentTimeMillis(),
    val note: String = "",
    val isSynced: Boolean = true
)

data class CategoryItem(
    val id: String,
    val nameEn: String,
    val nameBn: String,
    val icon: ImageVector,
    val color: Color,
    val isExpenseCategory: Boolean = true
)

object CategoryRegistry {
    val expenseCategories = listOf(
        CategoryItem("food", "Food & Dining", "খাবার ও ডাইনিং", Icons.Default.Fastfood, CatFood),
        CategoryItem("groceries", "Groceries & Market", "বাজার ও মুদি", Icons.Default.ShoppingCart, CatShopping),
        CategoryItem("transport", "Transport & Fuel", "যাতায়াত ও ভাড়া", Icons.Default.DirectionsBus, CatTransport),
        CategoryItem("bills", "Bills & Utilities", "বিল ও ইউটিলিটি", Icons.Default.ReceiptLong, CatBills),
        CategoryItem("health", "Health & Medicine", "স্বাস্থ্য ও ওষুধ", Icons.Default.MedicalServices, CatHealth),
        CategoryItem("entertainment", "Entertainment", "বিনোদন ও ভ্রমণ", Icons.Default.Movie, CatEntertainment),
        CategoryItem("education", "Education & Books", "শিক্ষা ও বইপত্র", Icons.Default.School, CatEducation),
        CategoryItem("shopping", "Clothing & Shopping", "পোশাক ও কেনাকাটা", Icons.Default.ShoppingBag, CatShopping),
        CategoryItem("family", "Family & Home", "পরিবার ও বাড়ি", Icons.Default.Home, PurpleAccent),
        CategoryItem("other_exp", "Other Expense", "অন্যান্য খরচ", Icons.Default.Category, CatOther)
    )

    val incomeCategories = listOf(
        CategoryItem("salary", "Salary & Wages", "বেতন ও সম্মানী", Icons.Default.AccountBalanceWallet, CatSalary, false),
        CategoryItem("business", "Business Profit", "ব্যবসা ও মুনাফা", Icons.Default.Storefront, CatInvestment, false),
        CategoryItem("freelance", "Freelance / Gig", "ফ্রিল্যান্সিং", Icons.Default.LaptopMac, InfoBlue, false),
        CategoryItem("investment", "Investment / Return", "বিনিয়োগ ও লভ্যাংশ", Icons.Default.TrendingUp, CatInvestment, false),
        CategoryItem("gift", "Gift / Allowance", "উপহার ও অনুদান", Icons.Default.CardGiftcard, PurpleAccent, false),
        CategoryItem("other_inc", "Other Income", "অন্যান্য আয়", Icons.Default.Savings, CatOther, false)
    )

    val allCategories = expenseCategories + incomeCategories

    fun getCategoryItem(name: String): CategoryItem {
        return allCategories.find { 
            it.nameEn.equals(name, ignoreCase = true) || 
            it.nameBn.equals(name, ignoreCase = true) || 
            it.id.equals(name, ignoreCase = true) 
        } ?: CategoryItem("other", name, name, Icons.Default.Category, CatOther)
    }

    val paymentMethods = listOf(
        "Cash (নগদ)",
        "bKash (বিকাশ)",
        "Nagad (নগদ অ্যাপ)",
        "Bank Card (কার্ড)",
        "Bank Transfer (ব্যাংক)",
        "Rocket (রকেট)",
        "Other (অন্যান্য)"
    )
}
