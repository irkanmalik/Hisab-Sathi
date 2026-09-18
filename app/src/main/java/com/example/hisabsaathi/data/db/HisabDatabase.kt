package com.example.hisabsaathi.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserEntity::class,
        BusinessEntity::class,
        CustomerEntity::class,
        CustomerTransactionEntity::class,
        EmployeeEntity::class,
        AttendanceEntity::class,
        ReceiptEntity::class,
        PaymentLinkEntity::class,
        PaymentRecordEntity::class,
        WhatsAppSettingsEntity::class,
        WhatsAppOtpRequestEntity::class,
        WhatsAppMessageEntity::class,
        MessageTemplateEntity::class,
        NotificationEntity::class,
        UserSettingsEntity::class,
        AuditLogEntity::class,
        RecurringTransactionEntity::class,
        InventoryItemEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class HisabDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun businessDao(): BusinessDao
    abstract fun customerDao(): CustomerDao
    abstract fun transactionDao(): TransactionDao
    abstract fun employeeDao(): EmployeeDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun receiptDao(): ReceiptDao
    abstract fun paymentLinkDao(): PaymentLinkDao
    abstract fun whatsAppSettingsDao(): WhatsAppSettingsDao
    abstract fun otpDao(): OtpDao
    abstract fun notificationDao(): NotificationDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao
    abstract fun inventoryDao(): InventoryDao

    companion object {
        @Volatile
        private var INSTANCE: HisabDatabase? = null

        fun getDatabase(context: Context): HisabDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HisabDatabase::class.java,
                    "hisabsaathi_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
