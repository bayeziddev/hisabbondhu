package com.hisabbondhu.app.util

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import com.hisabbondhu.app.R
import com.hisabbondhu.app.core.Dates
import com.hisabbondhu.app.core.Money
import com.hisabbondhu.app.data.local.PartyWithBalance
import com.hisabbondhu.app.data.local.TxRow
import com.hisabbondhu.app.domain.Ledger
import com.hisabbondhu.app.domain.PartyType
import com.hisabbondhu.app.ui.labelRes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Generates an A4 customer/supplier statement PDF using only the Android SDK (works offline). */
object PdfStatement {
    private const val W = 595
    private const val H = 842
    private const val M = 36f

    suspend fun create(
        context: Context,
        party: PartyWithBalance,
        rows: List<TxRow>,
        businessName: String,
        businessPhone: String,
    ): File = withContext(Dispatchers.IO) {
        val bn = context.resources.configuration.locales[0].language == "bn"
        val regular = ResourcesCompat.getFont(context, R.font.hind_siliguri_regular) ?: Typeface.DEFAULT
        val bold = ResourcesCompat.getFont(context, R.font.hind_siliguri_bold) ?: Typeface.DEFAULT_BOLD
        fun paint(size: Float, tf: Typeface, color: Int = Color.parseColor("#212529")) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size; typeface = tf; this.color = color
        }
        val title = paint(18f, bold, Color.parseColor("#087F5B"))
        val h = paint(11f, bold)
        val body = paint(10f, regular)
        val muted = paint(9f, regular, Color.parseColor("#6C757D"))
        val line = Paint().apply { color = Color.parseColor("#DEE2E6"); strokeWidth = 1f }
        fun m(p: Long) = Money.format(p, bn)

        val doc = PdfDocument()
        var pageNo = 0
        var page: PdfDocument.Page? = null
        var y = 0f

        fun text(s: String, x: Float, yy: Float, p: TextPaint, width: Int = 200, align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL): Float {
            val layout = StaticLayout.Builder.obtain(s, 0, s.length, p, width).setAlignment(align).build()
            val c = page!!.canvas
            c.save(); c.translate(x, yy); layout.draw(c); c.restore()
            return layout.height.toFloat()
        }

        val cols = floatArrayOf(M, M + 90, M + 260, M + 345, M + 430) // date, detail, debit, credit, balance
        fun header() {
            text(context.getString(R.string.pdf_col_date), cols[0], y, h, 85)
            text(context.getString(R.string.pdf_col_details), cols[1], y, h, 165)
            text(context.getString(R.string.pdf_col_given), cols[2], y, h, 80, Layout.Alignment.ALIGN_OPPOSITE)
            text(context.getString(R.string.pdf_col_got), cols[3], y, h, 80, Layout.Alignment.ALIGN_OPPOSITE)
            text(context.getString(R.string.pdf_col_balance), cols[4], y, h, 90, Layout.Alignment.ALIGN_OPPOSITE)
            y += 18f
            page!!.canvas.drawLine(M, y, W - M, y, line)
            y += 6f
        }

        fun newPage() {
            page?.let { doc.finishPage(it) }
            pageNo++
            page = doc.startPage(PdfDocument.PageInfo.Builder(W, H, pageNo).create())
            y = M
            if (pageNo == 1) {
                y += text(businessName.ifBlank { context.getString(R.string.app_name) }, M, y, title, (W - 2 * M).toInt())
                if (businessPhone.isNotBlank()) y += text(businessPhone, M, y, muted, 300)
                y += 10f
                y += text(context.getString(R.string.pdf_statement_title), M, y, h, 300)
                y += text(party.party.name + (party.party.phone?.let { "  •  $it" } ?: ""), M, y, body, 400)
                y += text(context.getString(R.string.pdf_generated_on, Dates.formatDateTime(Dates.now(), bn)), M, y, muted, 400)
                y += 14f
            }
            header()
        }

        newPage()
        val sign = if (party.party.type == PartyType.CUSTOMER) 1 else -1
        var running = party.party.openingBalancePaisa * sign
        if (party.party.openingBalancePaisa != 0L) {
            text(context.getString(R.string.opening_balance), cols[1], y, body, 165)
            text(m(running * sign), cols[4], y, body, 90, Layout.Alignment.ALIGN_OPPOSITE)
            y += 18f
        }
        rows.forEach { r ->
            if (y > H - 80) newPage()
            if (r.isReversed) return@forEach
            val t = r.tx
            val delta = Ledger.receivableDelta(t.type, t.totalPaisa, t.paidPaisa)
            running += delta
            val details = buildString {
                append(context.getString(t.type.labelRes()))
                r.productName?.let { append(" • ").append(it) }
                t.note?.let { append(" • ").append(it) }
            }
            text(Dates.formatDate(t.occurredAt, bn), cols[0], y, body, 85)
            val hgt = text(details, cols[1], y, body, 165)
            // "given" = value given to party (increases what they owe us); "got" = value received from them
            if (delta > 0) text(m(delta), cols[2], y, body, 80, Layout.Alignment.ALIGN_OPPOSITE)
            if (delta < 0) text(m(-delta), cols[3], y, body, 80, Layout.Alignment.ALIGN_OPPOSITE)
            text(m(running * sign), cols[4], y, body, 90, Layout.Alignment.ALIGN_OPPOSITE)
            y += maxOf(18f, hgt + 4f)
        }
        y += 6f
        page!!.canvas.drawLine(M, y, W - M, y, line)
        y += 10f
        val finalLabel = when {
            party.receivablePaisa > 0 -> context.getString(R.string.balance_they_owe)
            party.receivablePaisa < 0 -> context.getString(R.string.balance_you_owe)
            else -> context.getString(R.string.balance_settled)
        }
        text("$finalLabel: ${m(kotlin.math.abs(party.receivablePaisa))}", M, y, h, (W - 2 * M).toInt(), Layout.Alignment.ALIGN_OPPOSITE)
        y += 30f
        text(context.getString(R.string.pdf_footer), M, H - 40f, muted, (W - 2 * M).toInt(), Layout.Alignment.ALIGN_CENTER)
        doc.finishPage(page!!)

        val safeName = party.party.name.replace(Regex("[^\\p{L}\\p{N}]+"), "_").take(30)
        val file = File(Share.exportDir(context), "statement-$safeName-${System.currentTimeMillis()}.pdf")
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
        file
    }
}
