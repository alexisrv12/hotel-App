package com.example.utils

import java.security.MessageDigest

/**
 * Utilidad de seguridad para hash de contraseñas y autenticación de usuarios.
 */
object SecurityUtils {

    /**
     * Genera un hash SHA-256 seguro a partir de la contraseña plana.
     */
    fun hashPassword(password: String): String {
        if (password.isBlank()) return ""
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.trim().toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Verifica si una contraseña coincide con el hash almacenado o con el PIN legado.
     */
    fun verifyPassword(inputPasswordOrPin: String, storedPasswordHash: String, storedPinCode: String): Boolean {
        val trimmedInput = inputPasswordOrPin.trim()
        if (trimmedInput.isBlank()) return false

        // 1. Verificar contra hash SHA-256 almacenado
        if (storedPasswordHash.isNotBlank()) {
            val inputHash = hashPassword(trimmedInput)
            if (inputHash.equals(storedPasswordHash, ignoreCase = true)) {
                return true
            }
        }

        // 2. Verificar contra PIN/contraseña legacy en texto plano
        if (storedPinCode.isNotBlank() && storedPinCode == trimmedInput) {
            return true
        }

        return false
    }
}
