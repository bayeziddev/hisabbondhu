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
    val email: String? = null,
    val name: String? = null,
    val phone: String? = null,
    val marketingOptIn: Boolean = false,
    val accessToken: String? = null,
    val refreshToken: String? = null,
) {
    val loggedIn: Boolean get() = !refreshToken.isNullOrBlank()
    /** Every account must have a name and a mobile number. */
    val profileComplete: Boolean get() = !name.isNullOrBlank() && !phone.isNullOrBlank()
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
        val bizLinked = booleanPreferencesKey("biz_linked")

        val userId = stringPreferencesKey("auth_user_id")
        val userEmail = stringPreferencesKey("auth_email")
        val userName = stringPreferencesKey("auth_name")
        val userPhone = stringPreferencesKey("auth_phone")
        val userOptIn = booleanPreferencesKey("auth_marketing")
        val dataOwner = stringPreferencesKey("data_owner_user_id")
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
        AuthState(
            userId = p[K.userId],
            email = p[K.userEmail],
            name = p[K.userName],
            phone = p[K.userPhone],
            marketingOptIn = p[K.userOptIn] ?: false,
            accessToken = p[K.accessToken],
            refreshToken = p[K.refreshToken],
        )
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

    suspend fun saveAuth(
        userId: String, email: String, name: String?, phone: String?, optIn: Boolean,
        access: String, refresh: String,
    ) {
        context.dataStore.edit {
            it[K.userId] = userId
            it[K.userEmail] = email
            if (name.isNullOrBlank()) it.remove(K.userName) else it[K.userName] = name
            if (phone.isNullOrBlank()) it.remove(K.userPhone) else it[K.userPhone] = phone
            it[K.userOptIn] = optIn
            it[K.accessToken] = access
            it[K.refreshToken] = refresh
            it[K.dataOwner] = userId
        }
    }

    suspend fun saveProfile(name: String?, phone: String?, optIn: Boolean) {
        context.dataStore.edit {
            if (name.isNullOrBlank()) it.remove(K.userName) else it[K.userName] = name
            if (phone.isNullOrBlank()) it.remove(K.userPhone) else it[K.userPhone] = phone
            it[K.userOptIn] = optIn
        }
    }

    /** The account that owns the data currently on this phone (survives session expiry). */
    suspend fun dataOwner(): String? = data.first()[K.dataOwner]

    suspend fun isBusinessLinked(): Boolean = data.first()[K.bizLinked] ?: false

    suspend fun setBusinessLinked(linked: Boolean) = context.dataStore.edit { it[K.bizLinked] = linked }

    suspend fun updateTokens(access: String, refresh: String) {
        context.dataStore.edit {
            it[K.accessToken] = access
            it[K.refreshToken] = refresh
        }
    }

    /** Session ended (expired or revoked). Local data stays; logging in again with the same account resumes it. */
    suspend fun clearAuth() {
        context.dataStore.edit {
            it.remove(K.accessToken); it.remove(K.refreshToken)
            it.remove(K.lastSyncError)
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

    /** Wipes everything (logout / delete). Keeps the chosen language so the user isn't asked again. */
    suspend fun clearAll(keepLanguage: Boolean = true) {
        context.dataStore.edit { prefs ->
            val lang = prefs[K.languageChosen]
            prefs.clear()
            if (keepLanguage && lang != null) prefs[K.languageChosen] = lang
        }
    }
}
