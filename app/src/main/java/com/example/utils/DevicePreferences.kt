package com.example.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Helper class for persisting device authorization and setup wizard status in SharedPreferences.
 */
object DevicePreferences {

    private const val PREF_NAME = "hotel_rivera_device_prefs"
    private const val KEY_IS_LINKED = "is_device_linked"
    private const val KEY_DEVICE_ID = "linked_device_id"
    private const val KEY_LINKED_EMAIL = "linked_email"
    private const val KEY_LINKED_TIMESTAMP = "linked_timestamp"
    private const val KEY_LINKED_ROLE = "linked_role"
    private const val KEY_LINKED_USER_NAME = "linked_user_name"
    private const val KEY_LAST_SCREEN = "last_active_screen"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Checks if the device is marked as authorized/linked locally.
     */
    fun isDeviceLinked(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_LINKED, false)
    }

    /**
     * Returns the currently linked device ID or generates a default local ID.
     */
    fun getLinkedDeviceId(context: Context): String {
        var id = getPrefs(context).getString(KEY_DEVICE_ID, null)
        if (id.isNull_or_empty_safe()) {
            id = "DEV-" + System.currentTimeMillis().toString().takeLast(6)
            getPrefs(context).edit().putString(KEY_DEVICE_ID, id).apply()
        }
        return id!!
    }

    /**
     * Returns the email associated with the linked device.
     */
    fun getLinkedEmail(context: Context): String? {
        return getPrefs(context).getString(KEY_LINKED_EMAIL, null)
    }

    /**
     * Returns the role assigned to this linked terminal (RECEPCION or GERENTE).
     */
    fun getLinkedRole(context: Context): String {
        return getPrefs(context).getString(KEY_LINKED_ROLE, "RECEPCION") ?: "RECEPCION"
    }

    /**
     * Returns the user/terminal display name assigned to this device.
     */
    fun getLinkedUserName(context: Context): String {
        val role = getLinkedRole(context)
        val defaultName = if (role.equals("GERENTE", ignoreCase = true)) "Gerencia Hotel Rivera" else "Recepción Principal"
        return getPrefs(context).getString(KEY_LINKED_USER_NAME, defaultName) ?: defaultName
    }

    /**
     * Retrieves the last active screen before exiting the app to resume seamlessly.
     */
    fun getLastActiveScreen(context: Context): String? {
        return getPrefs(context).getString(KEY_LAST_SCREEN, null)
    }

    /**
     * Persists the last active screen so upon reopening, the app returns directly to it.
     */
    fun setLastActiveScreen(context: Context, screenName: String) {
        getPrefs(context).edit().putString(KEY_LAST_SCREEN, screenName).apply()
    }

    /**
     * Saves device authorization and session state upon successful linking.
     */
    fun setDeviceLinked(
        context: Context,
        deviceId: String,
        email: String,
        role: String = "RECEPCION",
        userName: String = ""
    ) {
        val upperRole = role.uppercase()
        val displayName = if (userName.isNotBlank()) userName else {
            if (upperRole == "GERENTE") "Gerencia Hotel Rivera" else "Recepción Principal"
        }
        getPrefs(context).edit()
            .putBoolean(KEY_IS_LINKED, true)
            .putString(KEY_DEVICE_ID, deviceId)
            .putString(KEY_LINKED_EMAIL, email)
            .putString(KEY_LINKED_ROLE, upperRole)
            .putString(KEY_LINKED_USER_NAME, displayName)
            .putLong(KEY_LINKED_TIMESTAMP, System.currentTimeMillis())
            .apply()
    }

    fun setDeviceAuthorized(context: Context, authorized: Boolean) {
        getPrefs(context).edit()
            .putBoolean(KEY_IS_LINKED, authorized)
            .apply()
    }

    /**
     * Clears device authorization state only when explicitly unlinked by Manager.
     */
    fun clearDeviceLinked(context: Context) {
        getPrefs(context).edit()
            .putBoolean(KEY_IS_LINKED, false)
            .remove(KEY_LINKED_EMAIL)
            .remove(KEY_LINKED_ROLE)
            .remove(KEY_LINKED_USER_NAME)
            .remove(KEY_LAST_SCREEN)
            .remove(KEY_LINKED_TIMESTAMP)
            .apply()
    }
}

private fun String?.isNull_or_empty_safe(): Boolean {
    return this == null || this.trim().isEmpty()
}
