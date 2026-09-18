package com.example.hisabsaathi.receipt

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import com.example.hisabsaathi.data.db.ReceiptEntity
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.Locale

object FinancialUtils {
    fun formatInr(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        val formatted = format.format(amount)
        // Format returns ₹ symbol or Rs. Ensure clean ₹
        return if (formatted.startsWith("₹") || formatted.startsWith("Rs.")) {
            formatted.replace("Rs.", "₹").trim()
        } else {
            "₹$formatted"
        }
    }

    fun getBalanceStatus(balance: Double): Triple<String, Double, String> {
        return when {
            balance > 0.001 -> Triple("YOU WILL RECEIVE", balance, "RECEIVE")
            balance < -0.001 -> Triple("YOU HAVE TO PAY", kotlin.math.abs(balance), "PAY")
            else -> Triple("Settled", 0.0, "SETTLED")
        }
    }
}

class ReceiptService(private val context: Context) {

    fun printOrShareReceipt(receipt: ReceiptEntity, businessName: String, businessMobile: String) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val jobName = "HisabSaathi_Receipt_${receipt.receiptNumber}"

        printManager.print(jobName, object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?
            ) {
                callback?.onLayoutFinished(
                    android.print.PrintDocumentInfo.Builder(jobName)
                        .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .setPageCount(1)
                        .build(),
                    true
                )
            }

            override fun onWrite(
                pages: Array<out android.print.PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                if (destination == null) return
                val pdfDocument = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                val titlePaint = Paint().apply {
                    color = Color.rgb(15, 43, 72)
                    textSize = 24f
                    isFakeBoldText = true
                }
                val subPaint = Paint().apply {
                    color = Color.rgb(80, 80, 80)
                    textSize = 14f
                }
                val boldPaint = Paint().apply {
                    color = Color.BLACK
                    textSize = 16f
                    isFakeBoldText = true
                }
                val textPaint = Paint().apply {
                    color = Color.DKGRAY
                    textSize = 14f
                }
                val linePaint = Paint().apply {
                    color = Color.LTGRAY
                    strokeWidth = 1.5f
                }

                var y = 60f
                canvas.drawText("HISABSAATHI", 40f, y, titlePaint)
                y += 20f
                canvas.drawText("Smart Hisab • Jhadi • Attendance • Payment", 40f, y, subPaint)
                y += 25f
                canvas.drawLine(40f, y, 555f, y, linePaint)
                y += 35f

                canvas.drawText("BUSINESS: $businessName", 40f, y, boldPaint)
                canvas.drawText("Mobile: $businessMobile", 350f, y, textPaint)
                y += 25f
                canvas.drawText("RECEIPT NO: ${receipt.receiptNumber}", 40f, y, boldPaint)
                canvas.drawText("Date: ${receipt.date} ${receipt.time}", 350f, y, textPaint)
                y += 25f
                canvas.drawLine(40f, y, 555f, y, linePaint)
                y += 35f

                canvas.drawText("CUSTOMER DETAILS", 40f, y, boldPaint)
                y += 22f
                canvas.drawText("Name: ${receipt.customerName}", 40f, y, textPaint)
                canvas.drawText("Mobile: ${receipt.customerMobile}", 350f, y, textPaint)
                y += 30f

                canvas.drawText("TRANSACTION DETAILS", 40f, y, boldPaint)
                y += 22f
                val typeDesc = if (receipt.transactionType == "RECEIVED") "Payment Received ( मिला )" else "Payment Given ( दिया )"
                canvas.drawText("Type: $typeDesc", 40f, y, textPaint)
                canvas.drawText("Payment Mode: ${receipt.paymentMethod}", 350f, y, textPaint)
                y += 25f

                val amountPaint = Paint().apply {
                    color = if (receipt.transactionType == "RECEIVED") Color.rgb(16, 137, 78) else Color.rgb(211, 47, 47)
                    textSize = 22f
                    isFakeBoldText = true
                }
                canvas.drawText("AMOUNT: ${FinancialUtils.formatInr(receipt.amount)}", 40f, y, amountPaint)
                y += 30f

                if (receipt.description.isNotBlank()) {
                    canvas.drawText("Note / Description: ${receipt.description}", 40f, y, textPaint)
                    y += 25f
                }

                canvas.drawLine(40f, y, 555f, y, linePaint)
                y += 30f

                canvas.drawText("Previous Balance: ${FinancialUtils.formatInr(receipt.previousBalance)}", 40f, y, textPaint)
                y += 22f
                val (balLabel, balAmt, _) = FinancialUtils.getBalanceStatus(receipt.currentBalance)
                canvas.drawText("Current Balance: $balLabel ${FinancialUtils.formatInr(balAmt)}", 40f, y, boldPaint)
                y += 40f

                canvas.drawLine(40f, y, 555f, y, linePaint)
                y += 30f
                canvas.drawText("Generated electronically via HisabSaathi App. No signature required.", 40f, y, subPaint)

                pdfDocument.finishPage(page)
                try {
                    FileOutputStream(destination.fileDescriptor).use { output ->
                        pdfDocument.writeTo(output)
                    }
                    callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.message)
                } finally {
                    pdfDocument.close()
                }
            }
        }, null)
    }
}
