package com.hisabbondhu.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.hisabbondhu.app.domain.PartyType
import com.hisabbondhu.app.domain.SyncState
import kotlinx.coroutines.flow.Flow

/**
 * Ledger rules expressed in SQL. Balances are always *derived* from the append-only
 * transactions table, so data merged from several devices is automatically correct.
 * Keep in sync with [com.hisabbondhu.app.domain.Ledger].
 */
object LedgerSql {
    const val RECV_DELTA = "(CASE t.type " +
        "WHEN 'SALE' THEN t.totalPaisa - t.paidPaisa " +
        "WHEN 'PAYMENT_IN' THEN -t.paidPaisa " +
        "WHEN 'PAYMENT_OUT' THEN t.paidPaisa " +
        "WHEN 'SALE_RETURN' THEN t.paidPaisa - t.totalPaisa " +
        "WHEN 'PURCHASE' THEN t.paidPaisa - t.totalPaisa " +
        "WHEN 'PURCHASE_RETURN' THEN t.totalPaisa - t.paidPaisa " +
        "ELSE 0 END)"

    const val CASH_DELTA = "(CASE " +
        "WHEN t.type IN ('SALE','PAYMENT_IN','PURCHASE_RETURN','CASH_IN') THEN t.paidPaisa " +
        "WHEN t.type IN ('PURCHASE','PAYMENT_OUT','EXPENSE','SALE_RETURN','CASH_OUT') THEN -t.paidPaisa " +
        "ELSE 0 END)"

    const val STOCK_DELTA = "(CASE " +
        "WHEN t.type IN ('SALE','PURCHASE_RETURN') THEN -t.qtyMilli " +
        "WHEN t.type IN ('PURCHASE','SALE_RETURN','STOCK_ADJUST') THEN t.qtyMilli " +
        "ELSE 0 END)"

    const val PARTY_BALANCE = "((CASE WHEN p.type = 'CUSTOMER' THEN p.openingBalancePaisa ELSE -p.openingBalancePaisa END) " +
        "+ COALESCE((SELECT SUM($RECV_DELTA) FROM transactions t WHERE t.partyId = p.id), 0))"

    const val PARTY_LAST_TX = "(SELECT MAX(t.occurredAt) FROM transactions t WHERE t.partyId = p.id)"

    const val PARTY_SELECT = "SELECT p.*, $PARTY_BALANCE AS receivablePaisa, $PARTY_LAST_TX AS lastTxAt FROM parties p"

    const val PRODUCT_STOCK = "(p.openingStockMilli + COALESCE((SELECT SUM($STOCK_DELTA) FROM transactions t WHERE t.productId = p.id), 0))"

    const val PRODUCT_SELECT = "SELECT p.*, $PRODUCT_STOCK AS stockMilli FROM products p"

    const val TX_ROW_SELECT = "SELECT t.*, pa.name AS partyName, pa.phone AS partyPhone, " +
        "pr.name AS productName, pr.unit AS productUnit, " +
        "EXISTS(SELECT 1 FROM transactions r WHERE r.reversalOf = t.id) AS isReversed " +
        "FROM transactions t " +
        "LEFT JOIN parties pa ON pa.id = t.partyId " +
        "LEFT JOIN products pr ON pr.id = t.productId"

    const val SUMMARY_SELECT = "SELECT " +
        "COALESCE(SUM(CASE WHEN t.type = 'SALE' THEN t.totalPaisa ELSE 0 END), 0) AS sales, " +
        "COALESCE(SUM(CASE WHEN t.type = 'SALE' THEN t.totalPaisa - t.paidPaisa ELSE 0 END), 0) AS dueSales, " +
        "COALESCE(SUM(CASE WHEN t.type = 'PURCHASE' THEN t.totalPaisa ELSE 0 END), 0) AS purchases, " +
        "COALESCE(SUM(CASE WHEN t.type = 'EXPENSE' THEN t.totalPaisa ELSE 0 END), 0) AS expenses, " +
        "COALESCE(SUM(CASE WHEN t.type IN ('SALE','PAYMENT_IN','PURCHASE_RETURN','CASH_IN') THEN t.paidPaisa ELSE 0 END), 0) AS cashIn, " +
        "COALESCE(SUM(CASE WHEN t.type IN ('PURCHASE','PAYMENT_OUT','EXPENSE','SALE_RETURN','CASH_OUT') THEN t.paidPaisa ELSE 0 END), 0) AS cashOut, " +
        "COALESCE(SUM(CASE WHEN t.type = 'SALE_RETURN' THEN t.totalPaisa ELSE 0 END), 0) AS salesReturns, " +
        "COALESCE(SUM(CASE WHEN t.type = 'SALE' THEN t.qtyMilli * t.unitCostPaisa / 1000 ELSE 0 END), 0) AS costOfSales, " +
        "COALESCE(SUM(CASE WHEN t.type = 'SALE_RETURN' THEN t.qtyMilli * t.unitCostPaisa / 1000 ELSE 0 END), 0) AS costOfReturns, " +
        "COALESCE(SUM(CASE WHEN t.type = 'PAYMENT_IN' THEN t.paidPaisa ELSE 0 END), 0) AS paymentsIn, " +
        "COALESCE(SUM(CASE WHEN t.type = 'PAYMENT_OUT' THEN t.paidPaisa ELSE 0 END), 0) AS paymentsOut, " +
        "COUNT(CASE WHEN t.reversalOf IS NULL THEN 1 END) AS txCount " +
        "FROM transactions t WHERE t.occurredAt >= :from AND t.occurredAt < :to"
}

