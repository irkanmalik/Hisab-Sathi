package com.example.hisabsaathi.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Query("SELECT * FROM users WHERE mobile_number = :mobile LIMIT 1")
    suspend fun getUserByMobile(mobile: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Long): UserEntity?

    @Query("SELECT * FROM users WHERE is_verified = 1 LIMIT 1")
    suspend fun getAnyVerifiedUser(): UserEntity?

    @Query("SELECT * FROM users")
    fun getAllUsersFlow(): Flow<List<UserEntity>>

    @Update
    suspend fun updateUser(user: UserEntity)
}

@Dao
interface BusinessDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBusiness(business: BusinessEntity): Long

    @Query("SELECT * FROM businesses WHERE owner_user_id = :userId LIMIT 1")
    suspend fun getBusinessByOwner(userId: Long): BusinessEntity?

    @Query("SELECT * FROM businesses WHERE id = :id LIMIT 1")
    suspend fun getBusinessById(id: Long): BusinessEntity?

    @Query("SELECT * FROM businesses")
    fun getAllBusinessesFlow(): Flow<List<BusinessEntity>>

    @Query("SELECT * FROM businesses")
    suspend fun getAllBusinesses(): List<BusinessEntity>

    @Update
    suspend fun updateBusiness(business: BusinessEntity)
}

@Dao
interface CustomerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Query("SELECT * FROM customers WHERE business_id = :businessId ORDER BY full_name ASC")
    fun getCustomersByBusiness(businessId: Long): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id AND business_id = :businessId LIMIT 1")
    suspend fun getCustomerById(id: Long, businessId: Long): CustomerEntity?

    @Query("""
        SELECT * FROM customers 
        WHERE business_id = :businessId AND 
        (full_name LIKE '%' || :query || '%' OR mobile_number LIKE '%' || :query || '%' OR whatsapp_number LIKE '%' || :query || '%')
        ORDER BY full_name ASC
    """)
    fun searchCustomers(businessId: Long, query: String): Flow<List<CustomerEntity>>

    @Query("SELECT COUNT(*) FROM customers WHERE business_id = :businessId")
    fun getCustomerCount(businessId: Long): Flow<Int>

    @Query("SELECT * FROM customers WHERE business_id = :businessId ORDER BY id ASC")
    suspend fun getCustomersList(businessId: Long): List<CustomerEntity>

    @Query("SELECT * FROM customers")
    suspend fun getAllCustomers(): List<CustomerEntity>

    @Query("DELETE FROM customers WHERE id = :id AND business_id = :businessId")
    suspend fun deleteCustomer(id: Long, businessId: Long)

    @Query("DELETE FROM customers WHERE business_id = :businessId")
    suspend fun deleteCustomersByBusiness(businessId: Long)
}

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: CustomerTransactionEntity): Long

    @Query("SELECT * FROM customer_transactions WHERE business_id = :businessId ORDER BY created_at DESC")
    fun getTransactionsByBusiness(businessId: Long): Flow<List<CustomerTransactionEntity>>

    @Query("SELECT * FROM customer_transactions WHERE business_id = :businessId AND customer_id = :customerId ORDER BY created_at DESC")
    fun getTransactionsByCustomer(businessId: Long, customerId: Long): Flow<List<CustomerTransactionEntity>>

    @Query("SELECT * FROM customer_transactions WHERE id = :id AND business_id = :businessId LIMIT 1")
    suspend fun getTransactionById(id: Long, businessId: Long): CustomerTransactionEntity?

    @Query("SELECT * FROM customer_transactions WHERE business_id = :businessId AND date = :date ORDER BY created_at DESC")
    fun getTransactionsByDate(businessId: Long, date: String): Flow<List<CustomerTransactionEntity>>

    @Query("SELECT * FROM customer_transactions WHERE business_id = :businessId ORDER BY id ASC")
    suspend fun getTransactionsList(businessId: Long): List<CustomerTransactionEntity>

    @Query("SELECT * FROM customer_transactions")
    suspend fun getAllTransactions(): List<CustomerTransactionEntity>

    @Query("DELETE FROM customer_transactions WHERE id = :id AND business_id = :businessId")
    suspend fun deleteTransaction(id: Long, businessId: Long)

    @Query("DELETE FROM customer_transactions WHERE business_id = :businessId")
    suspend fun deleteTransactionsByBusiness(businessId: Long)
}

