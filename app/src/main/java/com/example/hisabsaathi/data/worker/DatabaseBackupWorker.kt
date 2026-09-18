package com.example.hisabsaathi.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.hisabsaathi.data.db.HisabDatabase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class DatabaseBackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val database = HisabDatabase.getDatabase(applicationContext)
            val businesses = database.businessDao().getAllBusinesses()
            val customers = database.customerDao().getAllCustomers()
            val transactions = database.transactionDao().getAllTransactions()

            val backupJson = JSONObject().apply {
                put("timestamp", System.currentTimeMillis())
                put("businessesCount", businesses.size)
                put("customersCount", customers.size)
                put("transactionsCount", transactions.size)

                val transactionsArray = JSONArray()
                for (tx in transactions) {
                    val obj = JSONObject().apply {
                        put("id", tx.id)
                        put("businessId", tx.businessId)
                        put("customerId", tx.customerId)
                        put("amount", tx.amount)
                        put("type", tx.type)
                        put("paymentMethod", tx.paymentMethod)
                        put("description", tx.description)
                        put("date", tx.date)
                        put("time", tx.time)
                        put("balanceAfter", tx.balanceAfter)
                        put("createdAt", tx.createdAt)
                    }
                    transactionsArray.put(obj)
                }
                put("transactions", transactionsArray)
            }

            val currentUser = FirebaseAuth.getInstance().currentUser
            val userId = currentUser?.uid ?: "anonymous_user"

            val firestore = FirebaseFirestore.getInstance()
            val backupRecord = hashMapOf(
                "timestamp" to System.currentTimeMillis(),
                "userId" to userId,
                "backupData" to backupJson.toString()
            )

            firestore.collection("database_backups")
                .document("backup_${System.currentTimeMillis()}")
                .set(backupRecord)
                .await()

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
