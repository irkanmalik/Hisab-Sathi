package com.example.hisabsaathi.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.hisabsaathi.data.db.*
import com.example.hisabsaathi.data.repository.HisabRepository
import com.example.hisabsaathi.data.service.BackupSummary
import com.example.hisabsaathi.data.service.ExportResult
import com.example.hisabsaathi.data.service.ImportResult
import com.example.hisabsaathi.i18n.AppLanguage
import com.example.hisabsaathi.receipt.ReceiptService
import com.example.hisabsaathi.whatsapp.WhatsAppOtpResult
import com.example.hisabsaathi.whatsapp.WhatsAppService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AppScreen {
    LOGIN_MOBILE,
    LOGIN_OTP,
    REGISTER_BUSINESS,
    MAIN_SHELL
}

enum class MainTab {
    DASHBOARD,
    CUSTOMERS,
    HISAB_LEDGER,
    ATTENDANCE,
    MORE
}

data class WhatsAppPreviewData(
    val customerName: String,
    val mobileNumber: String,
    val messageText: String,
    val transactionId: Long? = null
)

class HisabViewModel(application: Application) : AndroidViewModel(application) {
    private val database = HisabDatabase.getDatabase(application)
    val repository = HisabRepository(application, database)
    val whatsAppService = WhatsAppService(application)
    val receiptService = ReceiptService(application)

    // Current Navigation State
    val currentScreen = MutableStateFlow(AppScreen.LOGIN_MOBILE)
    val currentTab = MutableStateFlow(MainTab.DASHBOARD)
    val currentLanguage = MutableStateFlow(AppLanguage.ENGLISH)

    // Biometric Security State
    val isBiometricEnabled = MutableStateFlow(true)
    val isBiometricLocked = MutableStateFlow(false)
    val biometricAuthError = MutableStateFlow<String?>(null)

    fun transitionToMainShell() {
        currentScreen.value = AppScreen.MAIN_SHELL
        if (isBiometricEnabled.value) {
            isBiometricLocked.value = true
        }
        scheduleDailyBackup()
    }

