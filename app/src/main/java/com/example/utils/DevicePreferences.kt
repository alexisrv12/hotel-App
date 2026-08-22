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

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Checks if the device is marked as authorized/linked locally.
     */
    fun isDeviceLinked(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_LINKED, false)
    }

    fun isDeviceAuthorized(context: Context): Boolean {
        return isDeviceLinked(context)
    }

    fun setDeviceAuthorized(context: Context, authorized: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_IS_LINKED, authorized).apply()
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
     * Saves device authorization state upon successful setup wizard completion.
     */
    fun setDeviceLinked(context: Context, deviceId: String, email: String) {
        getPrefs(context).edit()
            .putBoolean(KEY_IS_LINKED, true)
            .putString(KEY_DEVICE_ID, deviceId)
            .putString(KEY_LINKED_EMAIL, email)
            .putLong(KEY_LINKED_TIMESTAMP, System.currentTimeMillis())
            .apply()
    }

    /**
     * Clears device authorization state when unlinked by Manager or reset.
     */
    fun clearDeviceLinked(context: Context) {
        getPrefs(context).edit()
            .putBoolean(KEY_IS_LINKED, false)
            .remove(KEY_LINKED_EMAIL)
            .remove(KEY_LINKED_TIMESTAMP)
            .apply()
    }
}

private fun String?.isNull_or_empty_safe(): Boolean {
    return this == null || this.trim().isEmpty()
}
