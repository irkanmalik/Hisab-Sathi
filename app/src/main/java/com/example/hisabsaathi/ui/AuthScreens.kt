package com.example.hisabsaathi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hisabsaathi.i18n.AppLanguage
import com.example.hisabsaathi.i18n.Translations
import com.example.ui.theme.*

@Composable
fun LoginMobileScreen(
    viewModel: HisabViewModel,
    modifier: Modifier = Modifier
) {
    val isSignup by viewModel.isSignupMode.collectAsState()
    val adminId by viewModel.adminId.collectAsState()
    val adminPassword by viewModel.adminPassword.collectAsState()
    
    val signupName by viewModel.signupName.collectAsState()
    val signupEmail by viewModel.signupEmail.collectAsState()
    val signupMobile by viewModel.signupMobile.collectAsState()
    val signupPassword by viewModel.signupPassword.collectAsState()

    val authError by viewModel.authError.collectAsState()
    val showPasswordReset by viewModel.showPasswordResetDialog.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Slate50)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Logo & Branding
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(PrimaryNavy),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = "HisabSaathi",
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Hisab Sathi",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryNavy
            )

            Text(
                text = "Secure Business Ledger & Khata Management",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Slate600,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    // Tab Selector: Login / Signup
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Slate100, RoundedCornerShape(10.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TabButton(
                            title = "Login",
                            selected = !isSignup,
                            onClick = { 
                                viewModel.isSignupMode.value = false
                                viewModel.authError.value = null
                            },
                            modifier = Modifier.weight(1f)
                        )
                        TabButton(
                            title = "Sign Up",
                            selected = isSignup,
                            onClick = { 
                                viewModel.isSignupMode.value = true
                                viewModel.authError.value = null
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    if (isSignup) {
                        // Sign Up Form
                        Text(
                            text = "Create New Account",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate800
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Register with your details in the original database.",
                            fontSize = 12.sp,
                            color = Slate600
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = signupName,
                            onValueChange = { viewModel.signupName.value = it },
                            label = { Text("Full Name *") },
                            placeholder = { Text("Enter your full name") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryNavy) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = signupMobile,
                            onValueChange = { viewModel.signupMobile.value = it },
                            label = { Text("Mobile Number *") },
                            placeholder = { Text("Enter 10-digit mobile number") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = PrimaryNavy) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = signupEmail,
                            onValueChange = { viewModel.signupEmail.value = it },
                            label = { Text("Email Address (Optional)") },
                            placeholder = { Text("Enter email address") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = PrimaryNavy) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = signupPassword,
                            onValueChange = { viewModel.signupPassword.value = it },
                            label = { Text("Password * (Min 6 chars)") },
                            placeholder = { Text("Create password") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryNavy) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (authError != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                color = CrimsonRed.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Error, contentDescription = null, tint = CrimsonRed, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = authError!!,
                                        color = CrimsonRed,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = { viewModel.performSignup() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy)
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Register Account", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }

                    } else {
                        // Login Form
                        Text(
                            text = "Welcome Back",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate800
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Login with your Email or Mobile and Password.",
                            fontSize = 12.sp,
                            color = Slate600
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = adminId,
                            onValueChange = { viewModel.adminId.value = it },
                            label = { Text("Email or Mobile Number") },
                            placeholder = { Text("Enter email or mobile") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryNavy) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_id_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = adminPassword,
                            onValueChange = { viewModel.adminPassword.value = it },
                            label = { Text("Password") },
                            placeholder = { Text("Enter password") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryNavy) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_password_input")
                        )

                        if (authError != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                color = CrimsonRed.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Error, contentDescription = null, tint = CrimsonRed, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = authError!!,
                                        color = CrimsonRed,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { 
                                viewModel.resetPasswordError.value = null
                                viewModel.resetPasswordSuccess.value = null
                                viewModel.showPasswordResetDialog.value = true 
                            }) {
                                Text("Forgot Password?", color = PrimaryNavy, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { viewModel.performLogin() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("admin_login_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy)
                        ) {
                            Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Login", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Slate200)
                        Text(
                            text = "  OR  ",
                            fontSize = 11.sp,
                            color = Slate500,
                            fontWeight = FontWeight.Bold
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Slate200)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Google Login / Signup Button
                    OutlinedButton(
                        onClick = {
                            viewModel.handleGoogleLoginClick()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate400),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate800)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = Color(0xFF4285F4),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isSignup) "Sign Up with Google" else "Login with Google",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }

    if (showPasswordReset) {
        PasswordResetDialog(viewModel = viewModel)
    }

    val showGoogleDialog by viewModel.showGoogleAuthDialog.collectAsState()
    if (showGoogleDialog) {
        GoogleAuthDialog(viewModel = viewModel)
    }
}

@Composable
fun GoogleAuthDialog(viewModel: HisabViewModel) {
    val email by viewModel.googleAuthEmail.collectAsState()
    val name by viewModel.googleAuthName.collectAsState()
    val errorMsg by viewModel.googleAuthError.collectAsState()

    AlertDialog(
        onDismissRequest = { viewModel.showGoogleAuthDialog.value = false },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Public, contentDescription = null, tint = Color(0xFF4285F4))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Google Account Sign-In", fontWeight = FontWeight.Bold, color = PrimaryNavy)
            }
        },
        text = {
            Column {
                Text(
                    text = "Please enter your real Google account details to sign in securely to the database.",
                    fontSize = 13.sp,
                    color = Slate600
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { viewModel.googleAuthEmail.value = it },
                    label = { Text("Google Email *") },
                    placeholder = { Text("your.email@gmail.com") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = PrimaryNavy) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { viewModel.googleAuthName.value = it },
                    label = { Text("Full Name (Optional)") },
                    placeholder = { Text("Enter your name") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryNavy) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMsg != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = errorMsg!!, color = CrimsonRed, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { viewModel.submitGoogleAuth() },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy)
            ) {
                Text("Continue with Google")
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.showGoogleAuthDialog.value = false }) {
                Text("Cancel", color = Slate600)
            }
        }
    )
}

@Composable
fun PasswordResetDialog(viewModel: HisabViewModel) {
    val resetEmail by viewModel.resetEmail.collectAsState()
    val resetCode by viewModel.resetVerificationCode.collectAsState()
    val newPassword by viewModel.newAdminPassword.collectAsState()
    val confirmPassword by viewModel.confirmNewAdminPassword.collectAsState()
    val step by viewModel.resetPasswordStep.collectAsState()
    val errorMsg by viewModel.resetPasswordError.collectAsState()
    val successMsg by viewModel.resetPasswordSuccess.collectAsState()

    AlertDialog(
        onDismissRequest = { viewModel.showPasswordResetDialog.value = false },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LockReset, contentDescription = null, tint = PrimaryNavy)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Admin Password Reset", fontWeight = FontWeight.Bold, color = PrimaryNavy)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                when (step) {
                    1 -> {
                        Text("Enter your authorized administrator email address to receive a secure reset code.", fontSize = 13.sp, color = Slate600)
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = resetEmail,
                            onValueChange = { viewModel.resetEmail.value = it },
                            label = { Text("Admin Email") },
                            placeholder = { Text("irkanmalik244255@gmail.com") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    2 -> {
                        Text("Enter the 6-digit verification code sent to your registered channel.", fontSize = 13.sp, color = Slate600)
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = resetCode,
                            onValueChange = { viewModel.resetVerificationCode.value = it },
                            label = { Text("Verification Code") },
                            placeholder = { Text("123456") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    3 -> {
                        Text("Enter your new secure admin password.", fontSize = 13.sp, color = Slate600)
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { viewModel.newAdminPassword.value = it },
                            label = { Text("New Password") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { viewModel.confirmNewAdminPassword.value = it },
                            label = { Text("Confirm New Password") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (errorMsg != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = errorMsg!!, color = CrimsonRed, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
                if (successMsg != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = successMsg!!, color = EmeraldGreen, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        },
        confirmButton = {
            when (step) {
                1 -> {
                    Button(
                        onClick = { viewModel.requestPasswordReset() },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy)
                    ) {
                        Text("Send Reset Code")
                    }
                }
                2 -> {
                    Button(
                        onClick = { viewModel.verifyResetCode() },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy)
                    ) {
                        Text("Verify Code")
                    }
                }
                3 -> {
                    Button(
                        onClick = { viewModel.confirmPasswordReset() },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                    ) {
                        Text("Update Password")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { 
                viewModel.showPasswordResetDialog.value = false
                viewModel.resetPasswordStep.value = 1
            }) {
                Text("Cancel", color = Slate600)
            }
        }
    )
}

@Composable
private fun TabButton(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(36.dp),
        shape = RoundedCornerShape(8.dp),
        color = if (selected) Color.White else Color.Transparent,
        shadowElevation = if (selected) 2.dp else 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) PrimaryNavy else Slate600
            )
        }
    }
}

@Composable
fun LoginOtpScreen(
    viewModel: HisabViewModel,
    modifier: Modifier = Modifier
) {
    val mobile by viewModel.loginMobile.collectAsState()
    val enteredOtp by viewModel.enteredOtp.collectAsState()
    val liveVerificationCode by viewModel.liveVerificationCode.collectAsState()
    val isCloudApiActive by viewModel.isCloudApiActive.collectAsState()
    val otpSentNotice by viewModel.otpSentNotice.collectAsState()
    val authError by viewModel.authError.collectAsState()
    val resendCooldown by viewModel.resendCooldown.collectAsState()
    val language by viewModel.currentLanguage.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Slate50)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(EmeraldLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = EmeraldGreen,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    color = if (isCloudApiActive) EmeraldLight else Slate100,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(if (isCloudApiActive) EmeraldGreen else PrimaryNavy, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isCloudApiActive) "WhatsApp Cloud API Active" else "Production Live Mode • SHA-256",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCloudApiActive) EmeraldGreen else PrimaryNavy
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = Translations.get("enter_otp", language),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate800
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Sent to +91 $mobile",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = PrimaryNavy
                )

                if (otpSentNotice != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = EmeraldLight.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = otpSentNotice!!,
                            color = Slate800,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(10.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = enteredOtp,
                    onValueChange = { if (it.length <= 6) viewModel.enteredOtp.value = it },
                    placeholder = { Text("_ _ _ _ _ _", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(
                        textAlign = TextAlign.Center,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 6.sp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("otp_input_field")
                )

                if (authError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = authError!!,
                        color = CrimsonRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { viewModel.verifyOtp() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("verify_otp_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy)
                ) {
                    Text(Translations.get("verify_otp", language), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { viewModel.resendOtp() },
                        enabled = resendCooldown == 0
                    ) {
                        Text(
                            text = if (resendCooldown > 0)
                                String.format(Translations.get("resend_in", language), resendCooldown)
                            else
                                Translations.get("resend_otp", language),
                            fontSize = 13.sp,
                            color = if (resendCooldown > 0) Slate600 else EmeraldGreen
                        )
                    }

                    TextButton(onClick = { viewModel.changeNumber() }) {
                        Text(
                            text = Translations.get("change_number", language),
                            fontSize = 13.sp,
                            color = Slate600
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RegisterBusinessScreen(
    viewModel: HisabViewModel,
    modifier: Modifier = Modifier
) {
    val businessName by viewModel.regBusinessName.collectAsState()
    val ownerName by viewModel.regOwnerName.collectAsState()
    val businessMobile by viewModel.regBusinessMobile.collectAsState()
    val address by viewModel.regAddress.collectAsState()
    val city by viewModel.regCity.collectAsState()
    val state by viewModel.regState.collectAsState()
    val authError by viewModel.authError.collectAsState()
    val language by viewModel.currentLanguage.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Slate50)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = Translations.get("create_business_profile", language),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryNavy
                )
                Text(
                    text = "Configure your store or business identity for receipts and customer hisab.",
                    fontSize = 13.sp,
                    color = Slate600,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                )

                OutlinedTextField(
                    value = businessName,
                    onValueChange = { viewModel.regBusinessName.value = it },
                    label = { Text(Translations.get("business_name", language)) },
                    placeholder = { Text("e.g. Malik Kirana & General Store") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("reg_business_name")
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = ownerName,
                    onValueChange = { viewModel.regOwnerName.value = it },
                    label = { Text(Translations.get("owner_name", language)) },
                    placeholder = { Text("e.g. Irkan Malik") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("reg_owner_name")
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = businessMobile,
                    onValueChange = { viewModel.regBusinessMobile.value = it },
                    label = { Text(Translations.get("business_mobile", language)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = address,
                    onValueChange = { viewModel.regAddress.value = it },
                    label = { Text(Translations.get("address", language)) },
                    placeholder = { Text("Shop No, Market, Road") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = city,
                        onValueChange = { viewModel.regCity.value = it },
                        label = { Text(Translations.get("city", language)) },
                        placeholder = { Text("Delhi") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = state,
                        onValueChange = { viewModel.regState.value = it },
                        label = { Text(Translations.get("state", language)) },
                        placeholder = { Text("Delhi") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Default info
                Surface(
                    color = Slate100,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Currency: INR ₹", fontSize = 12.sp, color = Slate600)
                        Text("Country: India", fontSize = 12.sp, color = Slate600)
                        Text("Timezone: Asia/Kolkata", fontSize = 12.sp, color = Slate600)
                    }
                }

                if (authError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = authError!!, color = CrimsonRed, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { viewModel.saveBusinessProfile() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_business_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy)
                ) {
                    Text(Translations.get("save_profile", language), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
