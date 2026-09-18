package com.example.hisabsaathi.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hisabsaathi.data.db.CustomerTransactionEntity
import com.example.hisabsaathi.receipt.FinancialUtils
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

enum class ChartTimeframe(val days: Int, val label: String) {
    WEEK(7, "7 Days"),
    FORTNIGHT(14, "14 Days"),
    MONTH(30, "30 Days")
}

enum class ChartDisplayMode(val label: String) {
    BAR_COMPARISON("Daily Bar"),
    AREA_TREND("Trend Curve"),
    PAYMENT_MODES("Payment Modes")
}

data class DailyTrendPoint(
    val dateKey: String,       // yyyy-MM-dd
    val displayLabel: String,  // e.g. "12 Sep"
    val dayOfWeek: String,     // e.g. "Fri"
    val receivableAmount: Double, // GIVEN
    val payableAmount: Double,    // RECEIVED
    val transactionCount: Int
)

data class PaymentModeStat(
    val method: String,
    val count: Int,
    val totalAmount: Double,
    val percentage: Float,
    val color: Color
)

@Composable
fun TransactionTrendVisualizer(
    transactions: List<CustomerTransactionEntity>,
    modifier: Modifier = Modifier
) {
    var selectedTimeframe by remember { mutableStateOf(ChartTimeframe.MONTH) }
    var selectedMode by remember { mutableStateOf(ChartDisplayMode.BAR_COMPARISON) }
    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }

    // Aggregate daily data points for the selected timeframe (default: past month / 30 days)
    val dailyPoints = remember(transactions, selectedTimeframe) {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val displayDateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
        val dayOfWeekFormat = SimpleDateFormat("EEE", Locale.getDefault())

        val daysCount = selectedTimeframe.days
        val list = mutableListOf<DailyTrendPoint>()

        for (i in (daysCount - 1) downTo 0) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            val dateStr = dateFormat.format(cal.time)
            val displayLabel = displayDateFormat.format(cal.time)
            val dayOfWeek = dayOfWeekFormat.format(cal.time)

            val dayTxs = transactions.filter { it.date == dateStr }
            val receivable = dayTxs.filter { it.type == "GIVEN" }.sumOf { it.amount }
            val payable = dayTxs.filter { it.type == "RECEIVED" }.sumOf { it.amount }

            list.add(
                DailyTrendPoint(
                    dateKey = dateStr,
                    displayLabel = displayLabel,
                    dayOfWeek = dayOfWeek,
                    receivableAmount = receivable,
                    payableAmount = payable,
                    transactionCount = dayTxs.size
                )
            )
        }
        list
    }

    // Payment mode breakdown (Past month patterns)
    val paymentModeStats = remember(transactions, selectedTimeframe) {
        val cutoffCalendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -selectedTimeframe.days)
        }
        val cutoffDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cutoffCalendar.time)
        val filteredTxs = transactions.filter { it.date >= cutoffDateStr }

        val totalVolume = filteredTxs.sumOf { it.amount }
        val methodGroups = filteredTxs.groupBy { it.paymentMethod.uppercase() }

        val palette = listOf(
            EmeraldGreen,
            PrimaryNavyLight,
            SaffronGold,
            CrimsonRed,
            Color(0xFF8B5CF6),
            Color(0xFF06B6D4)
        )

        var colorIdx = 0
        methodGroups.map { (method, txs) ->
            val amount = txs.sumOf { it.amount }
            val pct = if (totalVolume > 0) (amount / totalVolume).toFloat() else 0f
            val color = palette[colorIdx % palette.size]
            colorIdx++
            PaymentModeStat(
                method = if (method.isBlank()) "OTHER" else method,
                count = txs.size,
                totalAmount = amount,
                percentage = pct,
                color = color
            )
        }.sortedByDescending { it.totalAmount }
    }

    // Summary calculations for selected timeframe
    val totalReceivableMonth = remember(dailyPoints) { dailyPoints.sumOf { it.receivableAmount } }
    val totalPayableMonth = remember(dailyPoints) { dailyPoints.sumOf { it.payableAmount } }
    val totalTransactionsCount = remember(dailyPoints) { dailyPoints.sumOf { it.transactionCount } }
    val netBalanceDiff = totalReceivableMonth - totalPayableMonth

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("transaction_trend_visualizer")
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Title & Timeframe Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(PrimaryNavy.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = null,
                                tint = PrimaryNavy,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Text(
                            text = "Transaction Trends & Patterns",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate800
                        )
                    }
                    Text(
                        text = "Receivable vs Payable Analytics (${selectedTimeframe.label})",
                        fontSize = 11.sp,
                        color = Slate600
                    )
                }

                // Timeframe Selector Pills (7D / 14D / 30D)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Slate100,
                    modifier = Modifier.height(30.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        ChartTimeframe.values().forEach { tf ->
                            val isSelected = selectedTimeframe == tf
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) PrimaryNavy else Color.Transparent)
                                    .clickable {
                                        selectedTimeframe = tf
                                        selectedPointIndex = null
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tf.label,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else Slate600
                                )
                            }
                        }
                    }
                }
            }

            // High-Level Monthly Totals Summary Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Slate50)
                    .border(1.dp, Slate200, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Receivable (Given)
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(CrimsonRed))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Receivable (Given)", fontSize = 11.sp, color = Slate600, fontWeight = FontWeight.Medium)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = FinancialUtils.formatInr(totalReceivableMonth),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CrimsonRed
                    )
                }

                // Payable / Collected (Received)
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(EmeraldGreen))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Payable (Received)", fontSize = 11.sp, color = Slate600, fontWeight = FontWeight.Medium)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = FinancialUtils.formatInr(totalPayableMonth),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldGreen
                    )
                }

                // Net Trend
                Column(horizontalAlignment = Alignment.End) {
                    Text("Net Change", fontSize = 11.sp, color = Slate600, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = (if (netBalanceDiff >= 0) "+ " else "- ") + FinancialUtils.formatInr(kotlin.math.abs(netBalanceDiff)),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (netBalanceDiff >= 0) SaffronDark else EmeraldGreen
                    )
                }
            }

            // Mode Selector: Bar Comparison | Area Trend Curve | Payment Modes Breakdown
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Slate100)
                    .padding(2.dp)
            ) {
                ChartDisplayMode.values().forEach { mode ->
                    val isSelected = selectedMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) Color.White else Color.Transparent)
                            .clickable {
                                selectedMode = mode
                                selectedPointIndex = null
                            }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode.label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) PrimaryNavy else Slate600
                        )
                    }
                }
            }

            // Chart Render Section
            when (selectedMode) {
                ChartDisplayMode.BAR_COMPARISON -> {
                    DailyBarComparisonChart(
                        points = dailyPoints,
                        selectedIndex = selectedPointIndex,
                        onPointSelected = { selectedPointIndex = it }
                    )
                }
                ChartDisplayMode.AREA_TREND -> {
                    SmoothAreaTrendChart(
                        points = dailyPoints,
                        selectedIndex = selectedPointIndex,
                        onPointSelected = { selectedPointIndex = it }
                    )
                }
                ChartDisplayMode.PAYMENT_MODES -> {
                    PaymentPatternsBreakdown(
                        stats = paymentModeStats,
                        totalTransactions = totalTransactionsCount
                    )
                }
            }

            // Active Data Point Tooltip / Scrubber Inspector
            if (selectedPointIndex != null && selectedPointIndex in dailyPoints.indices && selectedMode != ChartDisplayMode.PAYMENT_MODES) {
                val point = dailyPoints[selectedPointIndex!!]
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Navy900,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${point.dayOfWeek}, ${point.displayLabel} (${point.dateKey})",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${point.transactionCount} transaction${if (point.transactionCount == 1) "" else "s"}",
                                color = Slate400,
                                fontSize = 10.sp
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Receivable (Given)", color = CrimsonLight, fontSize = 10.sp)
                                Text(
                                    text = FinancialUtils.formatInr(point.receivableAmount),
                                    color = Color(0xFFF87171),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Payable (Received)", color = EmeraldLight, fontSize = 10.sp)
                                Text(
                                    text = FinancialUtils.formatInr(point.payableAmount),
                                    color = Color(0xFF34D399),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Interactive D3/Recharts-style Dual Bar Chart (Receivable vs Payable per day)
 */
@Composable
fun DailyBarComparisonChart(
    points: List<DailyTrendPoint>,
    selectedIndex: Int?,
    onPointSelected: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val maxVal = remember(points) {
        val highest = points.maxOfOrNull { max(it.receivableAmount, it.payableAmount) } ?: 0.0
        if (highest <= 0.0) 1000.0 else highest * 1.15 // headroom
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .pointerInput(points) {
                    detectTapGestures { offset ->
                        val n = points.size
                        if (n > 0) {
                            val colWidth = size.width / n
                            val index = (offset.x / colWidth).toInt().coerceIn(0, n - 1)
                            onPointSelected(index)
                        }
                    }
                }
                .pointerInput(points) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val n = points.size
                            if (n > 0) {
                                val colWidth = size.width / n
                                val index = (offset.x / colWidth).toInt().coerceIn(0, n - 1)
                                onPointSelected(index)
                            }
                        },
                        onDrag = { change, _ ->
                            val n = points.size
                            if (n > 0) {
                                val colWidth = size.width / n
                                val index = (change.position.x / colWidth).toInt().coerceIn(0, n - 1)
                                onPointSelected(index)
                            }
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val chartWidth = size.width
                val chartHeight = size.height - 20.dp.toPx()
                val n = points.size
                if (n == 0) return@Canvas

                val slotWidth = chartWidth / n
                val barPairWidth = (slotWidth * 0.72f).coerceAtMost(28.dp.toPx())
                val singleBarWidth = barPairWidth / 2.2f

                // Draw Horizontal Gridlines and subtle guides
                val gridSteps = 3
                for (g in 0..gridSteps) {
                    val y = chartHeight - (chartHeight * (g.toFloat() / gridSteps))
                    drawLine(
                        color = Color(0xFFE2E8F0),
                        start = Offset(0f, y),
                        end = Offset(chartWidth, y),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                    )
                }

                // Baseline
                drawLine(
                    color = Color(0xFFCBD5E1),
                    start = Offset(0f, chartHeight),
                    end = Offset(chartWidth, chartHeight),
                    strokeWidth = 2f
                )

                // Draw bars for each day
                points.forEachIndexed { i, pt ->
                    val centerX = (i + 0.5f) * slotWidth
                    val isHighlighted = selectedIndex == i

                    // Highlight background pillar when touched
                    if (isHighlighted) {
                        drawRoundRect(
                            color = PrimaryNavy.copy(alpha = 0.08f),
                            topLeft = Offset(i * slotWidth, 0f),
                            size = Size(slotWidth, chartHeight),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }

                    // 1. Receivable Bar (Crimson Red)
                    val recRatio = (pt.receivableAmount / maxVal).toFloat().coerceIn(0f, 1f)
                    val recBarHeight = (chartHeight * recRatio).coerceAtLeast(if (pt.receivableAmount > 0) 3.dp.toPx() else 0f)
                    val recLeft = centerX - barPairWidth / 2f

                    if (recBarHeight > 0) {
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFFEF4444), Color(0xFFDC2626)),
                                startY = chartHeight - recBarHeight,
                                endY = chartHeight
                            ),
                            topLeft = Offset(recLeft, chartHeight - recBarHeight),
                            size = Size(singleBarWidth, recBarHeight),
                            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                        )
                    }

                    // 2. Payable/Received Bar (Emerald Green)
                    val payRatio = (pt.payableAmount / maxVal).toFloat().coerceIn(0f, 1f)
                    val payBarHeight = (chartHeight * payRatio).coerceAtLeast(if (pt.payableAmount > 0) 3.dp.toPx() else 0f)
                    val payLeft = recLeft + singleBarWidth + 2.dp.toPx()

                    if (payBarHeight > 0) {
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFF10B981), Color(0xFF059669)),
                                startY = chartHeight - payBarHeight,
                                endY = chartHeight
                            ),
                            topLeft = Offset(payLeft, chartHeight - payBarHeight),
                            size = Size(singleBarWidth, payBarHeight),
                            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                        )
                    }
                }
            }
        }

        // X-Axis Date Intervals (Show 4-5 evenly spaced date labels)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val labelIndices = remember(points) {
                if (points.isEmpty()) emptyList()
                else if (points.size <= 7) points.indices.toList()
                else listOf(
                    0,
                    points.size / 4,
                    points.size / 2,
                    (3 * points.size) / 4,
                    points.size - 1
                )
            }

            labelIndices.forEach { idx ->
                val pt = points.getOrNull(idx)
                if (pt != null) {
                    Text(
                        text = pt.displayLabel,
                        fontSize = 9.sp,
                        color = if (selectedIndex == idx) PrimaryNavy else Slate500,
                        fontWeight = if (selectedIndex == idx) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

/**
 * D3-style Smooth Cubic Bezier Area Trend Chart
 */
@Composable
fun SmoothAreaTrendChart(
    points: List<DailyTrendPoint>,
    selectedIndex: Int?,
    onPointSelected: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val maxVal = remember(points) {
        val highest = points.maxOfOrNull { max(it.receivableAmount, it.payableAmount) } ?: 0.0
        if (highest <= 0.0) 1000.0 else highest * 1.2
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .pointerInput(points) {
                    detectTapGestures { offset ->
                        val n = points.size
                        if (n > 0) {
                            val colWidth = size.width / n
                            val index = (offset.x / colWidth).toInt().coerceIn(0, n - 1)
                            onPointSelected(index)
                        }
                    }
                }
                .pointerInput(points) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val n = points.size
                            if (n > 0) {
                                val colWidth = size.width / n
                                val index = (offset.x / colWidth).toInt().coerceIn(0, n - 1)
                                onPointSelected(index)
                            }
                        },
                        onDrag = { change, _ ->
                            val n = points.size
                            if (n > 0) {
                                val colWidth = size.width / n
                                val index = (change.position.x / colWidth).toInt().coerceIn(0, n - 1)
                                onPointSelected(index)
                            }
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val chartWidth = size.width
                val chartHeight = size.height - 20.dp.toPx()
                val n = points.size
                if (n < 2) return@Canvas

                val stepX = chartWidth / (n - 1)

                // Grid lines
                for (g in 0..3) {
                    val y = chartHeight - (chartHeight * (g.toFloat() / 3f))
                    drawLine(
                        color = Color(0xFFE2E8F0),
                        start = Offset(0f, y),
                        end = Offset(chartWidth, y),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                    )
                }

                // 1. Draw Receivable Area & Curve (Crimson)
                val recPath = Path()
                val recFillPath = Path()
                val recCoords = mutableListOf<Offset>()

                points.forEachIndexed { i, pt ->
                    val x = i * stepX
                    val y = chartHeight - (chartHeight * (pt.receivableAmount / maxVal).toFloat()).coerceIn(0f, chartHeight)
                    recCoords.add(Offset(x, y))
                }

                drawCurvedSeries(
                    scope = this,
                    coords = recCoords,
                    chartHeight = chartHeight,
                    strokeColor = CrimsonRed,
                    fillGradient = listOf(CrimsonRed.copy(alpha = 0.22f), CrimsonRed.copy(alpha = 0.01f))
                )

                // 2. Draw Payable/Received Area & Curve (Emerald)
                val payCoords = mutableListOf<Offset>()
                points.forEachIndexed { i, pt ->
                    val x = i * stepX
                    val y = chartHeight - (chartHeight * (pt.payableAmount / maxVal).toFloat()).coerceIn(0f, chartHeight)
                    payCoords.add(Offset(x, y))
                }

                drawCurvedSeries(
                    scope = this,
                    coords = payCoords,
                    chartHeight = chartHeight,
                    strokeColor = EmeraldGreen,
                    fillGradient = listOf(EmeraldGreen.copy(alpha = 0.26f), EmeraldGreen.copy(alpha = 0.01f))
                )

                // 3. Draw vertical scrubber indicator if selected
                if (selectedIndex != null && selectedIndex in points.indices) {
                    val selX = selectedIndex * stepX
                    drawLine(
                        color = PrimaryNavy,
                        start = Offset(selX, 0f),
                        end = Offset(selX, chartHeight),
                        strokeWidth = 1.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                    )

                    // Circles on points
                    val recPt = recCoords[selectedIndex]
                    drawCircle(color = Color.White, radius = 5.dp.toPx(), center = recPt)
                    drawCircle(color = CrimsonRed, radius = 3.5.dp.toPx(), center = recPt)

                    val payPt = payCoords[selectedIndex]
                    drawCircle(color = Color.White, radius = 5.dp.toPx(), center = payPt)
                    drawCircle(color = EmeraldGreen, radius = 3.5.dp.toPx(), center = payPt)
                }
            }
        }

        // X-Axis Date Intervals
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val labelIndices = remember(points) {
                if (points.isEmpty()) emptyList()
                else if (points.size <= 7) points.indices.toList()
                else listOf(
                    0,
                    points.size / 4,
                    points.size / 2,
                    (3 * points.size) / 4,
                    points.size - 1
                )
            }

            labelIndices.forEach { idx ->
                val pt = points.getOrNull(idx)
                if (pt != null) {
                    Text(
                        text = pt.displayLabel,
                        fontSize = 9.sp,
                        color = if (selectedIndex == idx) PrimaryNavy else Slate500,
                        fontWeight = if (selectedIndex == idx) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

private fun drawCurvedSeries(
    scope: DrawScope,
    coords: List<Offset>,
    chartHeight: Float,
    strokeColor: Color,
    fillGradient: List<Color>
) {
    if (coords.isEmpty()) return

    val strokePath = Path()
    val fillPath = Path()

    strokePath.moveTo(coords[0].x, coords[0].y)
    fillPath.moveTo(coords[0].x, chartHeight)
    fillPath.lineTo(coords[0].x, coords[0].y)

    for (i in 0 until coords.size - 1) {
        val p0 = coords[i]
        val p1 = coords[i + 1]

        val controlX1 = p0.x + (p1.x - p0.x) / 2f
        val controlY1 = p0.y
        val controlX2 = p0.x + (p1.x - p0.x) / 2f
        val controlY2 = p1.y

        strokePath.cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
        fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
    }

    fillPath.lineTo(coords.last().x, chartHeight)
    fillPath.close()

    // Draw Fill Area
    scope.drawPath(
        path = fillPath,
        brush = Brush.verticalGradient(
            colors = fillGradient,
            startY = 0f,
            endY = chartHeight
        )
    )

    // Draw Stroke Line
    scope.drawPath(
        path = strokePath,
        color = strokeColor,
        style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

/**
 * Payment Patterns Breakdown (Donut Distribution + Method List)
 */
@Composable
fun PaymentPatternsBreakdown(
    stats: List<PaymentModeStat>,
    totalTransactions: Int,
    modifier: Modifier = Modifier
) {
    if (stats.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No payment pattern data recorded in this timeframe.", color = Slate500, fontSize = 12.sp)
        }
        return
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Payment Methods Distribution",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Slate800
            )
            Text(
                text = "$totalTransactions total operations",
                fontSize = 11.sp,
                color = Slate600
            )
        }

        // Horizontal Segmented Bar (Recharts Donut / Distribution Bar equivalent)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(Slate100)
        ) {
            stats.forEach { stat ->
                if (stat.percentage > 0.001f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(stat.percentage)
                            .background(stat.color)
                    )
                }
            }
        }

        // Breakdown items
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            stats.forEach { stat ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Slate50)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(stat.color)
                        )
                        Text(
                            text = stat.method,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate800
                        )
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = stat.color.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = "${(stat.percentage * 100).toInt()}%",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = stat.color,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = FinancialUtils.formatInr(stat.totalAmount),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate800
                        )
                        Text(
                            text = "${stat.count} txns",
                            fontSize = 10.sp,
                            color = Slate500
                        )
                    }
                }
            }
        }
    }
}
