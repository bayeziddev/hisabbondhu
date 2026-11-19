package com.hisabbondhu.app.data.repo

import androidx.room.withTransaction
import com.hisabbondhu.app.core.Dates
import com.hisabbondhu.app.core.newId
import com.hisabbondhu.app.data.local.AppDatabase
import com.hisabbondhu.app.data.local.PartyEntity
import com.hisabbondhu.app.data.local.ProductEntity
import com.hisabbondhu.app.data.local.TxEntity
import com.hisabbondhu.app.data.prefs.SettingsStore
import com.hisabbondhu.app.domain.Ledger
import com.hisabbondhu.app.domain.PartyType
import com.hisabbondhu.app.domain.PayMethod
import com.hisabbondhu.app.domain.SyncState
import com.hisabbondhu.app.domain.TxType

data class TxDraft(
    val type: TxType,
    val partyId: String? = null,
    val productId: String? = null,
    val qtyMilli: Long = 0,
    val ratePaisa: Long = 0,
    val discountPaisa: Long = 0,
    val totalPaisa: Long,
    val paidPaisa: Long,
    val method: PayMethod = PayMethod.CASH,
    val category: String? = null,
    val note: String? = null,
    val occurredAt: Long = Dates.now(),
)

/** Validation failure with a machine-readable reason that the UI maps to a Bangla message. */
class LedgerValidationException(val reason: Reason) : IllegalArgumentException(reason.name) {
    enum class Reason { AMOUNT_REQUIRED, PAID_TOO_HIGH, PARTY_REQUIRED, PRODUCT_REQUIRED, QTY_REQUIRED, ALREADY_REVERSED, NOT_FOUND }
}

