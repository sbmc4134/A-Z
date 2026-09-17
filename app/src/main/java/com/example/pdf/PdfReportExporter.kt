package com.example.pdf

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

object PdfReportExporter {

    fun generateAndShareMonthlyReport(
        context: Context,
        monthYear: String, // "yyyy-MM"
        transactions: List<TransactionEntity>,
        currencySymbol: String = "৳"
    ): File? {
        if (transactions.isEmpty()) {
            Toast.makeText(context, "No transactions to export for $monthYear", Toast.LENGTH_SHORT).show()
            return null
        }

        val pdfDocument = PdfDocument()
        val pageWidth = 595 // A4 standard width at 72dpi
        val pageHeight = 842 // A4 standard height at 72dpi

        val sdfMonth = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val inputFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val displayMonth = try {
            val date = inputFormat.parse(monthYear)
            sdfMonth.format(date ?: Date())
        } catch (e: Exception) {
            monthYear
        }

        val totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val totalExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val netSavings = totalIncome - totalExpense

        // Category breakdown
        val expenseTx = transactions.filter { it.type == TransactionType.EXPENSE }
        val categoryBreakdown = expenseTx
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val numFormat = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        var yPosition = 40f

        // Draw Header background
        paint.color = Color.parseColor("#0D9488")
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 95f, paint)

