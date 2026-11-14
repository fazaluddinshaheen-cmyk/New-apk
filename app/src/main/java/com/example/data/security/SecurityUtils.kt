package com.example.data.security

import java.security.MessageDigest

object SecurityUtils {
    // Cryptographic digest used to verify admin authorization without storing plaintext credentials in UI or strings.
    private const val ADMIN_DIGEST = "23a6776f24c1339569476787d4584e316ca1cdd7c8f079b4aac839b4a1fdbdb5"

    fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun isAdminCredentialMatch(username: String, password: String): Boolean {
        val combined = "${username.trim()}:${password}"
        return sha256(combined) == ADMIN_DIGEST
    }

    fun adminAccessHashForCredentials(username: String, password: String): String {
        return sha256("${username.trim()}:${password}")
    }
}
