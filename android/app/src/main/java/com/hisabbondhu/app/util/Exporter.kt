package com.hisabbondhu.app.util

import android.content.Context
import com.hisabbondhu.app.AppContainer
import com.hisabbondhu.app.core.Dates
import com.hisabbondhu.app.data.remote.AppJson
import com.hisabbondhu.app.data.remote.dto.BusinessDto
import com.hisabbondhu.app.data.remote.dto.PartyDto
import com.hisabbondhu.app.data.remote.dto.ProductDto
import com.hisabbondhu.app.data.remote.dto.TxDto
import com.hisabbondhu.app.data.repo.toDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File
import java.time.format.DateTimeFormatter

@Serializable
data class BackupFile(
    val format: String = "hisabbondhu-backup",
    val version: Int = 1,
    @SerialName("exported_at") val exportedAt: Long,
    val business: BusinessDto?,
    val parties: List<PartyDto>,
    val products: List<ProductDto>,
    val transactions: List<TxDto>,
)

/** Data portability: users can always take their data out (CSV for Excel, JSON full backup). */
object Exporter {
    private val stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")
    private val isoLocal = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    private fun fileStamp(): String = java.time.ZonedDateTime.now(Dates.zone).format(stamp)

    suspend fun transactionsCsv(context: Context, container: AppContainer, from: Long, to: Long): File =
        withContext(Dispatchers.IO) {
            val rows = container.db.dao().rowsInRange(from, to)
            val file = File(Share.exportDir(context), "hisabbondhu-transactions-${fileStamp()}.csv")
            file.bufferedWriter(Charsets.UTF_8).use { w ->
                w.write("﻿") // BOM so Excel shows Bangla correctly
                w.write(
                    "date,type,party,product,quantity,rate_tk,discount_tk,total_tk,paid_tk,due_tk," +
                        "method,category,note,is_reversal,id\n",
                )
                rows.forEach { r ->
                    val t = r.tx
                    val cols = listOf(
                        java.time.Instant.ofEpochMilli(t.occurredAt).atZone(Dates.zone).format(isoLocal),
                        t.type.name,
                        r.partyName.orEmpty(),
                        r.productName.orEmpty(),
                        if (t.qtyMilli != 0L) (t.qtyMilli / 1000.0).toString() else "",
                        tk(t.ratePaisa),
                        tk(t.discountPaisa),
                        tk(t.totalPaisa),
                        tk(t.paidPaisa),
                        tk(t.totalPaisa - t.paidPaisa),
                        t.method.name,
                        t.category.orEmpty(),
                        t.note.orEmpty(),
                        if (t.reversalOf != null) "yes" else "",
                        t.id,
                    )
                    w.write(cols.joinToString(",") { csv(it) })
                    w.write("\n")
                }
            }
            file
        }

    suspend fun fullBackupJson(context: Context, container: AppContainer): File = withContext(Dispatchers.IO) {
        val dao = container.db.dao()
        val backup = BackupFile(
            exportedAt = Dates.now(),
            business = container.settings.currentBusiness()?.toDto(),
            parties = dao.allParties().map { it.toDto() },
            products = dao.allProducts().map { it.toDto() },
            transactions = dao.allTx().map { it.toDto() },
        )
        val file = File(Share.exportDir(context), "hisabbondhu-backup-${fileStamp()}.json")
        file.writeText(AppJson.encodeToString(BackupFile.serializer(), backup), Charsets.UTF_8)
        file
    }

    private fun tk(paisa: Long): String {
        val sign = if (paisa < 0) "-" else ""
        val a = kotlin.math.abs(paisa)
        return sign + (a / 100).toString() + "." + (a % 100).toString().padStart(2, '0')
    }

    private fun csv(value: String): String {
        val needsQuote = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        // Prevent CSV/formula injection when opened in spreadsheet apps.
        val safe = if (value.startsWith("=") || value.startsWith("+") || value.startsWith("@")) "'$value" else value
        return if (needsQuote) "\"" + safe.replace("\"", "\"\"") + "\"" else safe
    }
}
