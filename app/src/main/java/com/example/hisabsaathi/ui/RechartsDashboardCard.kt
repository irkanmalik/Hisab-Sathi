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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hisabsaathi.receipt.FinancialUtils
import com.example.ui.theme.*
import kotlin.math.abs
import kotlin.math.max

/**
 * Recharts-Inspired Visualization Card for Jetpack Compose.
 * Displays aggregated monthly Receivable vs Payable data from DashboardStatsViewModel
 * with interactive Line & Bar chart modes, Recharts-styled tooltips, cartesian grids,
 * and dynamic legend toggling.
 */
@Composable
fun RechartsDashboardCard(
    statsViewModel: DashboardStatsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by statsViewModel.uiState.collectAsState()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("recharts_dashboard_card")
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
            // Recharts Header & Type Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(PrimaryNavy.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (uiState.chartType == RechartsChartType.BAR) Icons.Default.BarChart else Icons.Default.ShowChart,
                                contentDescription = null,
                                tint = PrimaryNavy,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Text(
                            text = "Monthly Receivable vs Payable",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate800
                        )
                    }
                    Text(
                        text = "Aggregated monthly trends (${uiState.timeScale.label})",
                        fontSize = 11.sp,
                        color = Slate600
                    )
                }

                // Line vs Bar Toggle Pills
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Slate100,
                    modifier = Modifier.height(30.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        RechartsChartType.values().forEach { type ->
                            val isSelected = uiState.chartType == type
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) PrimaryNavy else Color.Transparent)
                                    .clickable { statsViewModel.setChartType(type) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = if (type == RechartsChartType.BAR) Icons.Default.BarChart else Icons.Default.ShowChart,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else Slate600,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = if (type == RechartsChartType.BAR) "Bar" else "Line",
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Slate600
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Time Scale Selector (6M / 12M / 30D)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Slate100)
                    .padding(2.dp)
            ) {
                RechartsTimeScale.values().forEach { scale ->
                    val isSelected = uiState.timeScale == scale
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) Color.White else Color.Transparent)
                            .clickable { statsViewModel.setTimeScale(scale) }
                            .padding(vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = scale.label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) PrimaryNavy else Slate600
                        )
                    }
                }
            }

            // Summary Metric Bar
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
                // Receivable
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(CrimsonRed))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Receivable (Given)", fontSize = 11.sp, color = Slate600, fontWeight = FontWeight.Medium)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = FinancialUtils.formatInr(uiState.summary.totalReceivable),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CrimsonRed
                    )
                }

                // Payable / Collected
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(EmeraldGreen))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Payable (Received)", fontSize = 11.sp, color = Slate600, fontWeight = FontWeight.Medium)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = FinancialUtils.formatInr(uiState.summary.totalPayable),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldGreen
                    )
                }

                // Net Shift
                Column(horizontalAlignment = Alignment.End) {
                    Text("Net Difference", fontSize = 11.sp, color = Slate600, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(2.dp))
                    val net = uiState.summary.netBalance
                    Text(
                        text = (if (net >= 0) "+ " else "- ") + FinancialUtils.formatInr(abs(net)),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (net >= 0) SaffronDark else EmeraldGreen
                    )
                }
            }

            // Recharts-Style Interactive Legend (with toggle series on click)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Receivable Legend Item
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { statsViewModel.toggleReceivableVisibility() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (uiState.isReceivableVisible) CrimsonRed else Slate400)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Receivable (Given)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (uiState.isReceivableVisible) Slate800 else Slate400,
                        textDecoration = if (uiState.isReceivableVisible) TextDecoration.None else TextDecoration.LineThrough
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Payable Legend Item
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { statsViewModel.togglePayableVisibility() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (uiState.isPayableVisible) EmeraldGreen else Slate400)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Payable (Received)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (uiState.isPayableVisible) Slate800 else Slate400,
                        textDecoration = if (uiState.isPayableVisible) TextDecoration.None else TextDecoration.LineThrough
                    )
                }
            }

            // Chart Render Area
            if (uiState.chartType == RechartsChartType.BAR) {
                RechartsBarCanvas(
                    points = uiState.points,
                    isReceivableVisible = uiState.isReceivableVisible,
                    isPayableVisible = uiState.isPayableVisible,
                    selectedIndex = uiState.selectedIndex,
                    onSelectIndex = { statsViewModel.selectPoint(it) }
                )
            } else {
                RechartsLineCanvas(
                    points = uiState.points,
                    isReceivableVisible = uiState.isReceivableVisible,
                    isPayableVisible = uiState.isPayableVisible,
                    selectedIndex = uiState.selectedIndex,
                    onSelectIndex = { statsViewModel.selectPoint(it) }
                )
            }

            // Recharts-Style Floating / Docked Tooltip Card
            if (uiState.selectedIndex != null && uiState.selectedIndex in uiState.points.indices) {
                val point = uiState.points[uiState.selectedIndex!!]
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White,
                    shadowElevation = 4.dp,
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = point.label,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryNavy
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Slate100
                            ) {
                                Text(
                                    text = "${point.transactionCount} transactions",
                                    fontSize = 10.sp,
                                    color = Slate600,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Divider(color = Slate100, thickness = 1.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (uiState.isReceivableVisible) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(CrimsonRed))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Receivable: ", fontSize = 11.sp, color = Slate600)
                                    Text(
                                        text = FinancialUtils.formatInr(point.receivable),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CrimsonRed
                                    )
                                }
                            }

                            if (uiState.isPayableVisible) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(EmeraldGreen))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Payable: ", fontSize = 11.sp, color = Slate600)
                                    Text(
                                        text = FinancialUtils.formatInr(point.payable),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldGreen
                                    )
                                }
                            }
                        }

                        // Net shift for this month
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            val diff = point.netDifference
                            Text(
                                text = "Period Net: " + (if (diff >= 0) "+ " else "- ") + FinancialUtils.formatInr(abs(diff)),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (diff >= 0) SaffronDark else EmeraldGreen
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Recharts Grouped Bar Chart Canvas with Cartesian Grid and hover pillars
 */
@Composable
fun RechartsBarCanvas(
    points: List<MonthlyDataPoint>,
    isReceivableVisible: Boolean,
    isPayableVisible: Boolean,
    selectedIndex: Int?,
    onSelectIndex: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val maxVal = remember(points, isReceivableVisible, isPayableVisible) {
        val highest = points.maxOfOrNull { pt ->
            var h = 0.0
            if (isReceivableVisible) h = max(h, pt.receivable)
            if (isPayableVisible) h = max(h, pt.payable)
            h
        } ?: 0.0
        if (highest <= 0.0) 1000.0 else highest * 1.18
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .pointerInput(points) {
                    detectTapGestures { offset ->
                        val n = points.size
                        if (n > 0) {
                            val slotWidth = size.width / n
                            val index = (offset.x / slotWidth).toInt().coerceIn(0, n - 1)
                            onSelectIndex(index)
                        }
                    }
                }
                .pointerInput(points) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val n = points.size
                            if (n > 0) {
                                val slotWidth = size.width / n
                                val index = (offset.x / slotWidth).toInt().coerceIn(0, n - 1)
                                onSelectIndex(index)
                            }
                        },
                        onDrag = { change, _ ->
                            val n = points.size
                            if (n > 0) {
                                val slotWidth = size.width / n
                                val index = (change.position.x / slotWidth).toInt().coerceIn(0, n - 1)
                                onSelectIndex(index)
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
                val barPairWidth = (slotWidth * 0.72f).coerceAtMost(32.dp.toPx())
                val singleBarWidth = if (isReceivableVisible && isPayableVisible) barPairWidth / 2.2f else barPairWidth * 0.7f

                // Recharts CartesianGrid: Horizontal dashed lines
                val gridSteps = 3
                for (g in 0..gridSteps) {
                    val y = chartHeight - (chartHeight * (g.toFloat() / gridSteps))
                    drawLine(
                        color = Color(0xFFE2E8F0),
                        start = Offset(0f, y),
                        end = Offset(chartWidth, y),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                    )
                }

                // Baseline
                drawLine(
                    color = Color(0xFFCBD5E1),
                    start = Offset(0f, chartHeight),
                    end = Offset(chartWidth, chartHeight),
                    strokeWidth = 1.5f
                )

                // Draw Bars for each data point
                points.forEachIndexed { i, pt ->
                    val centerX = (i + 0.5f) * slotWidth
                    val isHighlighted = selectedIndex == i

                    // Subtle hover background pillar
                    if (isHighlighted) {
                        drawRoundRect(
                            color = PrimaryNavy.copy(alpha = 0.07f),
                            topLeft = Offset(i * slotWidth, 0f),
                            size = Size(slotWidth, chartHeight),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }

                    if (isReceivableVisible && isPayableVisible) {
                        // Dual bars side-by-side
                        val recLeft = centerX - barPairWidth / 2f
                        val payLeft = recLeft + singleBarWidth + 2.dp.toPx()

                        // 1. Receivable Bar (Crimson)
                        val recRatio = (pt.receivable / maxVal).toFloat().coerceIn(0f, 1f)
                        val recHeight = (chartHeight * recRatio).coerceAtLeast(if (pt.receivable > 0) 3.dp.toPx() else 0f)
                        if (recHeight > 0) {
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color(0xFFEF4444), Color(0xFFDC2626)),
                                    startY = chartHeight - recHeight,
                                    endY = chartHeight
                                ),
                                topLeft = Offset(recLeft, chartHeight - recHeight),
                                size = Size(singleBarWidth, recHeight),
                                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                            )
                        }

                        // 2. Payable Bar (Emerald)
                        val payRatio = (pt.payable / maxVal).toFloat().coerceIn(0f, 1f)
                        val payHeight = (chartHeight * payRatio).coerceAtLeast(if (pt.payable > 0) 3.dp.toPx() else 0f)
                        if (payHeight > 0) {
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color(0xFF10B981), Color(0xFF059669)),
                                    startY = chartHeight - payHeight,
                                    endY = chartHeight
                                ),
                                topLeft = Offset(payLeft, chartHeight - payHeight),
                                size = Size(singleBarWidth, payHeight),
                                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                            )
                        }
                    } else if (isReceivableVisible) {
                        // Only Receivable Bar centered
                        val recLeft = centerX - singleBarWidth / 2f
                        val recRatio = (pt.receivable / maxVal).toFloat().coerceIn(0f, 1f)
                        val recHeight = (chartHeight * recRatio).coerceAtLeast(if (pt.receivable > 0) 3.dp.toPx() else 0f)
                        if (recHeight > 0) {
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color(0xFFEF4444), Color(0xFFDC2626)),
                                    startY = chartHeight - recHeight,
                                    endY = chartHeight
                                ),
                                topLeft = Offset(recLeft, chartHeight - recHeight),
                                size = Size(singleBarWidth, recHeight),
                                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                            )
                        }
                    } else if (isPayableVisible) {
                        // Only Payable Bar centered
                        val payLeft = centerX - singleBarWidth / 2f
                        val payRatio = (pt.payable / maxVal).toFloat().coerceIn(0f, 1f)
                        val payHeight = (chartHeight * payRatio).coerceAtLeast(if (pt.payable > 0) 3.dp.toPx() else 0f)
                        if (payHeight > 0) {
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color(0xFF10B981), Color(0xFF059669)),
                                    startY = chartHeight - payHeight,
                                    endY = chartHeight
                                ),
                                topLeft = Offset(payLeft, chartHeight - payHeight),
                                size = Size(singleBarWidth, payHeight),
                                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                            )
                        }
                    }
                }
            }
        }

        // X-Axis Labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val labelIndices = remember(points) {
                if (points.size <= 7) points.indices.toList()
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
                        text = pt.shortLabel,
                        fontSize = 10.sp,
                        color = if (selectedIndex == idx) PrimaryNavy else Slate500,
                        fontWeight = if (selectedIndex == idx) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

