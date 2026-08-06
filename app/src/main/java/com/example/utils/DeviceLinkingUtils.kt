package com.example.utils

import java.util.UUID
import kotlin.random.Random

/**
 * Data class representing generated temporal QR payload for device linking.
 */
data class TemporalQrPayload(
    val qrCodeString: String,
    val timestamp: Long,
    val expiresAt: Long,
    val sessionNonce: String,
    val deviceId: String
)

/**
 * Result sealed class for PIN code validation.
 */
sealed class PinValidationResult {
    object Valid : PinValidationResult()
    object InvalidFormat : PinValidationResult()
    object IncorrectPin : PinValidationResult()
    data class RateLimited(val remainingSeconds: Int) : PinValidationResult()
}

/**
 * Utility object for device linking workflow, including temporal QR generation and PIN validation.
 */
object DeviceLinkingUtils {

    private const val QR_PREFIX = "HOTEL_LINK_V1"
    private const val DEFAULT_EXPIRATION_MS = 5 * 60 * 1000L // 5 minutes

    /**
     * Generates a temporal QR code payload string encoded with timestamp, expiration, and session nonce.
     */
    fun generateTemporalQrCode(
        deviceId: String = UUID.randomUUID().toString().take(8),
        expirationDurationMs: Long = DEFAULT_EXPIRATION_MS
    ): TemporalQrPayload {
        val currentTime = System.currentTimeMillis()
        val expiresAt = currentTime + expirationDurationMs
        val nonce = UUID.randomUUID().toString().take(8)
        val qrString = "$QR_PREFIX:$deviceId:$currentTime:$expiresAt:$nonce"

        return TemporalQrPayload(
            qrCodeString = qrString,
            timestamp = currentTime,
            expiresAt = expiresAt,
            sessionNonce = nonce,
            deviceId = deviceId
        )
    }

    /**
     * Validates if a QR code payload string is valid and not expired.
     */
    fun isQrCodeValid(qrCodeString: String, currentTime: Long = System.currentTimeMillis()): Boolean {
        if (!qrCodeString.startsWith("$QR_PREFIX:")) return false
        val parts = qrCodeString.split(":")
        if (parts.size < 5) return false

        val expiresAt = parts[3].toLongOrNull() ?: return false
        return currentTime < expiresAt
    }

    /**
     * Parses a temporal QR code string into [TemporalQrPayload], returning null if invalid format.
     */
    fun parseTemporalQrCode(qrCodeString: String): TemporalQrPayload? {
        if (!qrCodeString.startsWith("$QR_PREFIX:")) return null
        val parts = qrCodeString.split(":")
        if (parts.size < 5) return null

        val deviceId = parts[1]
        val timestamp = parts[2].toLongOrNull() ?: return null
        val expiresAt = parts[3].toLongOrNull() ?: return null
        val nonce = parts[4]

        return TemporalQrPayload(
            qrCodeString = qrCodeString,
            timestamp = timestamp,
            expiresAt = expiresAt,
            sessionNonce = nonce,
            deviceId = deviceId
        )
    }

    /**
     * Validates an input PIN string against the expected PIN code.
     */
    fun validatePinCode(
        inputPin: String,
        expectedPin: String,
        allowDigitsOnly: Boolean = true
    ): PinValidationResult {
        val trimmed = inputPin.trim()
        if (trimmed.length !in 4..8) {
            return PinValidationResult.InvalidFormat
        }
        if (allowDigitsOnly && !trimmed.all { it.isDigit() }) {
            return PinValidationResult.InvalidFormat
        }
        return if (trimmed == expectedPin.trim()) {
            PinValidationResult.Valid
        } else {
            PinValidationResult.IncorrectPin
        }
    }

    /**
     * Generates a random numeric PIN code of specified length (default 6 digits).
     */
    fun generateNumericPin(length: Int = 6): String {
        require(length in 4..8) { "PIN length must be between 4 and 8 digits." }
        return (1..length)
            .map { Random.nextInt(0, 10) }
            .joinToString("")
    }
}
