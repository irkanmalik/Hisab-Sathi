package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.hisabsaathi.data.db.*
import com.example.hisabsaathi.data.repository.HisabRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RecurringTransactionUnitTest {

    private lateinit var database: HisabDatabase
    private lateinit var repository: HisabRepository

    @Before
    fun setup() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, HisabDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = HisabRepository(context, database)

        // Seed default user and business
        val userId = database.userDao().insertUser(
            UserEntity(
                email = "test@example.com",
                mobileNumber = "9876543210",
                role = "ADMIN",
                isVerified = true
            )
        )
        val businessId = database.businessDao().insertBusiness(
            BusinessEntity(
                ownerUserId = userId,
                businessName = "Malik Kirana Store",
                ownerName = "Irkan Malik",
                businessMobile = "9876543210"
            )
        )

        // Seed test customer
        database.customerDao().insertCustomer(
            CustomerEntity(
                id = 101L,
                businessId = businessId,
                fullName = "Ramesh Kumar",
                mobileNumber = "9876543211",
                currentBalance = 0.0
            )
        )

        repository.initializeDefaultSession()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testCreateAndFetchRecurringTransaction() = runBlocking {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val id = repository.addRecurringTransaction(
            customerId = 101L,
            title = "Monthly Rent",
            type = "RECEIVED",
            amount = 15000.0,
            frequency = "MONTHLY",
            startDate = today,
            endDate = null,
            isIndefinite = true,
            paymentMethod = "UPI",
            description = "Main market shop rent"
        )

        assertTrue(id > 0)

        val list = repository.getRecurringTransactions().first()
        assertEquals(1, list.size)
        val item = list.first()
        assertEquals("Monthly Rent", item.title)
        assertEquals(15000.0, item.amount, 0.01)
        assertEquals("MONTHLY", item.frequency)
        assertTrue(item.isIndefinite)
        assertTrue(item.isActive)
        assertEquals(today, item.nextExecutionDate)
    }

    @Test
    fun testProcessDueRecurringTransactionsAutoPosts() = runBlocking {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // Set up recurring due today
        repository.addRecurringTransaction(
            customerId = 101L,
            title = "Daily Milk Delivery",
            type = "GIVEN",
            amount = 120.0,
            frequency = "DAILY",
            startDate = today,
            isIndefinite = true,
            paymentMethod = "CASH",
            description = "2 Litres Buffalo Milk"
        )

        // Process due transactions
        val postedCount = repository.processDueRecurringTransactions()
        assertEquals(1, postedCount)

        // Verify transaction is recorded in ledger
        val transactions = repository.getAllTransactions().first()
        assertEquals(1, transactions.size)
        assertEquals(120.0, transactions.first().amount, 0.01)
        assertEquals("GIVEN", transactions.first().type)

        // Verify recurring entity execution counters updated
        val recurringList = repository.getRecurringTransactions().first()
        val item = recurringList.first()
        assertEquals(1, item.totalPostedCount)
        assertNotNull(item.lastPostedAt)
        assertNotEquals(today, item.nextExecutionDate) // Advanced to tomorrow
    }

    @Test
    fun testRecurringTransactionWithEndDateStopsWhenFinished() = runBlocking {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // Set up recurring with end date as today
        repository.addRecurringTransaction(
            customerId = 101L,
            title = "One Month Temporary Subscription",
            type = "RECEIVED",
            amount = 500.0,
            frequency = "MONTHLY",
            startDate = today,
            endDate = today,
            isIndefinite = false,
            paymentMethod = "UPI"
        )

        // Process today
        val posted = repository.processDueRecurringTransactions()
        assertEquals(1, posted)

        // After posting, since nextExecutionDate will be next month (which is > endDate today), isActive should become false
        val recurringList = repository.getRecurringTransactions().first()
        val item = recurringList.first()
        assertFalse("Item should be deactivated after passing end date", item.isActive)
    }
}
