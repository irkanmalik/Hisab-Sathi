package com.example.hisabsaathi.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["mobile_number"], unique = true), Index(value = ["email"])]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "mobile_number")
    val mobileNumber: String,
    @ColumnInfo(name = "email")
    val email: String? = null,
    @ColumnInfo(name = "name")
    val name: String = "",
    @ColumnInfo(name = "password")
    val password: String = "",
    @ColumnInfo(name = "role")
    val role: String = "user", // "admin" or "user"
    @ColumnInfo(name = "is_verified")
    val isVerified: Boolean = false,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "businesses",
    indices = [Index(value = ["owner_user_id"])]
)
data class BusinessEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "owner_user_id")
    val ownerUserId: Long,
    @ColumnInfo(name = "business_name")
    val businessName: String,
    @ColumnInfo(name = "owner_name")
    val ownerName: String,
    @ColumnInfo(name = "business_mobile")
    val businessMobile: String,
    @ColumnInfo(name = "address")
    val address: String = "",
    @ColumnInfo(name = "city")
    val city: String = "",
    @ColumnInfo(name = "district")
    val district: String = "",
    @ColumnInfo(name = "state")
    val state: String = "",
    @ColumnInfo(name = "country")
    val country: String = "India",
    @ColumnInfo(name = "preferred_language")
    val preferredLanguage: String = "en",
    @ColumnInfo(name = "currency")
    val currency: String = "INR",
    @ColumnInfo(name = "timezone")
    val timezone: String = "Asia/Kolkata",
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "customers",
    indices = [
        Index(value = ["business_id"]),
        Index(value = ["business_id", "mobile_number"]),
        Index(value = ["business_id", "full_name"])
    ]
)
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "business_id")
    val businessId: Long,
    @ColumnInfo(name = "full_name")
    val fullName: String,
    @ColumnInfo(name = "mobile_number")
    val mobileNumber: String,
    @ColumnInfo(name = "whatsapp_number")
    val whatsappNumber: String = "",
    @ColumnInfo(name = "alternate_number")
    val alternateNumber: String = "",
    @ColumnInfo(name = "address")
    val address: String = "",
    @ColumnInfo(name = "city")
    val city: String = "",
    @ColumnInfo(name = "district")
    val district: String = "",
    @ColumnInfo(name = "state")
    val state: String = "",
    @ColumnInfo(name = "notes")
    val notes: String = "",
    @ColumnInfo(name = "opening_balance")
    val openingBalance: Double = 0.0,
    @ColumnInfo(name = "balance_type")
    val balanceType: String = "RECEIVABLE", // "RECEIVABLE" or "PAYABLE"
    @ColumnInfo(name = "current_balance")
    val currentBalance: Double = 0.0, // Positive = You will receive, Negative = You have to pay
    @ColumnInfo(name = "total_received")
    val totalReceived: Double = 0.0,
    @ColumnInfo(name = "total_given")
    val totalGiven: Double = 0.0,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "customer_transactions",
    indices = [
        Index(value = ["business_id"]),
        Index(value = ["customer_id"]),
        Index(value = ["business_id", "date"])
    ]
)
data class CustomerTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "business_id")
    val businessId: Long,
    @ColumnInfo(name = "customer_id")
    val customerId: Long,
    @ColumnInfo(name = "type")
    val type: String, // "RECEIVED" or "GIVEN"
    @ColumnInfo(name = "amount")
    val amount: Double,
    @ColumnInfo(name = "date")
    val date: String, // YYYY-MM-DD
    @ColumnInfo(name = "time")
    val time: String, // HH:MM AM/PM
    @ColumnInfo(name = "payment_method")
    val paymentMethod: String = "CASH", // CASH, UPI, BANK, CARD, OTHER
    @ColumnInfo(name = "description")
    val description: String = "",
    @ColumnInfo(name = "reference_number")
    val referenceNumber: String = "",
    @ColumnInfo(name = "previous_balance")
    val previousBalance: Double = 0.0,
    @ColumnInfo(name = "balance_after")
    val balanceAfter: Double = 0.0,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "employees",
    indices = [Index(value = ["business_id"]), Index(value = ["business_id", "employee_id_code"])]
)
data class EmployeeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "business_id")
    val businessId: Long,
    @ColumnInfo(name = "employee_name")
    val employeeName: String,
    @ColumnInfo(name = "employee_id_code")
    val employeeIdCode: String,
    @ColumnInfo(name = "mobile")
    val mobile: String,
    @ColumnInfo(name = "whatsapp_number")
    val whatsappNumber: String = "",
    @ColumnInfo(name = "position")
    val position: String = "",
    @ColumnInfo(name = "joining_date")
    val joiningDate: String = "",
    @ColumnInfo(name = "address")
    val address: String = "",
    @ColumnInfo(name = "status")
    val status: String = "ACTIVE", // ACTIVE or INACTIVE
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "attendance",
    indices = [
        Index(value = ["business_id"]),
        Index(value = ["employee_id"]),
        Index(value = ["business_id", "date"]),
        Index(value = ["employee_id", "date"], unique = true)
    ]
)
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "business_id")
    val businessId: Long,
    @ColumnInfo(name = "employee_id")
    val employeeId: Long,
    @ColumnInfo(name = "date")
    val date: String, // YYYY-MM-DD
    @ColumnInfo(name = "status")
    val status: String, // PRESENT, ABSENT, HALF_DAY, LEAVE, HOLIDAY
    @ColumnInfo(name = "check_in_time")
    val checkInTime: String = "",
    @ColumnInfo(name = "check_out_time")
    val checkOutTime: String = "",
    @ColumnInfo(name = "working_hours")
    val workingHours: Double = 0.0,
    @ColumnInfo(name = "notes")
    val notes: String = "",
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "receipts",
    indices = [
        Index(value = ["business_id"]),
        Index(value = ["receipt_number"], unique = true),
        Index(value = ["transaction_id"])
    ]
)
data class ReceiptEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "business_id")
    val businessId: Long,
    @ColumnInfo(name = "receipt_number")
    val receiptNumber: String,
    @ColumnInfo(name = "transaction_id")
    val transactionId: Long,
    @ColumnInfo(name = "customer_id")
    val customerId: Long,
    @ColumnInfo(name = "customer_name")
    val customerName: String,
    @ColumnInfo(name = "customer_mobile")
    val customerMobile: String,
    @ColumnInfo(name = "date")
    val date: String,
    @ColumnInfo(name = "time")
    val time: String,
    @ColumnInfo(name = "transaction_type")
    val transactionType: String,
    @ColumnInfo(name = "amount")
    val amount: Double,
    @ColumnInfo(name = "payment_method")
    val paymentMethod: String,
    @ColumnInfo(name = "previous_balance")
    val previousBalance: Double,
    @ColumnInfo(name = "current_balance")
    val currentBalance: Double,
    @ColumnInfo(name = "description")
    val description: String = "",
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "payment_links",
    indices = [
        Index(value = ["business_id"]),
        Index(value = ["link_code"], unique = true),
        Index(value = ["customer_id"])
    ]
)
data class PaymentLinkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "business_id")
    val businessId: Long,
    @ColumnInfo(name = "customer_id")
    val customerId: Long,
    @ColumnInfo(name = "customer_name")
    val customerName: String,
    @ColumnInfo(name = "amount")
    val amount: Double,
    @ColumnInfo(name = "purpose")
    val purpose: String,
    @ColumnInfo(name = "due_date")
    val dueDate: String,
    @ColumnInfo(name = "reference_number")
    val referenceNumber: String,
    @ColumnInfo(name = "link_code")
    val linkCode: String,
    @ColumnInfo(name = "status")
    val status: String = "PENDING", // PENDING, PAID, EXPIRED, CANCELLED
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "paid_at")
    val paidAt: Long? = null
)

