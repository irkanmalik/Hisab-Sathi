package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.fragment.app.FragmentActivity
import com.example.hisabsaathi.ui.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : FragmentActivity() {
  private val viewModel: HisabViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val currentScreen by viewModel.currentScreen.collectAsState()
        val language by viewModel.currentLanguage.collectAsState()
        val isBiometricLocked by viewModel.isBiometricLocked.collectAsState()

        val layoutDirection = if (language.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

        CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
          Surface(modifier = Modifier.fillMaxSize()) {
            when (currentScreen) {
              AppScreen.LOGIN_MOBILE -> LoginMobileScreen(viewModel = viewModel)
              AppScreen.LOGIN_OTP -> LoginOtpScreen(viewModel = viewModel)
              AppScreen.REGISTER_BUSINESS -> RegisterBusinessScreen(viewModel = viewModel)
              AppScreen.MAIN_SHELL -> {
                if (isBiometricLocked) {
                  BiometricLockScreen(viewModel = viewModel)
                } else {
                  MainShellScreen(viewModel = viewModel)
                }
              }
            }
          }
        }
      }
    }
  }
}
