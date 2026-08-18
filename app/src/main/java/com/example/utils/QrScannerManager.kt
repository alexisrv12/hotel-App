package com.example.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.journeyapps.barcodescanner.ScanOptions
import org.json.JSONObject

/**
 * Data structure representing the parsed payload from a scanned QR Code.
 */
data class ScannedQrData(
    val rawContent: String,
    val token: String,
    val role: String,
    val deviceName: String?,
    val branchId: String?,
    val timestamp: Long?
)

/**
 * Utility manager wrapping the zxing-android-embedded library.
 * Configures ScanOptions, checks permissions, and parses scanned results reactively.
 */
object QrScannerManager {

    /**
     * Builds standard ScanOptions for pairing QR codes.
     */
    fun createScanOptions(
        promptText: String = "Encuadre el código QR de vinculación del hotel",
        isBeepEnabled: Boolean = true,
        isTorchEnabled: Boolean = false
    ): ScanOptions {
        return ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setPrompt(promptText)
            setCameraId(0) // Back camera
            setBeepEnabled(isBeepEnabled)
            setBarcodeImageEnabled(false)
            setOrientationLocked(true)
            setTimeout(60000) // 60 seconds timeout
            if (isTorchEnabled) {
                setTorchEnabled(true)
            }
        }
    }

    /**
     * Checks if the camera permission is currently granted.
     */
    fun isCameraPermissionGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Parses the scanned QR code content reactively, supporting both JSON payloads
     * and formatted token strings (e.g., LINK_SESSION|...).
     */
    fun parseScannedQr(rawResult: String?): ScannedQrData? {
        if (rawResult.isNullOrBlank()) return null
        val trimmed = rawResult.trim()

        // 1. Try parsing JSON format
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            try {
                val json = JSONObject(trimmed)
                val token = json.optString("token").ifBlank {
                    json.optString("qrToken").ifBlank { trimmed }
                }
                val role = json.optString("role", "RECEPCION").uppercase()
                val deviceName = json.optString("deviceName", "Dispositivo Móvil")
                val branch = json.optString("branchId", "RIVERA-MAIN")
                val ts = json.optLong("timestamp", System.currentTimeMillis())

                return ScannedQrData(
                    rawContent = trimmed,
                    token = token,
                    role = role,
                    deviceName = deviceName,
                    branchId = branch,
                    timestamp = ts
                )
            } catch (e: Exception) {
                // Fallback to plain string handling below
            }
        }

        // 2. Try parsing delimited string: LINK_SESSION|TOKEN|ROLE
        if (trimmed.contains("|")) {
            val parts = trimmed.split("|")
            val token = parts.getOrNull(1) ?: trimmed
            val role = parts.getOrNull(2)?.uppercase() ?: "RECEPCION"
            return ScannedQrData(
                rawContent = trimmed,
                token = token,
                role = role,
                deviceName = "Dispositivo Móvil",
                branchId = "RIVERA-MAIN",
                timestamp = System.currentTimeMillis()
            )
        }

        // 3. Raw Token string
        return ScannedQrData(
            rawContent = trimmed,
            token = trimmed,
            role = "RECEPCION",
            deviceName = "Dispositivo Móvil",
            branchId = "RIVERA-MAIN",
            timestamp = System.currentTimeMillis()
        )
    }
}
