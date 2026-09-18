package com.example.hisabsaathi.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.hisabsaathi.data.db.HisabDatabase
import com.example.hisabsaathi.data.repository.HisabRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.abs

data class DashboardSummaryUiState(
    val totalReceivable: Double = 0.0,
    val totalPayable: Double = 0.0,
    val netBalance: Double = 0.0,
    val totalCustomers: Int = 0,
    val isLoading: Boolean = false
)

/**
 * DashboardViewModel calculates total net balance, receivable, and payable figures
 * directly from the local Room database via HisabRepository, exposing reactive flows
 * for home screen display.
 */
class DashboardViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository: HisabRepository = HisabRepository(
        application,
        HisabDatabase.getDatabase(application)
    )

    private val _uiState = MutableStateFlow(DashboardSummaryUiState(isLoading = true))
    val uiState: StateFlow<DashboardSummaryUiState> = _uiState.asStateFlow()

    init {
        observeDashboardData()
    }

    private fun observeDashboardData() {
        viewModelScope.launch {
            repository.getCustomers().collect { customers ->
                var receivable = 0.0
                var payable = 0.0

                for (customer in customers) {
                    if (customer.currentBalance > 0.001) {
                        receivable += customer.currentBalance
                    } else if (customer.currentBalance < -0.001) {
                        payable += abs(customer.currentBalance)
                    }
                }

                val netBalance = receivable - payable

                _uiState.value = DashboardSummaryUiState(
                    totalReceivable = receivable,
                    totalPayable = payable,
                    netBalance = netBalance,
                    totalCustomers = customers.size,
                    isLoading = false
                )
            }
        }
    }
}