@Dao
interface EmployeeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployee(employee: EmployeeEntity): Long

    @Update
    suspend fun updateEmployee(employee: EmployeeEntity)

    @Query("SELECT * FROM employees WHERE business_id = :businessId ORDER BY employee_name ASC")
    fun getEmployeesByBusiness(businessId: Long): Flow<List<EmployeeEntity>>

    @Query("SELECT * FROM employees WHERE id = :id AND business_id = :businessId LIMIT 1")
    suspend fun getEmployeeById(id: Long, businessId: Long): EmployeeEntity?

    @Query("SELECT * FROM employees WHERE business_id = :businessId ORDER BY id ASC")
    suspend fun getEmployeesList(businessId: Long): List<EmployeeEntity>

    @Query("DELETE FROM employees WHERE id = :id AND business_id = :businessId")
    suspend fun deleteEmployee(id: Long, businessId: Long)

    @Query("DELETE FROM employees WHERE business_id = :businessId")
    suspend fun deleteEmployeesByBusiness(businessId: Long)
}

@Dao
interface AttendanceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: AttendanceEntity): Long

    @Update
    suspend fun updateAttendance(attendance: AttendanceEntity)

    @Query("SELECT * FROM attendance WHERE business_id = :businessId AND date = :date")
    fun getAttendanceForDate(businessId: Long, date: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE business_id = :businessId AND employee_id = :employeeId ORDER BY date DESC")
    fun getAttendanceForEmployee(businessId: Long, employeeId: Long): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE employee_id = :employeeId AND date = :date LIMIT 1")
    suspend fun getAttendanceRecord(employeeId: Long, date: String): AttendanceEntity?

    @Query("SELECT * FROM attendance WHERE business_id = :businessId ORDER BY id ASC")
    suspend fun getAttendanceByBusiness(businessId: Long): List<AttendanceEntity>

    @Query("DELETE FROM attendance WHERE business_id = :businessId")
    suspend fun deleteAttendanceByBusiness(businessId: Long)
}

@Dao
interface ReceiptDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceipt(receipt: ReceiptEntity): Long

    @Query("SELECT * FROM receipts WHERE business_id = :businessId ORDER BY created_at DESC")
    fun getReceiptsByBusiness(businessId: Long): Flow<List<ReceiptEntity>>

    @Query("SELECT * FROM receipts WHERE transaction_id = :transactionId LIMIT 1")
    suspend fun getReceiptByTransaction(transactionId: Long): ReceiptEntity?

    @Query("SELECT COUNT(*) FROM receipts WHERE business_id = :businessId")
    suspend fun getReceiptCount(businessId: Long): Int

    @Query("SELECT * FROM receipts WHERE business_id = :businessId ORDER BY id ASC")
    suspend fun getReceiptsList(businessId: Long): List<ReceiptEntity>

    @Query("DELETE FROM receipts WHERE business_id = :businessId")
    suspend fun deleteReceiptsByBusiness(businessId: Long)
}

@Dao
interface PaymentLinkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaymentLink(link: PaymentLinkEntity): Long

    @Update
    suspend fun updatePaymentLink(link: PaymentLinkEntity)

    @Query("SELECT * FROM payment_links WHERE business_id = :businessId ORDER BY created_at DESC")
    fun getPaymentLinksByBusiness(businessId: Long): Flow<List<PaymentLinkEntity>>

    @Query("SELECT * FROM payment_links WHERE link_code = :code LIMIT 1")
    suspend fun getPaymentLinkByCode(code: String): PaymentLinkEntity?

    @Query("SELECT * FROM payment_links WHERE id = :id LIMIT 1")
    suspend fun getPaymentLinkById(id: Long): PaymentLinkEntity?

    @Query("SELECT * FROM payment_links WHERE reference_number = :referenceNumber LIMIT 1")
    suspend fun getPaymentLinkByReference(referenceNumber: String): PaymentLinkEntity?

    @Query("SELECT * FROM payment_links WHERE business_id = :businessId AND status = 'PENDING'")
    fun getPendingPaymentLinks(businessId: Long): Flow<List<PaymentLinkEntity>>

    @Query("SELECT * FROM payment_links WHERE business_id = :businessId ORDER BY id ASC")
    suspend fun getPaymentLinksList(businessId: Long): List<PaymentLinkEntity>

    @Query("DELETE FROM payment_links WHERE business_id = :businessId")
    suspend fun deletePaymentLinksByBusiness(businessId: Long)
}

