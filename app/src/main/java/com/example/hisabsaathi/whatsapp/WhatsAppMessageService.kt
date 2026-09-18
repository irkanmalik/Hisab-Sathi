package com.example.hisabsaathi.whatsapp

import com.example.hisabsaathi.data.db.CustomerTransactionEntity
import com.example.hisabsaathi.payment.PaymentLinkService
import com.example.hisabsaathi.receipt.FinancialUtils

object WhatsAppMessageService {

    /**
     * Generates a rich, formatted WhatsApp message for a transaction including amount, type,
     * date, time, description, current balance, and a direct UPI payment link / web link.
     */
    fun buildTransactionShareMessage(
        businessName: String,
        customerName: String,
        transaction: CustomerTransactionEntity,
        businessVpa: String = "hisabsaathi@upi"
    ): String {
        val isReceived = transaction.type == "RECEIVED"
        val actionText = if (isReceived) "Payment Received (Credit)" else "Amount Given (Debit)"
        val linkCode = "HS-TX-${transaction.id}-${transaction.customerId}"
        val paymentUrl = PaymentLinkService.getPaymentUrl(linkCode)
        val upiUri = PaymentLinkService.generateUpiUri(
            vpa = businessVpa,
            payeeName = businessName,
            amount = transaction.amount,
            transactionNote = transaction.description.ifBlank { "Hisab Settlement for Tx #${transaction.id}" },
            referenceCode = linkCode
        )

        return """
            Namaste $customerName ji,
            
            Here is your transaction update from $businessName:
            
            📄 *Transaction Details*
            • Type: $actionText
            • Amount: ${FinancialUtils.formatInr(transaction.amount)}
            • Date: ${transaction.date} at ${transaction.time.ifBlank { "N/A" }}
            • Payment Mode: ${transaction.paymentMethod}
            ${if (transaction.description.isNotBlank()) "• Note: ${transaction.description}" else ""}
            
            💰 *Updated Balance*: ${FinancialUtils.formatInr(transaction.balanceAfter)}
            
            👉 *Pay/Settle Securely via UPI*:
            $upiUri
            
            🌐 *Web Payment Link*:
            $paymentUrl
            
            Thank you for your business!
            -- $businessName via HisabSaathi
        """.trimIndent()
    }

    /**
     * Generates a formatted payment reminder / request WhatsApp message.
     */
    fun buildPaymentRequestMessage(
        businessName: String,
        customerName: String,
        amount: Double,
        purpose: String,
        businessVpa: String = "hisabsaathi@upi"
    ): String {
        val refCode = PaymentLinkService.generateReconciliationRef("HS-REQ")
        val paymentUrl = PaymentLinkService.getPaymentUrl(refCode)
        val upiUri = PaymentLinkService.generateUpiUri(
            vpa = businessVpa,
            payeeName = businessName,
            amount = amount,
            transactionNote = purpose,
            referenceCode = refCode
        )

        return """
            Namaste $customerName ji,
            
            Payment request of *${FinancialUtils.formatInr(amount)}* from $businessName.
            
            📌 Purpose: $purpose
            🔑 Ref Code: $refCode
            
            👉 *Pay directly via UPI (GPay / PhonePe / Paytm / BHIM)*:
            $upiUri
            
            🌐 *Web Payment Link*:
            $paymentUrl
            
            Thank you!
            -- $businessName via HisabSaathi
        """.trimIndent()
    }
}
