package com.hisabbondhu.app.data.repo

import androidx.room.withTransaction
import com.hisabbondhu.app.core.Dates
import com.hisabbondhu.app.core.newId
import com.hisabbondhu.app.data.local.AppDatabase
import com.hisabbondhu.app.data.prefs.SettingsStore
import com.hisabbondhu.app.data.prefs.SyncCursors
import com.hisabbondhu.app.data.remote.Network
import com.hisabbondhu.app.data.remote.SessionExpiredException
import com.hisabbondhu.app.data.remote.apiErrorCode
import com.hisabbondhu.app.data.remote.dto.Cursors
import com.hisabbondhu.app.data.remote.dto.PullRequest
import com.hisabbondhu.app.data.remote.dto.PullResponse
import com.hisabbondhu.app.data.remote.dto.PushRequest
import com.hisabbondhu.app.domain.SyncState
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException
import java.io.IOException

sealed interface SyncOutcome {
    data object Success : SyncOutcome
    data object NotLoggedIn : SyncOutcome
    data class Failure(val code: String, val retryable: Boolean) : SyncOutcome
}

/**
 * Two-way sync: push PENDING rows (idempotent upserts keyed by UUID), then pull everything
 * newer than our per-table cursor. Transactions are append-only so merging never conflicts;
 * parties/products use last-write-wins on `updatedAt`.
 */
class SyncEngine(
    private val db: AppDatabase,
    private val settings: SettingsStore,
    private val network: Network,
) {
    private val dao = db.dao()
    private val mutex = Mutex()

    suspend fun syncOnce(): SyncOutcome {
        return mutex.withLock { syncLocked() }
    }

    private suspend fun syncLocked(): SyncOutcome {
        val auth = settings.currentAuth()
        if (!auth.loggedIn) return SyncOutcome.NotLoggedIn
        val business = settings.currentBusiness() ?: return SyncOutcome.NotLoggedIn
        return try {
            pushBusinessIfDirty()
            push(business.id)
            pull(business.id)
            settings.setSyncResult(Dates.now(), null)
            SyncOutcome.Success
        } catch (e: SessionExpiredException) {
            settings.setSyncResult(null, "session_expired")
            SyncOutcome.Failure("session_expired", retryable = false)
        } catch (e: HttpException) {
            val code = e.apiErrorCode() ?: "http_${e.code()}"
            if (e.code() == 401) settings.clearAuth()
            settings.setSyncResult(null, code)
            SyncOutcome.Failure(code, retryable = e.code() >= 500 || e.code() == 429)
        } catch (e: IOException) {
            settings.setSyncResult(null, "network")
            SyncOutcome.Failure("network", retryable = true)
        }
    }

    private suspend fun pushBusinessIfDirty() {
        if (!settings.isBusinessDirty()) return
        val biz = settings.currentBusiness() ?: return
        val res = network.api.updateBusiness(biz.id, biz.toDto())
        if (res.updatedAt > biz.updatedAt) settings.saveBusiness(res.toProfile(), dirty = false)
        else settings.clearBusinessDirty(biz.updatedAt)
    }

    private suspend fun push(businessId: String) {
        val deviceId = settings.deviceId()
        repeat(MAX_PUSH_ROUNDS) {
            val parties = dao.pendingParties(PUSH_BATCH)
            val products = dao.pendingProducts(PUSH_BATCH)
            val txs = dao.pendingTx(PUSH_BATCH)
            if (parties.isEmpty() && products.isEmpty() && txs.isEmpty()) return

            val res = network.api.push(
                businessId = businessId,
                idempotencyKey = newId(),
                body = PushRequest(
                    deviceId = deviceId,
                    parties = parties.map { it.toDto() },
                    products = products.map { it.toDto() },
                    transactions = txs.map { it.toDto() },
                ),
            )
            val rejected = res.rejected.groupBy({ it.entity }, { it.id }).mapValues { it.value.toSet() }
            db.withTransaction {
                parties.forEach {
                    val state = if (it.id in rejected["parties"].orEmpty()) SyncState.REJECTED else SyncState.SYNCED
                    dao.setPartyState(it.id, it.updatedAt, state)
                }
                products.forEach {
                    val state = if (it.id in rejected["products"].orEmpty()) SyncState.REJECTED else SyncState.SYNCED
                    dao.setProductState(it.id, it.updatedAt, state)
                }
                val rejectedTx = rejected["transactions"].orEmpty()
                val (bad, good) = txs.map { it.id }.partition { it in rejectedTx }
                if (good.isNotEmpty()) dao.setTxState(good, SyncState.SYNCED)
                if (bad.isNotEmpty()) dao.setTxState(bad, SyncState.REJECTED)
            }
            if (parties.size < PUSH_BATCH && products.size < PUSH_BATCH && txs.size < PUSH_BATCH) return
        }
    }

    private suspend fun pull(businessId: String) {
        var cursors = settings.cursors()
        repeat(MAX_PULL_ROUNDS) {
            val res = network.api.pull(
                businessId,
                PullRequest(Cursors(cursors.parties, cursors.products, cursors.transactions), PULL_LIMIT),
            )
            apply(res)
            cursors = SyncCursors(res.cursors.parties, res.cursors.products, res.cursors.transactions)
            settings.saveCursors(cursors)
            if (!res.hasMore) return
        }
    }

    private suspend fun apply(res: PullResponse) {
        res.business?.let { remote ->
            val local = settings.currentBusiness()
            if (local == null || (remote.updatedAt > local.updatedAt && !settings.isBusinessDirty())) {
                settings.saveBusiness(remote.toProfile(), dirty = false)
            }
        }
        db.withTransaction {
            if (res.parties.isNotEmpty()) {
                val local = dao.partiesByIds(res.parties.map { it.id }).associateBy { it.id }
                val incoming = res.parties.mapNotNull { dto ->
                    val l = local[dto.id]
                    val newer = l == null || dto.updatedAt > l.updatedAt ||
                        (dto.updatedAt == l.updatedAt && l.syncState != SyncState.PENDING)
                    if (newer) dto.toEntityOrNull() else null
                }
                if (incoming.isNotEmpty()) dao.upsertParties(incoming)
            }
            if (res.products.isNotEmpty()) {
                val local = dao.productsByIds(res.products.map { it.id }).associateBy { it.id }
                val incoming = res.products.filter { dto ->
                    val l = local[dto.id]
                    l == null || dto.updatedAt > l.updatedAt ||
                        (dto.updatedAt == l.updatedAt && l.syncState != SyncState.PENDING)
                }.map { it.toEntity() }
                if (incoming.isNotEmpty()) dao.upsertProducts(incoming)
            }
            if (res.transactions.isNotEmpty()) {
                dao.insertTxIgnore(res.transactions.mapNotNull { it.toEntityOrNull() })
            }
        }
    }

    companion object {
        private const val PUSH_BATCH = 200
        private const val PULL_LIMIT = 500
        private const val MAX_PUSH_ROUNDS = 50
        private const val MAX_PULL_ROUNDS = 200
    }
}
