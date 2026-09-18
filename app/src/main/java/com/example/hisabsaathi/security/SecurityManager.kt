package com.example.hisabsaathi.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

object SecurityManager {
    private val secureRandom = SecureRandom()
    const val ADMIN_EMAIL = "irkanmalik244255@GMAIL.COM"

    fun generateSalt(): String {
        val saltBytes = ByteArray(16)
        secureRandom.nextBytes(saltBytes)
        return Base64.getEncoder().encodeToString(saltBytes)
    }

    fun generateSecure6DigitOtp(): String {
        val number = 100000 + secureRandom.nextInt(900000)
        return number.toString()
    }

    fun hashOtp(otp: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val input = "$otp:$salt"
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun verifyOtp(enteredOtp: String, salt: String, expectedHash: String): Boolean {
        val hash = hashOtp(enteredOtp, salt)
        return MessageDigest.isEqual(hash.toByteArray(Charsets.UTF_8), expectedHash.toByteArray(Charsets.UTF_8))
    }

    fun isAuthorizedAdmin(email: String?, mobile: String?): Boolean {
        if (email != null && email.trim().equals(ADMIN_EMAIL, ignoreCase = true)) {
            return true
        }
        return false
    }
}
