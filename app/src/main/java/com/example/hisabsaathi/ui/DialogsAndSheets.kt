package com.example.hisabsaathi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import android.widget.Toast
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.compose.ui.viewinterop.AndroidView
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.example.hisabsaathi.payment.PaymentLinkService
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.hisabsaathi.data.db.CustomerEntity
import com.example.hisabsaathi.i18n.AppLanguage
import com.example.hisabsaathi.i18n.Translations
import com.example.hisabsaathi.receipt.FinancialUtils
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AddCustomerDialog(
    viewModel: HisabViewModel,
    onDismiss: () -> Unit
) {
    val language by viewModel.currentLanguage.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var fullName by remember { mutableStateOf("") }
    var mobileNumber by remember { mutableStateOf("") }
    var whatsappNumber by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var openingBalanceStr by remember { mutableStateOf("") }
    var balanceType by remember { mutableStateOf("RECEIVABLE") } // RECEIVABLE or PAYABLE
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = Translations.get("add_customer", language),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryNavy
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text(Translations.get("customer_name", language)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_customer_name")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = mobileNumber,
                    onValueChange = { if (it.length <= 10) mobileNumber = it },
                    label = { Text(Translations.get("customer_mobile", language)) },
                    leadingIcon = { Text("+91 ", fontWeight = FontWeight.Bold, color = PrimaryNavy) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_customer_mobile")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = whatsappNumber,
                    onValueChange = { if (it.length <= 10) whatsappNumber = it },
                    label = { Text("${Translations.get("whatsapp_number", language)} (Optional)") },
                    leadingIcon = { Text("+91 ", fontWeight = FontWeight.Bold, color = EmeraldGreen) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text(Translations.get("city", language)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = openingBalanceStr,
                    onValueChange = { openingBalanceStr = it },
                    label = { Text(Translations.get("opening_balance", language)) },
                    placeholder = { Text("0.00") },
                    leadingIcon = { Text("₹ ", fontWeight = FontWeight.Bold, color = PrimaryNavy) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Balance Type Selector
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceFilterChip(
                        modifier = Modifier.weight(1f),
                        label = Translations.get("you_will_receive", language),
                        isSelected = balanceType == "RECEIVABLE",
                        activeColor = EmeraldGreen,
                        activeBg = EmeraldLight,
                        onClick = { balanceType = "RECEIVABLE" }
                    )
                    ChoiceFilterChip(
                        modifier = Modifier.weight(1f),
                        label = Translations.get("you_have_to_pay", language),
                        isSelected = balanceType == "PAYABLE",
                        activeColor = CrimsonRed,
                        activeBg = CrimsonLight,
                        onClick = { balanceType = "PAYABLE" }
                    )
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(errorMessage!!, color = CrimsonRed, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = Slate600) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (fullName.isBlank() || mobileNumber.length < 10) {
                                errorMessage = "Please enter valid customer name and 10-digit mobile number."
                                return@Button
                            }
                            val opBal = openingBalanceStr.toDoubleOrNull() ?: 0.0
                            coroutineScope.launch {
                                viewModel.repository.addCustomer(
                                    fullName = fullName.trim(),
                                    mobileNumber = mobileNumber.trim(),
                                    whatsappNumber = whatsappNumber.trim().ifBlank { mobileNumber.trim() },
                                    alternateNumber = "",
                                    address = address.trim(),
                                    city = city.trim(),
                                    district = "",
                                    state = "",
                                    notes = "",
                                    openingBalance = opBal,
                                    balanceType = balanceType
                                )
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("confirm_add_customer_button")
                    ) {
                        Text(Translations.get("save_profile", language))
                    }
                }
            }
        }
    }
}

@Composable
fun ChoiceFilterChip(
    modifier: Modifier = Modifier,
    label: String,
    isSelected: Boolean,
    activeColor: Color,
    activeBg: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) activeBg else Slate100,
        onClick = onClick
    ) {
        Box(modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp), contentAlignment = Alignment.Center) {
            Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, color = if (isSelected) activeColor else Slate600)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHisabBottomSheet(
    viewModel: HisabViewModel,
    onDismiss: () -> Unit
) {
    val language by viewModel.currentLanguage.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val selectedCustomerState by viewModel.selectedCustomer.collectAsState()
    val business by viewModel.currentBusiness.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var selectedCustomer by remember { mutableStateOf(selectedCustomerState ?: customers.firstOrNull()) }
    var transactionType by remember { mutableStateOf("RECEIVED") } // RECEIVED (मिला) or GIVEN (दिया)
    var amountStr by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("UPI") } // CASH, UPI, BANK, CARD, OTHER
    var description by remember { mutableStateOf("") }
    var referenceNumber by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showConfirmation by remember { mutableStateOf(false) }

    val amount = amountStr.toDoubleOrNull() ?: 0.0
    val prevBalance = selectedCustomer?.currentBalance ?: 0.0
    val balanceAfter = if (transactionType == "RECEIVED") prevBalance - amount else prevBalance + amount

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = Translations.get("add_hisab", language),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryNavy
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Select Customer Dropdown
            var showQrScannerDialog by remember { mutableStateOf(false) }
            val context = LocalContext.current
            if (showQrScannerDialog) {
                CustomerQrScannerDialog(
                    customers = customers,
                    onCustomerScanned = { matched ->
                        selectedCustomer = matched
                        showQrScannerDialog = false
                        Toast.makeText(context, "Customer Identified: ${matched.fullName}", Toast.LENGTH_SHORT).show()
                    },
                    onDismiss = { showQrScannerDialog = false }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(Translations.get("customer_name", language), fontSize = 12.sp, color = Slate600)
                OutlinedButton(
                    onClick = { showQrScannerDialog = true },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(30.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryNavy)
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Scan QR", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            var customerExpanded by remember { mutableStateOf(false) }

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedCard(
                    onClick = { customerExpanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(selectedCustomer?.fullName ?: "Select Customer", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Slate800)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                }
                DropdownMenu(
                    expanded = customerExpanded,
                    onDismissRequest = { customerExpanded = false }
                ) {
                    customers.forEach { cust ->
                        DropdownMenuItem(
                            text = { Text("${cust.fullName} (+91 ${cust.mobileNumber})") },
                            onClick = {
                                selectedCustomer = cust
                                customerExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Transaction Type (Received / Given)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ChoiceFilterChip(
                    modifier = Modifier.weight(1f),
                    label = "Payment Received ( मिला )",
                    isSelected = transactionType == "RECEIVED",
                    activeColor = EmeraldGreen,
                    activeBg = EmeraldLight,
                    onClick = { transactionType = "RECEIVED" }
                )
                ChoiceFilterChip(
                    modifier = Modifier.weight(1f),
                    label = "Payment Given ( दिया )",
                    isSelected = transactionType == "GIVEN",
                    activeColor = CrimsonRed,
                    activeBg = CrimsonLight,
                    onClick = { transactionType = "GIVEN" }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = amountStr,
                onValueChange = { amountStr = it },
                label = { Text(Translations.get("amount", language)) },
                placeholder = { Text("0.00") },
                leadingIcon = { Text("₹ ", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PrimaryNavy) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("hisab_amount_field")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Payment Methods
            Text("Payment Method", fontSize = 12.sp, color = Slate600)
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("UPI", "CASH", "BANK", "CARD").forEach { method ->
                    ChoiceFilterChip(
                        modifier = Modifier.weight(1f),
                        label = method,
                        isSelected = paymentMethod == method,
                        activeColor = PrimaryNavy,
                        activeBg = Slate200,
                        onClick = { paymentMethod = method }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Note / Description (Optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(errorMessage!!, color = CrimsonRed, fontSize = 12.sp)
            }

            // Preview Confirmation Step
            if (amount > 0 && selectedCustomer != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate100)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Confirmation Summary:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate800)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Customer: ${selectedCustomer?.fullName}", fontSize = 12.sp, color = Slate600)
                        Text("Amount: ${FinancialUtils.formatInr(amount)} ($transactionType)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (transactionType == "RECEIVED") EmeraldGreen else CrimsonRed)
                        val (balLabel, balAbs, _) = FinancialUtils.getBalanceStatus(balanceAfter)
                        Text("Balance After Transaction: $balLabel ${FinancialUtils.formatInr(balAbs)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryNavy)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (selectedCustomer == null || amount <= 0) {
                            errorMessage = "Please choose a customer and enter an amount."
                            return@Button
                        }
                        coroutineScope.launch {
                            viewModel.repository.addTransaction(
                                customerId = selectedCustomer!!.id,
                                type = transactionType,
                                amount = amount,
                                paymentMethod = paymentMethod,
                                description = description,
                                referenceNumber = referenceNumber
                            )
                            onDismiss()
                        }
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy)
                ) {
                    Text("Confirm & Save", fontSize = 13.sp)
                }

                Button(
                    onClick = {
                        if (selectedCustomer == null || amount <= 0) {
                            errorMessage = "Please choose a customer and enter an amount."
                            return@Button
                        }
                        coroutineScope.launch {
                            val (_, receipt) = viewModel.repository.addTransaction(
                                customerId = selectedCustomer!!.id,
                                type = transactionType,
                                amount = amount,
                                paymentMethod = paymentMethod,
                                description = description,
                                referenceNumber = referenceNumber
                            )
                            onDismiss()
                            val msg = """
                                Hello ${selectedCustomer!!.fullName},
                                Your payment of ${FinancialUtils.formatInr(amount)} ($transactionType) is recorded.
                                Receipt: ${receipt.receiptNumber}
                                Current Balance: ${FinancialUtils.formatInr(receipt.currentBalance)}
                                
                                Thank you,
                                ${business?.businessName ?: "HisabSaathi"}
                            """.trimIndent()
                            viewModel.openWhatsAppMessage(selectedCustomer!!.fullName, selectedCustomer!!.whatsappNumber.ifBlank { selectedCustomer!!.mobileNumber }, msg, receipt.transactionId)
                        }
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                ) {
                    Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save & WhatsApp", fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun WhatsAppMessageReadyDialog(
    previewData: WhatsAppPreviewData,
    viewModel: HisabViewModel,
    onDismiss: () -> Unit
) {
    var editableMessage by remember { mutableStateOf(previewData.messageText) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Chat, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(24.dp))
                    Text(
                        text = "WHATSAPP MESSAGE READY",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryNavy
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Recipient: ${previewData.customerName} (+91 ${previewData.mobileNumber})", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Slate800)

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = editableMessage,
                    onValueChange = { editableMessage = it },
                    label = { Text("Message Preview") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 220.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = Slate600) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { viewModel.confirmOpenWhatsApp(previewData.mobileNumber, editableMessage) },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open WhatsApp")
                    }
                }
            }
        }
    }
}

@Composable
fun AddEmployeeDialog(
    viewModel: HisabViewModel,
    onDismiss: () -> Unit
) {
    val language by viewModel.currentLanguage.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("EMP-" + (100..999).random()) }
    var mobile by remember { mutableStateOf("") }
    var position by remember { mutableStateOf("Staff") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(Translations.get("add_employee", language), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PrimaryNavy)
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Employee Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Employee ID Code") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = mobile,
                    onValueChange = { if (it.length <= 10) mobile = it },
                    label = { Text("Mobile / WhatsApp Number") },
                    leadingIcon = { Text("+91 ", fontWeight = FontWeight.Bold, color = PrimaryNavy) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = position,
                    onValueChange = { position = it },
                    label = { Text("Role / Position") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = Slate600) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank() && mobile.isNotBlank()) {
                                coroutineScope.launch {
                                    viewModel.repository.addEmployee(name.trim(), code.trim(), mobile.trim(), position.trim())
                                    onDismiss()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Add Employee")
                    }
                }
            }
        }
    }
}

@Composable
fun CreatePaymentLinkDialog(
    viewModel: HisabViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val customers by viewModel.customers.collectAsState()
    val business by viewModel.currentBusiness.collectAsState()
    val activeTx by viewModel.activeUpiTransaction.collectAsState()
    val activeCust by viewModel.activeUpiCustomer.collectAsState()
    val activeAmt by viewModel.activeUpiAmount.collectAsState()
    val activeNote by viewModel.activeUpiNote.collectAsState()

    var selectedCustomer by remember {
        mutableStateOf(activeCust ?: customers.firstOrNull())
    }
    var customerDropdownExpanded by remember { mutableStateOf(false) }

    val defaultAmount = activeAmt?.let { String.format(Locale.US, "%.2f", it) }
        ?: activeTx?.let { String.format(Locale.US, "%.2f", it.amount) }
        ?: (selectedCustomer?.currentBalance?.takeIf { it > 0 }?.let { String.format(Locale.US, "%.2f", it) } ?: "")

    var amountStr by remember { mutableStateOf(defaultAmount) }
    var vpa by remember {
        val initialVpa = business?.businessMobile?.takeIf { it.isNotBlank() }?.let { "$it@upi" } ?: "hisabsaathi@upi"
        mutableStateOf(initialVpa)
    }

    val defaultPurpose = activeNote
        ?: activeTx?.let { "Payment for Tx #${it.id} (${it.date})" }
        ?: "Hisab Settlement"

    var purpose by remember { mutableStateOf(defaultPurpose) }

    val linkCode = remember(activeTx?.id) {
        if (activeTx != null) {
            "HS-TX-${activeTx!!.id}-${(1000..9999).random()}"
        } else {
            PaymentLinkService.generateReconciliationRef("HS-PL")
        }
    }

    val parsedAmount = amountStr.toDoubleOrNull() ?: 0.0
    val payeeName = business?.businessName ?: "HisabSaathi Store"

    val upiUri = remember(vpa, payeeName, parsedAmount, purpose, linkCode) {
        PaymentLinkService.generateUpiUri(
            vpa = vpa.ifBlank { "hisabsaathi@upi" },
            payeeName = payeeName,
            amount = parsedAmount,
            transactionNote = purpose.ifBlank { "Payment" },
            referenceCode = linkCode
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SaffronGold.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.QrCode2,
                                contentDescription = null,
                                tint = SaffronGold,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "UPI Payment Link",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryNavy
                            )
                            Text(
                                text = "NPCI Standard Deep Link & QR",
                                fontSize = 11.sp,
                                color = Slate600
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate400)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Transaction Context Badge (if linked to a specific transaction)
                if (activeTx != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = PrimaryNavy.copy(alpha = 0.08f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = PrimaryNavy,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Linked to Tx #${activeTx!!.id} • ${activeTx!!.date} (${activeTx!!.type})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = PrimaryNavy
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Customer Selection Dropdown
                Text("Select Customer", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate600)
                Spacer(modifier = Modifier.height(4.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { customerDropdownExpanded = true },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = selectedCustomer?.fullName ?: "Select Customer",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryNavy
                                )
                                if (selectedCustomer != null) {
                                    Text(
                                        text = "+91 ${selectedCustomer!!.mobileNumber} • Balance: ${FinancialUtils.formatInr(selectedCustomer!!.currentBalance)}",
                                        fontSize = 11.sp,
                                        color = if (selectedCustomer!!.currentBalance > 0) CrimsonRed else EmeraldGreen
                                    )
                                }
                            }
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Slate600)
                        }
                    }

                    DropdownMenu(
                        expanded = customerDropdownExpanded,
                        onDismissRequest = { customerDropdownExpanded = false }
                    ) {
                        customers.forEach { customer ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(customer.fullName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(
                                            "+91 ${customer.mobileNumber} (Bal: ${FinancialUtils.formatInr(customer.currentBalance)})",
                                            fontSize = 11.sp,
                                            color = Slate600
                                        )
                                    }
                                },
                                onClick = {
                                    selectedCustomer = customer
                                    customerDropdownExpanded = false
                                    if (amountStr.isBlank() && customer.currentBalance > 0) {
                                        amountStr = String.format(Locale.US, "%.2f", customer.currentBalance)
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Amount Field
                Text("Transaction Amount (₹)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate600)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    placeholder = { Text("0.00") },
                    leadingIcon = { Text("₹ ", fontWeight = FontWeight.Bold, color = PrimaryNavy) },
                    trailingIcon = {
                        val bal = selectedCustomer?.currentBalance ?: 0.0
                        if (bal > 0) {
                            TextButton(onClick = { amountStr = String.format(Locale.US, "%.2f", bal) }) {
                                Text("Full Bal", fontSize = 11.sp, color = EmeraldGreen, fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("upi_amount_field"),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Payee UPI VPA
                Text("Payee UPI ID (VPA)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate600)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = vpa,
                    onValueChange = { vpa = it },
                    placeholder = { Text("e.g. yourname@upi or 9876543210@paytm") },
                    leadingIcon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Slate600, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("upi_vpa_field"),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Purpose / Note
                Text("Transaction Note / Purpose", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate600)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = purpose,
                    onValueChange = { purpose = it },
                    placeholder = { Text("Purpose (e.g. Ledger Settlement)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Unique Reconciliation Reference Card
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Slate100,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Reconciliation Ref:", fontSize = 11.sp, color = Slate600)
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = PrimaryNavy.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = linkCode,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryNavy,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = upiUri,
                            fontSize = 10.sp,
                            color = Slate600,
                            maxLines = 1,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons Row: WhatsApp & Share
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (parsedAmount <= 0) {
                                Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val custName = selectedCustomer?.fullName ?: "Customer"
                            val mobile = selectedCustomer?.whatsappNumber?.ifBlank { selectedCustomer?.mobileNumber } ?: ""
                            val shareMessage = PaymentLinkService.buildCustomerShareMessage(
                                customerName = custName,
                                amount = parsedAmount,
                                businessName = payeeName,
                                upiUri = upiUri,
                                reconciliationRef = linkCode,
                                purpose = purpose
                            )
                            // Save link to DB as PENDING
                            coroutineScope.launch {
                                val now = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                viewModel.repository.createPaymentLink(
                                    customerId = selectedCustomer?.id ?: 1,
                                    amount = parsedAmount,
                                    purpose = purpose,
                                    dueDate = now,
                                    referenceNumber = activeTx?.id?.toString() ?: "",
                                    customLinkCode = linkCode
                                )
                            }
                            viewModel.openWhatsAppMessage(custName, mobile, shareMessage, activeTx?.id ?: 0)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("share_whatsapp_upi_button")
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("WhatsApp", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            if (parsedAmount <= 0) {
                                Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                                return@OutlinedButton
                            }
                            val custName = selectedCustomer?.fullName ?: "Customer"
                            coroutineScope.launch {
                                val now = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                viewModel.repository.createPaymentLink(
                                    customerId = selectedCustomer?.id ?: 1,
                                    amount = parsedAmount,
                                    purpose = purpose,
                                    dueDate = now,
                                    referenceNumber = activeTx?.id?.toString() ?: "",
                                    customLinkCode = linkCode
                                )
                            }
                            PaymentLinkService.sharePaymentLink(
                                context = context,
                                customerName = custName,
                                amount = parsedAmount,
                                paymentUrl = PaymentLinkService.getPaymentUrl(linkCode),
                                businessName = payeeName,
                                upiUri = upiUri,
                                reconciliationRef = linkCode
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share Link", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action Row: Copy Link & Open in UPI App
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            PaymentLinkService.copyToClipboard(context, upiUri, "UPI Payment Link")
                            Toast.makeText(context, "UPI Link copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("copy_upi_link_button")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy Link", fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            if (parsedAmount <= 0) {
                                Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            try {
                                val intent = PaymentLinkService.createUpiIntent(upiUri)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "No UPI app found on this device", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("open_upi_app_button")
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pay / App", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Save Link Only Button
                Button(
                    onClick = {
                        if (parsedAmount <= 0) {
                            Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (selectedCustomer == null) {
                            Toast.makeText(context, "Please select a customer", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        coroutineScope.launch {
                            val now = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                            viewModel.repository.createPaymentLink(
                                customerId = selectedCustomer!!.id,
                                amount = parsedAmount,
                                purpose = purpose,
                                dueDate = now,
                                referenceNumber = activeTx?.id?.toString() ?: "",
                                customLinkCode = linkCode
                            )
                            Toast.makeText(context, "UPI Payment Link saved!", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronGold, contentColor = Navy900),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("save_upi_link_button")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save & Track in Payment Links", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun AddRecurringTransactionDialog(
    viewModel: HisabViewModel,
    onDismiss: () -> Unit
) {
    val language by viewModel.currentLanguage.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    var selectedCustomer by remember { mutableStateOf(customers.firstOrNull()) }
    var title by remember { mutableStateOf("") }
    var transactionType by remember { mutableStateOf("RECEIVED") } // RECEIVED or GIVEN
    var amountStr by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("MONTHLY") } // DAILY, WEEKLY, MONTHLY, YEARLY
    var startDate by remember { mutableStateOf(todayStr) }
    var isIndefinite by remember { mutableStateOf(true) }
    var endDate by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("CASH") }
    var description by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Autorenew, contentDescription = null, tint = EmeraldGreen)
                    Text(
                        text = Translations.get("recurring_transactions", language),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryNavy
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Select Customer Dropdown
                Text(Translations.get("customer_name", language), fontSize = 12.sp, color = Slate600)
                Spacer(modifier = Modifier.height(4.dp))
                var customerExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedCard(
                        onClick = { customerExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(selectedCustomer?.fullName ?: "Select Customer", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Slate800)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }
                    DropdownMenu(
                        expanded = customerExpanded,
                        onDismissRequest = { customerExpanded = false }
                    ) {
                        customers.forEach { cust ->
                            DropdownMenuItem(
                                text = { Text("${cust.fullName} (+91 ${cust.mobileNumber})") },
                                onClick = {
                                    selectedCustomer = cust
                                    customerExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Title / Purpose
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title (e.g. Shop Rent, Daily Milk, Sales)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("recurring_title_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Transaction Type
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceFilterChip(
                        modifier = Modifier.weight(1f),
                        label = "Payment Received ( मिला )",
                        isSelected = transactionType == "RECEIVED",
                        activeColor = EmeraldGreen,
                        activeBg = EmeraldLight,
                        onClick = { transactionType = "RECEIVED" }
                    )
                    ChoiceFilterChip(
                        modifier = Modifier.weight(1f),
                        label = "Payment Given ( दिया )",
                        isSelected = transactionType == "GIVEN",
                        activeColor = CrimsonRed,
                        activeBg = CrimsonLight,
                        onClick = { transactionType = "GIVEN" }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Amount
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text(Translations.get("amount", language)) },
                    placeholder = { Text("0.00") },
                    leadingIcon = { Text("₹ ", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PrimaryNavy) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("recurring_amount_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Frequency Selector
                Text("Schedule Frequency", fontSize = 12.sp, color = Slate600, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("DAILY", "WEEKLY", "MONTHLY", "YEARLY").forEach { freq ->
                        ChoiceFilterChip(
                            modifier = Modifier.weight(1f),
                            label = when (freq) {
                                "DAILY" -> Translations.get("daily", language)
                                "WEEKLY" -> Translations.get("weekly", language)
                                "MONTHLY" -> Translations.get("monthly", language)
                                else -> Translations.get("yearly", language)
                            },
                            isSelected = frequency == freq,
                            activeColor = PrimaryNavy,
                            activeBg = Slate200,
                            onClick = { frequency = freq }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Start Date
                OutlinedTextField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    label = { Text("${Translations.get("start_date", language)} (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Recurrence Duration / Indefinite vs End Date Option
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate100)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Continue Indefinitely", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Slate800)
                                Text("Repeats automatically without an end date", fontSize = 11.sp, color = Slate600)
                            }
                            Switch(
                                checked = isIndefinite,
                                onCheckedChange = { isIndefinite = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = EmeraldGreen
                                ),
                                modifier = Modifier.testTag("recurring_indefinite_switch")
                            )
                        }

                        if (!isIndefinite) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = endDate,
                                onValueChange = { endDate = it },
                                label = { Text("${Translations.get("end_date", language)} (YYYY-MM-DD)") },
                                placeholder = { Text("e.g. 2026-12-31") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("recurring_end_date_input")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Payment Method
                Text("Default Payment Method", fontSize = 12.sp, color = Slate600)
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("CASH", "UPI", "BANK", "CARD").forEach { method ->
                        ChoiceFilterChip(
                            modifier = Modifier.weight(1f),
                            label = method,
                            isSelected = paymentMethod == method,
                            activeColor = PrimaryNavy,
                            activeBg = Slate200,
                            onClick = { paymentMethod = method }
                        )
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(errorMessage!!, color = CrimsonRed, fontSize = 12.sp)
                }

                // Summary Card
                val amt = amountStr.toDoubleOrNull() ?: 0.0
                if (amt > 0 && selectedCustomer != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = EmeraldLight)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Automatic Posting Schedule:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EmeraldGreen)
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Auto-posts ₹$amt ($transactionType) $frequency for ${selectedCustomer!!.fullName}, starting on $startDate" +
                                        if (isIndefinite) " indefinitely." else " until $endDate.",
                                fontSize = 11.sp,
                                color = Navy900
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = Slate600) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (selectedCustomer == null) {
                                errorMessage = "Please select a customer."
                                return@Button
                            }
                            if (amt <= 0) {
                                errorMessage = "Please enter a valid amount."
                                return@Button
                            }
                            if (!isIndefinite && endDate.isBlank()) {
                                errorMessage = "Please specify an end date or choose Indefinite."
                                return@Button
                            }
                            coroutineScope.launch {
                                viewModel.repository.addRecurringTransaction(
                                    customerId = selectedCustomer!!.id,
                                    title = title.trim().ifBlank { "Recurring $frequency $transactionType" },
                                    type = transactionType,
                                    amount = amt,
                                    frequency = frequency,
                                    startDate = startDate.trim(),
                                    endDate = if (isIndefinite) null else endDate.trim(),
                                    isIndefinite = isIndefinite,
                                    paymentMethod = paymentMethod,
                                    description = description.trim()
                                )
                                // Immediately run check in case the start date is today
                                viewModel.processDueRecurringNow()
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("save_recurring_button")
                    ) {
                        Text("Save Recurring Schedule")
                    }
                }
            }
        }
    }
}

@Composable
fun LanguageSelectionDialog(
    currentLanguage: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Select Language / भाषा चुनें",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryNavy
                )
                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    items(AppLanguage.values().toList()) { lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(lang) }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(lang.displayName, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Slate800)
                                Text(lang.englishName, fontSize = 12.sp, color = Slate600)
                            }
                            if (lang == currentLanguage) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldGreen)
                            }
                        }
                        Divider(color = Slate100)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Close", color = Slate600) }
                }
            }
        }
    }
}

@Composable
fun CustomerDetailDialog(
    customer: CustomerEntity,
    viewModel: HisabViewModel,
    onDismiss: () -> Unit
) {
    val (statusLabel, absAmt, _) = FinancialUtils.getBalanceStatus(customer.currentBalance)
    val business by viewModel.currentBusiness.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(customer.fullName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PrimaryNavy)
                        Text("+91 ${customer.mobileNumber}", fontSize = 13.sp, color = Slate600)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate600)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate100)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Current Balance:", fontSize = 13.sp, color = Slate600)
                            Text("$statusLabel ${FinancialUtils.formatInr(absAmt)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (customer.currentBalance >= 0) EmeraldGreen else CrimsonRed)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Received:", fontSize = 12.sp, color = Slate600)
                            Text(FinancialUtils.formatInr(customer.totalReceived), fontSize = 12.sp, color = EmeraldGreen, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Given:", fontSize = 12.sp, color = Slate600)
                            Text(FinancialUtils.formatInr(customer.totalGiven), fontSize = 12.sp, color = CrimsonRed, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            onDismiss()
                            viewModel.showAddHisabSheet.value = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Add Hisab", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            onDismiss()
                            val msg = """
                                Hello ${customer.fullName},
                                Greeting from ${business?.businessName ?: "HisabSaathi"}.
                                Current balance: $statusLabel ${FinancialUtils.formatInr(absAmt)}.
                                
                                Thank you.
                            """.trimIndent()
                            viewModel.openWhatsAppMessage(customer.fullName, customer.whatsappNumber.ifBlank { customer.mobileNumber }, msg)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("WhatsApp", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun BusinessBackupDialog(
    viewModel: HisabViewModel,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val currentUser by viewModel.currentUser.collectAsState()
    val currentBusiness by viewModel.currentBusiness.collectAsState()

    val exportJson by viewModel.backupExportJson.collectAsState()
    val summary by viewModel.backupSummary.collectAsState()
    val statusMessage by viewModel.backupStatusMessage.collectAsState()
    val errorMessage by viewModel.backupError.collectAsState()
    val isLoading by viewModel.backupIsLoading.collectAsState()
    val restoreInput by viewModel.restoreInputJson.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0 = Export, 1 = Restore
    var wipeExisting by remember { mutableStateOf(true) }

    val createDocumentLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.downloadExportedFile(context, uri)
        }
    }

    val openDocumentLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.readAndRestoreFromUri(context, uri, wipeExisting)
        }
    }

    Dialog(onDismissRequest = {
        viewModel.clearBackupState()
        onDismiss()
    }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PrimaryNavy.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Backup, contentDescription = null, tint = PrimaryNavy, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Data Backup & Restore", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Slate800)
                            Text(
                                "Relational JSON Export & Atomic Restore",
                                fontSize = 11.sp,
                                color = Slate600
                            )
                        }
                    }
                    IconButton(onClick = {
                        viewModel.clearBackupState()
                        onDismiss()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate600)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Mode Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Slate100,
                    contentColor = PrimaryNavy,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Export JSON", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Restore JSON", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Authorization Badge
                Surface(
                    color = EmeraldLight.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        val roleText = if (currentUser?.role == "admin") "ADMIN" else "BUSINESS OWNER"
                        Text(
                            "Authorization: $roleText (${currentUser?.mobileNumber ?: "Active"}) • SHA-256 Verified",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = EmeraldGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Status & Error Banners
                if (errorMessage != null) {
                    Surface(
                        color = CrimsonRed.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = CrimsonRed, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(errorMessage!!, color = CrimsonRed, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                if (statusMessage != null) {
                    Surface(
                        color = EmeraldLight,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(statusMessage!!, color = EmeraldGreen, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (selectedTab == 0) {
                        // EXPORT TAB
                        Text("Business: ${currentBusiness?.businessName ?: "My Business"}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Slate800)
                        Text("Generates a complete relational JSON backup containing all customers, ledger transactions, recurring hisab, employees, attendance, and receipts.", fontSize = 12.sp, color = Slate600)

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { viewModel.exportBusinessData() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                            shape = RoundedCornerShape(10.dp),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Exporting...")
                            } else {
                                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generate & Export JSON Backup", fontWeight = FontWeight.Bold)
                            }
                        }

                        if (summary != null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Text("Export Summary", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate800)
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(color = Slate50, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("• Customers: ${summary!!.customerCount}", fontSize = 12.sp, color = Slate800)
                                    Text("• Transactions: ${summary!!.transactionCount}", fontSize = 12.sp, color = Slate800)
                                    Text("• Recurring Hisab: ${summary!!.recurringCount}", fontSize = 12.sp, color = Slate800)
                                    Text("• Employees: ${summary!!.employeeCount}", fontSize = 12.sp, color = Slate800)
                                    Text("• Attendance Records: ${summary!!.attendanceCount}", fontSize = 12.sp, color = Slate800)
                                    Text("• Receipts: ${summary!!.receiptCount}", fontSize = 12.sp, color = Slate800)
                                    if (summary!!.checksum.isNotBlank()) {
                                        Text("• Checksum: ${summary!!.checksum.take(16)}...", fontSize = 11.sp, color = Slate600)
                                    }
                                }
                            }
                        }

                        if (exportJson != null) {
                            Spacer(modifier = Modifier.height(14.dp))

                            // Direct file download via Android SAF
                            Button(
                                onClick = {
                                    val suggestedName = viewModel.getSuggestedExportFileName()
                                    createDocumentLauncher.launch(suggestedName)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Download JSON File to Device", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(exportJson!!))
                                        viewModel.backupStatusMessage.value = "Backup JSON copied to clipboard!"
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy JSON", fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        val sendIntent = android.content.Intent().apply {
                                            action = android.content.Intent.ACTION_SEND
                                            putExtra(android.content.Intent.EXTRA_TEXT, exportJson!!)
                                            type = "text/plain"
                                        }
                                        context.startActivity(android.content.Intent.createChooser(sendIntent, "Share Backup JSON"))
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Share JSON", fontSize = 12.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text("JSON Preview (First 500 chars):", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Slate600)
                            Surface(
                                color = Navy900,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = exportJson!!.take(500) + if (exportJson!!.length > 500) "\n..." else "",
                                    color = EmeraldGreen,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    } else {
                        // RESTORE TAB
                        Text("Restore From Backup JSON", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Slate800)
                        Text(
                            "Import previously exported JSON files. All relational foreign keys (Transactions ⇄ Customers, Attendance ⇄ Employees, Receipts ⇄ Transactions) are strictly validated before committing. If any foreign key is missing or invalid, the restore rolls back completely.",
                            fontSize = 12.sp,
                            color = Slate600
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Safe Relational Constraints info box
                        Surface(
                            color = Slate100,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryNavy, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Relational Constraint Protection", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryNavy)
                                }
                                Text("• Strict Customer ID mapping for transactions & recurring hisab", fontSize = 10.sp, color = Slate600)
                                Text("• Strict Employee ID mapping for attendance records", fontSize = 10.sp, color = Slate600)
                                Text("• Atomic Room transaction: auto-rollback on constraint failure", fontSize = 10.sp, color = Slate600)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { wipeExisting = !wipeExisting }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = wipeExisting,
                                onCheckedChange = { wipeExisting = it },
                                colors = CheckboxDefaults.colors(checkedColor = PrimaryNavy)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text("Clean Restore (Replace existing records)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Slate800)
                                Text("Prevents duplicate keys by replacing current business records.", fontSize = 11.sp, color = Slate600)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Option A: Choose file from device
                        Button(
                            onClick = {
                                openDocumentLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                            shape = RoundedCornerShape(10.dp),
                            enabled = !isLoading
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pick JSON Backup File from Device", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Option B: Or paste raw JSON
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Or Paste JSON Backup Manually", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate800)
                            TextButton(
                                onClick = {
                                    val clip = clipboardManager.getText()
                                    if (clip != null) {
                                        viewModel.restoreInputJson.value = clip.text
                                    }
                                }
                            ) {
                                Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Paste from Clipboard", fontSize = 11.sp)
                            }
                        }

                        OutlinedTextField(
                            value = restoreInput,
                            onValueChange = { viewModel.restoreInputJson.value = it },
                            placeholder = { Text("{\n  \"metadata\": { ... },\n  \"data\": { ... }\n}", fontSize = 12.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            shape = RoundedCornerShape(10.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { viewModel.importBusinessData(restoreInput, wipeExisting) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = if (wipeExisting) CrimsonRed else PrimaryNavy),
                            shape = RoundedCornerShape(10.dp),
                            enabled = !isLoading && restoreInput.isNotBlank()
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Verifying & Restoring...")
                            } else {
                                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (wipeExisting) "Validate & Clean Restore" else "Validate & Restore", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RoleManagementDialog(
    viewModel: HisabViewModel,
    onDismiss: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val currentRole = com.example.hisabsaathi.security.UserRoleManager.normalizeRole(currentUser?.role)

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = PrimaryNavy, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Role & Permissions (RBAC)", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Slate800)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "HisabSaathi provides robust server-side and client-side role-based access control (RBAC). Select a role to test permissions live:",
                    fontSize = 13.sp,
                    color = Slate600
                )

                com.example.hisabsaathi.security.UserRole.values().forEach { role ->
                    val isSelected = currentRole == role
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.updateUserRole(role.code)
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) PrimaryNavy.copy(alpha = 0.1f) else Color.White
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 1.dp),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryNavy) else null
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(role.displayName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = if (isSelected) PrimaryNavy else Slate800)
                                if (isSelected) {
                                    Surface(
                                        color = PrimaryNavy,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("Active Role", modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(role.description, fontSize = 12.sp, color = Slate600)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text("Key Permissions for Active Role (${currentRole.displayName}):", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Slate800)

                com.example.hisabsaathi.security.Permission.values().forEach { permission ->
                    val allowed = com.example.hisabsaathi.security.UserRoleManager.hasPermission(currentUser?.role, permission)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(permission.name.replace("_", " "), fontSize = 12.sp, color = Slate600)
                        Icon(
                            imageVector = if (allowed) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            contentDescription = null,
                            tint = if (allowed) EmeraldGreen else CrimsonRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun CustomerQrScannerDialog(
    customers: List<CustomerEntity>,
    onCustomerScanned: (CustomerEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = PrimaryNavy)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Scan Customer QR Code", fontWeight = FontWeight.Bold, color = PrimaryNavy, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (hasCameraPermission) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black)
                    ) {
                        val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                        val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
                        
                        AndroidView(
                            factory = { ctx ->
                                val previewView = PreviewView(ctx)
                                val executor = Executors.newSingleThreadExecutor()
                                cameraProviderFuture.addListener({
                                    val cameraProvider = cameraProviderFuture.get()
                                    val preview = Preview.Builder().build().also {
                                        it.setSurfaceProvider(previewView.surfaceProvider)
                                    }
                                    val imageAnalyzer = ImageAnalysis.Builder()
                                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                        .build()
                                        .also { analyzer ->
                                            analyzer.setAnalyzer(executor) { imageProxy ->
                                                processImageProxy(imageProxy, customers) { matchedCustomer ->
                                                    onCustomerScanned(matchedCustomer)
                                                }
                                            }
                                        }
                                    try {
                                        cameraProvider.unbindAll()
                                        cameraProvider.bindToLifecycle(
                                            lifecycleOwner,
                                            CameraSelector.DEFAULT_BACK_CAMERA,
                                            preview,
                                            imageAnalyzer
                                        )
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }, ContextCompat.getMainExecutor(ctx))
                                previewView
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Scanner overlay frame
                        Box(
                            modifier = Modifier
                                .size(180.dp)
                                .align(Alignment.Center)
                        ) {
                            Surface(
                                color = Color.Transparent,
                                border = androidx.compose.foundation.BorderStroke(2.dp, EmeraldGreen),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {}
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Align customer QR code within the frame",
                        fontSize = 12.sp,
                        color = Slate600
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(48.dp), tint = Slate400)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Camera permission is required to scan QR codes.", fontSize = 13.sp, color = Slate600)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { launcher.launch(Manifest.permission.CAMERA) },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy)
                            ) {
                                Text("Grant Permission")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Or quick-select customer (Simulator / Test):",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate600
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(customers) { cust ->
                        Surface(
                            color = Slate100,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCustomerScanned(cust) }
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(cust.fullName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate800)
                                Text("+91 ${cust.mobileNumber}", fontSize = 11.sp, color = Slate600)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Slate600)
            }
        }
    )
}

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun processImageProxy(
    imageProxy: ImageProxy,
    customers: List<CustomerEntity>,
    onMatch: (CustomerEntity) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        val scanner = BarcodeScanning.getClient()
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    val rawValue = barcode.rawValue ?: continue
                    val matched = customers.find {
                        rawValue.contains(it.id.toString()) ||
                        rawValue.contains(it.mobileNumber) ||
                        rawValue.contains(it.fullName, ignoreCase = true)
                    }
                    if (matched != null) {
                        onMatch(matched)
                        break
                    }
                }
            }
            .addOnFailureListener {
                // Ignore
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}

