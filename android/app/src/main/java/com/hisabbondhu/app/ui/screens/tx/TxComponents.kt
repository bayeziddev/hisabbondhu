package com.hisabbondhu.app.ui.screens.tx

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hisabbondhu.app.R
import com.hisabbondhu.app.data.local.TxRow
import com.hisabbondhu.app.domain.TxType
import com.hisabbondhu.app.ui.color
import com.hisabbondhu.app.ui.components.IconBadge
import com.hisabbondhu.app.ui.date
import com.hisabbondhu.app.ui.expenseCategoryRes
import com.hisabbondhu.app.ui.icon
import com.hisabbondhu.app.ui.labelRes
import com.hisabbondhu.app.ui.money
import com.hisabbondhu.app.ui.qty
import com.hisabbondhu.app.ui.theme.LocalMoneyColors
import com.hisabbondhu.app.ui.time

/** One ledger line used on Home, party profile, product history and the full history screen. */
@Composable
fun TxListItem(row: TxRow, showDate: Boolean = true, onClick: () -> Unit) {
    val t = row.tx
    val typeLabel = stringResource(t.type.labelRes())
    val title = when {
        row.partyName != null -> row.partyName
        row.productName != null -> row.productName
        t.type == TxType.EXPENSE -> stringResource(expenseCategoryRes(t.category))
        else -> typeLabel
    }
    val dateText = date(t.occurredAt)
    val timeText = time(t.occurredAt)
    val sub = buildList {
        add(typeLabel)
        if (row.partyName != null && row.productName != null) add(row.productName)
        add(if (showDate) "$dateText • $timeText" else timeText)
    }.joinToString(" • ")
    val amountText = when (t.type) {
        TxType.STOCK_ADJUST -> qty(t.qtyMilli) + " " + (row.productUnit ?: "")
        TxType.PAYMENT_IN, TxType.PAYMENT_OUT -> money(t.paidPaisa)
        else -> money(t.totalPaisa)
    }
    val due = t.totalPaisa - t.paidPaisa
    val mc = LocalMoneyColors.current

    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(t.type.icon(), t.type.color())
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (row.isReversed) TextDecoration.LineThrough else null,
            )
            Text(
                sub,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            t.note?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                amountText,
                style = MaterialTheme.typography.titleMedium,
                color = if (row.isReversed) MaterialTheme.colorScheme.outline else t.type.color(),
                textDecoration = if (row.isReversed) TextDecoration.LineThrough else null,
                textAlign = TextAlign.End,
            )
            when {
                row.isReversed -> Tag(stringResource(R.string.cancelled), MaterialTheme.colorScheme.outline)
                due > 0 && t.type == TxType.SALE -> Text(
                    stringResource(R.string.due_amount, money(due)),
                    style = MaterialTheme.typography.labelSmall,
                    color = mc.negative,
                )
                due > 0 && t.type == TxType.PURCHASE -> Text(
                    stringResource(R.string.payable_amount, money(due)),
                    style = MaterialTheme.typography.labelSmall,
                    color = mc.warning,
                )
            }
        }
    }
}

@Composable
fun Tag(text: String, color: androidx.compose.ui.graphics.Color) {
    Surface(color = color.copy(alpha = 0.12f), contentColor = color, shape = MaterialTheme.shapes.extraSmall) {
        Text(text, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
    }
}
