package com.example.hisabsaathi.payment

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.hisabsaathi.receipt.FinancialUtils
import java.util.Locale
import java.util.UUID

object PaymentLinkService {

    fun generateUniqueLinkCode(): String {
        return "HS-PL-" + UUID.randomUUID().toString().substring(0, 8).uppercase()
    }

    fun generateReconciliationRef(prefix: String = "HS-REF"): String {
        val randomSuffix = (10000..99999).random()
        return "$prefix-$randomSuffix"
    }

    fun getPaymentUrl(linkCode: String): String {
        return "https://hisabsaathi.in/pay/$linkCode"
    }

    /**
     * Constructs a standard NPCI compliant UPI URI:
     * upi://pay?pa=...&pn=...&am=...&cu=INR&tn=...&tr=...
     */
    fun generateUpiUri(
        vpa: String,
        payeeName: String,
        amount: Double,
        transactionNote: String,
        referenceCode: String,
        currency: String = "INR"
    ): String {
        val cleanVpa = vpa.trim()
        val cleanPayee = payeeName.trim()
        val amountFormatted = String.format(Locale.US, "%.2f", amount)

        return Uri.parse("upi://pay").buildUpon()
            .appendQueryParameter("pa", cleanVpa)
            .appendQueryParameter("pn", cleanPayee)
            .appendQueryParameter("am", amountFormatted)
            .appendQueryParameter("cu", currency)
            .appendQueryParameter("tn", transactionNote)
            .appendQueryParameter("tr", referenceCode)
            .build()
            .toString()
    }

    fun createUpiIntent(
        upiUri: String
    ): Intent {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(upiUri))
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        return intent
    }

    fun createUpiIntent(
        vpa: String,
        payeeName: String,
        amount: Double,
        transactionNote: String
    ): Intent {
        val refCode = generateUniqueLinkCode()
        val uriStr = generateUpiUri(vpa, payeeName, amount, transactionNote, refCode)
        return createUpiIntent(uriStr)
    }

    fun buildCustomerShareMessage(
        customerName: String,
        amount: Double,
        businessName: String,
        upiUri: String,
        reconciliationRef: String,
        purpose: String
    ): String {
        return """
            Namaste $customerName ji,
            
            Payment request of ${FinancialUtils.formatInr(amount)} from $businessName.
            
            📌 Purpose: $purpose
            🔑 Reconciliation Ref: $reconciliationRef
            
            👉 Pay directly via Google Pay / PhonePe / Paytm / BHIM:
            $upiUri
            
            🌐 Web Link: ${getPaymentUrl(reconciliationRef)}
            
            Thank you!
            $businessName via HisabSaathi
        """.trimIndent()
    }

    fun sharePaymentLink(
        context: Context,
        customerName: String,
        amount: Double,
        paymentUrl: String,
        businessName: String,
        upiUri: String? = null,
        reconciliationRef: String? = null
    ) {
        val shareText = if (upiUri != null && reconciliationRef != null) {
            buildCustomerShareMessage(
                customerName = customerName,
                amount = amount,
                businessName = businessName,
                upiUri = upiUri,
                reconciliationRef = reconciliationRef,
                purpose = "Hisab Settlement"
            )
        } else {
            """
                Dear $customerName,
                Please make the payment of ${FinancialUtils.formatInr(amount)} to $businessName.
                Click here to pay securely via UPI/Card/NetBanking:
                $paymentUrl
                
                Thank you,
                $businessName via HisabSaathi
            """.trimIndent()
        }

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share UPI Payment Link")
        shareIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(shareIntent)
    }

    fun copyToClipboard(context: Context, text: String, label: String = "UPI Payment Link") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard?.setPrimaryClip(clip)
    }

    fun isValidVpa(vpa: String): Boolean {
        val trimmed = vpa.trim()
        return trimmed.contains("@") && trimmed.length >= 5 && !trimmed.contains(" ")
    }
}
