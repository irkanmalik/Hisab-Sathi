package com.example.hisabsaathi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hisabsaathi.i18n.Translations
import com.example.ui.theme.*

@Composable
fun MainShellScreen(
    viewModel: HisabViewModel,
    modifier: Modifier = Modifier
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val language by viewModel.currentLanguage.collectAsState()
    val noticeMessage by viewModel.userNoticeMessage.collectAsState()

    // Dialog state collectors
    val showAddCustomer by viewModel.showAddCustomerDialog.collectAsState()
    val showAddHisab by viewModel.showAddHisabSheet.collectAsState()
    val showAddRecurring by viewModel.showAddRecurringDialog.collectAsState()
    val showAddEmployee by viewModel.showAddEmployeeDialog.collectAsState()
    val showCreatePaymentLink by viewModel.showCreatePaymentLinkDialog.collectAsState()
    val showLanguageDialog by viewModel.showLanguageDialog.collectAsState()
    val showRoleDialog by viewModel.showRoleDialog.collectAsState()
    val showWhatsAppSettings by viewModel.showWhatsAppSettingsDialog.collectAsState()
    val showBackup by viewModel.showBackupDialog.collectAsState()
    val selectedCustomer by viewModel.selectedCustomer.collectAsState()
    val activeWhatsAppPreview by viewModel.activeWhatsAppPreview.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Slate50,
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 6.dp,
                modifier = Modifier.height(64.dp)
            ) {
                NavigationBarItem(
                    selected = currentTab == MainTab.DASHBOARD,
                    onClick = { viewModel.currentTab.value = MainTab.DASHBOARD },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text(Translations.get("home", language), fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryNavy,
                        selectedTextColor = PrimaryNavy,
                        indicatorColor = PrimaryNavy.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.testTag("nav_tab_dashboard")
                )

                NavigationBarItem(
                    selected = currentTab == MainTab.CUSTOMERS,
                    onClick = { viewModel.currentTab.value = MainTab.CUSTOMERS },
                    icon = { Icon(Icons.Default.People, contentDescription = "Customers") },
                    label = { Text(Translations.get("customers", language), fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryNavy,
                        selectedTextColor = PrimaryNavy,
                        indicatorColor = PrimaryNavy.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.testTag("nav_tab_customers")
                )

                NavigationBarItem(
                    selected = currentTab == MainTab.HISAB_LEDGER,
                    onClick = { viewModel.currentTab.value = MainTab.HISAB_LEDGER },
                    icon = { Icon(Icons.Default.MenuBook, contentDescription = "Jhadi") },
                    label = { Text(Translations.get("jhadi", language), fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EmeraldGreen,
                        selectedTextColor = EmeraldGreen,
                        indicatorColor = EmeraldLight
                    ),
                    modifier = Modifier.testTag("nav_tab_jhadi")
                )

                NavigationBarItem(
                    selected = currentTab == MainTab.ATTENDANCE,
                    onClick = { viewModel.currentTab.value = MainTab.ATTENDANCE },
                    icon = { Icon(Icons.Default.Badge, contentDescription = "Attendance") },
                    label = { Text(Translations.get("attendance", language), fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryNavy,
                        selectedTextColor = PrimaryNavy,
                        indicatorColor = PrimaryNavy.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.testTag("nav_tab_attendance")
                )

                NavigationBarItem(
                    selected = currentTab == MainTab.MORE,
                    onClick = { viewModel.currentTab.value = MainTab.MORE },
                    icon = { Icon(Icons.Default.MoreHoriz, contentDescription = "More") },
                    label = { Text(Translations.get("more", language), fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryNavy,
                        selectedTextColor = PrimaryNavy,
                        indicatorColor = PrimaryNavy.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.testTag("nav_tab_more")
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentTab) {
                MainTab.DASHBOARD -> DashboardScreen(viewModel)
                MainTab.CUSTOMERS -> CustomersScreen(viewModel)
                MainTab.HISAB_LEDGER -> HisabLedgerScreen(viewModel)
                MainTab.ATTENDANCE -> AttendanceScreen(viewModel)
                MainTab.MORE -> MoreScreen(viewModel)
            }

            // User Notice Toast Banner (for WhatsApp open confirmation, etc.)
            if (noticeMessage != null) {
                Surface(
                    color = Navy900,
                    shape = RoundedCornerShape(10.dp),
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp, start = 20.dp, end = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(20.dp))
                        Text(noticeMessage!!, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = { viewModel.userNoticeMessage.value = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }

    // Modal Overlays
    if (showAddCustomer) {
        AddCustomerDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.showAddCustomerDialog.value = false }
        )
    }

    if (showAddHisab) {
        AddHisabBottomSheet(
            viewModel = viewModel,
            onDismiss = { viewModel.showAddHisabSheet.value = false }
        )
    }

    if (showAddRecurring) {
        AddRecurringTransactionDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.showAddRecurringDialog.value = false }
        )
    }

    if (showAddEmployee) {
        AddEmployeeDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.showAddEmployeeDialog.value = false }
        )
    }

    if (showCreatePaymentLink) {
        CreatePaymentLinkDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.showCreatePaymentLinkDialog.value = false }
        )
    }

    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguage = language,
            onSelect = {
                viewModel.setLanguage(it)
                viewModel.showLanguageDialog.value = false
            },
            onDismiss = { viewModel.showLanguageDialog.value = false }
        )
    }

    if (showWhatsAppSettings) {
        AlertDialog(
            onDismissRequest = { viewModel.showWhatsAppSettingsDialog.value = false },
            title = { Text("WhatsApp API Provider Settings") },
            text = { WhatsAppSettingsSubView(viewModel) },
            confirmButton = {
                TextButton(onClick = { viewModel.showWhatsAppSettingsDialog.value = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showBackup) {
        BusinessBackupDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.showBackupDialog.value = false }
        )
    }

    if (showRoleDialog) {
        RoleManagementDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.showRoleDialog.value = false }
        )
    }

    if (activeWhatsAppPreview != null) {
        WhatsAppMessageReadyDialog(
            previewData = activeWhatsAppPreview!!,
            viewModel = viewModel,
            onDismiss = { viewModel.activeWhatsAppPreview.value = null }
        )
    }

    if (selectedCustomer != null && !showAddHisab) {
        CustomerDetailDialog(
            customer = selectedCustomer!!,
            viewModel = viewModel,
            onDismiss = { viewModel.selectedCustomer.value = null }
        )
    }
}
