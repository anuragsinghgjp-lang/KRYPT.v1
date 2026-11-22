package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.kryptDataStore by preferencesDataStore(name = "krypt_preferences")

data class GhostModeState(
    val enabled: Boolean = false,
    val expiresAtEpochMs: Long = 0L,
    val durationLabel: String = "Until Turned Off"
) {
    fun isEffectivelyActive(nowMs: Long = System.currentTimeMillis()): Boolean {
        if (!enabled) return false
        return expiresAtEpochMs == 0L || nowMs < expiresAtEpochMs
    }
}

class KryptPreferences(private val context: Context) {

    private val ghostEnabledKey = booleanPreferencesKey("ghost_enabled")
    private val ghostExpiresAtKey = longPreferencesKey("ghost_expires_at")
    private val ghostLabelKey = stringPreferencesKey("ghost_label")
    private val deviceSessionIdKey = stringPreferencesKey("device_session_id")
    private val failedPassAttemptsKey = intPreferencesKey("failed_pass_attempts")
    private val passCooldownUntilKey = longPreferencesKey("pass_cooldown_until")
    private val hideLockScreenKey = booleanPreferencesKey("hide_lock_screen")

    val ghostModeFlow: Flow<GhostModeState> = context.kryptDataStore.data.map { prefs ->
        val enabled = prefs[ghostEnabledKey] ?: false
        val expiresAt = prefs[ghostExpiresAtKey] ?: 0L
        val label = prefs[ghostLabelKey] ?: "Until Turned Off"
        val now = System.currentTimeMillis()
        if (enabled && expiresAt > 0L && now >= expiresAt) {
            GhostModeState(enabled = false, expiresAtEpochMs = 0L, durationLabel = label)
        } else {
            GhostModeState(enabled = enabled, expiresAtEpochMs = expiresAt, durationLabel = label)
        }
    }

    val hideLockScreenFlow: Flow<Boolean> = context.kryptDataStore.data.map { prefs ->
        prefs[hideLockScreenKey] ?: true
    }

    suspend fun setGhostMode(enabled: Boolean, durationMs: Long?, label: String) {
        val expiresAt = if (enabled && durationMs != null) {
            System.currentTimeMillis() + durationMs
        } else {
            0L
        }
        context.kryptDataStore.edit { prefs ->
            prefs[ghostEnabledKey] = enabled
            prefs[ghostExpiresAtKey] = expiresAt
            prefs[ghostLabelKey] = label
        }
    }

    suspend fun setHideLockScreen(hidden: Boolean) {
        context.kryptDataStore.edit { prefs ->
            prefs[hideLockScreenKey] = hidden
        }
    }

    suspend fun getOrCreateDeviceSessionId(): String {
        var id = ""
        context.kryptDataStore.edit { prefs ->
            val existing = prefs[deviceSessionIdKey]
            id = if (!existing.isNullOrBlank()) existing else {
                val created = "sess_${UUID.randomUUID().toString().replace("-", "").take(16)}"
                prefs[deviceSessionIdKey] = created
                created
            }
        }
        return id
    }

    suspend fun recordFailedPassAttempt(): Long {
        var cooldownUntil = 0L
        context.kryptDataStore.edit { prefs ->
            val attempts = (prefs[failedPassAttemptsKey] ?: 0) + 1
            prefs[failedPassAttemptsKey] = attempts
            if (attempts >= 5) {
                cooldownUntil = System.currentTimeMillis() + 60_000L * (attempts - 4)
                prefs[passCooldownUntilKey] = cooldownUntil
            }
        }
        return cooldownUntil
    }

    suspend fun clearFailedPassAttempts() {
        context.kryptDataStore.edit { prefs ->
            prefs[failedPassAttemptsKey] = 0
            prefs[passCooldownUntilKey] = 0L
        }
    }

    val passCooldownFlow: Flow<Long> = context.kryptDataStore.data.map { prefs ->
        prefs[passCooldownUntilKey] ?: 0L
    }
}
