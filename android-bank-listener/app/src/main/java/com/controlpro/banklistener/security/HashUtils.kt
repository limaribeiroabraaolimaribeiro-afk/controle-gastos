package com.controlpro.banklistener.security

import java.security.MessageDigest

object HashUtils {

    fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun buildNotificationHash(
        packageName: String,
        title: String,
        text: String,
        timestamp: Long
    ): String {
        val normalized = normalizeForHash(title, text)
        // Truncar timestamp para minuto para absorver pequenas variações
        val minuteTs = (timestamp / 60_000) * 60_000
        val raw = "$packageName|$normalized|$minuteTs"
        return sha256(raw)
    }

    private fun normalizeForHash(title: String, text: String): String {
        val combined = "$title $text"
        return combined
            .lowercase()
            .replace(Regex("[^a-z0-9,.]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun maskSensitiveNumbers(text: String): String {
        // Mascarar números longos (cartão, conta, CPF etc.)
        return text
            .replace(Regex("\\d{4}[\\s.-]?\\d{4}[\\s.-]?\\d{4}[\\s.-]?\\d{4}"), "****")
            .replace(Regex("\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}"), "***.***.***-**")
            .replace(Regex("\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2}"), "**.***.***/***/***-**")
    }
}
