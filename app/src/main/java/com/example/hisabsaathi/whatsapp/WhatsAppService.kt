package com.example.hisabsaathi.whatsapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.hisabsaathi.data.db.WhatsAppSettingsEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

sealed class WhatsAppOtpResult {
    data class Sent(val message: String) : WhatsAppOtpResult()
    data class NotConfigured(val message: String) : WhatsAppOtpResult()
    data class Error(val message: String) : WhatsAppOtpResult()
}

class WhatsAppService(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun openWhatsAppDirect(recipientMobile: String, messageText: String): Boolean {
        return try {
            val intent = createWhatsAppIntent(recipientMobile, messageText)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            // Fallback to web link
            try {
                val cleanNumber = recipientMobile.replace("+", "").replace(" ", "").replace("-", "")
                val formattedNumber = if (cleanNumber.length == 10) "91$cleanNumber" else cleanNumber
                val encodedMessage = URLEncoder.encode(messageText, "UTF-8")
                val webUri = Uri.parse("https://api.whatsapp.com/send?phone=$formattedNumber&text=$encodedMessage")
                val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(webIntent)
                true
            } catch (e2: Exception) {
                false
            }
        }
    }

    fun createWhatsAppIntent(recipientMobile: String, messageText: String): Intent {
        val cleanNumber = recipientMobile.replace("+", "").replace(" ", "").replace("-", "")
        val formattedNumber = if (cleanNumber.length == 10) "91$cleanNumber" else cleanNumber
        val encodedMessage = URLEncoder.encode(messageText, "UTF-8")
        
        val uri = Uri.parse("whatsapp://send?phone=$formattedNumber&text=$encodedMessage")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return intent
    }

    fun createBalanceReminderShareIntent(
        customerName: String,
        recipientMobile: String,
        formattedMessage: String
    ): Intent {
        val cleanNumber = recipientMobile.replace("+", "").replace(" ", "").replace("-", "")
        val formattedNumber = if (cleanNumber.length == 10) "91$cleanNumber" else cleanNumber
        val encodedMessage = URLEncoder.encode(formattedMessage, "UTF-8")
        
        val uri = Uri.parse("whatsapp://send?phone=$formattedNumber&text=$encodedMessage")
        return Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    fun triggerBalanceReminderShare(
        businessName: String,
        customerName: String,
        recipientMobile: String,
        balanceAmount: Double,
        purpose: String = "Outstanding Balance Settlement"
    ): Boolean {
        val message = WhatsAppMessageService.buildPaymentRequestMessage(
            businessName = businessName,
            customerName = customerName,
            amount = balanceAmount,
            purpose = purpose
        )
        return openWhatsAppDirect(recipientMobile, message)
    }

    fun triggerAndroidShareIntent(
        businessName: String,
        customerName: String,
        recipientMobile: String,
        balanceAmount: Double,
        purpose: String = "Outstanding Balance Settlement"
    ): Boolean {
        return try {
            val message = WhatsAppMessageService.buildPaymentRequestMessage(
                businessName = businessName,
                customerName = customerName,
                amount = balanceAmount,
                purpose = purpose
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Payment Reminder from $businessName")
                putExtra(Intent.EXTRA_TEXT, message)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val chooser = Intent.createChooser(shareIntent, "Send Payment Reminder via WhatsApp").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun sendWhatsAppCloudOtp(
        settings: WhatsAppSettingsEntity?,
        recipientMobile: String,
        otpCode: String
    ): WhatsAppOtpResult = withContext(Dispatchers.IO) {
        if (settings == null || !settings.isConnected || settings.accessToken.isBlank() || settings.phoneNumberId.isBlank()) {
            return@withContext WhatsAppOtpResult.NotConfigured(
                "WhatsApp OTP is not configured yet. Please configure the WhatsApp Business messaging provider."
            )
        }

        try {
            val cleanNumber = recipientMobile.replace("+", "").replace(" ", "").replace("-", "")
            val to = if (cleanNumber.length == 10) "91$cleanNumber" else cleanNumber
            val url = "https://graph.facebook.com/${settings.apiVersion}/${settings.phoneNumberId}/messages"

            val json = JSONObject().apply {
                put("messaging_product", "whatsapp")
                put("recipient_type", "individual")
                put("to", to)
                put("type", "text")
                put("text", JSONObject().apply {
                    put("preview_url", false)
                    put("body", "Your HisabSaathi verification OTP is $otpCode. Valid for 5 minutes. Please do not share this OTP with anyone.")
                })
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer ${settings.accessToken.trim()}")
                .addHeader("Content-Type", "application/json")
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                WhatsAppOtpResult.Sent("OTP sent successfully to your WhatsApp.")
            } else {
                WhatsAppOtpResult.Error("WhatsApp API error: ${response.code}. Please check configuration.")
            }
        } catch (e: Exception) {
            WhatsAppOtpResult.Error("Failed to connect to WhatsApp API: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    suspend fun testConnection(settings: WhatsAppSettingsEntity): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (settings.accessToken.isBlank() || settings.phoneNumberId.isBlank()) {
            return@withContext Pair(false, "Access Token and Phone Number ID are required.")
        }
        try {
            val url = "https://graph.facebook.com/${settings.apiVersion}/${settings.phoneNumberId}"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer ${settings.accessToken.trim()}")
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Pair(true, "WhatsApp Business API connected successfully!")
            } else {
                Pair(false, "Verification failed with HTTP status ${response.code}")
            }
        } catch (e: Exception) {
            Pair(false, "Connection error: ${e.localizedMessage}")
        }
    }
}