@Dao
interface LedgerDao {

    // ---------------- Parties ----------------
    @Upsert
    suspend fun upsertParty(party: PartyEntity)

    @Upsert
    suspend fun upsertParties(parties: List<PartyEntity>)

    @Query("SELECT * FROM parties WHERE id = :id")
    suspend fun getParty(id: String): PartyEntity?

    @Query("SELECT * FROM parties WHERE id IN (:ids)")
    suspend fun partiesByIds(ids: List<String>): List<PartyEntity>

    @Query("${LedgerSql.PARTY_SELECT} WHERE p.deleted = 0 AND p.type = :type ORDER BY p.name COLLATE NOCASE")
    fun observeParties(type: PartyType): Flow<List<PartyWithBalance>>

    @Query("${LedgerSql.PARTY_SELECT} WHERE p.deleted = 0 ORDER BY p.name COLLATE NOCASE")
    fun observeAllParties(): Flow<List<PartyWithBalance>>

    @Query("${LedgerSql.PARTY_SELECT} WHERE p.id = :id")
    fun observeParty(id: String): Flow<PartyWithBalance?>

    @Query("${LedgerSql.PARTY_SELECT} WHERE p.id = :id")
    suspend fun partyWithBalance(id: String): PartyWithBalance?

    @Query(
        "SELECT COALESCE(SUM(CASE WHEN bal > 0 THEN bal ELSE 0 END), 0) AS receivable, " +
            "COALESCE(SUM(CASE WHEN bal < 0 THEN -bal ELSE 0 END), 0) AS payable " +
            "FROM (SELECT ${LedgerSql.PARTY_BALANCE} AS bal FROM parties p WHERE p.deleted = 0)",
    )
    fun observeDueTotals(): Flow<DueTotals>

    @Query(
        "${LedgerSql.PARTY_SELECT} WHERE p.deleted = 0 AND p.type = 'CUSTOMER' " +
            "AND p.reminderAt IS NOT NULL AND p.reminderAt < :before",
    )
    suspend fun dueReminders(before: Long): List<PartyWithBalance>

    @Query("SELECT * FROM parties")
    suspend fun allParties(): List<PartyEntity>

    // ---------------- Products ----------------
    @Upsert
    suspend fun upsertProduct(product: ProductEntity)

    @Upsert
    suspend fun upsertProducts(products: List<ProductEntity>)

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProduct(id: String): ProductEntity?

    @Query("SELECT * FROM products WHERE id IN (:ids)")
    suspend fun productsByIds(ids: List<String>): List<ProductEntity>

    @Query("${LedgerSql.PRODUCT_SELECT} WHERE p.deleted = 0 ORDER BY p.name COLLATE NOCASE")
    fun observeProducts(): Flow<List<ProductWithStock>>

    @Query("${LedgerSql.PRODUCT_SELECT} WHERE p.id = :id")
    fun observeProduct(id: String): Flow<ProductWithStock?>

    @Query(
        "SELECT COUNT(*) FROM products p WHERE p.deleted = 0 AND p.minStockMilli > 0 " +
            "AND ${LedgerSql.PRODUCT_STOCK} <= p.minStockMilli",
    )
    fun observeLowStockCount(): Flow<Int>

    @Query(
        "SELECT COALESCE(SUM(CASE WHEN ${LedgerSql.PRODUCT_STOCK} > 0 " +
            "THEN ${LedgerSql.PRODUCT_STOCK} * p.purchasePricePaisa / 1000 ELSE 0 END), 0) " +
            "FROM products p WHERE p.deleted = 0",
    )
    fun observeStockValue(): Flow<Long>

    @Query("SELECT * FROM products")
    suspend fun allProducts(): List<ProductEntity>

