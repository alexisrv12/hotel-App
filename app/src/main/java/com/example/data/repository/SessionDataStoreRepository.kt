package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_session_preferences")

data class UserSession(
    val userRole: String?,
    val userEmail: String?,
    val userName: String?,
    val isDeviceAuthorized: Boolean,
    val deviceId: String,
    val authToken: String?
)

/**
 * Repository using Jetpack DataStore to manage persistent storage of linking tokens,
 * device authorization, user roles, and local session states, ensuring security
 * and non-volatile persistence.
 */
class SessionDataStoreRepository(private val context: Context) {

    companion object {
        val KEY_USER_ROLE = stringPreferencesKey("user_role")
        val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        val KEY_USER_NAME = stringPreferencesKey("user_name")
        val KEY_IS_DEVICE_AUTHORIZED = booleanPreferencesKey("is_device_authorized")
        val KEY_DEVICE_ID = stringPreferencesKey("linked_device_id")
        val KEY_AUTH_TOKEN = stringPreferencesKey("auth_token")
        val KEY_ACTIVE_LINKING_PIN = stringPreferencesKey("active_linking_pin")
        val KEY_ACTIVE_LINKING_PIN_TS = longPreferencesKey("active_linking_pin_ts")
        val KEY_ACTIVE_LINKING_QR = stringPreferencesKey("active_linking_qr")
        val KEY_ACTIVE_LINKING_QR_TS = longPreferencesKey("active_linking_qr_ts")
        val KEY_LAST_SCREEN = stringPreferencesKey("last_active_screen")
        val KEY_IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
    }

    private val dataStore = context.sessionDataStore

    val lastScreenFlow: Flow<String?> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> preferences[KEY_LAST_SCREEN] }

    val isLoggedInFlow: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> preferences[KEY_IS_LOGGED_IN] ?: (preferences[KEY_USER_ROLE] != null) }

    /**
     * Flow of the currently authenticated/authorized user role (e.g., "GERENTE", "RECEPCION", null)
     */
    val userRoleFlow: Flow<String?> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_USER_ROLE]
        }

    /**
     * Flow of device authorization status
     */
    val isDeviceAuthorizedFlow: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_IS_DEVICE_AUTHORIZED] ?: false
        }

    /**
     * Flow of the full active user session
     */
    val userSessionFlow: Flow<UserSession> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            UserSession(
                userRole = preferences[KEY_USER_ROLE],
                userEmail = preferences[KEY_USER_EMAIL],
                userName = preferences[KEY_USER_NAME],
                isDeviceAuthorized = preferences[KEY_IS_DEVICE_AUTHORIZED] ?: false,
                deviceId = preferences[KEY_DEVICE_ID] ?: "",
                authToken = preferences[KEY_AUTH_TOKEN]
            )
        }

    /**
     * Flow of active linking PIN
     */
    val activeLinkingPinFlow: Flow<Pair<String?, Long>> = dataStore.data
        .map { preferences ->
            Pair(
                preferences[KEY_ACTIVE_LINKING_PIN],
                preferences[KEY_ACTIVE_LINKING_PIN_TS] ?: 0L
            )
        }

    /**
     * Flow of active linking QR token
     */
    val activeLinkingQrFlow: Flow<Pair<String?, Long>> = dataStore.data
        .map { preferences ->
            Pair(
                preferences[KEY_ACTIVE_LINKING_QR],
                preferences[KEY_ACTIVE_LINKING_QR_TS] ?: 0L
            )
        }

    /**
     * Saves user session data to persistent DataStore
     */
    suspend fun saveSession(
        userRole: String,
        userEmail: String,
        userName: String,
        authToken: String? = null
    ) {
        dataStore.edit { preferences ->
            preferences[KEY_IS_LOGGED_IN] = true
            preferences[KEY_USER_ROLE] = userRole.uppercase()
            preferences[KEY_USER_EMAIL] = userEmail
            preferences[KEY_USER_NAME] = userName
            if (authToken != null) {
                preferences[KEY_AUTH_TOKEN] = authToken
            }
        }
    }

    /**
     * Saves the current active screen so app returns to the same view when reopened
     */
    suspend fun saveLastScreen(screenName: String) {
        dataStore.edit { preferences ->
            preferences[KEY_LAST_SCREEN] = screenName
        }
    }

    /**
     * Saves device authorization along with assigned role into DataStore
     */
    suspend fun saveDeviceAuthorization(
        deviceId: String,
        role: String,
        email: String,
        token: String? = null
    ) {
        dataStore.edit { preferences ->
            preferences[KEY_IS_DEVICE_AUTHORIZED] = true
            preferences[KEY_DEVICE_ID] = deviceId
            preferences[KEY_USER_ROLE] = role.uppercase()
            preferences[KEY_USER_EMAIL] = email
            if (token != null) {
                preferences[KEY_AUTH_TOKEN] = token
            }
        }
    }

    /**
     * Saves active temporary PIN for pairing
     */
    suspend fun saveActiveLinkingPin(pin: String, timestamp: Long) {
        dataStore.edit { preferences ->
            preferences[KEY_ACTIVE_LINKING_PIN] = pin
            preferences[KEY_ACTIVE_LINKING_PIN_TS] = timestamp
        }
    }

    /**
     * Saves active temporary QR session token
     */
    suspend fun saveActiveLinkingQr(qrToken: String, timestamp: Long) {
        dataStore.edit { preferences ->
            preferences[KEY_ACTIVE_LINKING_QR] = qrToken
            preferences[KEY_ACTIVE_LINKING_QR_TS] = timestamp
        }
    }

    /**
     * Clears all session data on logout while retaining authorization if desired
     */
    suspend fun clearSession() {
        dataStore.edit { preferences ->
            preferences[KEY_IS_LOGGED_IN] = false
            preferences.remove(KEY_USER_ROLE)
            preferences.remove(KEY_USER_EMAIL)
            preferences.remove(KEY_USER_NAME)
            preferences.remove(KEY_AUTH_TOKEN)
            preferences.remove(KEY_LAST_SCREEN)
        }
    }

    /**
     * Completely unlinks and clears device authorization
     */
    suspend fun clearDeviceAuthorization() {
        dataStore.edit { preferences ->
            preferences[KEY_IS_DEVICE_AUTHORIZED] = false
            preferences.remove(KEY_USER_ROLE)
            preferences.remove(KEY_USER_EMAIL)
            preferences.remove(KEY_AUTH_TOKEN)
            preferences.remove(KEY_ACTIVE_LINKING_PIN)
            preferences.remove(KEY_ACTIVE_LINKING_QR)
        }
    }

    suspend fun getLastScreen(): String? = lastScreenFlow.first()

    suspend fun isLoggedIn(): Boolean = isLoggedInFlow.first()

    suspend fun getUserRole(): String? = userRoleFlow.first()

    suspend fun getDeviceId(): String? = userSessionFlow.first().deviceId.ifEmpty { null }

    suspend fun getUserName(): String? = userSessionFlow.first().userName

    suspend fun isDeviceAuthorized(): Boolean = isDeviceAuthorizedFlow.first()

    suspend fun getUserSession(): UserSession = userSessionFlow.first()

    suspend fun getActivePin(): Pair<String?, Long> = activeLinkingPinFlow.first()

    suspend fun getActiveQrToken(): Pair<String?, Long> = activeLinkingQrFlow.first()
}
