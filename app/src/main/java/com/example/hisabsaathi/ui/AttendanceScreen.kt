package com.example.hisabsaathi.ui

import androidx.compose.foundation.background
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
import com.example.hisabsaathi.data.db.AttendanceEntity
import com.example.hisabsaathi.data.db.EmployeeEntity
import com.example.hisabsaathi.i18n.Translations
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AttendanceScreen(
    viewModel: HisabViewModel,
    modifier: Modifier = Modifier
) {
    val language by viewModel.currentLanguage.collectAsState()
    val employees by viewModel.employees.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val attendanceList by viewModel.attendanceForDate.collectAsState()
    val business by viewModel.currentBusiness.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val presentCount = attendanceList.count { it.status == "PRESENT" }
    val absentCount = attendanceList.count { it.status == "ABSENT" }
    val halfDayCount = attendanceList.count { it.status == "HALF_DAY" }
    val leaveCount = attendanceList.count { it.status == "LEAVE" }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Slate50,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.showAddEmployeeDialog.value = true },
                containerColor = PrimaryNavy,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                text = { Text(Translations.get("add_employee", language), fontWeight = FontWeight.SemiBold) },
                modifier = Modifier.padding(bottom = 70.dp).testTag("fab_add_employee")
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

            // Date Bar & Summary
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Daily Attendance Roster", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Slate800)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = { viewModel.changeAttendanceDate(-1) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Day", tint = PrimaryNavy)
                                }
                                Text(selectedDate, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = PrimaryNavy)
                                IconButton(
                                    onClick = { viewModel.changeAttendanceDate(1) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Day", tint = PrimaryNavy)
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            OutlinedButton(
                                onClick = { 
                                    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                    viewModel.setAttendanceDate(today)
                                },
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Text("Today", fontSize = 11.sp)
                            }
                            IconButton(
                                onClick = { viewModel.showAttendanceDatePicker.value = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.DateRange, contentDescription = "Pick Date", tint = PrimaryNavy)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        AttendanceSummaryPill(label = "Present", count = presentCount, color = EmeraldGreen, bg = EmeraldLight)
                        AttendanceSummaryPill(label = "Absent", count = absentCount, color = CrimsonRed, bg = CrimsonLight)
                        AttendanceSummaryPill(label = "Half Day", count = halfDayCount, color = SaffronGold, bg = SaffronLight)
                        AttendanceSummaryPill(label = "Leave", count = leaveCount, color = Slate600, bg = Slate200)
                    }
                }
            }

            val showDatePicker by viewModel.showAttendanceDatePicker.collectAsState()
            if (showDatePicker) {
                AttendanceDatePickerDialog(viewModel = viewModel)
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (employees.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No staff members added yet. Tap '+ Add Employee' to manage staff and daily attendance.",
                        color = Slate600,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    items(employees) { emp ->
                        val currentAttendance = attendanceList.find { it.employeeId == emp.id }
                        EmployeeAttendanceCard(
                            employee = emp,
                            attendance = currentAttendance,
                            onStatusChange = { newStatus ->
                                coroutineScope.launch {
                                    val checkInTime = if (newStatus == "PRESENT" || newStatus == "HALF_DAY") {
                                        currentAttendance?.checkInTime?.ifBlank { "09:30 AM" } ?: "09:30 AM"
                                    } else ""
                                    val hours = if (newStatus == "PRESENT") 8.0 else if (newStatus == "HALF_DAY") 4.0 else 0.0
                                    viewModel.repository.markAttendance(
                                        employeeId = emp.id,
                                        date = selectedDate,
                                        status = newStatus,
                                        checkIn = checkInTime,
                                        checkOut = "",
                                        workingHours = hours
                                    )
                                }
                            },
                            onWhatsAppNotify = {
                                val status = currentAttendance?.status ?: "PRESENT"
                                val checkIn = currentAttendance?.checkInTime?.ifBlank { "09:30 AM" } ?: "09:30 AM"
                                val msg = """
                                    Employee: ${emp.employeeName}
                                    Date: $selectedDate
                                    Status: $status
                                    Check-in: $checkIn
                                    
                                    ${business?.businessName ?: "HisabSaathi"}
                                """.trimIndent()
                                viewModel.openWhatsAppMessage(emp.employeeName, emp.whatsappNumber.ifBlank { emp.mobile }, msg)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AttendanceSummaryPill(label: String, count: Int, color: Color, bg: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bg
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = count.toString(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
            Text(text = label, fontSize = 10.sp, color = color)
        }
    }
}

@Composable
fun EmployeeAttendanceCard(
    employee: EmployeeEntity,
    attendance: AttendanceEntity?,
    onStatusChange: (String) -> Unit,
    onWhatsAppNotify: () -> Unit
) {
    val currentStatus = attendance?.status ?: "NONE"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
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
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(PrimaryNavy.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = employee.employeeName.take(1).uppercase(),
                            color = PrimaryNavy,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Column {
                        Text(employee.employeeName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Slate800)
                        Text("${employee.position} • ID: ${employee.employeeIdCode}", fontSize = 11.sp, color = Slate600)
                    }
                }

                IconButton(
                    onClick = onWhatsAppNotify,
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(EmeraldLight)
                ) {
                    Icon(Icons.Default.Chat, contentDescription = "Notify on WhatsApp", tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Attendance Status Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StatusChoiceChip(
                    modifier = Modifier.weight(1f),
                    label = "Present",
                    isSelected = currentStatus == "PRESENT",
                    activeColor = EmeraldGreen,
                    activeBg = EmeraldLight,
                    onClick = { onStatusChange("PRESENT") }
                )
                StatusChoiceChip(
                    modifier = Modifier.weight(1f),
                    label = "Absent",
                    isSelected = currentStatus == "ABSENT",
                    activeColor = CrimsonRed,
                    activeBg = CrimsonLight,
                    onClick = { onStatusChange("ABSENT") }
                )
                StatusChoiceChip(
                    modifier = Modifier.weight(1f),
                    label = "Half Day",
                    isSelected = currentStatus == "HALF_DAY",
                    activeColor = SaffronGold,
                    activeBg = SaffronLight,
                    onClick = { onStatusChange("HALF_DAY") }
                )
                StatusChoiceChip(
                    modifier = Modifier.weight(1f),
                    label = "Leave",
                    isSelected = currentStatus == "LEAVE",
                    activeColor = Slate600,
                    activeBg = Slate200,
                    onClick = { onStatusChange("LEAVE") }
                )
            }
        }
    }
}

@Composable
fun AttendanceDatePickerDialog(viewModel: HisabViewModel) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    var tempDate by remember { mutableStateOf(selectedDate) }

    AlertDialog(
        onDismissRequest = { viewModel.showAttendanceDatePicker.value = false },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = PrimaryNavy)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select Attendance Date", fontWeight = FontWeight.Bold, color = PrimaryNavy)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Enter date in YYYY-MM-DD format or pick from quick options below:", fontSize = 13.sp, color = Slate600)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = tempDate,
                    onValueChange = { tempDate = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Quick Select:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate800)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val cal = java.util.Calendar.getInstance()
                    
                    OutlinedButton(
                        onClick = { 
                            tempDate = sdf.format(cal.time)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Today", fontSize = 11.sp)
                    }
                    
                    cal.add(java.util.Calendar.DAY_OF_MONTH, -1)
                    OutlinedButton(
                        onClick = { 
                            tempDate = sdf.format(cal.time)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Yesterday", fontSize = 11.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (tempDate.isNotBlank()) {
                        viewModel.setAttendanceDate(tempDate.trim())
                    }
                    viewModel.showAttendanceDatePicker.value = false
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy)
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.showAttendanceDatePicker.value = false }) {
                Text("Cancel", color = Slate600)
            }
        }
    )
}

@Composable
fun StatusChoiceChip(
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
        Box(
            modifier = Modifier.padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) activeColor else Slate600
            )
        }
    }
}
