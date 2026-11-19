package com.hisabbondhu.app.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.hisabbondhu.app.domain.PartyType
import com.hisabbondhu.app.domain.PayMethod
import com.hisabbondhu.app.domain.SyncState
import com.hisabbondhu.app.domain.TxType

@Entity(
    tableName = "parties",
    indices = [Index("type"), Index("syncState"), Index("name")],
)
data class PartyEntity(
    @PrimaryKey val id: String,
    val type: PartyType,
    val name: String,
    val phone: String?,
    val address: String?,
    val note: String?,
    /** Opening due in the party's natural direction (customer owes us / we owe supplier). */
    val openingBalancePaisa: Long,
    val reminderAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    val deleted: Boolean = false,
    val deviceId: String,
    val syncState: SyncState = SyncState.PENDING,
)

@Entity(
    tableName = "products",
    indices = [Index("syncState"), Index("name"), Index("barcode")],
)
data class ProductEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String?,
    val sku: String?,
    val barcode: String?,
    val unit: String,
    val purchasePricePaisa: Long,
    val salePricePaisa: Long,
    val openingStockMilli: Long,
    val minStockMilli: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val deleted: Boolean = false,
    val deviceId: String,
    val syncState: SyncState = SyncState.PENDING,
)

@Entity(
    tableName = "transactions",
    indices = [
        Index("occurredAt"), Index("partyId"), Index("productId"), Index("type"),
        Index("syncState"), Index("reversalOf"),
    ],
)
data class TxEntity(
    @PrimaryKey val id: String,
    val type: TxType,
    val partyId: String?,
    val productId: String?,
    val qtyMilli: Long,
    val ratePaisa: Long,
    /** Purchase price per unit at the time of a sale (for profit estimate). */
    val unitCostPaisa: Long,
    val discountPaisa: Long,
    val totalPaisa: Long,
    val paidPaisa: Long,
    val method: PayMethod,
    val category: String?,
    val note: String?,
    val occurredAt: Long,
    val createdAt: Long,
    val deviceId: String,
    val reversalOf: String?,
    val syncState: SyncState = SyncState.PENDING,
) {
    val duePaisa: Long get() = totalPaisa - paidPaisa
}

data class PartyWithBalance(
    @Embedded val party: PartyEntity,
    /** Positive = they owe us. Negative = we owe them. */
    val receivablePaisa: Long,
    val lastTxAt: Long?,
)

data class ProductWithStock(
    @Embedded val product: ProductEntity,
    val stockMilli: Long,
)

data class TxRow(
    @Embedded val tx: TxEntity,
    val partyName: String?,
    val partyPhone: String?,
    val productName: String?,
    val productUnit: String?,
    val isReversed: Boolean,
)

data class PeriodSummary(
    val sales: Long,
    val dueSales: Long,
    val purchases: Long,
    val expenses: Long,
    val cashIn: Long,
    val cashOut: Long,
    val salesReturns: Long,
    val costOfSales: Long,
    val costOfReturns: Long,
    val paymentsIn: Long,
    val paymentsOut: Long,
    val txCount: Int,
) {
    val netSales: Long get() = sales - salesReturns
    val estimatedProfit: Long get() = netSales - (costOfSales - costOfReturns) - expenses

    companion object {
        val EMPTY = PeriodSummary(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
    }
}

data class DueTotals(val receivable: Long, val payable: Long)

data class MethodTotal(val method: com.hisabbondhu.app.domain.PayMethod, val amount: Long)

data class ExpenseCategoryTotal(val category: String?, val amount: Long)

data class PendingCounts(val parties: Int, val products: Int, val transactions: Int) {
    val total: Int get() = parties + products + transactions
}