        // Header Title
        paint.color = Color.WHITE
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("EXPENSE & FINANCIAL REPORT", 30f, 45f, paint)

        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Period: $displayMonth  |  Generated: ${SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())}", 30f, 68f, paint)

        yPosition = 120f

        // Summary Cards Box
        val cardWidth = (pageWidth - 60f - 20f) / 3f
        
        // Income Box
        drawStatCard(
            canvas, paint, 30f, yPosition, cardWidth, 65f,
            "TOTAL INCOME",
            "+ $currencySymbol ${numFormat.format(totalIncome)}",
            Color.parseColor("#E6F4EA"),
            Color.parseColor("#137333")
        )

        // Expense Box
        drawStatCard(
            canvas, paint, 30f + cardWidth + 10f, yPosition, cardWidth, 65f,
            "TOTAL EXPENSES",
            "- $currencySymbol ${numFormat.format(totalExpense)}",
            Color.parseColor("#FCE8E6"),
            Color.parseColor("#C5221F")
        )

        // Net Balance Box
        val netColorBg = if (netSavings >= 0) Color.parseColor("#E8F0FE") else Color.parseColor("#FCE8E6")
        val netColorText = if (netSavings >= 0) Color.parseColor("#1A73E8") else Color.parseColor("#C5221F")
        drawStatCard(
            canvas, paint, 30f + (cardWidth * 2) + 20f, yPosition, cardWidth, 65f,
            "NET BALANCE",
            "$currencySymbol ${numFormat.format(netSavings)}",
            netColorBg,
            netColorText
        )

        yPosition += 85f

        // Category Summary Section
        if (categoryBreakdown.isNotEmpty()) {
            paint.color = Color.parseColor("#0F172A")
            paint.textSize = 13f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("SPENDING BY CATEGORY", 30f, yPosition, paint)
            yPosition += 15f

            // Category Mini Table Header
            paint.color = Color.parseColor("#F1F5F9")
            canvas.drawRect(30f, yPosition, pageWidth - 30f, yPosition + 22f, paint)

            paint.color = Color.parseColor("#475569")
            paint.textSize = 9.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("CATEGORY", 40f, yPosition + 15f, paint)
            canvas.drawText("AMOUNT ($currencySymbol)", 320f, yPosition + 15f, paint)
            canvas.drawText("PERCENTAGE", 460f, yPosition + 15f, paint)
            yPosition += 26f

            // Top categories (up to 5 in summary)
            val topCategories = categoryBreakdown.take(5)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            topCategories.forEach { (cat, amount) ->
                val percentage = if (totalExpense > 0) (amount / totalExpense) * 100 else 0.0
                paint.color = Color.parseColor("#1E293B")
                paint.textSize = 9.5f
                canvas.drawText(cat, 40f, yPosition + 12f, paint)
                canvas.drawText("$currencySymbol ${numFormat.format(amount)}", 320f, yPosition + 12f, paint)
                canvas.drawText(String.format(Locale.US, "%.1f%%", percentage), 460f, yPosition + 12f, paint)

                // Thin divider
                paint.color = Color.parseColor("#E2E8F0")
                canvas.drawLine(30f, yPosition + 18f, pageWidth - 30f, yPosition + 18f, paint)
                yPosition += 20f
            }
            yPosition += 15f
        }

        // Transactions Table Section
        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("ITEMIZED TRANSACTIONS LOG (${transactions.size} records)", 30f, yPosition, paint)
        yPosition += 15f

        // Table Header
        paint.color = Color.parseColor("#0F172A")
        canvas.drawRect(30f, yPosition, pageWidth - 30f, yPosition + 24f, paint)

        paint.color = Color.WHITE
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("DATE", 38f, yPosition + 16f, paint)
        canvas.drawText("DESCRIPTION", 105f, yPosition + 16f, paint)
        canvas.drawText("CATEGORY", 240f, yPosition + 16f, paint)
        canvas.drawText("PAYMENT", 360f, yPosition + 16f, paint)
        canvas.drawText("AMOUNT ($currencySymbol)", 470f, yPosition + 16f, paint)
        yPosition += 28f

        val itemSdf = SimpleDateFormat("dd MMM", Locale.getDefault())

        for (tx in transactions) {
            // Check if page overflow
            if (yPosition > pageHeight - 50f) {
                // Draw footer on current page
                drawFooter(canvas, paint, pageWidth, pageHeight, pageNumber)
                pdfDocument.finishPage(page)

                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas

                yPosition = 40f
                // Header on subsequent pages
                paint.color = Color.parseColor("#0F172A")
                canvas.drawRect(30f, yPosition, pageWidth - 30f, yPosition + 22f, paint)
                paint.color = Color.WHITE
                paint.textSize = 9f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("DATE", 38f, yPosition + 15f, paint)
                canvas.drawText("DESCRIPTION", 105f, yPosition + 15f, paint)
                canvas.drawText("CATEGORY", 240f, yPosition + 15f, paint)
                canvas.drawText("PAYMENT", 360f, yPosition + 15f, paint)
                canvas.drawText("AMOUNT ($currencySymbol)", 470f, yPosition + 15f, paint)
                yPosition += 26f
            }

            val isExpense = tx.type == TransactionType.EXPENSE
            val dateStr = itemSdf.format(Date(tx.dateMillis))
            val titleTrimmed = if (tx.title.length > 24) tx.title.take(22) + ".." else tx.title
            val catTrimmed = if (tx.category.length > 20) tx.category.take(18) + ".." else tx.category
            val payTrimmed = if (tx.paymentMethod.length > 18) tx.paymentMethod.take(16) + ".." else tx.paymentMethod

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.color = Color.parseColor("#334155")
            paint.textSize = 8.5f
            canvas.drawText(dateStr, 38f, yPosition + 13f, paint)
            
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.color = Color.parseColor("#0F172A")
            canvas.drawText(titleTrimmed, 105f, yPosition + 13f, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.color = Color.parseColor("#475569")
            canvas.drawText(catTrimmed, 240f, yPosition + 13f, paint)
            canvas.drawText(payTrimmed, 360f, yPosition + 13f, paint)

            // Amount with color
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.color = if (isExpense) Color.parseColor("#DC2626") else Color.parseColor("#16A34A")
            val prefix = if (isExpense) "-" else "+"
            canvas.drawText("$prefix$currencySymbol ${numFormat.format(tx.amount)}", 470f, yPosition + 13f, paint)

            // Line separator
            paint.color = Color.parseColor("#F1F5F9")
            canvas.drawLine(30f, yPosition + 18f, pageWidth - 30f, yPosition + 18f, paint)

            yPosition += 20f
        }

        // Draw footer on last page
        drawFooter(canvas, paint, pageWidth, pageHeight, pageNumber)
        pdfDocument.finishPage(page)

        // Save PDF file to storage
        val reportsDir = File(context.cacheDir, "reports").apply { if (!exists()) mkdirs() }
        val fileName = "Expense_Report_${monthYear.replace("-", "_")}.pdf"
        val pdfFile = File(reportsDir, fileName)

        try {
            FileOutputStream(pdfFile).use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            pdfDocument.close()
            return pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            Toast.makeText(context, "Error saving PDF: ${e.message}", Toast.LENGTH_SHORT).show()
            return null
        }
    }

    private fun drawStatCard(
        canvas: Canvas,
        paint: Paint,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        title: String,
        amount: String,
        bgColor: Int,
        textColor: Int
    ) {
        paint.color = bgColor
        val rect = RectF(x, y, x + width, y + height)
        canvas.drawRoundRect(rect, 8f, 8f, paint)

        paint.color = Color.parseColor("#64748B")
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(title, x + 12f, y + 20f, paint)

        paint.color = textColor
        paint.textSize = 12.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(amount, x + 12f, y + 46f, paint)
    }

    private fun drawFooter(canvas: Canvas, paint: Paint, width: Int, height: Int, pageNum: Int) {
        paint.color = Color.parseColor("#CBD5E1")
        canvas.drawLine(30f, height - 35f, width - 30f, height - 35f, paint)

        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Generated by Expense Tracker Android App  •  Secure Local & Cloud Sync", 30f, height - 20f, paint)
        canvas.drawText("Page $pageNum", width - 65f, height - 20f, paint)
    }

    fun openOrSharePdf(context: Context, pdfFile: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Monthly Expense Report - ${pdfFile.name}")
                putExtra(Intent.EXTRA_TEXT, "Here is your monthly expense and financial report generated from Expense Tracker.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Share or Save Expense PDF"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Cannot open PDF chooser: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