@Dao
interface WhatsAppSettingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: WhatsAppSettingsEntity): Long

    @Query("SELECT * FROM whatsapp_settings WHERE business_id = :businessId LIMIT 1")
    suspend fun getSettings(businessId: Long): WhatsAppSettingsEntity?

    @Query("SELECT * FROM whatsapp_settings WHERE business_id = :businessId LIMIT 1")
    fun getSettingsFlow(businessId: Long): Flow<WhatsAppSettingsEntity?>
}

@Dao
interface OtpDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOtpRequest(request: WhatsAppOtpRequestEntity): Long

    @Query("SELECT * FROM whatsapp_otp_requests WHERE mobile_number = :mobile ORDER BY created_at DESC LIMIT 1")
    suspend fun getLatestOtpRequest(mobile: String): WhatsAppOtpRequestEntity?

    @Update
    suspend fun updateOtpRequest(request: WhatsAppOtpRequestEntity)
}

@Dao
interface NotificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity): Long

    @Query("SELECT * FROM notifications WHERE business_id = :businessId ORDER BY timestamp DESC")
    fun getNotifications(businessId: Long): Flow<List<NotificationEntity>>

    @Query("UPDATE notifications SET is_read = 1 WHERE business_id = :businessId")
    suspend fun markAllAsRead(businessId: Long)
}

@Dao
interface AuditLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AuditLogEntity): Long

    @Query("SELECT * FROM audit_logs WHERE business_id = :businessId ORDER BY timestamp DESC LIMIT 100")
    fun getLogsByBusiness(businessId: Long): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 200")
    fun getAllLogsAdmin(): Flow<List<AuditLogEntity>>
}

@Dao
interface RecurringTransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurring(item: RecurringTransactionEntity): Long

    @Update
    suspend fun updateRecurring(item: RecurringTransactionEntity)

    @Query("DELETE FROM recurring_transactions WHERE id = :id")
    suspend fun deleteRecurring(id: Long)

    @Query("SELECT * FROM recurring_transactions WHERE business_id = :businessId ORDER BY is_active DESC, next_execution_date ASC")
    fun getRecurringTransactions(businessId: Long): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions WHERE business_id = :businessId AND is_active = 1 ORDER BY next_execution_date ASC")
    fun getActiveRecurring(businessId: Long): Flow<List<RecurringTransactionEntity>>

    @Query("""
        SELECT * FROM recurring_transactions 
        WHERE business_id = :businessId AND is_active = 1 AND next_execution_date <= :currentDate
        ORDER BY next_execution_date ASC
    """)
    suspend fun getDueRecurring(businessId: Long, currentDate: String): List<RecurringTransactionEntity>

    @Query("SELECT * FROM recurring_transactions WHERE id = :id LIMIT 1")
    suspend fun getRecurringById(id: Long): RecurringTransactionEntity?

    @Query("UPDATE recurring_transactions SET is_active = :isActive WHERE id = :id")
    suspend fun setActiveStatus(id: Long, isActive: Boolean)

    @Query("SELECT * FROM recurring_transactions WHERE business_id = :businessId ORDER BY id ASC")
    suspend fun getRecurringList(businessId: Long): List<RecurringTransactionEntity>

    @Query("DELETE FROM recurring_transactions WHERE business_id = :businessId")
    suspend fun deleteRecurringByBusiness(businessId: Long)
}

@Dao
interface InventoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: InventoryItemEntity): Long

    @Update
    suspend fun updateItem(item: InventoryItemEntity)

    @Query("DELETE FROM inventory_items WHERE id = :id")
    suspend fun deleteItem(id: Long)

    @Query("SELECT * FROM inventory_items WHERE business_id = :businessId ORDER BY item_name ASC")
    fun getInventoryItems(businessId: Long): Flow<List<InventoryItemEntity>>

    @Query("SELECT * FROM inventory_items WHERE business_id = :businessId AND quantity <= low_stock_threshold ORDER BY quantity ASC")
    fun getLowStockItems(businessId: Long): Flow<List<InventoryItemEntity>>

    @Query("SELECT * FROM inventory_items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: Long): InventoryItemEntity?

    @Query("DELETE FROM inventory_items WHERE business_id = :businessId")
    suspend fun deleteInventoryByBusiness(businessId: Long)
}
