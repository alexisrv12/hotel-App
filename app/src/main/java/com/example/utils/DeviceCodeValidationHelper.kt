package com.example.utils

import java.util.Locale

/**
 * Result state for device code and QR token validation.
 */
sealed class CodeValidationResult {
    object Valid : CodeValidationResult()
    object Empty : CodeValidationResult()
    object Incorrect : CodeValidationResult()
    data class Expired(val expiredMinutesAgo: Long) : CodeValidationResult()
    data class InvalidFormat(val reason: String) : CodeValidationResult()
}

/**
 * Helper class that enforces time-sensitive validation and expiration logic
 * for device linking PINs and QR tokens, ensuring they are valid for at most 5 minutes (300,000 ms).
 */
class DeviceCodeValidationHelper(
    val maxValidityDurationMs: Long = DEFAULT_EXPIRATION_DURATION_MS
) {

    /**
     * Checks whether a code created at [createdTimestampMs] has expired relative to [currentTimeMs].
     */
    fun isExpired(
        createdTimestampMs: Long,
        currentTimeMs: Long = System.currentTimeMillis()
    ): Boolean {
        if (createdTimestampMs <= 0L) return true
        return (currentTimeMs - createdTimestampMs) > maxValidityDurationMs
    }

    /**
     * Calculates remaining time in milliseconds before code expiration.
     */
    fun getRemainingMillis(
        createdTimestampMs: Long,
        currentTimeMs: Long = System.currentTimeMillis()
    ): Long {
        if (createdTimestampMs <= 0L) return 0L
        val elapsed = currentTimeMs - createdTimestampMs
        val remaining = maxValidityDurationMs - elapsed
        return if (remaining > 0L) remaining else 0L
    }

    /**
     * Formats remaining time into MM:SS string (e.g. "04:52").
     */
    fun getFormattedCountdown(
        createdTimestampMs: Long,
        currentTimeMs: Long = System.currentTimeMillis()
    ): String {
        val remainingMs = getRemainingMillis(createdTimestampMs, currentTimeMs)
        val totalSeconds = remainingMs / 1000L
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }

    /**
     * Calculates validity progress ratio from 1.0 (fresh) down to 0.0 (expired).
     */
    fun getRemainingProgressRatio(
        createdTimestampMs: Long,
        currentTimeMs: Long = System.currentTimeMillis()
    ): Float {
        val remainingMs = getRemainingMillis(createdTimestampMs, currentTimeMs)
        return (remainingMs.toFloat() / maxValidityDurationMs.toFloat()).coerceIn(0f, 1f)
    }

    /**
     * Validates an input PIN code against the expected PIN code and its creation timestamp.
     */
    fun validatePinCode(
        inputPin: String,
        expectedPin: String,
        createdTimestampMs: Long,
        currentTimeMs: Long = System.currentTimeMillis()
    ): CodeValidationResult {
        val trimmedInput = inputPin.trim()
        val trimmedExpected = expectedPin.trim()

        if (trimmedInput.isBlank()) {
            return CodeValidationResult.Empty
        }

        if (trimmedInput.length != 6 || !trimmedInput.all { it.isDigit() }) {
            return CodeValidationResult.InvalidFormat("El PIN debe constar de 6 dígitos numéricos.")
        }

        if (isExpired(createdTimestampMs, currentTimeMs)) {
            val elapsedMs = currentTimeMs - createdTimestampMs
            val expiredMinutes = (elapsedMs - maxValidityDurationMs) / (60 * 1000L)
            return CodeValidationResult.Expired(expiredMinutesAgo = expiredMinutes)
        }

        return if (trimmedInput == trimmedExpected) {
            CodeValidationResult.Valid
        } else {
            CodeValidationResult.Incorrect
        }
    }

    /**
     * Validates a QR code token string against expected session token and its creation timestamp.
     */
    fun validateQrToken(
        inputQrToken: String,
        expectedQrToken: String,
        createdTimestampMs: Long,
        currentTimeMs: Long = System.currentTimeMillis()
    ): CodeValidationResult {
        val trimmedInput = inputQrToken.trim()
        val trimmedExpected = expectedQrToken.trim()

        if (trimmedInput.isBlank()) {
            return CodeValidationResult.Empty
        }

        if (isExpired(createdTimestampMs, currentTimeMs)) {
            val elapsedMs = currentTimeMs - createdTimestampMs
            val expiredMinutes = (elapsedMs - maxValidityDurationMs) / (60 * 1000L)
            return CodeValidationResult.Expired(expiredMinutesAgo = expiredMinutes)
        }

        // Match expected token or decoded session format or JSON QR payload
        val isJsonQr = (trimmedInput.startsWith("{") && trimmedInput.endsWith("}")) &&
                (trimmedInput.contains("role", ignoreCase = true) ||
                 trimmedInput.contains("branch", ignoreCase = true) ||
                 trimmedInput.contains("deviceId", ignoreCase = true) ||
                 trimmedInput.contains("pin", ignoreCase = true) ||
                 trimmedInput.contains("LINK_SESSION", ignoreCase = true))

        val matchesExpected = trimmedInput == trimmedExpected ||
                trimmedInput.contains(trimmedExpected.take(12)) ||
                DeviceLinkingUtils.isQrCodeValid(trimmedInput) ||
                DeviceLinkingUtility.getInstance().decodeTemporarySessionQrString(trimmedInput) != null ||
                isJsonQr

        return if (matchesExpected) {
            CodeValidationResult.Valid
        } else {
            CodeValidationResult.Incorrect
        }
    }

    companion object {
        const val DEFAULT_EXPIRATION_DURATION_MS = 5 * 60 * 1000L // 5 minutes (300,000 ms)

        private val instance = DeviceCodeValidationHelper()

        fun getInstance(): DeviceCodeValidationHelper = instance
    }
}
