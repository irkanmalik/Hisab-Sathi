package com.example.hisabsaathi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hisabsaathi.data.db.CustomerTransactionEntity
import com.example.hisabsaathi.data.db.RecurringTransactionEntity
import com.example.hisabsaathi.i18n.Translations
import com.example.hisabsaathi.receipt.FinancialUtils
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LedgerViewMode {
    TRANSACTIONS,
    RECURRING
}

@Composable
fun HisabLedgerScreen(
    viewModel: HisabViewModel,
    modifier: Modifier = Modifier
) {
    val language by viewModel.currentLanguage.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val recurringList by viewModel.recurringTransactions.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val business by viewModel.currentBusiness.collectAsState()
    val context = LocalContext.current

    var viewMode by remember { mutableStateOf(LedgerViewMode.TRANSACTIONS) }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, TODAY, RECEIVED, GIVEN, THIS_WEEK, THIS_MONTH
    var searchQuery by remember { mutableStateOf("") }

    val inventoryItems by viewModel.inventoryItems.collectAsState()
    val lowStockItems by viewModel.lowStockItems.collectAsState()
    var showInventoryDialog by remember { mutableStateOf(false) }
    var showAddInventoryItemDialog by remember { mutableStateOf(false) }

    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    val filteredTransactions = remember(transactions, customers, selectedFilter, searchQuery) {
        transactions.filter { tx ->
            val customer = customers.find { it.id == tx.customerId }
            val cName = customer?.fullName ?: ""
            val matchesSearch = searchQuery.isBlank() ||
                cName.contains(searchQuery, ignoreCase = true) ||
                tx.description.contains(searchQuery, ignoreCase = true) ||
                tx.paymentMethod.contains(searchQuery, ignoreCase = true) ||
                tx.amount.toString().contains(searchQuery) ||
                tx.date.contains(searchQuery)

            val matchesFilter = when (selectedFilter) {
                "TODAY" -> tx.date == todayStr
                "RECEIVED" -> tx.type == "RECEIVED"
                "GIVEN" -> tx.type == "GIVEN"
                "THIS_WEEK" -> true
                "THIS_MONTH" -> true
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    val totalReceived = filteredTransactions.filter { it.type == "RECEIVED" }.sumOf { it.amount }
    val totalGiven = filteredTransactions.filter { it.type == "GIVEN" }.sumOf { it.amount }
    val netBalance = totalGiven - totalReceived

    val activeRecurringCount = recurringList.count { it.isActive }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Slate50,
        floatingActionButton = {
            if (viewMode == LedgerViewMode.TRANSACTIONS) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.showAddHisabSheet.value = true },
                    containerColor = EmeraldGreen,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.AddCard, contentDescription = null) },
                    text = { Text(Translations.get("add_hisab", language), fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.padding(bottom = 70.dp).testTag("fab_add_hisab_ledger")
                )
            } else {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.showAddRecurringDialog.value = true },
                    containerColor = EmeraldGreen,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.Autorenew, contentDescription = null) },
                    text = { Text(Translations.get("add_recurring", language), fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.padding(bottom = 70.dp).testTag("fab_add_recurring_ledger")
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Low Stock Alert Banner
            if (lowStockItems.isNotEmpty()) {
                Surface(
                    color = CrimsonLight,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CrimsonRed.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showInventoryDialog = true }
                        .testTag("low_stock_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Low Stock Warning",
                            tint = CrimsonRed,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Low Stock Alert! (${lowStockItems.size} items below threshold)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = CrimsonRed
                            )
                            Text(
                                text = lowStockItems.joinToString(", ") { "${it.itemName} (${it.quantity} ${it.unit})" },
                                fontSize = 11.sp,
                                color = Slate800,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = CrimsonRed
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Inventory & Stock Management Button
            OutlinedButton(
                onClick = { showInventoryDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("open_inventory_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White,
                    contentColor = PrimaryNavy
                ),
                border = BorderStroke(1.dp, if (lowStockItems.isNotEmpty()) CrimsonRed else PrimaryNavy)
            ) {
                Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(18.dp), tint = if (lowStockItems.isNotEmpty()) CrimsonRed else PrimaryNavy)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (lowStockItems.isNotEmpty()) "Manage Inventory (${lowStockItems.size} Low Stock)" else "Manage Inventory & Stock",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (showInventoryDialog) {
                InventoryManagementDialog(
                    viewModel = viewModel,
                    onDismiss = { showInventoryDialog = false },
                    onOpenAdd = { showAddInventoryItemDialog = true }
                )
            }

            if (showAddInventoryItemDialog) {
                AddEditInventoryItemDialog(
                    viewModel = viewModel,
                    onDismiss = { showAddInventoryItemDialog = false }
                )
            }

            // Mode Selector: All Transactions vs Recurring Auto-Hisab
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Slate200, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    onClick = { viewMode = LedgerViewMode.TRANSACTIONS },
                    shape = RoundedCornerShape(10.dp),
                    color = if (viewMode == LedgerViewMode.TRANSACTIONS) Color.White else Color.Transparent,
                    shadowElevation = if (viewMode == LedgerViewMode.TRANSACTIONS) 2.dp else 0.dp,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = if (viewMode == LedgerViewMode.TRANSACTIONS) PrimaryNavy else Slate600,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = Translations.get("jhadi", language),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (viewMode == LedgerViewMode.TRANSACTIONS) PrimaryNavy else Slate600
                        )
                    }
                }

                Surface(
                    onClick = { viewMode = LedgerViewMode.RECURRING },
                    shape = RoundedCornerShape(10.dp),
                    color = if (viewMode == LedgerViewMode.RECURRING) Color.White else Color.Transparent,
                    shadowElevation = if (viewMode == LedgerViewMode.RECURRING) 2.dp else 0.dp,
                    modifier = Modifier.weight(1f).testTag("tab_recurring_transactions")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Autorenew,
                            contentDescription = null,
                            tint = if (viewMode == LedgerViewMode.RECURRING) EmeraldGreen else Slate600,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = Translations.get("recurring_transactions", language),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (viewMode == LedgerViewMode.RECURRING) PrimaryNavy else Slate600
                        )
                        if (activeRecurringCount > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = EmeraldGreen,
                                contentColor = Color.White
                            ) {
                                Text(
                                    text = "$activeRecurringCount",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (viewMode == LedgerViewMode.TRANSACTIONS) {
                // Top Summary Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PrimaryNavy)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "${Translations.get("jhadi", language)} / Khata Bahi",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Net: ${FinancialUtils.formatInr(netBalance)}",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        Divider(color = Color.White.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(Translations.get("todays_received", language), color = EmeraldLight, fontSize = 11.sp)
                                Text(FinancialUtils.formatInr(totalReceived), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(Translations.get("todays_given", language), color = CrimsonLight, fontSize = 11.sp)
                                Text(FinancialUtils.formatInr(totalGiven), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by customer name, note, amount...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryNavy) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = Slate600)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        disabledContainerColor = Color.White,
                        focusedBorderColor = PrimaryNavy,
                        unfocusedBorderColor = Slate600
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ledger_search_bar")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == "ALL",
                        onClick = { selectedFilter = "ALL" },
                        label = { Text("All") },
                        leadingIcon = if (selectedFilter == "ALL") { { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) } } else null
                    )
                    FilterChip(
                        selected = selectedFilter == "TODAY",
                        onClick = { selectedFilter = "TODAY" },
                        label = { Text("Today") },
                        leadingIcon = if (selectedFilter == "TODAY") { { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) } } else null
                    )
                    FilterChip(
                        selected = selectedFilter == "RECEIVED",
                        onClick = { selectedFilter = "RECEIVED" },
                        label = { Text("Received") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = EmeraldLight),
                        leadingIcon = if (selectedFilter == "RECEIVED") { { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) } } else null
                    )
                    FilterChip(
                        selected = selectedFilter == "GIVEN",
                        onClick = { selectedFilter = "GIVEN" },
                        label = { Text("Given") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CrimsonLight),
                        leadingIcon = if (selectedFilter == "GIVEN") { { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) } } else null
                    )
                    FilterChip(
                        selected = selectedFilter == "THIS_WEEK",
                        onClick = { selectedFilter = "THIS_WEEK" },
                        label = { Text("This Week") }
                    )
                    FilterChip(
                        selected = selectedFilter == "THIS_MONTH",
                        onClick = { selectedFilter = "THIS_MONTH" },
                        label = { Text("This Month") }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (filteredTransactions.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No transactions found in this period. Tap '+ Add Hisab' to record transactions.",
                            color = Slate600,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 90.dp)
                    ) {
                        items(filteredTransactions) { tx ->
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
                                        note = "Settlement for ${tx.description.ifBlank { "Ledger Tx #${tx.id} (${tx.date})" }}"
                                    )
                                }
                            )
                        }
                    }
                }
            } else {
                // ==========================================
                // RECURRING TRANSACTIONS VIEW
                // ==========================================
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = EmeraldGreen)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = Translations.get("recurring_transactions", language),
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Auto-posts monthly rent, daily sales, or supplier bills automatically on due dates.",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 11.sp
                                )
                            }
                            IconButton(
                                onClick = { viewModel.processDueRecurringNow() },
                                modifier = Modifier.testTag("run_recurring_now_button")
                            ) {
                                Icon(
                                    Icons.Default.Sync,
                                    contentDescription = Translations.get("run_now", language),
                                    tint = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.processDueRecurringNow() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(Translations.get("run_now", language), fontSize = 11.sp)
                            }

                            Button(
                                onClick = { viewModel.showAddRecurringDialog.value = true },
                                colors = ButtonDefaults.buttonColors(containerColor = SaffronGold, contentColor = Navy900),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("add_recurring_hisab_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(Translations.get("add_recurring", language), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (recurringList.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Autorenew, contentDescription = null, tint = Slate400, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = Translations.get("no_recurring", language),
                                color = Slate600,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "e.g. Set ₹15,000 monthly rent to post on 1st of every month, or ₹500 daily milk delivery.",
                                color = Slate500,
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { viewModel.showAddRecurringDialog.value = true },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(Translations.get("add_recurring", language))
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 90.dp)
                    ) {
                        items(recurringList) { item ->
                            RecurringTransactionItemCard(
                                item = item,
                                onToggleActive = { active ->
                                    viewModel.toggleRecurringActive(item.id, active)
                                },
                                onDelete = {
                                    viewModel.deleteRecurringTransaction(item.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecurringTransactionItemCard(
    item: RecurringTransactionEntity,
    onToggleActive: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(if (item.isActive) EmeraldGreen else Slate400, RoundedCornerShape(5.dp))
                    )
                    Column {
                        Text(
                            text = item.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (item.isActive) PrimaryNavy else Slate600
                        )
                        Text(
                            text = item.customerName,
                            fontSize = 12.sp,
                            color = Slate600
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = FinancialUtils.formatInr(item.amount),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (item.type == "RECEIVED") EmeraldGreen else CrimsonRed
                    )
                    Text(
                        text = if (item.type == "RECEIVED") "Received (मिला)" else "Given (दिया)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (item.type == "RECEIVED") EmeraldGreen else CrimsonRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = Slate200)
            Spacer(modifier = Modifier.height(10.dp))

            // Badges and Schedule Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        color = Slate100,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = item.frequency,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryNavy,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Surface(
                        color = if (item.isIndefinite) EmeraldLight else SaffronLight,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (item.isIndefinite) "Indefinite" else "Ends: ${item.endDate}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (item.isIndefinite) EmeraldGreen else SaffronDark,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = "Next: ${item.nextExecutionDate}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryNavy
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Posted: ${item.totalPostedCount} times" + (item.paymentMethod.let { " • $it" }),
                    fontSize = 11.sp,
                    color = Slate600
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = item.isActive,
                        onCheckedChange = onToggleActive,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = EmeraldGreen
                        ),
                        modifier = Modifier.size(width = 44.dp, height = 24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Delete Recurring",
                            tint = CrimsonRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryManagementDialog(
    viewModel: HisabViewModel,
    onDismiss: () -> Unit,
    onOpenAdd: () -> Unit
) {
    val items by viewModel.inventoryItems.collectAsState()
    val lowStock by viewModel.lowStockItems.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Inventory2, contentDescription = null, tint = PrimaryNavy)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Stock & Inventory Management", fontWeight = FontWeight.Bold, color = PrimaryNavy, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total Items: ${items.size} • Low Stock: ${lowStock.size}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (lowStock.isNotEmpty()) CrimsonRed else EmeraldGreen
                    )
                    Button(
                        onClick = onOpenAdd,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Item", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (items.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No inventory items added yet.", fontSize = 13.sp, color = Slate600)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(items) { item ->
                            val isLow = item.quantity <= item.lowStockThreshold
                            Surface(
                                color = if (isLow) CrimsonLight.copy(alpha = 0.5f) else Color.White,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, if (isLow) CrimsonRed.copy(alpha = 0.4f) else Slate200),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = item.itemName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = Slate800
                                            )
                                            if (isLow) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    color = CrimsonRed,
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = "LOW STOCK",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Category: ${item.category} • Price: ₹${item.unitPrice}",
                                            fontSize = 11.sp,
                                            color = Slate600
                                        )
                                        Text(
                                            text = "Qty: ${item.quantity} ${item.unit} (Threshold: ${item.lowStockThreshold})",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isLow) CrimsonRed else PrimaryNavy
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                viewModel.deleteInventoryItem(item.id)
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = CrimsonRed, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy)
            ) {
                Text("Close")
            }
        }
    )
}

@Composable
fun AddEditInventoryItemDialog(
    viewModel: HisabViewModel,
    onDismiss: () -> Unit
) {
    var itemName by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("General") }
    var quantityStr by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("pcs") }
    var unitPriceStr by remember { mutableStateOf("") }
    var thresholdStr by remember { mutableStateOf("5") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Add Inventory Item", fontWeight = FontWeight.Bold, color = PrimaryNavy, fontSize = 18.sp)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    label = { Text("Item Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quantityStr,
                        onValueChange = { quantityStr = it },
                        label = { Text("Quantity *") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit (pcs/kg)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = unitPriceStr,
                        onValueChange = { unitPriceStr = it },
                        label = { Text("Unit Price (₹)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = thresholdStr,
                        onValueChange = { thresholdStr = it },
                        label = { Text("Low Stock Alert") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = errorMessage!!, color = CrimsonRed, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (itemName.isBlank()) {
                        errorMessage = "Please enter item name"
                        return@Button
                    }
                    val qty = quantityStr.toIntOrNull() ?: 0
                    val price = unitPriceStr.toDoubleOrNull() ?: 0.0
                    val threshold = thresholdStr.toIntOrNull() ?: 5

                    viewModel.saveInventoryItem(
                        itemName = itemName.trim(),
                        category = category.trim().ifBlank { "General" },
                        quantity = qty,
                        unit = unit.trim().ifBlank { "pcs" },
                        unitPrice = price,
                        lowStockThreshold = threshold
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy)
            ) {
                Text("Save Item")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Slate600)
            }
        }
    )
}
