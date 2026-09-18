package com.example.hisabsaathi.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hisabsaathi.data.db.CustomerTransactionEntity
import com.example.hisabsaathi.i18n.Translations
import com.example.hisabsaathi.receipt.FinancialUtils
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: HisabViewModel,
    statsViewModel: DashboardStatsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    modifier: Modifier = Modifier
) {
    val language by viewModel.currentLanguage.collectAsState()
    val business by viewModel.currentBusiness.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val paymentLinks by viewModel.paymentLinks.collectAsState()
    val attendance by viewModel.attendanceForDate.collectAsState()

    // Financial calculations
    val totalReceivable = customers.filter { it.currentBalance > 0 }.sumOf { it.currentBalance }
    val totalPayable = customers.filter { it.currentBalance < 0 }.sumOf { kotlin.math.abs(it.currentBalance) }

    val todayDate = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val todayTransactions = transactions.filter { it.date == todayDate }
    val todaysReceived = todayTransactions.filter { it.type == "RECEIVED" }.sumOf { it.amount }
    val todaysGiven = todayTransactions.filter { it.type == "GIVEN" }.sumOf { it.amount }
    val pendingPaymentsCount = paymentLinks.count { it.status == "PENDING" }
    val todayAttendanceCount = attendance.count { it.status == "PRESENT" || it.status == "HALF_DAY" }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Slate50)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PrimaryNavy)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = business?.businessName ?: "HisabSaathi",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${business?.ownerName ?: "Owner"} • ${business?.city ?: "India"}",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { viewModel.showLanguageDialog.value = true },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                        ) {
                            Icon(Icons.Default.Language, contentDescription = "Language", tint = Color.White, modifier = Modifier.size(20.dp))
                        }

                        IconButton(
                            onClick = { viewModel.showWhatsAppSettingsDialog.value = true },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        // Visual Connection Status Indicator (Online vs Pending Sync based on Room DB sync status)
        item {
            val syncStatus by viewModel.roomSyncStatus.collectAsState()
            val syncing by viewModel.isSyncing.collectAsState()

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("connection_status_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (syncStatus == "ONLINE") Color(0xFFF0FDF4) else Color(0xFFFFFBEB)
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (syncStatus == "ONLINE") Color(0xFFBBF7D0) else Color(0xFFFEF08A)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(
                                    color = if (syncStatus == "ONLINE") Color(0xFF22C55E) else Color(0xFFEAB308),
                                    shape = CircleShape
                                )
                        )
                        Column {
                            Text(
                                text = if (syncStatus == "ONLINE") "Room DB: Online & Synced" else "Room DB: Pending Sync",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (syncStatus == "ONLINE") Color(0xFF166534) else Color(0xFF854D0E)
                            )
                            Text(
                                text = if (syncStatus == "ONLINE") "All local records up to date" else "Local transactions queued for cloud sync",
                                fontSize = 11.sp,
                                color = if (syncStatus == "ONLINE") Color(0xFF15803D) else Color(0xFFA16207)
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (syncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = PrimaryNavy
                            )
                        } else {
                            OutlinedButton(
                                onClick = { viewModel.triggerRoomSync() },
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (syncStatus == "ONLINE") Color(0xFF166534) else Color(0xFF854D0E)
                                )
                            ) {
                                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (syncStatus == "ONLINE") "Sync" else "Sync Now", fontSize = 11.sp)
                            }
                        }

                        IconButton(
                            onClick = { viewModel.toggleRoomSyncMode() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (syncStatus == "ONLINE") Icons.Default.CloudDone else Icons.Default.CloudOff,
                                contentDescription = "Toggle Sync Mode",
                                tint = if (syncStatus == "ONLINE") Color(0xFF166534) else Color(0xFF854D0E),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Top-Level Net Balance Card (Receivable - Payable)
        item {
            val netBalance = totalReceivable - totalPayable
            val isPositive = netBalance >= 0
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("net_balance_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isPositive) Color(0xFFEFF6FF) else Color(0xFFFFFBEB)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isPositive) Icons.Default.AccountBalanceWallet else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isPositive) Color(0xFF2563EB) else Color(0xFFD97706),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = Translations.get("net_balance", language),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isPositive) Color(0xFF1E40AF) else Color(0xFFB45309)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${if (netBalance >= 0) "+" else ""}${FinancialUtils.formatInr(netBalance)}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isPositive) Color(0xFF1D4ED8) else Color(0xFF92400E)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isPositive) "Net Receivable (You will get overall)" else "Net Payable (You owe overall)",
                            fontSize = 11.sp,
                            color = Slate600
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (isPositive) Color(0xFFDBEAFE) else Color(0xFFFEF3C7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                            contentDescription = null,
                            tint = if (isPositive) Color(0xFF2563EB) else Color(0xFFD97706),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // Primary Balance Cards: Total Receivable & Total Payable
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Receivable Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = EmeraldLight),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = Translations.get("you_will_receive", language),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGreen
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = FinancialUtils.formatInr(totalReceivable),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = EmeraldGreen
                        )
                        Text(
                            text = Translations.get("total_receivable", language),
                            fontSize = 11.sp,
                            color = Slate600
                        )
                    }
                }

                // Payable Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CrimsonLight),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = CrimsonRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = Translations.get("you_have_to_pay", language),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CrimsonRed
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = FinancialUtils.formatInr(totalPayable),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = CrimsonRed
                        )
                        Text(
                            text = Translations.get("total_payable", language),
                            fontSize = 11.sp,
                            color = Slate600
                        )
                    }
                }
            }
        }

        // Secondary Metrics Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricChip(
                        modifier = Modifier.weight(1f),
                        label = Translations.get("todays_received", language),
                        value = FinancialUtils.formatInr(todaysReceived),
                        icon = Icons.Default.CurrencyRupee,
                        iconTint = EmeraldGreen
                    )
                    MetricChip(
                        modifier = Modifier.weight(1f),
                        label = Translations.get("todays_given", language),
                        value = FinancialUtils.formatInr(todaysGiven),
                        icon = Icons.Default.CurrencyRupee,
                        iconTint = CrimsonRed
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricChip(
                        modifier = Modifier.weight(1f),
                        label = Translations.get("todays_transactions", language),
                        value = todayTransactions.size.toString(),
                        icon = Icons.Default.ReceiptLong,
                        iconTint = PrimaryNavy
                    )
                    MetricChip(
                        modifier = Modifier.weight(1f),
                        label = Translations.get("total_customers", language),
                        value = customers.size.toString(),
                        icon = Icons.Default.People,
                        iconTint = SaffronGold
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricChip(
                        modifier = Modifier.weight(1f),
                        label = Translations.get("todays_attendance", language),
                        value = "$todayAttendanceCount Present",
                        icon = Icons.Default.Badge,
                        iconTint = PrimaryNavyLight
                    )
                    MetricChip(
                        modifier = Modifier.weight(1f),
                        label = Translations.get("pending_payments", language),
                        value = pendingPaymentsCount.toString(),
                        icon = Icons.Default.Link,
                        iconTint = SaffronGold
                    )
                }
            }
        }

        // Recharts Monthly Receivable vs Payable Visualizer (Line or Bar Chart from DashboardStatsViewModel)
        item {
            RechartsDashboardCard(
                statsViewModel = statsViewModel,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Daily Transaction Trends & Payment Patterns Visualizer (Receivable vs Payable past month)
        item {
            TransactionTrendVisualizer(
                transactions = transactions,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Quick Actions Row
        item {
            Text(
                text = Translations.get("quick_actions", language),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Slate800,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                QuickActionButton(
                    icon = Icons.Default.PersonAdd,
                    label = Translations.get("add_customer", language),
                    color = PrimaryNavy,
                    onClick = { viewModel.showAddCustomerDialog.value = true }
                )
                QuickActionButton(
                    icon = Icons.Default.AddCard,
                    label = Translations.get("add_hisab", language),
                    color = EmeraldGreen,
                    onClick = { viewModel.showAddHisabSheet.value = true }
                )
                QuickActionButton(
                    icon = Icons.Default.Autorenew,
                    label = Translations.get("recurring_transactions", language),
                    color = EmeraldGreen,
                    onClick = { viewModel.showAddRecurringDialog.value = true }
                )
                QuickActionButton(
                    icon = Icons.Default.Book,
                    label = Translations.get("jhadi", language),
                    color = PrimaryNavyLight,
                    onClick = { viewModel.currentTab.value = MainTab.HISAB_LEDGER }
                )
                QuickActionButton(
                    icon = Icons.Default.QrCode2,
                    label = Translations.get("payment_link", language),
                    color = SaffronGold,
                    onClick = { viewModel.openCreatePaymentLink() }
                )
                QuickActionButton(
                    icon = Icons.Default.HowToReg,
                    label = Translations.get("attendance", language),
                    color = PrimaryNavy,
                    onClick = { viewModel.currentTab.value = MainTab.ATTENDANCE }
                )
            }
        }

        // Recent Transactions Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Translations.get("recent_transactions", language),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate800
                )
                TextButton(onClick = { viewModel.currentTab.value = MainTab.HISAB_LEDGER }) {
                    Text(Translations.get("statement", language), fontSize = 13.sp, color = PrimaryNavy)
                }
            }
        }

        if (transactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(Translations.get("no_transactions", language), color = Slate600, fontSize = 14.sp)
                    }
                }
            }
        } else {
            items(transactions.take(8)) { tx ->
                val customer = customers.find { it.id == tx.customerId }
                TransactionItemCard(
                    transaction = tx,
                    customerName = customer?.fullName ?: "Customer",
                    customerMobile = customer?.mobileNumber ?: "",
                    onWhatsAppClick = {
                        val bName: String = business?.businessName ?: "HisabSaathi"
                        val cName: String = customer?.fullName ?: "Customer"
                        val vpa: String = viewModel.businessUpiVpa.value
                        val msg = com.example.hisabsaathi.whatsapp.WhatsAppMessageService.buildTransactionShareMessage(
                            businessName = bName,
                            customerName = cName,
                            transaction = tx,
                            businessVpa = vpa
                        )
                        viewModel.openWhatsAppMessage(cName, customer?.mobileNumber ?: "", msg, tx.id)
                    },
                    onUpiClick = {
                        viewModel.openCreatePaymentLink(
                            customer = customer,
                            transaction = tx,
                            amount = tx.amount,
                            note = "Payment for ${tx.description.ifBlank { "Tx #${tx.id} (${tx.date})" }}"
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun MetricChip(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: ImageVector,
    iconTint: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
            }
            Column {
                Text(text = label, fontSize = 11.sp, color = Slate600)
                Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Slate800)
            }
        }
    }
}

@Composable
fun QuickActionButton(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, fontSize = 11.sp, color = Slate800, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun TransactionItemCard(
    transaction: CustomerTransactionEntity,
    customerName: String,
    customerMobile: String,
    onWhatsAppClick: () -> Unit,
    onUpiClick: (() -> Unit)? = null
) {
    val isReceived = transaction.type == "RECEIVED"
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isReceived) EmeraldLight else CrimsonLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isReceived) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = if (isReceived) EmeraldGreen else CrimsonRed,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(text = customerName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Slate800)
                    Text(
                        text = "${transaction.date} • ${transaction.paymentMethod}${if (transaction.description.isNotBlank()) " • ${transaction.description}" else ""}",
                        fontSize = 11.sp,
                        color = Slate600
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = (if (isReceived) "+ " else "- ") + FinancialUtils.formatInr(transaction.amount),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isReceived) EmeraldGreen else CrimsonRed
                    )
                    val (_, absAmt, _) = FinancialUtils.getBalanceStatus(transaction.balanceAfter)
                    Text(
                        text = "Bal: ${FinancialUtils.formatInr(absAmt)}",
                        fontSize = 11.sp,
                        color = Slate600
                    )
                }

                if (onUpiClick != null) {
                    IconButton(
                        onClick = onUpiClick,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(SaffronGold.copy(alpha = 0.15f))
                            .testTag("upi_link_button_${transaction.id}")
                    ) {
                        Icon(
                            Icons.Default.QrCode2,
                            contentDescription = "Generate UPI Payment Link",
                            tint = SaffronGold,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onWhatsAppClick,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(EmeraldLight)
                ) {
                    Icon(Icons.Default.Chat, contentDescription = "WhatsApp", tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
