package com.hisabbondhu.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.hisabbondhu.app.R
import com.hisabbondhu.app.core.Dates
import com.hisabbondhu.app.domain.TxType
import com.hisabbondhu.app.ui.appViewModel
import com.hisabbondhu.app.ui.color
import com.hisabbondhu.app.ui.components.AppTopBar
import com.hisabbondhu.app.ui.components.EmptyState
import com.hisabbondhu.app.ui.components.QuickAction
import com.hisabbondhu.app.ui.components.SectionCard
import com.hisabbondhu.app.ui.components.StatTile
import com.hisabbondhu.app.ui.components.SyncChip
import com.hisabbondhu.app.ui.date
import com.hisabbondhu.app.ui.icon
import com.hisabbondhu.app.ui.labelRes
import com.hisabbondhu.app.ui.money
import com.hisabbondhu.app.ui.nav.Routes
import com.hisabbondhu.app.ui.nav.switchTab
import com.hisabbondhu.app.ui.num
import com.hisabbondhu.app.ui.screens.tx.TxListItem
import com.hisabbondhu.app.ui.theme.LocalMoneyColors

@Composable
fun HomeScreen(nav: NavHostController) {
    val vm = appViewModel { c, _ -> HomeViewModel(c) }
    val ui by vm.ui.collectAsStateWithLifecycle()
    val mc = LocalMoneyColors.current

    Scaffold(
        topBar = {
            AppTopBar(
                title = ui.businessName.ifBlank { stringResource(R.string.app_name) },
                subtitle = date(Dates.now()),
                actions = {
                    SyncChip(ui.sync, ui.pending) { nav.navigate(Routes.ACCOUNT) }
                    Spacer(Modifier.width(8.dp))
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            if (!ui.online) {
                item { OfflineBanner() }
            }
            item {
                HeroCard(
                    cash = ui.cashBalance,
                    receivable = ui.dues.receivable,
                    payable = ui.dues.payable,
                    onDues = { nav.switchTab(Routes.PARTIES) },
                )
            }
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    QuickAction(TxType.SALE.icon(), stringResource(R.string.action_sale), mc.sale, Modifier.weight(1f)) {
                        nav.navigate(Routes.txNew(TxType.SALE))
                    }
                    QuickAction(TxType.PAYMENT_IN.icon(), stringResource(R.string.action_payment_in), mc.paymentIn, Modifier.weight(1f)) {
                        nav.navigate(Routes.txNew(TxType.PAYMENT_IN))
                    }
                    QuickAction(TxType.EXPENSE.icon(), stringResource(R.string.action_expense), mc.expense, Modifier.weight(1f)) {
                        nav.navigate(Routes.txNew(TxType.EXPENSE))
                    }
                    QuickAction(TxType.PURCHASE.icon(), stringResource(R.string.action_purchase), mc.purchase, Modifier.weight(1f)) {
                        nav.navigate(Routes.txNew(TxType.PURCHASE))
                    }
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        TxType.PAYMENT_OUT, TxType.SALE_RETURN, TxType.PURCHASE_RETURN,
                        TxType.STOCK_ADJUST, TxType.CASH_IN, TxType.CASH_OUT,
                    ).forEach { type ->
                        AssistChip(
                            onClick = { nav.navigate(Routes.txNew(type)) },
                            label = { Text(stringResource(type.labelRes())) },
                            leadingIcon = { Icon(type.icon(), null, Modifier.size(18.dp), tint = type.color()) },
                            colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.surface),
                        )
                    }
                }
            }
            item {
                SectionCard(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    title = stringResource(R.string.today_summary),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile(stringResource(R.string.today_sales), money(ui.today.netSales), mc.sale, Modifier.weight(1f))
                        StatTile(stringResource(R.string.today_cash_in), money(ui.today.cashIn), mc.paymentIn, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile(stringResource(R.string.today_due_sales), money(ui.today.dueSales), mc.negative, Modifier.weight(1f))
                        StatTile(stringResource(R.string.today_expense), money(ui.today.expenses), mc.expense, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                    StatTile(
                        stringResource(R.string.today_profit_estimate),
                        money(ui.today.estimatedProfit),
                        if (ui.today.estimatedProfit >= 0) mc.positive else mc.negative,
                        Modifier.fillMaxWidth(),
                        icon = Icons.Rounded.TrendingUp,
                    )
                }
            }
            if (ui.lowStock > 0) {
                item {
                    AlertRow(
                        text = stringResource(R.string.low_stock_alert, num(ui.lowStock)),
                        color = mc.warning,
                    ) { nav.switchTab(Routes.PRODUCTS) }
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.recent_transactions), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    TextButton(onClick = { nav.navigate(Routes.TRANSACTIONS) }) { Text(stringResource(R.string.see_all)) }
                }
            }
            if (ui.loaded && ui.recent.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Rounded.ReceiptLong,
                        title = stringResource(R.string.empty_home_title),
                        message = stringResource(R.string.empty_home_message),
                        actionLabel = stringResource(R.string.action_first_sale),
                        onAction = { nav.navigate(Routes.txNew(TxType.SALE)) },
                    )
                }
            }
            items(ui.recent, key = { it.tx.id }) { row ->
                TxListItem(row) { nav.navigate(Routes.tx(row.tx.id)) }
            }
        }
    }
}

@Composable
private fun HeroCard(cash: Long, receivable: Long, payable: Long, onDues: () -> Unit) {
    val mc = LocalMoneyColors.current
    Box(
        Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(Brush.linearGradient(listOf(mc.heroStart, mc.heroEnd))),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(stringResource(R.string.cash_in_hand), color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelLarge)
            Text(money(cash), color = Color.White, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable(onClick = onDues)) {
                HeroStat(stringResource(R.string.you_will_get), money(receivable), Modifier.weight(1f))
                Box(Modifier.width(1.dp).height(44.dp).background(Color.White.copy(alpha = 0.25f)).align(Alignment.CenterVertically))
                HeroStat(stringResource(R.string.you_will_give), money(payable), Modifier.weight(1f).padding(start = 14.dp))
            }
        }
    }
}

@Composable
private fun HeroStat(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(label, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelMedium)
        Text(value, color = Color.White, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun OfflineBanner() {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.WifiOff, null, Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.offline_banner), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun AlertRow(text: String, color: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = color.copy(alpha = 0.12f),
        contentColor = color,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Inventory2, null, Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}
