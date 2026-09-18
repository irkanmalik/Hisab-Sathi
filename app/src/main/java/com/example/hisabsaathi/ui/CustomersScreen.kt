package com.example.hisabsaathi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hisabsaathi.data.db.CustomerEntity
import com.example.hisabsaathi.i18n.Translations
import com.example.hisabsaathi.receipt.FinancialUtils
import com.example.ui.theme.*

@Composable
fun CustomersScreen(
    viewModel: HisabViewModel,
    modifier: Modifier = Modifier
) {
    val language by viewModel.currentLanguage.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val business by viewModel.currentBusiness.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val filteredCustomers = remember(customers, searchQuery) {
        if (searchQuery.isBlank()) customers
        else {
            customers.filter {
                it.fullName.contains(searchQuery, ignoreCase = true) ||
                it.mobileNumber.contains(searchQuery) ||
                it.whatsappNumber.contains(searchQuery) ||
                it.city.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Slate50,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.showAddCustomerDialog.value = true },
                containerColor = PrimaryNavy,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                text = { Text(Translations.get("add_customer", language), fontWeight = FontWeight.SemiBold) },
                modifier = Modifier.padding(bottom = 70.dp).testTag("fab_add_customer")
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(Translations.get("search_customer", language), fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Slate600) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = Slate600)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("customer_search_bar")
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredCustomers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isEmpty()) "No customers added yet. Tap '+ Add Customer' to begin." else "No customers matching '$searchQuery'",
                        color = Slate600,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    items(filteredCustomers) { customer ->
                        CustomerCardItem(
                            customer = customer,
                            onCustomerClick = { viewModel.selectedCustomer.value = customer },
                            onAddHisabClick = {
                                viewModel.selectedCustomer.value = customer
                                viewModel.showAddHisabSheet.value = true
                            },
                            onWhatsAppClick = {
                                val (statusLabel, absAmt, _) = FinancialUtils.getBalanceStatus(customer.currentBalance)
                                val msg = """
                                    Hello ${customer.fullName},
                                    Greeting from ${business?.businessName ?: "HisabSaathi"}.
                                    Your current account balance: $statusLabel ${FinancialUtils.formatInr(absAmt)}.
                                    
                                    Thank you.
                                """.trimIndent()
                                viewModel.openWhatsAppMessage(customer.fullName, customer.whatsappNumber.ifBlank { customer.mobileNumber }, msg)
                            },
                            onUpiClick = {
                                viewModel.openCreatePaymentLink(
                                    customer = customer,
                                    amount = if (customer.currentBalance > 0) customer.currentBalance else null,
                                    note = "Hisab settlement for ${customer.fullName}"
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerCardItem(
    customer: CustomerEntity,
    onCustomerClick: () -> Unit,
    onAddHisabClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    onUpiClick: (() -> Unit)? = null
) {
    val (statusLabel, absAmt, statusType) = FinancialUtils.getBalanceStatus(customer.currentBalance)
    val badgeColor = when (statusType) {
        "RECEIVE" -> EmeraldGreen
        "PAY" -> CrimsonRed
        else -> Slate600
    }
    val badgeBg = when (statusType) {
        "RECEIVE" -> EmeraldLight
        "PAY" -> CrimsonLight
        else -> Slate200
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCustomerClick),
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
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(PrimaryNavy.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = customer.fullName.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = PrimaryNavy,
                        fontSize = 18.sp
                    )
                }

                Column {
                    Text(
                        text = customer.fullName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate800
                    )
                    Text(
                        text = "+91 ${customer.mobileNumber}${if (customer.city.isNotBlank()) " • ${customer.city}" else ""}",
                        fontSize = 12.sp,
                        color = Slate600
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeBg
                ) {
                    Text(
                        text = "$statusLabel ${FinancialUtils.formatInr(absAmt)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (onUpiClick != null) {
                        IconButton(
                            onClick = onUpiClick,
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(SaffronGold.copy(alpha = 0.15f))
                                .testTag("upi_customer_button_${customer.id}")
                        ) {
                            Icon(Icons.Default.QrCode2, contentDescription = "UPI Payment Link", tint = SaffronGold, modifier = Modifier.size(15.dp))
                        }
                    }
                    IconButton(
                        onClick = onWhatsAppClick,
                        modifier = Modifier.size(30.dp).clip(CircleShape).background(EmeraldLight)
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = "WhatsApp", tint = EmeraldGreen, modifier = Modifier.size(15.dp))
                    }
                    IconButton(
                        onClick = onAddHisabClick,
                        modifier = Modifier.size(30.dp).clip(CircleShape).background(PrimaryNavy.copy(alpha = 0.1f))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Hisab", tint = PrimaryNavy, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
