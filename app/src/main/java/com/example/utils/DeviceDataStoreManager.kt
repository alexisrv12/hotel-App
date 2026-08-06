package com.example.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.deviceDataStore: DataStore<Preferences> by preferencesDataStore(name = "device_linking_preferences")

/**
 * DataStore wrapper for storing device authorization status and user email,
 * ensuring the configuration wizard only appears when the device is not yet linked.
 */
class DeviceDataStoreManager(private val context: Context) {

    companion object {
        val KEY_IS_LINKED = booleanPreferencesKey("is_device_linked")
        val KEY_DEVICE_ID = stringPreferencesKey("linked_device_id")
        val KEY_LINKED_EMAIL = stringPreferencesKey("linked_user_email")
    }

    /**
     * Flow emitting the device authorization status.
     */
    val isDeviceLinkedFlow: Flow<Boolean> = context.deviceDataStore.data
        .map { preferences ->
            preferences[KEY_IS_LINKED] ?: DevicePreferences.isDeviceLinked(context)
        }

    /**
     * Flow emitting the associated user email.
     */
    val userEmailFlow: Flow<String?> = context.deviceDataStore.data
        .map { preferences ->
            preferences[KEY_LINKED_EMAIL] ?: DevicePreferences.getLinkedEmail(context)
        }

    /**
     * Flow emitting the unique device ID.
     */
    val deviceIdFlow: Flow<String> = context.deviceDataStore.data
        .map { preferences ->
            preferences[KEY_DEVICE_ID] ?: DevicePreferences.getLinkedDeviceId(context)
        }

    /**
     * Updates authorization status and email in DataStore and SharedPreferences for compatibility.
     */
    suspend fun saveDeviceAuthorization(deviceId: String, email: String) {
        context.deviceDataStore.edit { preferences ->
            preferences[KEY_IS_LINKED] = true
            preferences[KEY_DEVICE_ID] = deviceId
            preferences[KEY_LINKED_EMAIL] = email
        }
        DevicePreferences.setDeviceLinked(context, deviceId, email)
    }

    /**
     * Clears authorization status when unlinked.
     */
    suspend fun clearDeviceAuthorization() {
        context.deviceDataStore.edit { preferences ->
            preferences[KEY_IS_LINKED] = false
            preferences.remove(KEY_LINKED_EMAIL)
        }
        DevicePreferences.clearDeviceLinked(context)
    }

    suspend fun isDeviceLinked(): Boolean {
        return isDeviceLinkedFlow.first()
    }

    suspend fun getUserEmail(): String? {
        return userEmailFlow.first()
    }

    suspend fun getDeviceId(): String {
        return deviceIdFlow.first()
    }
}
