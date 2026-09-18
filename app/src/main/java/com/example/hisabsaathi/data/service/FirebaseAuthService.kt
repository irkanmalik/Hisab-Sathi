package com.example.hisabsaathi.data.service

import android.app.Activity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

class FirebaseAuthService {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    fun verifyPhoneNumber(
        activity: Activity,
        phoneNumber: String,
        onCodeSent: (String) -> Unit,
        onVerificationFailed: (String) -> Unit,
        onVerificationCompleted: (PhoneAuthCredential) -> Unit
    ) {
        val formattedNumber = if (phoneNumber.startsWith("+")) phoneNumber else "+91$phoneNumber"
        
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                onVerificationCompleted(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                onVerificationFailed(e.localizedMessage ?: "Firebase Phone Verification Failed")
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                onCodeSent(verificationId)
            }
        }

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(formattedNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    suspend fun checkIsAdminFromToken(): Boolean =
        suspendCancellableCoroutine { continuation ->
            val user = auth.currentUser
            if (user == null) {
                continuation.resume(false)
                return@suspendCancellableCoroutine
            }
            user.getIdToken(true)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val claims = task.result?.claims
                        val isAdminClaim = claims?.get("admin") == true || claims?.get("role") == "admin"
                        continuation.resume(isAdminClaim)
                    } else {
                        continuation.resume(false)
                    }
                }
        }

    suspend fun signInWithCredential(credential: PhoneAuthCredential): Boolean =
        suspendCancellableCoroutine { continuation ->
            auth.signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        continuation.resume(true)
                    } else {
                        continuation.resume(false)
                    }
                }
        }
}