class LedgerRepository(
    private val db: AppDatabase,
    private val settings: SettingsStore,
    private val onLocalChange: () -> Unit,
) {
    private val dao = db.dao()

    // ---- Reads (Room Flows re-emit automatically after every write) ----
    fun parties(type: PartyType) = dao.observeParties(type)
    fun allParties() = dao.observeAllParties()
    fun party(id: String) = dao.observeParty(id)
    fun partyTransactions(id: String) = dao.observePartyTx(id)
    suspend fun partyStatementRows(id: String) = dao.partyTxAscending(id)
    suspend fun partyWithBalance(id: String) = dao.partyWithBalance(id)

    fun products() = dao.observeProducts()
    fun product(id: String) = dao.observeProduct(id)
    fun productTransactions(id: String) = dao.observeProductTx(id)
    suspend fun getProduct(id: String) = dao.getProduct(id)
    suspend fun getParty(id: String) = dao.getParty(id)

    fun recent(limit: Int) = dao.observeRecent(limit)
    fun range(from: Long, to: Long, limit: Int = 500) = dao.observeRange(from, to, limit)
    fun transaction(id: String) = dao.observeTxRow(id)

    fun summary(from: Long, to: Long) = dao.observeSummary(from, to)
    fun methodTotals(from: Long, to: Long) = dao.observeMethodTotals(from, to)
    fun balanceByMethod() = dao.observeBalanceByMethod()
    fun expenseByCategory(from: Long, to: Long) = dao.observeExpenseByCategory(from, to)
    fun dueTotals() = dao.observeDueTotals()
    fun lowStockCount() = dao.observeLowStockCount()
    fun stockValue() = dao.observeStockValue()
    fun pending() = dao.observePending()

    // ---- Writes ----
    suspend fun saveParty(
        id: String?,
        type: PartyType,
        name: String,
        phone: String?,
        address: String?,
        note: String?,
        openingBalancePaisa: Long,
        reminderAt: Long?,
    ): String {
        val now = Dates.now()
        val existing = id?.let { dao.getParty(it) }
        val entity = PartyEntity(
            id = existing?.id ?: newId(),
            type = existing?.type ?: type,
            name = name.trim(),
            phone = phone?.trim()?.ifBlank { null },
            address = address?.trim()?.ifBlank { null },
            note = note?.trim()?.ifBlank { null },
            openingBalancePaisa = openingBalancePaisa,
            reminderAt = reminderAt,
            createdAt = existing?.createdAt ?: now,
            updatedAt = maxOf(now, (existing?.updatedAt ?: 0) + 1),
            deleted = false,
            deviceId = settings.deviceId(),
            syncState = SyncState.PENDING,
        )
        dao.upsertParty(entity)
        onLocalChange()
        return entity.id
    }

    suspend fun setReminder(partyId: String, reminderAt: Long?) {
        val p = dao.getParty(partyId) ?: return
        dao.upsertParty(
            p.copy(
                reminderAt = reminderAt,
                updatedAt = maxOf(Dates.now(), p.updatedAt + 1),
                deviceId = settings.deviceId(),
                syncState = SyncState.PENDING,
            ),
        )
        onLocalChange()
    }

    suspend fun deleteParty(id: String) {
        val p = dao.getParty(id) ?: return
        dao.upsertParty(
            p.copy(
                deleted = true,
                updatedAt = maxOf(Dates.now(), p.updatedAt + 1),
                deviceId = settings.deviceId(),
                syncState = SyncState.PENDING,
            ),
        )
        onLocalChange()
    }

    suspend fun saveProduct(
        id: String?,
        name: String,
        category: String?,
        sku: String?,
        barcode: String?,
        unit: String,
        purchasePricePaisa: Long,
        salePricePaisa: Long,
        openingStockMilli: Long,
        minStockMilli: Long,
    ): String {
        val now = Dates.now()
        val existing = id?.let { dao.getProduct(it) }
        val entity = ProductEntity(
            id = existing?.id ?: newId(),
            name = name.trim(),
            category = category?.trim()?.ifBlank { null },
            sku = sku?.trim()?.ifBlank { null },
            barcode = barcode?.trim()?.ifBlank { null },
            unit = unit,
            purchasePricePaisa = purchasePricePaisa,
            salePricePaisa = salePricePaisa,
            openingStockMilli = openingStockMilli,
            minStockMilli = minStockMilli,
            createdAt = existing?.createdAt ?: now,
            updatedAt = maxOf(now, (existing?.updatedAt ?: 0) + 1),
            deleted = false,
            deviceId = settings.deviceId(),
            syncState = SyncState.PENDING,
        )
        dao.upsertProduct(entity)
        onLocalChange()
        return entity.id
    }

    suspend fun deleteProduct(id: String) {
        val p = dao.getProduct(id) ?: return
        dao.upsertProduct(
            p.copy(
                deleted = true,
                updatedAt = maxOf(Dates.now(), p.updatedAt + 1),
                deviceId = settings.deviceId(),
                syncState = SyncState.PENDING,
            ),
        )
        onLocalChange()
    }

    /** Validates and stores a new transaction. Ledger, stock and cash update together (one SQL row). */
    suspend fun addTransaction(draft: TxDraft): TxEntity {
        validate(draft)
        val product = draft.productId?.let { dao.getProduct(it) }
        val unitCost = if (draft.type == TxType.SALE || draft.type == TxType.SALE_RETURN) {
            product?.purchasePricePaisa ?: 0
        } else {
            0
        }
        val now = Dates.now()
        val tx = TxEntity(
            id = newId(),
            type = draft.type,
            partyId = draft.partyId,
            productId = draft.productId,
            qtyMilli = if (product != null) draft.qtyMilli else 0,
            ratePaisa = draft.ratePaisa,
            unitCostPaisa = unitCost,
            discountPaisa = draft.discountPaisa,
            totalPaisa = draft.totalPaisa,
            paidPaisa = draft.paidPaisa,
            method = draft.method,
            category = draft.category,
            note = draft.note?.trim()?.ifBlank { null },
            occurredAt = draft.occurredAt,
            createdAt = now,
            deviceId = settings.deviceId(),
            reversalOf = null,
        )
        dao.insertTx(tx)
        onLocalChange()
        return tx
    }

    /** Cancels a transaction by appending a mirror row with negated amounts (audit-safe). */
    suspend fun reverseTransaction(id: String): TxEntity {
        val reversal = db.withTransaction {
            val original = dao.getTx(id)
                ?: throw LedgerValidationException(LedgerValidationException.Reason.NOT_FOUND)
            if (original.reversalOf != null || dao.reversalCount(id) > 0) {
                throw LedgerValidationException(LedgerValidationException.Reason.ALREADY_REVERSED)
            }
            val r = original.copy(
                id = newId(),
                qtyMilli = -original.qtyMilli,
                totalPaisa = -original.totalPaisa,
                paidPaisa = -original.paidPaisa,
                discountPaisa = -original.discountPaisa,
                createdAt = Dates.now(),
                deviceId = settings.deviceId(),
                reversalOf = original.id,
                syncState = SyncState.PENDING,
            )
            dao.insertTx(r)
            r
        }
        onLocalChange()
        return reversal
    }

    private fun validate(d: TxDraft) {
        fun fail(r: LedgerValidationException.Reason): Nothing = throw LedgerValidationException(r)
        if (d.type == TxType.STOCK_ADJUST) {
            if (d.productId == null) fail(LedgerValidationException.Reason.PRODUCT_REQUIRED)
            if (d.qtyMilli == 0L) fail(LedgerValidationException.Reason.QTY_REQUIRED)
            return
        }
        if (d.totalPaisa <= 0) fail(LedgerValidationException.Reason.AMOUNT_REQUIRED)
        if (d.paidPaisa < 0 || d.paidPaisa > d.totalPaisa) fail(LedgerValidationException.Reason.PAID_TOO_HIGH)
        if (Ledger.needsParty(d.type) && d.partyId == null) fail(LedgerValidationException.Reason.PARTY_REQUIRED)
        if (Ledger.allowsCredit(d.type) && d.paidPaisa < d.totalPaisa && d.partyId == null) {
            fail(LedgerValidationException.Reason.PARTY_REQUIRED)
        }
        if (d.productId != null && d.qtyMilli <= 0) fail(LedgerValidationException.Reason.QTY_REQUIRED)
    }
}
