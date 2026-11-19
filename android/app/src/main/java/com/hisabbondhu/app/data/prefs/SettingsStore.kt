package com.hisabbondhu.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hisabbondhu.app.core.newId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "hisab_settings")

data class BusinessProfile(
    val id: String,
    val name: String,
    val ownerName: String,
    val phone: String,
    val address: String,
    val type: String,
    val openingCashPaisa: Long,
    val updatedAt: Long,
)

data class AuthState(
    val userId: String? = null,
    val phone: String? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null,
) {
    val loggedIn: Boolean get() = !refreshToken.isNullOrBlank()
}

data class SyncInfo(
    val lastSyncAt: Long? = null,
    val lastError: String? = null,
)

data class SyncCursors(val parties: Long = 0, val products: Long = 0, val transactions: Long = 0)

class SettingsStore(private val context: Context) {
    private object K {
        val onboardingDone = booleanPreferencesKey("onboarding_done")
        val languageChosen = booleanPreferencesKey("language_chosen")
        val deviceId = stringPreferencesKey("device_id")

        val bizId = stringPreferencesKey("biz_id")
        val bizName = stringPreferencesKey("biz_name")
        val bizOwner = stringPreferencesKey("biz_owner")
        val bizPhone = stringPreferencesKey("biz_phone")
        val bizAddress = stringPreferencesKey("biz_address")
        val bizType = stringPreferencesKey("biz_type")
        val bizOpeningCash = longPreferencesKey("biz_opening_cash")
        val bizUpdatedAt = longPreferencesKey("biz_updated_at")
        val bizDirty = booleanPreferencesKey("biz_dirty")

        val userId = stringPreferencesKey("auth_user_id")
        val userPhone = stringPreferencesKey("auth_phone")
        val accessToken = stringPreferencesKey("auth_access")
        val refreshToken = stringPreferencesKey("auth_refresh")

        val lastSyncAt = longPreferencesKey("sync_last_at")
        val lastSyncError = stringPreferencesKey("sync_last_error")
        val cursorParties = longPreferencesKey("cursor_parties")
        val cursorProducts = longPreferencesKey("cursor_products")
        val cursorTx = longPreferencesKey("cursor_tx")

        val remindersEnabled = booleanPreferencesKey("reminders_enabled")
        val messageSignature = stringPreferencesKey("message_signature")
    }

    private val data = context.dataStore.data

    val onboardingDone: Flow<Boolean> = data.map { it[K.onboardingDone] ?: false }.distinctUntilChanged()
    val languageChosen: Flow<Boolean> = data.map { it[K.languageChosen] ?: false }.distinctUntilChanged()

    val business: Flow<BusinessProfile?> = data.map { p ->
        val id = p[K.bizId] ?: return@map null
        BusinessProfile(
            id = id,
            name = p[K.bizName].orEmpty(),
            ownerName = p[K.bizOwner].orEmpty(),
            phone = p[K.bizPhone].orEmpty(),
            address = p[K.bizAddress].orEmpty(),
            type = p[K.bizType] ?: "general_store",
            openingCashPaisa = p[K.bizOpeningCash] ?: 0L,
            updatedAt = p[K.bizUpdatedAt] ?: 0L,
        )
    }.distinctUntilChanged()

    val auth: Flow<AuthState> = data.map { p ->
        AuthState(p[K.userId], p[K.userPhone], p[K.accessToken], p[K.refreshToken])
    }.distinctUntilChanged()

    val syncInfo: Flow<SyncInfo> = data.map { SyncInfo(it[K.lastSyncAt], it[K.lastSyncError]) }.distinctUntilChanged()

    val remindersEnabled: Flow<Boolean> = data.map { it[K.remindersEnabled] ?: true }.distinctUntilChanged()

    suspend fun setLanguageChosen() = context.dataStore.edit { it[K.languageChosen] = true }

    suspend fun setOnboardingDone() = context.dataStore.edit { it[K.onboardingDone] = true }

    suspend fun setRemindersEnabled(enabled: Boolean) = context.dataStore.edit { it[K.remindersEnabled] = enabled }

    suspend fun deviceId(): String {
        data.first()[K.deviceId]?.let { return it }
        val id = newId()
        context.dataStore.edit { if (it[K.deviceId] == null) it[K.deviceId] = id }
        return data.first()[K.deviceId] ?: id
    }

    suspend fun currentBusiness(): BusinessProfile? = business.first()

    suspend fun saveBusiness(profile: BusinessProfile, dirty: Boolean) {
        context.dataStore.edit {
            it[K.bizId] = profile.id
            it[K.bizName] = profile.name
            it[K.bizOwner] = profile.ownerName
            it[K.bizPhone] = profile.phone
            it[K.bizAddress] = profile.address
            it[K.bizType] = profile.type
            it[K.bizOpeningCash] = profile.openingCashPaisa
            it[K.bizUpdatedAt] = profile.updatedAt
            it[K.bizDirty] = dirty
        }
    }

    suspend fun isBusinessDirty(): Boolean = data.first()[K.bizDirty] ?: false

    suspend fun clearBusinessDirty(updatedAt: Long) {
        context.dataStore.edit { if ((it[K.bizUpdatedAt] ?: 0L) == updatedAt) it[K.bizDirty] = false }
    }

    suspend fun currentAuth(): AuthState = auth.first()

    suspend fun saveAuth(userId: String, phone: String, access: String, refresh: String) {
        context.dataStore.edit {
            it[K.userId] = userId
            it[K.userPhone] = phone
            it[K.accessToken] = access
            it[K.refreshToken] = refresh
        }
    }

    suspend fun updateTokens(access: String, refresh: String) {
        context.dataStore.edit {
            it[K.accessToken] = access
            it[K.refreshToken] = refresh
        }
    }

    suspend fun clearAuth() {
        context.dataStore.edit {
            it.remove(K.userId); it.remove(K.userPhone); it.remove(K.accessToken); it.remove(K.refreshToken)
            it.remove(K.cursorParties); it.remove(K.cursorProducts); it.remove(K.cursorTx)
            it.remove(K.lastSyncAt); it.remove(K.lastSyncError)
        }
    }

    suspend fun cursors(): SyncCursors {
        val p = data.first()
        return SyncCursors(p[K.cursorParties] ?: 0, p[K.cursorProducts] ?: 0, p[K.cursorTx] ?: 0)
    }

    suspend fun saveCursors(c: SyncCursors) {
        context.dataStore.edit {
            it[K.cursorParties] = c.parties
            it[K.cursorProducts] = c.products
            it[K.cursorTx] = c.transactions
        }
    }

    suspend fun setSyncResult(at: Long?, error: String?) {
        context.dataStore.edit {
            if (at != null) it[K.lastSyncAt] = at
            if (error == null) it.remove(K.lastSyncError) else it[K.lastSyncError] = error
        }
    }

    /** Wipes everything (used by logout and "delete my data"). */
    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
