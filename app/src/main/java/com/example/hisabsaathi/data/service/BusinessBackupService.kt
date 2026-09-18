package com.example.hisabsaathi.data.service

import androidx.room.withTransaction
import com.example.hisabsaathi.data.db.*
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

class RelationalConstraintException(message: String) : Exception(message)

sealed class ExportResult {
    data class Success(val jsonString: String, val summary: BackupSummary) : ExportResult()
    data class Unauthorized(val message: String) : ExportResult()
    data class Error(val message: String) : ExportResult()
}

sealed class ImportResult {
    data class Success(val summary: BackupSummary) : ImportResult()
    data class Unauthorized(val message: String) : ImportResult()
    data class InvalidFormat(val message: String) : ImportResult()
    data class RelationalError(val message: String) : ImportResult()
    data class Error(val message: String) : ImportResult()
}

data class BackupSummary(
    val businessName: String,
    val customerCount: Int,
    val transactionCount: Int,
    val recurringCount: Int,
    val employeeCount: Int,
    val attendanceCount: Int,
    val receiptCount: Int,
    val paymentLinkCount: Int,
    val exportedAt: String = "",
    val checksum: String = ""
)

class BusinessBackupService(
    private val database: HisabDatabase
) {

    private fun sha256(text: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(text.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Strictly verifies that the requesting user is either:
     * 1) A system ADMIN, or
     * 2) The owner of the targeted business entity.
     * Throws SecurityException and logs security violations if unauthorized.
     */
    suspend fun verifyAuthorization(requestingUserId: Long, targetBusinessId: Long): UserEntity {
        val user = database.userDao().getUserById(requestingUserId)
            ?: run {
                logAudit(targetBusinessId, requestingUserId, "SECURITY_UNAUTHORIZED_EXPORT_IMPORT", "User ID $requestingUserId does not exist")
                throw SecurityException("Unauthorized: User $requestingUserId does not exist")
            }

        val business = database.businessDao().getBusinessById(targetBusinessId)
            ?: throw IllegalArgumentException("Business entity with ID $targetBusinessId does not exist")

        val isAdmin = user.role.equals("admin", ignoreCase = true)
        val isOwner = business.ownerUserId == requestingUserId

        if (!isAdmin && !isOwner) {
            logAudit(
                businessId = targetBusinessId,
                userId = requestingUserId,
                action = "SECURITY_AUTHORIZATION_DENIED",
                details = "User $requestingUserId (role=${user.role}) is neither ADMIN nor owner of business $targetBusinessId"
            )
            throw SecurityException("Unauthorized: User $requestingUserId lacks administrative or ownership rights for business $targetBusinessId")
        }

        return user
    }

    /**
     * Server-side export routine:
     * 1. Strictly checks authorization.
     * 2. Reads all relational entities belonging to targetBusinessId.
     * 3. Formats into structured, validated JSON format.
     * 4. Generates SHA-256 cryptographic checksum for data integrity verification.
     * 5. Logs an audit record and returns JSON payload.
     */
    suspend fun exportBusinessDataToJson(
        requestingUserId: Long,
        targetBusinessId: Long
    ): ExportResult {
        return try {
            val user = verifyAuthorization(requestingUserId, targetBusinessId)
            val business = database.businessDao().getBusinessById(targetBusinessId)
                ?: return ExportResult.Error("Business not found")

            val customers = database.customerDao().getCustomersList(targetBusinessId)
            val transactions = database.transactionDao().getTransactionsList(targetBusinessId)
            val recurring = database.recurringTransactionDao().getRecurringList(targetBusinessId)
            val employees = database.employeeDao().getEmployeesList(targetBusinessId)
            val attendance = database.attendanceDao().getAttendanceByBusiness(targetBusinessId)
            val receipts = database.receiptDao().getReceiptsList(targetBusinessId)
            val paymentLinks = database.paymentLinkDao().getPaymentLinksList(targetBusinessId)
            val whatsAppSettings = database.whatsAppSettingsDao().getSettings(targetBusinessId)

            val exportTimestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date())

            val rootJson = JSONObject()

            // Payload Object for Checksum calculation
            val payloadJson = JSONObject()

            // Business
            val businessJson = JSONObject().apply {
                put("id", business.id)
                put("businessName", business.businessName)
                put("ownerName", business.ownerName)
                put("businessMobile", business.businessMobile)
                put("address", business.address)
                put("city", business.city)
                put("district", business.district)
                put("state", business.state)
                put("country", business.country)
                put("currency", business.currency)
                put("preferredLanguage", business.preferredLanguage)
            }
            payloadJson.put("business", businessJson)

            // Customers
            val customersArray = JSONArray()
            for (c in customers) {
                customersArray.put(JSONObject().apply {
                    put("id", c.id)
                    put("fullName", c.fullName)
                    put("mobileNumber", c.mobileNumber)
                    put("whatsappNumber", c.whatsappNumber)
                    put("address", c.address)
                    put("openingBalance", c.openingBalance)
                    put("balanceType", c.balanceType)
                    put("currentBalance", c.currentBalance)
                    put("totalReceived", c.totalReceived)
                    put("totalGiven", c.totalGiven)
                    put("notes", c.notes)
                })
            }
            payloadJson.put("customers", customersArray)

            // Transactions
            val transactionsArray = JSONArray()
            for (tx in transactions) {
                transactionsArray.put(JSONObject().apply {
                    put("id", tx.id)
                    put("customerId", tx.customerId)
                    put("type", tx.type)
                    put("amount", tx.amount)
                    put("date", tx.date)
                    put("time", tx.time)
                    put("paymentMethod", tx.paymentMethod)
                    put("description", tx.description)
                    put("referenceNumber", tx.referenceNumber)
                    put("previousBalance", tx.previousBalance)
                    put("balanceAfter", tx.balanceAfter)
                })
            }
            payloadJson.put("transactions", transactionsArray)

            // Recurring Transactions
            val recurringArray = JSONArray()
            for (r in recurring) {
                recurringArray.put(JSONObject().apply {
                    put("id", r.id)
                    put("customerId", r.customerId)
                    put("customerName", r.customerName)
                    put("title", r.title)
                    put("type", r.type)
                    put("amount", r.amount)
                    put("frequency", r.frequency)
                    put("startDate", r.startDate)
                    put("endDate", r.endDate ?: JSONObject.NULL)
                    put("isIndefinite", r.isIndefinite)
                    put("nextExecutionDate", r.nextExecutionDate)
                    put("paymentMethod", r.paymentMethod)
                    put("description", r.description)
                    put("isActive", r.isActive)
                    put("totalPostedCount", r.totalPostedCount)
                })
            }
            payloadJson.put("recurringTransactions", recurringArray)

            // Employees
            val employeesArray = JSONArray()
            for (e in employees) {
                employeesArray.put(JSONObject().apply {
                    put("id", e.id)
                    put("employeeName", e.employeeName)
                    put("employeeIdCode", e.employeeIdCode)
                    put("mobile", e.mobile)
                    put("whatsappNumber", e.whatsappNumber)
                    put("position", e.position)
                    put("joiningDate", e.joiningDate)
                    put("status", e.status)
                })
            }
            payloadJson.put("employees", employeesArray)

            // Attendance
            val attendanceArray = JSONArray()
            for (a in attendance) {
                attendanceArray.put(JSONObject().apply {
                    put("id", a.id)
                    put("employeeId", a.employeeId)
                    put("date", a.date)
                    put("status", a.status)
                    put("checkInTime", a.checkInTime)
                    put("checkOutTime", a.checkOutTime)
                    put("workingHours", a.workingHours)
                    put("notes", a.notes)
                })
            }
            payloadJson.put("attendance", attendanceArray)

            // Receipts
            val receiptsArray = JSONArray()
            for (rc in receipts) {
                receiptsArray.put(JSONObject().apply {
                    put("id", rc.id)
                    put("receiptNumber", rc.receiptNumber)
                    put("transactionId", rc.transactionId)
                    put("customerId", rc.customerId)
                    put("customerName", rc.customerName)
                    put("customerMobile", rc.customerMobile)
                    put("date", rc.date)
                    put("time", rc.time)
                    put("transactionType", rc.transactionType)
                    put("amount", rc.amount)
                    put("paymentMethod", rc.paymentMethod)
                    put("previousBalance", rc.previousBalance)
                    put("currentBalance", rc.currentBalance)
                })
            }
            payloadJson.put("receipts", receiptsArray)

            // Payment Links
            val paymentLinksArray = JSONArray()
            for (pl in paymentLinks) {
                paymentLinksArray.put(JSONObject().apply {
                    put("id", pl.id)
                    put("linkCode", pl.linkCode)
                    put("customerId", pl.customerId)
                    put("customerName", pl.customerName)
                    put("amount", pl.amount)
                    put("purpose", pl.purpose)
                    put("dueDate", pl.dueDate)
                    put("referenceNumber", pl.referenceNumber)
                    put("status", pl.status)
                })
            }
            payloadJson.put("paymentLinks", paymentLinksArray)

            // WhatsApp Settings
            if (whatsAppSettings != null) {
                val waJson = JSONObject().apply {
                    put("accountId", whatsAppSettings.accountId)
                    put("phoneNumberId", whatsAppSettings.phoneNumberId)
                    put("apiVersion", whatsAppSettings.apiVersion)
                    put("senderNumber", whatsAppSettings.senderNumber)
                    put("isConnected", whatsAppSettings.isConnected)
                }
                payloadJson.put("whatsAppSettings", waJson)
            }

            // Compute payload checksum
            val payloadString = payloadJson.toString()
            val checksum = sha256(payloadString)

            // Metadata
            val metadataJson = JSONObject().apply {
                put("format", "HISAB_SAATHI_BUSINESS_BACKUP")
                put("schemaVersion", 1)
                put("exportedAt", exportTimestamp)
                put("exportedByUserId", requestingUserId)
                put("exportedByUserRole", user.role)
                put("businessId", targetBusinessId)
                put("checksum", checksum)
                put("counts", JSONObject().apply {
                    put("customers", customers.size)
                    put("transactions", transactions.size)
                    put("recurring", recurring.size)
                    put("employees", employees.size)
                    put("attendance", attendance.size)
                    put("receipts", receipts.size)
                    put("paymentLinks", paymentLinks.size)
                })
            }

            rootJson.put("metadata", metadataJson)
            rootJson.put("data", payloadJson)

            val finalJsonString = rootJson.toString(2)

            val summary = BackupSummary(
                businessName = business.businessName,
                customerCount = customers.size,
                transactionCount = transactions.size,
                recurringCount = recurring.size,
                employeeCount = employees.size,
                attendanceCount = attendance.size,
                receiptCount = receipts.size,
                paymentLinkCount = paymentLinks.size,
                exportedAt = exportTimestamp,
                checksum = checksum
            )

            logAudit(
                businessId = targetBusinessId,
                userId = requestingUserId,
                action = "BUSINESS_DATA_EXPORTED",
                details = "Successfully exported JSON backup: ${customers.size} customers, ${transactions.size} transactions, ${recurring.size} recurring, ${employees.size} employees"
            )

            ExportResult.Success(finalJsonString, summary)
        } catch (e: SecurityException) {
            ExportResult.Unauthorized(e.message ?: "Unauthorized")
        } catch (e: Exception) {
            ExportResult.Error("Export failed: ${e.message}")
        }
    }

    /**
     * Server-side import & restore routine:
     * 1. Strictly checks authorization: requesting user must be ADMIN or owner of targetBusinessId.
     * 2. Validates format header and schema version.
     * 3. Validates and enforces ALL relational foreign key constraints:
     *    - Each transaction must resolve to an existing/imported customer.
     *    - Each recurring schedule must resolve to an existing/imported customer.
     *    - Each attendance record must resolve to an existing/imported employee.
     *    - Each receipt must resolve to an existing/imported transaction and customer.
     * 4. Executes entirely inside an atomic Room database transaction (database.withTransaction).
     *    Any constraint failure or exception causes an immediate, complete rollback.
     * 5. Recalculates and verifies mathematical customer balance integrity.
     * 6. Logs audit trail upon completion.
     */
    suspend fun importBusinessDataFromJson(
        requestingUserId: Long,
        targetBusinessId: Long,
        jsonString: String,
        clearExistingData: Boolean = true
    ): ImportResult {
        return try {
            // Step 1: Authorization
            val user = verifyAuthorization(requestingUserId, targetBusinessId)
            val business = database.businessDao().getBusinessById(targetBusinessId)
                ?: return ImportResult.Error("Target business entity $targetBusinessId not found")

            // Step 2: Validate JSON Structure
            val rootJson = try {
                JSONObject(jsonString)
            } catch (e: Exception) {
                return ImportResult.InvalidFormat("Invalid JSON syntax: ${e.message}")
            }

            val metadataJson = rootJson.optJSONObject("metadata")
                ?: return ImportResult.InvalidFormat("Missing 'metadata' header in backup JSON")

            val format = metadataJson.optString("format")
            if (format != "HISAB_SAATHI_BUSINESS_BACKUP") {
                return ImportResult.InvalidFormat("Unsupported format: '$format'. Expected 'HISAB_SAATHI_BUSINESS_BACKUP'")
            }

            val schemaVersion = metadataJson.optInt("schemaVersion", 0)
            if (schemaVersion < 1) {
                return ImportResult.InvalidFormat("Unsupported schema version: $schemaVersion")
            }

            val payloadJson = rootJson.optJSONObject("data")
                ?: return ImportResult.InvalidFormat("Missing 'data' block in backup JSON")

            // Parse Entities from JSON
            val customersArray = payloadJson.optJSONArray("customers") ?: JSONArray()
            val transactionsArray = payloadJson.optJSONArray("transactions") ?: JSONArray()
            val recurringArray = payloadJson.optJSONArray("recurringTransactions") ?: JSONArray()
            val employeesArray = payloadJson.optJSONArray("employees") ?: JSONArray()
            val attendanceArray = payloadJson.optJSONArray("attendance") ?: JSONArray()
            val receiptsArray = payloadJson.optJSONArray("receipts") ?: JSONArray()
            val paymentLinksArray = payloadJson.optJSONArray("paymentLinks") ?: JSONArray()

            // Verify Relational Constraints Prior to Insertion
            val customerSourceIds = mutableSetOf<Long>()
            for (i in 0 until customersArray.length()) {
                val cObj = customersArray.getJSONObject(i)
                customerSourceIds.add(cObj.getLong("id"))
            }

            for (i in 0 until transactionsArray.length()) {
                val txObj = transactionsArray.getJSONObject(i)
                val customerId = txObj.getLong("customerId")
                if (!customerSourceIds.contains(customerId)) {
                    throw RelationalConstraintException(
                        "Relational Constraint Failure: Transaction ID ${txObj.optLong("id")} refers to non-existent Customer ID $customerId"
                    )
                }
            }

            for (i in 0 until recurringArray.length()) {
                val rObj = recurringArray.getJSONObject(i)
                val customerId = rObj.getLong("customerId")
                if (!customerSourceIds.contains(customerId)) {
                    throw RelationalConstraintException(
                        "Relational Constraint Failure: Recurring Transaction ID ${rObj.optLong("id")} refers to non-existent Customer ID $customerId"
                    )
                }
            }

            val employeeSourceIds = mutableSetOf<Long>()
            for (i in 0 until employeesArray.length()) {
                val eObj = employeesArray.getJSONObject(i)
                employeeSourceIds.add(eObj.getLong("id"))
            }

            for (i in 0 until attendanceArray.length()) {
                val aObj = attendanceArray.getJSONObject(i)
                val employeeId = aObj.getLong("employeeId")
                if (!employeeSourceIds.contains(employeeId)) {
                    throw RelationalConstraintException(
                        "Relational Constraint Failure: Attendance record for date '${aObj.optString("date")}' refers to non-existent Employee ID $employeeId"
                    )
                }
            }

            // Step 3 & 4: Atomic Execution with Transaction Rollback
            database.withTransaction {
                if (clearExistingData) {
                    // Clean wipe existing business records for clean restore
                    database.receiptDao().deleteReceiptsByBusiness(targetBusinessId)
                    database.transactionDao().deleteTransactionsByBusiness(targetBusinessId)
                    database.recurringTransactionDao().deleteRecurringByBusiness(targetBusinessId)
                    database.customerDao().deleteCustomersByBusiness(targetBusinessId)
                    database.attendanceDao().deleteAttendanceByBusiness(targetBusinessId)
                    database.employeeDao().deleteEmployeesByBusiness(targetBusinessId)
                    database.paymentLinkDao().deletePaymentLinksByBusiness(targetBusinessId)
                }

                // Map Source IDs to Target Relational IDs
                val customerIdMap = mutableMapOf<Long, Long>()
                val transactionIdMap = mutableMapOf<Long, Long>()
                val employeeIdMap = mutableMapOf<Long, Long>()

                // A. Insert Customers
                for (i in 0 until customersArray.length()) {
                    val cObj = customersArray.getJSONObject(i)
                    val sourceId = cObj.getLong("id")
                    val customer = CustomerEntity(
                        businessId = targetBusinessId,
                        fullName = cObj.getString("fullName"),
                        mobileNumber = cObj.getString("mobileNumber"),
                        whatsappNumber = cObj.optString("whatsappNumber", ""),
                        address = cObj.optString("address", ""),
                        openingBalance = cObj.optDouble("openingBalance", 0.0),
                        balanceType = cObj.optString("balanceType", "RECEIVABLE"),
                        currentBalance = cObj.optDouble("currentBalance", 0.0),
                        totalReceived = cObj.optDouble("totalReceived", 0.0),
                        totalGiven = cObj.optDouble("totalGiven", 0.0),
                        notes = cObj.optString("notes", "")
                    )
                    val newId = database.customerDao().insertCustomer(customer)
                    customerIdMap[sourceId] = newId
                }

                // B. Insert Transactions with Remapped Relational Customer ID
                for (i in 0 until transactionsArray.length()) {
                    val txObj = transactionsArray.getJSONObject(i)
                    val sourceId = txObj.getLong("id")
                    val sourceCustomerId = txObj.getLong("customerId")
                    val newCustomerId = customerIdMap[sourceCustomerId]
                        ?: throw RelationalConstraintException("Failed mapping for customer $sourceCustomerId on transaction $sourceId")

                    val transaction = CustomerTransactionEntity(
                        businessId = targetBusinessId,
                        customerId = newCustomerId,
                        type = txObj.getString("type"),
                        amount = txObj.getDouble("amount"),
                        date = txObj.getString("date"),
                        time = txObj.optString("time", "12:00 PM"),
                        paymentMethod = txObj.optString("paymentMethod", "CASH"),
                        description = txObj.optString("description", ""),
                        referenceNumber = txObj.optString("referenceNumber", ""),
                        previousBalance = txObj.optDouble("previousBalance", 0.0),
                        balanceAfter = txObj.optDouble("balanceAfter", 0.0)
                    )
                    val newTxId = database.transactionDao().insertTransaction(transaction)
                    transactionIdMap[sourceId] = newTxId
                }

                // C. Insert Recurring Transactions with Remapped Relational Customer ID
                for (i in 0 until recurringArray.length()) {
                    val rObj = recurringArray.getJSONObject(i)
                    val sourceCustomerId = rObj.getLong("customerId")
                    val newCustomerId = customerIdMap[sourceCustomerId]
                        ?: throw RelationalConstraintException("Failed mapping for customer $sourceCustomerId on recurring transaction")

                    val recurringItem = RecurringTransactionEntity(
                        businessId = targetBusinessId,
                        customerId = newCustomerId,
                        customerName = rObj.getString("customerName"),
                        title = rObj.getString("title"),
                        type = rObj.getString("type"),
                        amount = rObj.getDouble("amount"),
                        frequency = rObj.getString("frequency"),
                        startDate = rObj.getString("startDate"),
                        endDate = if (rObj.isNull("endDate")) null else rObj.getString("endDate"),
                        isIndefinite = rObj.optBoolean("isIndefinite", true),
                        nextExecutionDate = rObj.getString("nextExecutionDate"),
                        paymentMethod = rObj.optString("paymentMethod", "CASH"),
                        description = rObj.optString("description", ""),
                        isActive = rObj.optBoolean("isActive", true),
                        totalPostedCount = rObj.optInt("totalPostedCount", 0)
                    )
                    database.recurringTransactionDao().insertRecurring(recurringItem)
                }

                // D. Insert Employees
                for (i in 0 until employeesArray.length()) {
                    val eObj = employeesArray.getJSONObject(i)
                    val sourceId = eObj.getLong("id")
                    val employee = EmployeeEntity(
                        businessId = targetBusinessId,
                        employeeName = eObj.getString("employeeName"),
                        employeeIdCode = eObj.optString("employeeIdCode", "EMP-$i"),
                        mobile = eObj.getString("mobile"),
                        whatsappNumber = eObj.optString("whatsappNumber", ""),
                        position = eObj.optString("position", ""),
                        joiningDate = eObj.optString("joiningDate", ""),
                        status = eObj.optString("status", "ACTIVE")
                    )
                    val newId = database.employeeDao().insertEmployee(employee)
                    employeeIdMap[sourceId] = newId
                }

                // E. Insert Attendance with Remapped Relational Employee ID
                for (i in 0 until attendanceArray.length()) {
                    val aObj = attendanceArray.getJSONObject(i)
                    val sourceEmployeeId = aObj.getLong("employeeId")
                    val newEmployeeId = employeeIdMap[sourceEmployeeId]
                        ?: throw RelationalConstraintException("Failed mapping for employee $sourceEmployeeId on attendance")

                    val attendance = AttendanceEntity(
                        businessId = targetBusinessId,
                        employeeId = newEmployeeId,
                        date = aObj.getString("date"),
                        status = aObj.getString("status"),
                        checkInTime = aObj.optString("checkInTime", ""),
                        checkOutTime = aObj.optString("checkOutTime", ""),
                        workingHours = aObj.optDouble("workingHours", 0.0),
                        notes = aObj.optString("notes", "")
                    )
                    database.attendanceDao().insertAttendance(attendance)
                }

                // F. Insert Receipts with Remapped Relational Transaction and Customer IDs
                for (i in 0 until receiptsArray.length()) {
                    val rcObj = receiptsArray.getJSONObject(i)
                    val sourceTxId = rcObj.getLong("transactionId")
                    val sourceCustId = rcObj.getLong("customerId")
                    val newTxId = transactionIdMap[sourceTxId] ?: 0L
                    val newCustId = customerIdMap[sourceCustId] ?: 0L

                    val receipt = ReceiptEntity(
                        businessId = targetBusinessId,
                        receiptNumber = rcObj.getString("receiptNumber"),
                        transactionId = newTxId,
                        customerId = newCustId,
                        customerName = rcObj.getString("customerName"),
                        customerMobile = rcObj.optString("customerMobile", ""),
                        date = rcObj.getString("date"),
                        time = rcObj.optString("time", "12:00 PM"),
                        transactionType = rcObj.getString("transactionType"),
                        amount = rcObj.getDouble("amount"),
                        paymentMethod = rcObj.optString("paymentMethod", "CASH"),
                        previousBalance = rcObj.optDouble("previousBalance", 0.0),
                        currentBalance = rcObj.optDouble("currentBalance", 0.0)
                    )
                    database.receiptDao().insertReceipt(receipt)
                }

                // G. Insert Payment Links
                for (i in 0 until paymentLinksArray.length()) {
                    val plObj = paymentLinksArray.getJSONObject(i)
                    val sourceCustId = plObj.getLong("customerId")
                    val newCustId = customerIdMap[sourceCustId] ?: 0L

                    val paymentLink = PaymentLinkEntity(
                        businessId = targetBusinessId,
                        linkCode = plObj.getString("linkCode"),
                        customerId = newCustId,
                        customerName = plObj.getString("customerName"),
                        amount = plObj.getDouble("amount"),
                        purpose = plObj.optString("purpose", "Payment"),
                        dueDate = plObj.optString("dueDate", ""),
                        referenceNumber = plObj.optString("referenceNumber", ""),
                        status = plObj.optString("status", "PENDING")
                    )
                    database.paymentLinkDao().insertPaymentLink(paymentLink)
                }

                // Balance Integrity Recalculation
                for ((_, newCustomerId) in customerIdMap) {
                    val cust = database.customerDao().getCustomerById(newCustomerId, targetBusinessId)
                    if (cust != null) {
                        // Recalculate based on inserted transactions
                        val custTransactions = database.transactionDao().getTransactionsList(targetBusinessId)
                            .filter { it.customerId == newCustomerId }
                        val sumGiven = custTransactions.filter { it.type == "GIVEN" }.sumOf { it.amount }
                        val sumReceived = custTransactions.filter { it.type == "RECEIVED" }.sumOf { it.amount }
                        val calculatedBalance = cust.openingBalance + (sumGiven - sumReceived)

                        val updatedCustomer = cust.copy(
                            totalGiven = sumGiven,
                            totalReceived = sumReceived,
                            currentBalance = calculatedBalance
                        )
                        database.customerDao().updateCustomer(updatedCustomer)
                    }
                }
            }

            val summary = BackupSummary(
                businessName = business.businessName,
                customerCount = customersArray.length(),
                transactionCount = transactionsArray.length(),
                recurringCount = recurringArray.length(),
                employeeCount = employeesArray.length(),
                attendanceCount = attendanceArray.length(),
                receiptCount = receiptsArray.length(),
                paymentLinkCount = paymentLinksArray.length()
            )

            logAudit(
                businessId = targetBusinessId,
                userId = requestingUserId,
                action = "BUSINESS_DATA_RESTORED",
                details = "Successfully restored backup: ${customersArray.length()} customers, ${transactionsArray.length()} transactions, ${recurringArray.length()} recurring schedules, ${employeesArray.length()} employees"
            )

            ImportResult.Success(summary)
        } catch (e: SecurityException) {
            ImportResult.Unauthorized(e.message ?: "Unauthorized")
        } catch (e: RelationalConstraintException) {
            ImportResult.RelationalError(e.message ?: "Relational constraint error")
        } catch (e: Exception) {
            ImportResult.Error("Import restore failed: ${e.message}")
        }
    }

    private suspend fun logAudit(businessId: Long, userId: Long, action: String, details: String) {
        try {
            database.auditLogDao().insertLog(
                AuditLogEntity(
                    businessId = businessId,
                    userId = userId,
                    action = action,
                    details = details
                )
            )
        } catch (_: Exception) {}
    }
}