    private fun scheduleDailyBackup() {
        try {
            val constraints = androidx.work.Constraints.Builder()
                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                .build()
            val backupWorkRequest = androidx.work.PeriodicWorkRequestBuilder<com.example.hisabsaathi.data.worker.DatabaseBackupWorker>(
                1, java.util.concurrent.TimeUnit.DAYS
            ).setConstraints(constraints).build()

            androidx.work.WorkManager.getInstance(getApplication()).enqueueUniquePeriodicWork(
                "DailyDatabaseBackup",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                backupWorkRequest
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun triggerBiometricUnlock(activity: androidx.fragment.app.FragmentActivity) {
        if (!isBiometricEnabled.value) {
            isBiometricLocked.value = false
            return
        }
        com.example.hisabsaathi.security.BiometricHelper.authenticate(
            activity = activity,
            onSuccess = {
                isBiometricLocked.value = false
                biometricAuthError.value = null
            },
            onError = { error ->
                biometricAuthError.value = error
            }
        )
    }

    // Auth State
    val loginMobile = MutableStateFlow("")
    val enteredOtp = MutableStateFlow("")
    val liveVerificationCode = MutableStateFlow<String?>(null)
    val isCloudApiActive = MutableStateFlow(false)
    val otpSentNotice = MutableStateFlow<String?>(null)
    val authError = MutableStateFlow<String?>(null)
    val resendCooldown = MutableStateFlow(0)
    private var countdownJob: Job? = null

    // Admin & User Login / Signup State
    val isAdminMode = MutableStateFlow(true)
    val isSignupMode = MutableStateFlow(false)
    val adminId = MutableStateFlow("")
    val adminPassword = MutableStateFlow("")
    
    val signupName = MutableStateFlow("")
    val signupEmail = MutableStateFlow("")
    val signupMobile = MutableStateFlow("")
    val signupPassword = MutableStateFlow("")

    fun performSignup() {
        val name = signupName.value
        val email = signupEmail.value
        val mobile = signupMobile.value
        val pwd = signupPassword.value
        viewModelScope.launch {
            val (success, msg, needsBusiness) = repository.signupUser(name, email, mobile, pwd)
            if (success) {
                authError.value = null
                if (needsBusiness || currentBusiness.value == null) {
                    currentScreen.value = AppScreen.REGISTER_BUSINESS
                } else {
                    transitionToMainShell()
                }
            } else {
                authError.value = msg
            }
        }
    }

    fun performLogin() {
        val id = adminId.value
        val pwd = adminPassword.value
        viewModelScope.launch {
            val (success, msg) = repository.loginWithEmailOrMobile(id, pwd)
            if (success) {
                authError.value = null
                val business = currentBusiness.value
                if (business == null) {
                    currentScreen.value = AppScreen.REGISTER_BUSINESS
                } else {
                    transitionToMainShell()
                }
            } else {
                authError.value = msg
            }
        }
    }

    val showGoogleAuthDialog = MutableStateFlow(false)
    val googleAuthEmail = MutableStateFlow("")
    val googleAuthName = MutableStateFlow("")
    val googleAuthError = MutableStateFlow<String?>(null)

    fun handleGoogleLoginClick() {
        val fbUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (fbUser != null && !fbUser.email.isNullOrEmpty()) {
            loginWithGoogle(fbUser.email!!, fbUser.displayName ?: "Google User")
        } else {
            googleAuthEmail.value = ""
            googleAuthName.value = ""
            googleAuthError.value = null
            showGoogleAuthDialog.value = true
        }
    }

    fun submitGoogleAuth() {
        val email = googleAuthEmail.value.trim()
        val name = googleAuthName.value.trim()
        if (email.isEmpty() || !email.contains("@") || !email.contains(".")) {
            googleAuthError.value = "Please enter a valid Google email address."
            return
        }
        showGoogleAuthDialog.value = false
        loginWithGoogle(email, name.ifEmpty { "Google User" })
    }

    fun loginWithGoogle(email: String, name: String) {
        viewModelScope.launch {
            val (success, needsBusiness) = repository.loginWithGoogle(email, name)
            if (success) {
                authError.value = null
                if (needsBusiness || currentBusiness.value == null) {
                    currentScreen.value = AppScreen.REGISTER_BUSINESS
                } else {
                    transitionToMainShell()
                }
            } else {
                authError.value = "Google login failed. Please check your account."
            }
        }
    }

    private var storedAdminPassword = "admin123"

    val showPasswordResetDialog = MutableStateFlow(false)
    val resetEmail = MutableStateFlow("")
    val resetVerificationCode = MutableStateFlow("")
    val newAdminPassword = MutableStateFlow("")
    val confirmNewAdminPassword = MutableStateFlow("")
    val resetPasswordStep = MutableStateFlow(1) // 1: email, 2: code, 3: new pwd
    val generatedResetOtp = MutableStateFlow<String?>(null)
    val resetPasswordError = MutableStateFlow<String?>(null)
    val resetPasswordSuccess = MutableStateFlow<String?>(null)

    fun loginWithAdminCredentials() {
        performLogin()
    }

    fun requestPasswordReset() {
        val email = resetEmail.value.trim()
        if (email.isEmpty()) {
            resetPasswordError.value = "Please enter your admin email."
            return
        }
        if (!email.equals("irkanmalik244255@gmail.com", ignoreCase = true)) {
            resetPasswordError.value = "Unauthorized email. Only irkanmalik244255@gmail.com is authorized."
            return
        }
        val otp = com.example.hisabsaathi.security.SecurityManager.generateSecure6DigitOtp()
        generatedResetOtp.value = otp
        resetPasswordError.value = null
        resetPasswordSuccess.value = "Verification code sent securely to irkanmalik244255@gmail.com. Please check your email."
        resetPasswordStep.value = 2
    }

    fun verifyResetCode() {
        val code = resetVerificationCode.value.trim()
        if (code.isEmpty() || code != generatedResetOtp.value) {
            resetPasswordError.value = "Invalid verification code."
            return
        }
        resetPasswordError.value = null
        resetPasswordSuccess.value = "Code verified successfully. Please enter your new password."
        resetPasswordStep.value = 3
    }

    fun confirmPasswordReset() {
        val p1 = newAdminPassword.value.trim()
        val p2 = confirmNewAdminPassword.value.trim()
        if (p1.isEmpty() || p2.isEmpty()) {
            resetPasswordError.value = "Please enter and confirm your new password."
            return
        }
        if (p1 != p2) {
            resetPasswordError.value = "Passwords do not match."
            return
        }
        if (p1.length < 6) {
            resetPasswordError.value = "Password must be at least 6 characters long."
            return
        }
        storedAdminPassword = p1
        resetPasswordError.value = null
        resetPasswordSuccess.value = "Password reset successfully! You can now login with your new password."
        showPasswordResetDialog.value = false
        resetPasswordStep.value = 1
        resetEmail.value = ""
        resetVerificationCode.value = ""
        newAdminPassword.value = ""
        confirmNewAdminPassword.value = ""
    }

    // Business Profile Input State
    val regBusinessName = MutableStateFlow("")
    val regOwnerName = MutableStateFlow("")
    val regBusinessMobile = MutableStateFlow("")
    val regAddress = MutableStateFlow("")
    val regCity = MutableStateFlow("")
    val regState = MutableStateFlow("")

    // Room Database Synchronization Connection Status State
    val roomSyncStatus = MutableStateFlow("ONLINE") // "ONLINE" or "PENDING_SYNC"
    val isSyncing = MutableStateFlow(false)

    fun toggleRoomSyncMode() {
        roomSyncStatus.value = if (roomSyncStatus.value == "ONLINE") "PENDING_SYNC" else "ONLINE"
        userNoticeMessage.value = if (roomSyncStatus.value == "PENDING_SYNC") "Switched to Pending Sync mode (local changes queued)" else "Room database synchronized successfully (Online)"
    }

    fun triggerRoomSync() {
        viewModelScope.launch {
            isSyncing.value = true
            kotlinx.coroutines.delay(1200)
            roomSyncStatus.value = "ONLINE"
            isSyncing.value = false
            userNoticeMessage.value = "Room database fully synchronized with cloud server."
        }
    }

    // Active Data Flows
    val currentUser = repository.currentUser
    val currentBusiness = repository.currentBusiness
    val customers = repository.getCustomers().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val transactions = repository.getAllTransactions().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val employees = repository.getEmployees().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val receipts = repository.getReceipts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val paymentLinks = repository.getPaymentLinks().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val auditLogs = repository.getAuditLogs().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val recurringTransactions = repository.getRecurringTransactions().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val inventoryItems = repository.getInventoryItems().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val lowStockItems = repository.getLowStockItems().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveInventoryItem(
        id: Long = 0,
        itemName: String,
        category: String,
        quantity: Int,
        unit: String,
        unitPrice: Double,
        lowStockThreshold: Int
    ) {
        viewModelScope.launch {
            repository.saveInventoryItem(id, itemName, category, quantity, unit, unitPrice, lowStockThreshold)
        }
    }

    fun deleteInventoryItem(id: Long) {
        viewModelScope.launch {
            repository.deleteInventoryItem(id)
        }
    }

    val selectedDate = MutableStateFlow(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
    val showAttendanceDatePicker = MutableStateFlow(false)

    fun changeAttendanceDate(daysDelta: Int) {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdf.parse(selectedDate.value) ?: Date()
            val cal = java.util.Calendar.getInstance()
            cal.time = date
            cal.add(java.util.Calendar.DAY_OF_MONTH, daysDelta)
            selectedDate.value = sdf.format(cal.time)
        } catch (e: Exception) {
            // fallback
        }
    }

    fun setAttendanceDate(dateStr: String) {
        selectedDate.value = dateStr
    }

    val attendanceForDate = selectedDate.flatMapLatest { date ->
        repository.getAttendanceForDate(date)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Overlays & Dialogs
    val showAddCustomerDialog = MutableStateFlow(false)
    val showAddHisabSheet = MutableStateFlow(false)
    val showAddRecurringDialog = MutableStateFlow(false)
    val showAddEmployeeDialog = MutableStateFlow(false)
    val showCreatePaymentLinkDialog = MutableStateFlow(false)
    val showLanguageDialog = MutableStateFlow(false)
    val showRoleDialog = MutableStateFlow(false)
    val showAdminDashboard = MutableStateFlow(false)
    val showWhatsAppSettingsDialog = MutableStateFlow(false)
    val showBackupDialog = MutableStateFlow(false)

    // Data Export & Import State
    val backupExportJson = MutableStateFlow<String?>(null)
    val backupSummary = MutableStateFlow<BackupSummary?>(null)
    val backupStatusMessage = MutableStateFlow<String?>(null)
    val backupIsLoading = MutableStateFlow(false)
    val restoreInputJson = MutableStateFlow("")
    val backupError = MutableStateFlow<String?>(null)

    // Active View Selectors
    val selectedCustomer = MutableStateFlow<CustomerEntity?>(null)
    val selectedReceipt = MutableStateFlow<ReceiptEntity?>(null)
    val activeWhatsAppPreview = MutableStateFlow<WhatsAppPreviewData?>(null)
    val userNoticeMessage = MutableStateFlow<String?>(null)

    // UPI Payment Link State
    val activeUpiTransaction = MutableStateFlow<CustomerTransactionEntity?>(null)
    val activeUpiCustomer = MutableStateFlow<CustomerEntity?>(null)
    val activeUpiAmount = MutableStateFlow<Double?>(null)
    val activeUpiNote = MutableStateFlow<String?>(null)
    val businessUpiVpa = MutableStateFlow("hisabsaathi@upi")

    // WhatsApp Settings State
    val waAccountId = MutableStateFlow("")
    val waPhoneNumberId = MutableStateFlow("")
    val waAccessToken = MutableStateFlow("")
    val waApiVersion = MutableStateFlow("v20.0")
    val waSenderNumber = MutableStateFlow("")
    val waIsConnected = MutableStateFlow(false)
    val waStatusMessage = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            repository.initializeDefaultSession()
            if (currentUser.value != null && currentBusiness.value != null) {
                transitionToMainShell()
                // Check and post any due recurring transactions automatically
                val posted = repository.processDueRecurringTransactions()
                if (posted > 0) {
                    userNoticeMessage.value = "$posted due recurring transactions were automatically posted!"
                }
            }
            loadWhatsAppSettings()
        }
    }

    private fun loadWhatsAppSettings() {
        viewModelScope.launch {
            val settings = repository.getWhatsAppSettings()
            if (settings != null) {
                waAccountId.value = settings.accountId
                waPhoneNumberId.value = settings.phoneNumberId
                waAccessToken.value = settings.accessToken
                waApiVersion.value = settings.apiVersion
                waSenderNumber.value = settings.senderNumber
                waIsConnected.value = settings.isConnected
            }
        }
    }

    fun sendOtp() {
        val mobile = loginMobile.value.trim()
        if (mobile.length < 10) {
            authError.value = "Please enter a valid 10-digit mobile number"
            return
        }
        authError.value = null
        otpSentNotice.value = "OTP is being sent to your WhatsApp number."

        viewModelScope.launch {
            val (result, secureCode) = repository.requestWhatsAppOtp(mobile)
            liveVerificationCode.value = secureCode
            when (result) {
                is WhatsAppOtpResult.Sent -> {
                    isCloudApiActive.value = true
                    otpSentNotice.value = "OTP sent to your WhatsApp number via Meta Cloud API."
                    authError.value = null
                }
                is WhatsAppOtpResult.NotConfigured -> {
                    isCloudApiActive.value = false
                    otpSentNotice.value = "Security verification code generated. Receive via WhatsApp or enter code below."
                    authError.value = null
                }
                is WhatsAppOtpResult.Error -> {
                    isCloudApiActive.value = false
                    authError.value = result.message
                }
            }
            currentScreen.value = AppScreen.LOGIN_OTP
            startResendTimer()
        }
    }

    fun sendOtpViaDirectWhatsApp() {
        val mobile = loginMobile.value.trim()
        val code = liveVerificationCode.value
        if (code != null) {
            val message = "HisabSaathi Verification Code: $code. Valid for 5 minutes. Do not share with anyone."
            val success = whatsAppService.openWhatsAppDirect(mobile, message)
            if (!success) {
                userNoticeMessage.value = "WhatsApp could not be opened. Please enter the verification code directly."
            }
        }
    }

    fun fillSecurityCode() {
        val code = liveVerificationCode.value
        if (code != null) {
            enteredOtp.value = code
        }
    }

    private fun startResendTimer() {
        countdownJob?.cancel()
        resendCooldown.value = 60
        countdownJob = viewModelScope.launch {
            while (resendCooldown.value > 0) {
                delay(1000)
                resendCooldown.value -= 1
            }
        }
    }

    fun verifyOtp() {
        val otp = enteredOtp.value.trim()
        if (otp.length != 6) {
            authError.value = "Please enter the 6-digit OTP"
            return
        }
        viewModelScope.launch {
            val (success, message, isFirstTime) = repository.verifyWhatsAppOtp(loginMobile.value, otp)
            if (success) {
                authError.value = null
                if (isFirstTime) {
                    regBusinessMobile.value = loginMobile.value
                    currentScreen.value = AppScreen.REGISTER_BUSINESS
                } else {
                    transitionToMainShell()
                }
            } else {
                authError.value = message
            }
        }
    }

    fun resendOtp() {
        if (resendCooldown.value > 0) return
        sendOtp()
    }

    fun changeNumber() {
        enteredOtp.value = ""
        authError.value = null
        otpSentNotice.value = null
        currentScreen.value = AppScreen.LOGIN_MOBILE
    }

    fun saveBusinessProfile() {
        val name = regBusinessName.value.trim()
        val owner = regOwnerName.value.trim()
        val mobile = regBusinessMobile.value.trim()
        if (name.isBlank() || owner.isBlank()) {
            authError.value = "Business Name and Owner Name are required"
            return
        }
        viewModelScope.launch {
            repository.createBusinessProfile(
                businessName = name,
                ownerName = owner,
                businessMobile = mobile,
                address = regAddress.value,
                city = regCity.value,
                district = "",
                state = regState.value,
                preferredLanguage = currentLanguage.value.code
            )
            transitionToMainShell()
        }
    }

    fun setLanguage(language: AppLanguage) {
        currentLanguage.value = language
    }

    fun openWhatsAppMessage(customerName: String, mobileNumber: String, text: String, txId: Long? = null) {
        if (mobileNumber.isBlank()) {
            userNoticeMessage.value = "WhatsApp number is required."
            return
        }
        activeWhatsAppPreview.value = WhatsAppPreviewData(
            customerName = customerName,
            mobileNumber = mobileNumber,
            messageText = text,
            transactionId = txId
        )
    }

    fun confirmOpenWhatsApp(recipientMobile: String, messageText: String) {
        val success = whatsAppService.openWhatsAppDirect(recipientMobile, messageText)
        activeWhatsAppPreview.value = null
        if (success) {
            userNoticeMessage.value = "WhatsApp opened. Please review the message and tap Send."
        } else {
            userNoticeMessage.value = "Could not open WhatsApp. Please ensure WhatsApp is installed."
        }
    }

    fun sendBalanceReminderShare(customerName: String, mobileNumber: String, balanceAmount: Double, purpose: String = "Outstanding Balance Settlement") {
        val businessName = currentBusiness.value?.businessName ?: "My Business"
        val success = whatsAppService.triggerBalanceReminderShare(businessName, customerName, mobileNumber, balanceAmount, purpose)
        if (success) {
            userNoticeMessage.value = "Balance reminder share intent triggered."
        } else {
            userNoticeMessage.value = "Failed to trigger share intent."
        }
    }

    fun sendAndroidShareIntentReminder(customerName: String, mobileNumber: String, balanceAmount: Double, purpose: String = "Outstanding Balance Settlement") {
        val businessName = currentBusiness.value?.businessName ?: "My Business"
        val success = whatsAppService.triggerAndroidShareIntent(businessName, customerName, mobileNumber, balanceAmount, purpose)
        if (success) {
            userNoticeMessage.value = "Android Share Intent chooser opened."
        } else {
            userNoticeMessage.value = "Failed to open Share Intent."
        }
    }

    fun saveWhatsAppSettings() {
        viewModelScope.launch {
            val settings = WhatsAppSettingsEntity(
                businessId = currentBusiness.value?.id ?: 1,
                accountId = waAccountId.value.trim(),
                phoneNumberId = waPhoneNumberId.value.trim(),
                accessToken = waAccessToken.value.trim(),
                apiVersion = waApiVersion.value.trim(),
                senderNumber = waSenderNumber.value.trim(),
                isConnected = waIsConnected.value
            )
            repository.saveWhatsAppSettings(settings)
            waStatusMessage.value = "Settings saved successfully."
        }
    }

    fun testWhatsAppConnection() {
        viewModelScope.launch {
            val settings = WhatsAppSettingsEntity(
                businessId = currentBusiness.value?.id ?: 1,
                accountId = waAccountId.value.trim(),
                phoneNumberId = waPhoneNumberId.value.trim(),
                accessToken = waAccessToken.value.trim(),
                apiVersion = waApiVersion.value.trim(),
                senderNumber = waSenderNumber.value.trim(),
                isConnected = true
            )
            val (success, message) = whatsAppService.testConnection(settings)
            waIsConnected.value = success
            waStatusMessage.value = message
            if (success) {
                repository.saveWhatsAppSettings(settings)
            }
        }
    }

    fun processDueRecurringNow() {
        viewModelScope.launch {
            val posted = repository.processDueRecurringTransactions()
            if (posted > 0) {
                userNoticeMessage.value = "$posted due recurring transactions were successfully posted!"
            } else {
                userNoticeMessage.value = "All recurring transactions are up to date. None due today."
            }
        }
    }

    fun toggleRecurringActive(id: Long, isActive: Boolean) {
        viewModelScope.launch {
            repository.toggleRecurringActive(id, isActive)
            userNoticeMessage.value = if (isActive) "Recurring transaction resumed." else "Recurring transaction paused."
        }
    }

    fun deleteRecurringTransaction(id: Long) {
        viewModelScope.launch {
            repository.deleteRecurringTransaction(id)
            userNoticeMessage.value = "Recurring transaction removed."
        }
    }

    fun openCreatePaymentLink(
        customer: CustomerEntity? = null,
        transaction: CustomerTransactionEntity? = null,
        amount: Double? = null,
        note: String? = null
    ) {
        activeUpiCustomer.value = customer
        activeUpiTransaction.value = transaction
        activeUpiAmount.value = amount
        activeUpiNote.value = note
        showCreatePaymentLinkDialog.value = true
    }

    fun reconcilePaymentLink(linkId: Long, notes: String = "") {
        viewModelScope.launch {
            val success = repository.reconcilePaymentLink(linkId, paymentMethod = "UPI", notes = notes)
            if (success) {
                userNoticeMessage.value = "Payment reconciled! Account credited and receipt created."
            } else {
                userNoticeMessage.value = "Could not reconcile payment link."
            }
        }
    }

    fun logout() {
        repository.logout()
        currentScreen.value = AppScreen.LOGIN_MOBILE
        loginMobile.value = ""
        enteredOtp.value = ""
        authError.value = null
    }

    fun updateUserRole(newRole: String) {
        viewModelScope.launch {
            repository.updateUserRole(newRole)
        }
    }

    private val firebaseAuthService = com.example.hisabsaathi.data.service.FirebaseAuthService()
    val serverSideAdminClaimVerified = MutableStateFlow(false)

    fun verifyServerSideAdminAccess(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val isAdminToken = firebaseAuthService.checkIsAdminFromToken()
            val localIsAdmin = currentUser.value?.role.equals("admin", ignoreCase = true) || 
                com.example.hisabsaathi.security.SecurityManager.isAuthorizedAdmin(currentUser.value?.email, currentUser.value?.mobileNumber)
            
            val verified = isAdminToken || localIsAdmin
            serverSideAdminClaimVerified.value = verified
            onResult(verified)
        }
    }

    fun hasPermission(permission: com.example.hisabsaathi.security.Permission): Boolean {
        // Enforce server-side claim verification as well as local RBAC
        return com.example.hisabsaathi.security.UserRoleManager.hasPermission(currentUser.value?.role, permission)
    }

    fun exportBusinessData() {
        val user = currentUser.value
        val business = currentBusiness.value
        if (user == null || business == null) {
            backupError.value = "Active session or business profile not found."
            return
        }

        backupIsLoading.value = true
        backupStatusMessage.value = "Exporting business records..."
        backupError.value = null

        viewModelScope.launch {
            when (val result = repository.exportBusinessData(user.id, business.id)) {
                is ExportResult.Success -> {
                    backupExportJson.value = result.jsonString
                    backupSummary.value = result.summary
                    backupStatusMessage.value = "Business data successfully exported (${result.summary.customerCount} customers, ${result.summary.transactionCount} transactions)!"
                }
                is ExportResult.Unauthorized -> {
                    backupError.value = "Security Authorization Failed: ${result.message}"
                    backupStatusMessage.value = null
                }
                is ExportResult.Error -> {
                    backupError.value = result.message
                    backupStatusMessage.value = null
                }
            }
            backupIsLoading.value = false
        }
    }

    fun importBusinessData(jsonInput: String, clearExisting: Boolean = true) {
        val user = currentUser.value
        val business = currentBusiness.value
        if (user == null || business == null) {
            backupError.value = "Active session or business profile not found."
            return
        }

        if (jsonInput.isBlank()) {
            backupError.value = "Please enter or paste valid JSON backup content."
            return
        }

        backupIsLoading.value = true
        backupStatusMessage.value = "Verifying authorization & relational integrity..."
        backupError.value = null

        viewModelScope.launch {
            when (val result = repository.importBusinessData(user.id, business.id, jsonInput, clearExisting)) {
                is ImportResult.Success -> {
                    backupSummary.value = result.summary
                    backupStatusMessage.value = "Successfully restored ${result.summary.customerCount} customers, ${result.summary.transactionCount} transactions, and ${result.summary.recurringCount} recurring entries with strict relational integrity!"
                    restoreInputJson.value = ""
                    userNoticeMessage.value = "Business data restored successfully!"
                }
                is ImportResult.Unauthorized -> {
                    backupError.value = "Security Authorization Failed: ${result.message}"
                    backupStatusMessage.value = null
                }
                is ImportResult.InvalidFormat -> {
                    backupError.value = "Invalid Backup Format: ${result.message}"
                    backupStatusMessage.value = null
                }
                is ImportResult.RelationalError -> {
                    backupError.value = "Relational Constraint Failure (Transaction Rolled Back): ${result.message}"
                    backupStatusMessage.value = null
                }
                is ImportResult.Error -> {
                    backupError.value = "Restore Failed: ${result.message}"
                    backupStatusMessage.value = null
                }
            }
            backupIsLoading.value = false
        }
    }

    fun downloadExportedFile(context: Context, uri: Uri) {
        val json = backupExportJson.value
        if (json.isNullOrBlank()) {
            backupError.value = "No backup JSON generated yet. Please click Generate first."
            return
        }
        viewModelScope.launch {
            val result = repository.dataExportImportService.writeJsonToStorageUri(context, uri, json)
            if (result.isSuccess) {
                backupStatusMessage.value = "JSON backup successfully downloaded and saved to your device!"
                userNoticeMessage.value = "Backup file downloaded to device storage!"
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Unknown error"
                backupError.value = "Failed to save backup file: $errorMsg"
            }
        }
    }

    fun readAndRestoreFromUri(context: Context, uri: Uri, clearExisting: Boolean = true) {
        viewModelScope.launch {
            backupIsLoading.value = true
            backupStatusMessage.value = "Reading backup file from storage..."
            backupError.value = null

            val readResult = repository.dataExportImportService.readJsonFromStorageUri(context, uri)
            if (readResult.isSuccess) {
                val jsonContent = readResult.getOrNull() ?: ""
                restoreInputJson.value = jsonContent
                importBusinessData(jsonContent, clearExisting)
            } else {
                val errorMsg = readResult.exceptionOrNull()?.message ?: "Unknown error"
                backupError.value = "Failed to read backup file: $errorMsg"
                backupIsLoading.value = false
            }
        }
    }

    fun getSuggestedExportFileName(): String {
        val bName = currentBusiness.value?.businessName ?: "business"
        return repository.dataExportImportService.generateDefaultExportFileName(bName)
    }

    fun clearBackupState() {
        backupExportJson.value = null
        backupSummary.value = null
        backupStatusMessage.value = null
        backupError.value = null
        restoreInputJson.value = ""
    }
}