@Entity(
    tableName = "payment_records",
    indices = [Index(value = ["business_id"]), Index(value = ["payment_link_id"])]
)
data class PaymentRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "business_id")
    val businessId: Long,
    @ColumnInfo(name = "payment_link_id")
    val paymentLinkId: Long,
    @ColumnInfo(name = "transaction_id")
    val transactionId: Long? = null,
    @ColumnInfo(name = "gateway_name")
    val gatewayName: String,
    @ColumnInfo(name = "gateway_payment_id")
    val gatewayPaymentId: String,
    @ColumnInfo(name = "amount")
    val amount: Double,
    @ColumnInfo(name = "status")
    val status: String,
    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "whatsapp_settings",
    indices = [Index(value = ["business_id"], unique = true)]
)
data class WhatsAppSettingsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "business_id")
    val businessId: Long,
    @ColumnInfo(name = "account_id")
    val accountId: String = "",
    @ColumnInfo(name = "phone_number_id")
    val phoneNumberId: String = "",
    @ColumnInfo(name = "access_token")
    val accessToken: String = "",
    @ColumnInfo(name = "api_version")
    val apiVersion: String = "v20.0",
    @ColumnInfo(name = "sender_number")
    val senderNumber: String = "",
    @ColumnInfo(name = "is_connected")
    val isConnected: Boolean = false,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "whatsapp_otp_requests",
    indices = [Index(value = ["mobile_number"])]
)
data class WhatsAppOtpRequestEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "mobile_number")
    val mobileNumber: String,
    @ColumnInfo(name = "otp_hash")
    val otpHash: String,
    @ColumnInfo(name = "salt")
    val salt: String,
    @ColumnInfo(name = "attempts_left")
    val attemptsLeft: Int = 3,
    @ColumnInfo(name = "expires_at")
    val expiresAt: Long,
    @ColumnInfo(name = "is_verified")
    val isVerified: Boolean = false,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "whatsapp_messages",
    indices = [Index(value = ["business_id"]), Index(value = ["customer_id"])]
)
data class WhatsAppMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "business_id")
    val businessId: Long,
    @ColumnInfo(name = "customer_id")
    val customerId: Long?,
    @ColumnInfo(name = "recipient_number")
    val recipientNumber: String,
    @ColumnInfo(name = "message_text")
    val messageText: String,
    @ColumnInfo(name = "channel_type")
    val channelType: String, // REDIRECT or API
    @ColumnInfo(name = "status")
    val status: String, // READY, OPENED, SENT, FAILED
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "message_templates",
    indices = [Index(value = ["business_id"])]
)
data class MessageTemplateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "business_id")
    val businessId: Long,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "category")
    val category: String, // PAYMENT_RECEIVED, PAYMENT_GIVEN, BALANCE_REMINDER, RECEIPT, ATTENDANCE
    @ColumnInfo(name = "body_template")
    val bodyTemplate: String,
    @ColumnInfo(name = "is_default")
    val isDefault: Boolean = true
)

