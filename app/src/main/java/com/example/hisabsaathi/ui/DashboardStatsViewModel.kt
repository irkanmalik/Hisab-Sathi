package com.example.hisabsaathi.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.hisabsaathi.data.db.CustomerTransactionEntity
import com.example.hisabsaathi.data.db.HisabDatabase
import com.example.hisabsaathi.data.repository.HisabRepository
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

enum class RechartsChartType(val label: String) {
    BAR("Bar Chart"),
    LINE("Line Chart")
}

enum class RechartsTimeScale(val label: String, val months: Int) {
    MONTHS_6("Last 6 Months", 6),
    MONTHS_12("Last 12 Months", 12),
    DAYS_30("Past 30 Days", 1)
}

data class MonthlyDataPoint(
    val id: String,              // e.g. "2026-09" or "2026-09-17"
    val label: String,           // e.g. "Sep 2026" or "17 Sep"
    val shortLabel: String,      // e.g. "Sep" or "17"
    val receivable: Double,      // Amount given to customers (Receivable)
    val payable: Double,         // Amount received from customers (Payable/Collected)
    val netDifference: Double,   // receivable - payable
    val transactionCount: Int
)

data class DashboardStatsSummary(
    val totalReceivable: Double = 0.0,
    val totalPayable: Double = 0.0,
    val netBalance: Double = 0.0,
    val totalTransactions: Int = 0,
    val collectionRate: Float = 0f
)

data class DashboardStatsUiState(
    val isLoading: Boolean = false,
    val points: List<MonthlyDataPoint> = emptyList(),
    val summary: DashboardStatsSummary = DashboardStatsSummary(),
    val chartType: RechartsChartType = RechartsChartType.BAR,
    val timeScale: RechartsTimeScale = RechartsTimeScale.MONTHS_6,
    val isReceivableVisible: Boolean = true,
    val isPayableVisible: Boolean = true,
    val selectedIndex: Int? = null
)

/**
 * ViewModel responsible for aggregating monthly Receivable vs Payable transaction data
 * from the local Room database and feeding Recharts visual components.
 */
class DashboardStatsViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val repository: HisabRepository = HisabRepository(
        application,
        HisabDatabase.getDatabase(application)
    )

    private val _chartType = MutableStateFlow(RechartsChartType.BAR)
    val chartType: StateFlow<RechartsChartType> = _chartType.asStateFlow()

    private val _timeScale = MutableStateFlow(RechartsTimeScale.MONTHS_6)
    val timeScale: StateFlow<RechartsTimeScale> = _timeScale.asStateFlow()

    private val _isReceivableVisible = MutableStateFlow(true)
    val isReceivableVisible: StateFlow<Boolean> = _isReceivableVisible.asStateFlow()

    private val _isPayableVisible = MutableStateFlow(true)
    val isPayableVisible: StateFlow<Boolean> = _isPayableVisible.asStateFlow()

    private val _selectedIndex = MutableStateFlow<Int?>(null)
    val selectedIndex: StateFlow<Int?> = _selectedIndex.asStateFlow()

    // Transaction stream from repository
    private val transactionsFlow = repository.getAllTransactions()

    private data class ChartPreferences(
        val chartType: RechartsChartType,
        val timeScale: RechartsTimeScale,
        val recVisible: Boolean,
        val payVisible: Boolean,
        val selectedIdx: Int?
    )

    private val chartPrefsFlow = combine(
        _chartType,
        _timeScale,
        _isReceivableVisible,
        _isPayableVisible,
        _selectedIndex
    ) { chartType, timeScale, recVisible, payVisible, selectedIdx ->
        ChartPreferences(chartType, timeScale, recVisible, payVisible, selectedIdx)
    }

    // Combined UI state
    val uiState: StateFlow<DashboardStatsUiState> = combine(
        transactionsFlow,
        chartPrefsFlow
    ) { txs, prefs ->
        val points = aggregateDataPoints(txs, prefs.timeScale)
        val summary = calculateSummary(points)

        DashboardStatsUiState(
            isLoading = false,
            points = points,
            summary = summary,
            chartType = prefs.chartType,
            timeScale = prefs.timeScale,
            isReceivableVisible = prefs.recVisible,
            isPayableVisible = prefs.payVisible,
            selectedIndex = prefs.selectedIdx
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DashboardStatsUiState(isLoading = true)
    )

    fun setChartType(type: RechartsChartType) {
        _chartType.value = type
    }

    fun setTimeScale(scale: RechartsTimeScale) {
        _timeScale.value = scale
        _selectedIndex.value = null
    }

    fun toggleReceivableVisibility() {
        _isReceivableVisible.value = !_isReceivableVisible.value
    }

    fun togglePayableVisibility() {
        _isPayableVisible.value = !_isPayableVisible.value
    }

    fun selectPoint(index: Int?) {
        _selectedIndex.value = index
    }

    /**
     * Aggregates transactions into monthly or daily time buckets.
     */
    private fun aggregateDataPoints(
        transactions: List<CustomerTransactionEntity>,
        timeScale: RechartsTimeScale
    ): List<MonthlyDataPoint> {
        val monthKeyFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val monthDisplayFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        val monthShortFormat = SimpleDateFormat("MMM", Locale.getDefault())

        val dateKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayDisplayFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
        val dayShortFormat = SimpleDateFormat("dd", Locale.getDefault())

        val points = mutableListOf<MonthlyDataPoint>()

        if (timeScale == RechartsTimeScale.DAYS_30) {
            // Past 30 Days (Daily)
            for (i in 29 downTo 0) {
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, -i)
                val key = dateKeyFormat.format(cal.time)
                val display = dayDisplayFormat.format(cal.time)
                val short = dayShortFormat.format(cal.time)

                val dayTxs = transactions.filter { it.date == key }
                val receivable = dayTxs.filter { it.type == "GIVEN" }.sumOf { it.amount }
                val payable = dayTxs.filter { it.type == "RECEIVED" }.sumOf { it.amount }

                points.add(
                    MonthlyDataPoint(
                        id = key,
                        label = display,
                        shortLabel = short,
                        receivable = receivable,
                        payable = payable,
                        netDifference = receivable - payable,
                        transactionCount = dayTxs.size
                    )
                )
            }
        } else {
            // Monthly Aggregation (6 or 12 months)
            val monthsCount = timeScale.months
            for (i in (monthsCount - 1) downTo 0) {
                val cal = Calendar.getInstance()
                cal.add(Calendar.MONTH, -i)
                val key = monthKeyFormat.format(cal.time)
                val display = monthDisplayFormat.format(cal.time)
                val short = monthShortFormat.format(cal.time)

                // Filter transactions that started with YYYY-MM
                val monthTxs = transactions.filter { it.date.startsWith(key) }
                val receivable = monthTxs.filter { it.type == "GIVEN" }.sumOf { it.amount }
                val payable = monthTxs.filter { it.type == "RECEIVED" }.sumOf { it.amount }

                points.add(
                    MonthlyDataPoint(
                        id = key,
                        label = display,
                        shortLabel = short,
                        receivable = receivable,
                        payable = payable,
                        netDifference = receivable - payable,
                        transactionCount = monthTxs.size
                    )
                )
            }
        }

        return points
    }

    private fun calculateSummary(points: List<MonthlyDataPoint>): DashboardStatsSummary {
        val totalRec = points.sumOf { it.receivable }
        val totalPay = points.sumOf { it.payable }
        val totalCount = points.sumOf { it.transactionCount }
        val totalVolume = totalRec + totalPay
        val collectionRate = if (totalVolume > 0) (totalPay / totalVolume).toFloat() * 100f else 0f

        return DashboardStatsSummary(
            totalReceivable = totalRec,
            totalPayable = totalPay,
            netBalance = totalRec - totalPay,
            totalTransactions = totalCount,
            collectionRate = collectionRate
        )
    }
}
