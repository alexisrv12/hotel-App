package com.example.utils

import android.util.Base64
import java.security.SecureRandom
import java.util.UUID

/**
 * Utility class providing cryptographically secure 6-digit PIN generation
 * and Base64-encoded temporary session token strings for device linking QR scanning.
 */
class DeviceLinkingUtility {

    private val secureRandom = SecureRandom()

    /**
     * Generates a cryptographically secure random 6-digit numeric PIN (e.g., "048291").
     */
    fun generateSecurePin(): String {
        val number = secureRandom.nextInt(1_000_000)
        return String.format("%06d", number)
    }

    /**
     * Generates a Base64-encoded temporary session payload for QR code scanning.
     *
     * @param deviceId Identifier for the device being linked.
     * @param expirationDurationMs Duration in milliseconds until session expires (default 5 minutes).
     * @return Base64 encoded string payload containing device metadata, timestamps, and secure nonce.
     */
    fun generateTemporarySessionQrString(
        deviceId: String = UUID.randomUUID().toString().take(8),
        expirationDurationMs: Long = 5 * 60 * 1000L
    ): String {
        val currentTimeMs = System.currentTimeMillis()
        val expiresAtMs = currentTimeMs + expirationDurationMs
        
        val randomBytes = ByteArray(16)
        secureRandom.nextBytes(randomBytes)
        val nonceHex = randomBytes.joinToString("") { "%02x".format(it) }

        val rawPayload = "LINK_SESSION|$deviceId|$currentTimeMs|$expiresAtMs|$nonceHex"
        return Base64.encodeToString(rawPayload.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    }

    /**
     * Decodes a Base64-encoded temporary session string back to its raw payload.
     *
     * @param base64QrString Base64 encoded string from QR code.
     * @return Decoded payload string or null if decoding fails.
     */
    fun decodeTemporarySessionQrString(base64QrString: String): String? {
        return try {
            val decodedBytes = Base64.decode(base64QrString, Base64.NO_WRAP)
            String(decodedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private val instance = DeviceLinkingUtility()

        /**
         * Returns singleton instance of [DeviceLinkingUtility].
         */
        fun getInstance(): DeviceLinkingUtility = instance

        /**
         * Convenience static method to generate a secure 6-digit PIN.
         */
        fun generate6DigitPin(): String = instance.generateSecurePin()

        /**
         * Convenience static method to generate Base64 temporary QR session string.
         */
        fun generateQrSessionToken(deviceId: String = UUID.randomUUID().toString().take(8)): String =
            instance.generateTemporarySessionQrString(deviceId)
    }
}