    // ---------------- Transactions ----------------
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTx(tx: TxEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTxIgnore(list: List<TxEntity>)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTx(id: String): TxEntity?

    @Query("SELECT COUNT(*) FROM transactions WHERE reversalOf = :id")
    suspend fun reversalCount(id: String): Int

    @Query("${LedgerSql.TX_ROW_SELECT} WHERE t.id = :id")
    fun observeTxRow(id: String): Flow<TxRow?>

    @Query("${LedgerSql.TX_ROW_SELECT} WHERE t.reversalOf IS NULL ORDER BY t.occurredAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<TxRow>>

    @Query("${LedgerSql.TX_ROW_SELECT} WHERE t.reversalOf IS NULL AND t.partyId = :partyId ORDER BY t.occurredAt DESC")
    fun observePartyTx(partyId: String): Flow<List<TxRow>>

    @Query("${LedgerSql.TX_ROW_SELECT} WHERE t.reversalOf IS NULL AND t.partyId = :partyId ORDER BY t.occurredAt ASC")
    suspend fun partyTxAscending(partyId: String): List<TxRow>

    @Query("${LedgerSql.TX_ROW_SELECT} WHERE t.reversalOf IS NULL AND t.productId = :productId ORDER BY t.occurredAt DESC LIMIT 300")
    fun observeProductTx(productId: String): Flow<List<TxRow>>

    @Query(
        "${LedgerSql.TX_ROW_SELECT} WHERE t.reversalOf IS NULL AND t.occurredAt >= :from AND t.occurredAt < :to " +
            "ORDER BY t.occurredAt DESC LIMIT :limit",
    )
    fun observeRange(from: Long, to: Long, limit: Int): Flow<List<TxRow>>

    @Query("${LedgerSql.TX_ROW_SELECT} WHERE t.occurredAt >= :from AND t.occurredAt < :to ORDER BY t.occurredAt ASC")
    suspend fun rowsInRange(from: Long, to: Long): List<TxRow>

    @Query("SELECT * FROM transactions ORDER BY occurredAt ASC")
    suspend fun allTx(): List<TxEntity>

    @Query(LedgerSql.SUMMARY_SELECT)
    fun observeSummary(from: Long, to: Long): Flow<PeriodSummary>

    @Query(
        "SELECT t.method AS method, COALESCE(SUM(${LedgerSql.CASH_DELTA}), 0) AS amount FROM transactions t " +
            "WHERE t.occurredAt >= :from AND t.occurredAt < :to GROUP BY t.method",
    )
    fun observeMethodTotals(from: Long, to: Long): Flow<List<MethodTotal>>

    @Query("SELECT t.method AS method, COALESCE(SUM(${LedgerSql.CASH_DELTA}), 0) AS amount FROM transactions t GROUP BY t.method")
    fun observeBalanceByMethod(): Flow<List<MethodTotal>>

    @Query(
        "SELECT t.category AS category, COALESCE(SUM(t.totalPaisa), 0) AS amount FROM transactions t " +
            "WHERE t.type = 'EXPENSE' AND t.occurredAt >= :from AND t.occurredAt < :to " +
            "GROUP BY t.category ORDER BY amount DESC",
    )
    fun observeExpenseByCategory(from: Long, to: Long): Flow<List<ExpenseCategoryTotal>>

    // ---------------- Sync bookkeeping ----------------
    @Query("SELECT * FROM parties WHERE syncState = 'PENDING' ORDER BY updatedAt LIMIT :limit")
    suspend fun pendingParties(limit: Int): List<PartyEntity>

    @Query("SELECT * FROM products WHERE syncState = 'PENDING' ORDER BY updatedAt LIMIT :limit")
    suspend fun pendingProducts(limit: Int): List<ProductEntity>

    @Query("SELECT * FROM transactions WHERE syncState = 'PENDING' ORDER BY createdAt LIMIT :limit")
    suspend fun pendingTx(limit: Int): List<TxEntity>

    @Query("UPDATE parties SET syncState = :state WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun setPartyState(id: String, updatedAt: Long, state: SyncState)

    @Query("UPDATE products SET syncState = :state WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun setProductState(id: String, updatedAt: Long, state: SyncState)

    @Query("UPDATE transactions SET syncState = :state WHERE id IN (:ids)")
    suspend fun setTxState(ids: List<String>, state: SyncState)

    @Query(
        "SELECT (SELECT COUNT(*) FROM parties WHERE syncState = 'PENDING') AS parties, " +
            "(SELECT COUNT(*) FROM products WHERE syncState = 'PENDING') AS products, " +
            "(SELECT COUNT(*) FROM transactions WHERE syncState = 'PENDING') AS transactions",
    )
    fun observePending(): Flow<PendingCounts>

    @Query(
        "SELECT (SELECT COUNT(*) FROM parties WHERE syncState = 'PENDING') AS parties, " +
            "(SELECT COUNT(*) FROM products WHERE syncState = 'PENDING') AS products, " +
            "(SELECT COUNT(*) FROM transactions WHERE syncState = 'PENDING') AS transactions",
    )
    suspend fun pendingCounts(): PendingCounts

    @Query("UPDATE parties SET syncState = 'PENDING'")
    suspend fun markAllPartiesPending()

    @Query("UPDATE products SET syncState = 'PENDING'")
    suspend fun markAllProductsPending()

    @Query("UPDATE transactions SET syncState = 'PENDING'")
    suspend fun markAllTxPending()
}