@Entity(
    tableName = "notifications",
    indices = [Index(value = ["business_id"])]
)
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "business_id")
    val businessId: Long,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "message")
    val message: String,
    @ColumnInfo(name = "type")
    val type: String,
    @ColumnInfo(name = "is_read")
    val isRead: Boolean = false,
    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "user_settings",
    indices = [Index(value = ["user_id"], unique = true)]
)
data class UserSettingsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "user_id")
    val userId: Long,
    @ColumnInfo(name = "business_id")
    val businessId: Long,
    @ColumnInfo(name = "language")
    val language: String = "en",
    @ColumnInfo(name = "currency")
    val currency: String = "INR",
    @ColumnInfo(name = "timezone")
    val timezone: String = "Asia/Kolkata",
    @ColumnInfo(name = "theme")
    val theme: String = "SYSTEM"
)

@Entity(
    tableName = "audit_logs",
    indices = [Index(value = ["business_id"]), Index(value = ["user_id"])]
)
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "business_id")
    val businessId: Long,
    @ColumnInfo(name = "user_id")
    val userId: Long,
    @ColumnInfo(name = "action")
    val action: String,
    @ColumnInfo(name = "details")
    val details: String,
    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "recurring_transactions",
    indices = [
        Index(value = ["business_id"]),
        Index(value = ["customer_id"]),
        Index(value = ["is_active"]),
        Index(value = ["next_execution_date"])
    ]
)
data class RecurringTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "business_id")
    val businessId: Long,
    @ColumnInfo(name = "customer_id")
    val customerId: Long,
    @ColumnInfo(name = "customer_name")
    val customerName: String,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "type")
    val type: String, // "RECEIVED" or "GIVEN"
    @ColumnInfo(name = "amount")
    val amount: Double,
    @ColumnInfo(name = "frequency")
    val frequency: String, // "DAILY", "WEEKLY", "MONTHLY", "YEARLY"
    @ColumnInfo(name = "start_date")
    val startDate: String, // "YYYY-MM-DD"
    @ColumnInfo(name = "end_date")
    val endDate: String? = null, // "YYYY-MM-DD" or null
    @ColumnInfo(name = "is_indefinite")
    val isIndefinite: Boolean = true,
    @ColumnInfo(name = "next_execution_date")
    val nextExecutionDate: String, // "YYYY-MM-DD"
    @ColumnInfo(name = "payment_method")
    val paymentMethod: String = "CASH",
    @ColumnInfo(name = "description")
    val description: String = "",
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,
    @ColumnInfo(name = "total_posted_count")
    val totalPostedCount: Int = 0,
    @ColumnInfo(name = "last_posted_at")
    val lastPostedAt: Long? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "inventory_items",
    indices = [
        Index(value = ["business_id"]),
        Index(value = ["item_name"])
    ]
)
data class InventoryItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "business_id")
    val businessId: Long,
    @ColumnInfo(name = "item_name")
    val itemName: String,
    @ColumnInfo(name = "category")
    val category: String = "General",
    @ColumnInfo(name = "quantity")
    val quantity: Int = 0,
    @ColumnInfo(name = "unit")
    val unit: String = "pcs",
    @ColumnInfo(name = "unit_price")
    val unitPrice: Double = 0.0,
    @ColumnInfo(name = "low_stock_threshold")
    val lowStockThreshold: Int = 5,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