/**
 * Recharts Monotone Line Chart Canvas with Cartesian Grid and active dot markers
 */
@Composable
fun RechartsLineCanvas(
    points: List<MonthlyDataPoint>,
    isReceivableVisible: Boolean,
    isPayableVisible: Boolean,
    selectedIndex: Int?,
    onSelectIndex: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val maxVal = remember(points, isReceivableVisible, isPayableVisible) {
        val highest = points.maxOfOrNull { pt ->
            var h = 0.0
            if (isReceivableVisible) h = max(h, pt.receivable)
            if (isPayableVisible) h = max(h, pt.payable)
            h
        } ?: 0.0
        if (highest <= 0.0) 1000.0 else highest * 1.2
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .pointerInput(points) {
                    detectTapGestures { offset ->
                        val n = points.size
                        if (n > 0) {
                            val slotWidth = size.width / n
                            val index = (offset.x / slotWidth).toInt().coerceIn(0, n - 1)
                            onSelectIndex(index)
                        }
                    }
                }
                .pointerInput(points) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val n = points.size
                            if (n > 0) {
                                val slotWidth = size.width / n
                                val index = (offset.x / slotWidth).toInt().coerceIn(0, n - 1)
                                onSelectIndex(index)
                            }
                        },
                        onDrag = { change, _ ->
                            val n = points.size
                            if (n > 0) {
                                val slotWidth = size.width / n
                                val index = (change.position.x / slotWidth).toInt().coerceIn(0, n - 1)
                                onSelectIndex(index)
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

                // Cartesian Grid lines
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

                // 1. Receivable Series (Crimson Line)
                val recCoords = mutableListOf<Offset>()
                if (isReceivableVisible) {
                    points.forEachIndexed { i, pt ->
                        val x = i * stepX
                        val y = chartHeight - (chartHeight * (pt.receivable / maxVal).toFloat()).coerceIn(0f, chartHeight)
                        recCoords.add(Offset(x, y))
                    }
                    drawRechartsMonotoneCurve(
                        scope = this,
                        coords = recCoords,
                        strokeColor = CrimsonRed,
                        chartHeight = chartHeight,
                        fillColor = CrimsonRed.copy(alpha = 0.12f)
                    )
                }

                // 2. Payable Series (Emerald Line)
                val payCoords = mutableListOf<Offset>()
                if (isPayableVisible) {
                    points.forEachIndexed { i, pt ->
                        val x = i * stepX
                        val y = chartHeight - (chartHeight * (pt.payable / maxVal).toFloat()).coerceIn(0f, chartHeight)
                        payCoords.add(Offset(x, y))
                    }
                    drawRechartsMonotoneCurve(
                        scope = this,
                        coords = payCoords,
                        strokeColor = EmeraldGreen,
                        chartHeight = chartHeight,
                        fillColor = EmeraldGreen.copy(alpha = 0.14f)
                    )
                }

                // 3. Recharts Active Dot & Scrubber Line
                if (selectedIndex != null && selectedIndex in points.indices) {
                    val selX = selectedIndex * stepX
                    drawLine(
                        color = PrimaryNavy,
                        start = Offset(selX, 0f),
                        end = Offset(selX, chartHeight),
                        strokeWidth = 1.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                    )

                    if (isReceivableVisible && selectedIndex < recCoords.size) {
                        val pt = recCoords[selectedIndex]
                        drawCircle(color = Color.White, radius = 5.5.dp.toPx(), center = pt)
                        drawCircle(color = CrimsonRed, radius = 4.dp.toPx(), center = pt)
                    }

                    if (isPayableVisible && selectedIndex < payCoords.size) {
                        val pt = payCoords[selectedIndex]
                        drawCircle(color = Color.White, radius = 5.5.dp.toPx(), center = pt)
                        drawCircle(color = EmeraldGreen, radius = 4.dp.toPx(), center = pt)
                    }
                }
            }
        }

        // X-Axis Labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val labelIndices = remember(points) {
                if (points.size <= 7) points.indices.toList()
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
                        text = pt.shortLabel,
                        fontSize = 10.sp,
                        color = if (selectedIndex == idx) PrimaryNavy else Slate500,
                        fontWeight = if (selectedIndex == idx) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

private fun drawRechartsMonotoneCurve(
    scope: DrawScope,
    coords: List<Offset>,
    strokeColor: Color,
    chartHeight: Float,
    fillColor: Color
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
    scope.drawPath(path = fillPath, color = fillColor)

    // Draw Stroke Line
    scope.drawPath(
        path = strokePath,
        color = strokeColor,
        style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // Draw Recharts Data Dots
    with(scope) {
        val outerRadius = 3.5.dp.toPx()
        val innerRadius = 2.5.dp.toPx()
        coords.forEach { pt ->
            drawCircle(color = Color.White, radius = outerRadius, center = pt)
            drawCircle(color = strokeColor, radius = innerRadius, center = pt)
        }
    }
}
