package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.hisabsaathi.data.db.*
import com.example.hisabsaathi.data.repository.HisabRepository
import com.example.hisabsaathi.data.service.ExportResult
import com.example.hisabsaathi.data.service.ImportResult
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BusinessBackupUnitTest {

    private lateinit var database: HisabDatabase
    private lateinit var repository: HisabRepository

    private var adminUserId: Long = 0
    private var nonAdminUserId: Long = 0
    private var businessId: Long = 0
    private var customerId: Long = 0

    @Before
    fun setup() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            database = Room.inMemoryDatabaseBuilder(context, HisabDatabase::class.java)
                .allowMainThreadQueries()
                .build()
            repository = HisabRepository(context, database)

            // Seed Admin User
            adminUserId = database.userDao().insertUser(
                UserEntity(
                    email = "irkanmalik244255@gmail.com",
                    mobileNumber = "9876543210",
                    role = "admin",
                    isVerified = true
                )
            )

            // Seed Non-Admin User
            nonAdminUserId = database.userDao().insertUser(
                UserEntity(
                    email = "intruder@example.com",
                    mobileNumber = "9111122223",
                    role = "viewer",
                    isVerified = true
                )
            )

            // Seed Business belonging to admin
            businessId = database.businessDao().insertBusiness(
                BusinessEntity(
                    ownerUserId = adminUserId,
                    businessName = "Malik Electronics & Services",
                    ownerName = "Irkan Malik",
                    businessMobile = "9876543210"
                )
            )

            // Seed Customer
            customerId = database.customerDao().insertCustomer(
                CustomerEntity(
                    businessId = businessId,
                    fullName = "Suresh Verma",
                    mobileNumber = "9876500001",
                    openingBalance = 500.0,
                    currentBalance = 1500.0,
                    totalGiven = 1000.0,
                    totalReceived = 0.0
                )
            )

            // Seed Transaction
            val txId = database.transactionDao().insertTransaction(
                CustomerTransactionEntity(
                    businessId = businessId,
                    customerId = customerId,
                    type = "GIVEN",
                    amount = 1000.0,
                    date = "2026-09-17",
                    time = "10:30 AM",
                    paymentMethod = "CASH",
                    description = "Hardware purchase",
                    previousBalance = 500.0,
                    balanceAfter = 1500.0
                )
            )

            // Seed Receipt
            database.receiptDao().insertReceipt(
                ReceiptEntity(
                    businessId = businessId,
                    receiptNumber = "REC-TEST-001",
                    transactionId = txId,
                    customerId = customerId,
                    customerName = "Suresh Verma",
                    customerMobile = "9876500001",
                    date = "2026-09-17",
                    time = "10:30 AM",
                    transactionType = "GIVEN",
                    amount = 1000.0,
                    paymentMethod = "CASH",
                    previousBalance = 500.0,
                    currentBalance = 1500.0
                )
            )

            // Seed Employee
            val empId = database.employeeDao().insertEmployee(
                EmployeeEntity(
                    businessId = businessId,
                    employeeName = "Amit Sharma",
                    employeeIdCode = "EMP-001",
                    mobile = "9876500002",
                    position = "Technician",
                    joiningDate = "2026-01-01"
                )
            )

            // Seed Attendance
            database.attendanceDao().insertAttendance(
                AttendanceEntity(
                    businessId = businessId,
                    employeeId = empId,
                    date = "2026-09-17",
                    status = "PRESENT",
                    checkInTime = "09:00 AM",
                    checkOutTime = "06:00 PM",
                    workingHours = 9.0
                )
            )
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testExportBusinessData_authorizedAdmin_generatesValidJson() = runBlocking {
        val result = repository.exportBusinessData(adminUserId, businessId)
        assertTrue("Export should succeed for authorized admin", result is ExportResult.Success)

        val success = result as ExportResult.Success
        assertEquals(1, success.summary.customerCount)
        assertEquals(1, success.summary.transactionCount)
        assertEquals(1, success.summary.employeeCount)
        assertEquals(1, success.summary.attendanceCount)
        assertEquals(1, success.summary.receiptCount)
        assertTrue(success.summary.checksum.isNotBlank())

        val json = JSONObject(success.jsonString)
        val metadata = json.getJSONObject("metadata")
        assertEquals("HISAB_SAATHI_BUSINESS_BACKUP", metadata.getString("format"))
        assertEquals(1, metadata.getInt("schemaVersion"))
        assertEquals(businessId, metadata.getLong("businessId"))

        val data = json.getJSONObject("data")
        val customersArray = data.getJSONArray("customers")
        assertEquals(1, customersArray.length())
        assertEquals("Suresh Verma", customersArray.getJSONObject(0).getString("fullName"))
    }

    @Test
    fun testExportBusinessData_unauthorizedUser_returnsUnauthorized() = runBlocking {
        val result = repository.exportBusinessData(nonAdminUserId, businessId)
        assertTrue("Export should be unauthorized for non-admin/non-owner", result is ExportResult.Unauthorized)
    }

    @Test
    fun testImportBusinessData_authorizedAdmin_restoresRelationalDataSuccessfully() = runBlocking {
        // Step 1: Export initial state
        val exportResult = repository.exportBusinessData(adminUserId, businessId) as ExportResult.Success
        val exportedJson = exportResult.jsonString

        // Step 2: Clear or modify state
        database.customerDao().deleteCustomersByBusiness(businessId)
        assertEquals(0, database.customerDao().getCustomersList(businessId).size)

        // Step 3: Restore state
        val importResult = repository.importBusinessData(adminUserId, businessId, exportedJson, clearExisting = true)
        assertTrue("Import should succeed", importResult is ImportResult.Success)

        val summary = (importResult as ImportResult.Success).summary
        assertEquals(1, summary.customerCount)
        assertEquals(1, summary.transactionCount)
        assertEquals(1, summary.employeeCount)

        // Step 4: Verify restored database records and relational foreign keys
        val restoredCustomers = database.customerDao().getCustomersList(businessId)
        assertEquals(1, restoredCustomers.size)
        val restoredCustomer = restoredCustomers[0]
        assertEquals("Suresh Verma", restoredCustomer.fullName)

        val restoredTransactions = database.transactionDao().getTransactionsList(businessId)
        assertEquals(1, restoredTransactions.size)
        assertEquals(restoredCustomer.id, restoredTransactions[0].customerId)
        assertEquals(1000.0, restoredTransactions[0].amount, 0.01)

        val restoredEmployees = database.employeeDao().getEmployeesList(businessId)
        assertEquals(1, restoredEmployees.size)
        val restoredAttendance = database.attendanceDao().getAttendanceByBusiness(businessId)
        assertEquals(1, restoredAttendance.size)
        assertEquals(restoredEmployees[0].id, restoredAttendance[0].employeeId)
    }

    @Test
    fun testImportBusinessData_relationalConstraintViolation_failsAndRollsBack() = runBlocking {
        // Malformed JSON where transaction references a non-existent customer ID (99999)
        val malformedJson = """
            {
              "metadata": {
                "format": "HISAB_SAATHI_BUSINESS_BACKUP",
                "schemaVersion": 1,
                "businessId": $businessId
              },
              "data": {
                "customers": [
                  {
                    "id": 1,
                    "fullName": "Valid Customer",
                    "mobileNumber": "9876511111"
                  }
                ],
                "transactions": [
                  {
                    "id": 101,
                    "customerId": 99999,
                    "type": "GIVEN",
                    "amount": 500.0,
                    "date": "2026-09-17"
                  }
                ]
              }
            }
        """.trimIndent()

        val initialCustomerCount = database.customerDao().getCustomersList(businessId).size

        val importResult = repository.importBusinessData(adminUserId, businessId, malformedJson, clearExisting = false)
        assertTrue("Import must fail with relational error", importResult is ImportResult.RelationalError)

        // Verify that database was NOT modified (atomic integrity)
        val afterCustomerCount = database.customerDao().getCustomersList(businessId).size
        assertEquals(initialCustomerCount, afterCustomerCount)
    }

    @Test
    fun testDataExportImportService_defaultFileName_isSanitized() {
        val fileName = repository.dataExportImportService.generateDefaultExportFileName("Malik Electronics & Co.")
        assertTrue("Filename must start with prefix", fileName.startsWith("hisab_saathi_malik_electronics___"))
        assertTrue("Filename must end with .json", fileName.endsWith(".json"))
    }

    @Test
    fun testImportBusinessData_attendanceMissingEmployeeForeignKey_failsAndRollsBack() = runBlocking {
        // Malformed JSON where Attendance references an unmapped Employee ID (88888)
        val malformedJson = """
            {
              "metadata": {
                "format": "HISAB_SAATHI_BUSINESS_BACKUP",
                "schemaVersion": 1,
                "businessId": $businessId
              },
              "data": {
                "employees": [
                  {
                    "id": 1,
                    "employeeName": "Valid Employee",
                    "mobile": "9876540000"
                  }
                ],
                "attendance": [
                  {
                    "id": 10,
                    "employeeId": 88888,
                    "date": "2026-09-17",
                    "status": "PRESENT"
                  }
                ]
              }
            }
        """.trimIndent()

        val initialEmployeeCount = database.employeeDao().getEmployeesList(businessId).size

        val importResult = repository.dataExportImportService.importBusinessDataFromJson(
            requestingUserId = adminUserId,
            targetBusinessId = businessId,
            jsonString = malformedJson,
            clearExistingData = false
        )
        assertTrue("Import must fail with relational constraint failure", importResult is ImportResult.RelationalError)

        val afterEmployeeCount = database.employeeDao().getEmployeesList(businessId).size
        assertEquals(initialEmployeeCount, afterEmployeeCount)
    }
}
