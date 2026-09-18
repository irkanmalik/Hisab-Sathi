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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hisabsaathi.data.db.PaymentLinkEntity
import com.example.hisabsaathi.data.db.ReceiptEntity
import com.example.hisabsaathi.i18n.Translations
import com.example.hisabsaathi.payment.PaymentLinkService
import com.example.hisabsaathi.receipt.FinancialUtils
import com.example.hisabsaathi.security.SecurityManager
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MoreScreen(
    viewModel: HisabViewModel,
    modifier: Modifier = Modifier
) {
    val language by viewModel.currentLanguage.collectAsState()
    val receipts by viewModel.receipts.collectAsState()
    val paymentLinks by viewModel.paymentLinks.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val business by viewModel.currentBusiness.collectAsState()
    val context = LocalContext.current

    var selectedSection by remember { mutableStateOf<String?>(null) } // null = Menu, "RECEIPTS", "PAYMENT_LINKS", "REPORTS", "ADMIN", "SETTINGS"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate50)
            .padding(16.dp)
    ) {
        if (selectedSection != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                IconButton(onClick = { selectedSection = null }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = PrimaryNavy)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (selectedSection) {
                        "RECEIPTS" -> Translations.get("receipts", language)
                        "PAYMENT_LINKS" -> Translations.get("payment_links", language)
                        "REPORTS" -> Translations.get("reports", language)
                        "ADMIN" -> "Admin Dashboard"
                        "SETTINGS" -> "WhatsApp API Settings"
                        else -> "More"
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryNavy
                )
            }
        }

        when (selectedSection) {
            "RECEIPTS" -> ReceiptsSubView(receipts, business?.businessName ?: "HisabSaathi", business?.businessMobile ?: "", viewModel)
            "PAYMENT_LINKS" -> PaymentLinksSubView(paymentLinks, business?.businessName ?: "HisabSaathi", viewModel)
            "REPORTS" -> ReportsSubView(viewModel)
            "ADMIN" -> AdminSubView(viewModel)
            "SETTINGS" -> WhatsAppSettingsSubView(viewModel)
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = PrimaryNavy)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Store, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                                }
                                Column {
                                    Text(business?.businessName ?: "HisabSaathi", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text("Logged in as ${currentUser?.mobileNumber ?: "User"}", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    item {
                        MenuListItem(
                            icon = Icons.Default.Receipt,
                            title = Translations.get("receipts", language),
                            subtitle = "View, download PDF, print & share receipts",
                            onClick = { selectedSection = "RECEIPTS" }
                        )
                    }

                    item {
                        MenuListItem(
                            icon = Icons.Default.Link,
                            title = Translations.get("payment_links", language),
                            subtitle = "Generate UPI payment links & collect hisab",
                            onClick = { selectedSection = "PAYMENT_LINKS" }
                        )
                    }

                    item {
                        MenuListItem(
                            icon = Icons.Default.BarChart,
                            title = Translations.get("reports", language),
                            subtitle = "Daily, monthly & customer financial statements",
                            onClick = { selectedSection = "REPORTS" }
                        )
                    }

                    item {
                        MenuListItem(
                            icon = Icons.Default.Settings,
                            title = "WhatsApp API Provider Settings",
                            subtitle = "Meta Cloud API credentials & sender configuration",
                            onClick = { selectedSection = "SETTINGS" }
                        )
                    }

                    // Admin Dashboard item - available if admin role or admin email
                    val isAdmin = currentUser?.role == "admin" || SecurityManager.isAuthorizedAdmin(currentUser?.email, currentUser?.mobileNumber)
                    if (isAdmin) {
                        item {
                            MenuListItem(
                                icon = Icons.Default.AdminPanelSettings,
                                title = "Admin Dashboard (Private)",
                                subtitle = "Platform logs, user verification & business data",
                                badgeText = "Admin",
                                onClick = { selectedSection = "ADMIN" }
                            )
                        }
                    }

                    item {
                        MenuListItem(
                            icon = Icons.Default.Backup,
                            title = "Backup & Restore (Data Sync)",
                            subtitle = "Relational JSON export & atomic restore with rollback",
                            badgeText = "Security",
                            onClick = { viewModel.showBackupDialog.value = true }
                        )
                    }

                    item {
                        MenuListItem(
                            icon = Icons.Default.SupervisorAccount,
                            title = "User Roles & RBAC Permissions",
                            subtitle = "Manage Admin, Manager & Employee access control",
                            badgeText = "RBAC",
                            onClick = { viewModel.showRoleDialog.value = true }
                        )
                    }

                    item {
                        MenuListItem(
                            icon = Icons.Default.Language,
                            title = Translations.get("language", language),
                            subtitle = "Select from 12 Indian Languages",
                            onClick = { viewModel.showLanguageDialog.value = true }
                        )
                    }

                    item {
                        MenuListItem(
                            icon = Icons.Default.Logout,
                            title = Translations.get("logout", language),
                            subtitle = "Sign out of your HisabSaathi account",
                            textColor = CrimsonRed,
                            iconTint = CrimsonRed,
                            onClick = { viewModel.logout() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MenuListItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badgeText: String? = null,
    textColor: Color = Slate800,
    iconTint: Color = PrimaryNavy,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = textColor)
                        if (badgeText != null) {
                            Surface(color = EmeraldLight, shape = RoundedCornerShape(4.dp)) {
                                Text(badgeText, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EmeraldGreen, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                    Text(text = subtitle, fontSize = 12.sp, color = Slate600)
                }
            }

            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Slate600, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun ReceiptsSubView(receipts: List<ReceiptEntity>, businessName: String, businessMobile: String, viewModel: HisabViewModel) {
    if (receipts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text("No receipts generated yet. Receipts are automatically created when saving Hisab transactions.", color = Slate600, fontSize = 14.sp)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 80.dp)) {
            items(receipts) { receipt ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(receipt.receiptNumber, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryNavy)
                            Text("${receipt.date} ${receipt.time}", fontSize = 11.sp, color = Slate600)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Customer: ${receipt.customerName}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Slate800)
                        Text("Amount: ${FinancialUtils.formatInr(receipt.amount)} (${receipt.transactionType})", fontSize = 13.sp, color = if (receipt.transactionType == "RECEIVED") EmeraldGreen else CrimsonRed, fontWeight = FontWeight.Bold)

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.receiptService.printOrShareReceipt(receipt, businessName, businessMobile) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Print / PDF", fontSize = 12.sp)
                            }
                            Button(
                                onClick = {
                                    val msg = """
                                        Payment Receipt: ${receipt.receiptNumber}
                                        Customer: ${receipt.customerName}
                                        Amount: ${FinancialUtils.formatInr(receipt.amount)} (${receipt.transactionType})
                                        Mode: ${receipt.paymentMethod}
                                        Current Balance: ${FinancialUtils.formatInr(receipt.currentBalance)}
                                        
                                        $businessName via HisabSaathi
                                    """.trimIndent()
                                    viewModel.openWhatsAppMessage(receipt.customerName, receipt.customerMobile, msg, receipt.transactionId)
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("WhatsApp", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentLinksSubView(paymentLinks: List<PaymentLinkEntity>, businessName: String, viewModel: HisabViewModel) {
    val context = LocalContext.current
    val upiVpa by viewModel.businessUpiVpa.collectAsState()

    Column {
        Button(
            onClick = { viewModel.openCreatePaymentLink() },
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("create_payment_link_btn"),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SaffronGold)
        ) {
            Icon(Icons.Default.QrCode2, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create UPI Payment Link", fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Truthful notice
        Surface(color = SaffronLight, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("NPCI compliant UPI links generate instant payments directly into your account ($upiVpa) with automated reconciliation.", fontSize = 11.sp, color = Slate800)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (paymentLinks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("No payment links created yet.", color = Slate600, fontSize = 14.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 80.dp)) {
                items(paymentLinks) { link ->
                    val upiUri = PaymentLinkService.generateUpiUri(
                        vpa = upiVpa,
                        payeeName = businessName.ifBlank { "HisabSaathi Merchant" },
                        amount = link.amount,
                        transactionNote = link.purpose.ifBlank { "Hisab Settlement" },
                        referenceCode = link.linkCode
                    )

                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.QrCode2, contentDescription = null, tint = PrimaryNavy, modifier = Modifier.size(18.dp))
                                    Text(link.linkCode, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryNavy)
                                }
                                Surface(
                                    color = when (link.status) {
                                        "PAID" -> EmeraldLight
                                        "EXPIRED" -> Slate200
                                        else -> SaffronLight
                                    },
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        link.status,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (link.status) {
                                            "PAID" -> EmeraldGreen
                                            "EXPIRED" -> Slate600
                                            else -> SaffronGold
                                        },
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Customer: ${link.customerName} • Purpose: ${link.purpose}", fontSize = 13.sp, color = Slate800)
                            Text("Amount: ${FinancialUtils.formatInr(link.amount)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = EmeraldGreen)

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        PaymentLinkService.copyToClipboard(context, upiUri)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy", fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        PaymentLinkService.sharePaymentLink(
                                            context = context,
                                            customerName = link.customerName,
                                            amount = link.amount,
                                            paymentUrl = PaymentLinkService.getPaymentUrl(link.linkCode),
                                            businessName = businessName,
                                            upiUri = upiUri,
                                            reconciliationRef = link.linkCode
                                        )
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                                    modifier = Modifier.weight(1.5f)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Share UPI", fontSize = 12.sp)
                                }
                            }

                            if (link.status == "PENDING") {
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        viewModel.reconcilePaymentLink(
                                            linkId = link.id,
                                            notes = "Reconciled via UPI Payment Link"
                                        )
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                    modifier = Modifier.fillMaxWidth().testTag("reconcile_btn_${link.id}")
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Mark as Paid & Reconcile Account", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReportsSubView(viewModel: HisabViewModel) {
    val customers by viewModel.customers.collectAsState()
    val transactions by viewModel.transactions.collectAsState()

    val totalRec = customers.filter { it.currentBalance > 0 }.sumOf { it.currentBalance }
    val totalPay = customers.filter { it.currentBalance < 0 }.sumOf { kotlin.math.abs(it.currentBalance) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 80.dp)) {
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Executive Financial Summary", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Slate800)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Market Receivable:", color = Slate600, fontSize = 13.sp)
                        Text(FinancialUtils.formatInr(totalRec), fontWeight = FontWeight.Bold, color = EmeraldGreen, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Market Payable:", color = Slate600, fontSize = 13.sp)
                        Text(FinancialUtils.formatInr(totalPay), fontWeight = FontWeight.Bold, color = CrimsonRed, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Transactions Logged:", color = Slate600, fontSize = 13.sp)
                        Text("${transactions.size} records", fontWeight = FontWeight.Bold, color = PrimaryNavy, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun WhatsAppSettingsSubView(viewModel: HisabViewModel) {
    val accountId by viewModel.waAccountId.collectAsState()
    val phoneId by viewModel.waPhoneNumberId.collectAsState()
    val token by viewModel.waAccessToken.collectAsState()
    val apiVer by viewModel.waApiVersion.collectAsState()
    val sender by viewModel.waSenderNumber.collectAsState()
    val isConnected by viewModel.waIsConnected.collectAsState()
    val statusMsg by viewModel.waStatusMessage.collectAsState()

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 80.dp)) {
        item {
            Surface(
                color = if (isConnected) EmeraldLight else SaffronLight,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (isConnected) Icons.Default.CheckCircle else Icons.Default.Warning, contentDescription = null, tint = if (isConnected) EmeraldGreen else SaffronGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isConnected) "WhatsApp Business API Connected" else "WhatsApp Cloud API not configured yet. WhatsApp links will open the WhatsApp application directly.",
                        fontSize = 12.sp,
                        color = Slate800
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = accountId,
                onValueChange = { viewModel.waAccountId.value = it },
                label = { Text("WhatsApp Business Account ID") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = phoneId,
                onValueChange = { viewModel.waPhoneNumberId.value = it },
                label = { Text("Phone Number ID") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = token,
                onValueChange = { viewModel.waAccessToken.value = it },
                label = { Text("Permanent Access Token") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = sender,
                onValueChange = { viewModel.waSenderNumber.value = it },
                label = { Text("Sender Mobile Number") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (statusMsg != null) {
            item {
                Text(statusMsg!!, fontSize = 12.sp, color = PrimaryNavy)
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { viewModel.saveWhatsAppSettings() },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save Settings")
                }
                Button(
                    onClick = { viewModel.testWhatsAppConnection() },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Test Connection")
                }
            }
        }
    }
}

@Composable
fun AdminSubView(viewModel: HisabViewModel) {
    val auditLogs by viewModel.auditLogs.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val transactions by viewModel.transactions.collectAsState()

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 80.dp)) {
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Navy800)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Authorized Administrator: ${SecurityManager.ADMIN_EMAIL}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("System Security & Audit Engine: Active", color = EmeraldLight, fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.showBackupDialog.value = true },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Launch JSON Backup & Restore Routine", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        item {
            Text("Audit Logs (${auditLogs.size})", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Slate800)
        }

        items(auditLogs.take(25)) { log ->
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(log.action, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PrimaryNavy)
                        Text(log.details, fontSize = 11.sp, color = Slate600)
                    }
                    Text(SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(log.timestamp)), fontSize = 11.sp, color = Slate600)
                }
            }
        }
    }
}
