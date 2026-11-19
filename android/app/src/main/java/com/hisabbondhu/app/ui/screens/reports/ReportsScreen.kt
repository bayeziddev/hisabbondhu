package com.hisabbondhu.app.ui.screens.reports

import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.ListAlt
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.hisabbondhu.app.AppContainer
import com.hisabbondhu.app.R
import com.hisabbondhu.app.core.Dates
import com.hisabbondhu.app.core.ReportPeriod
import com.hisabbondhu.app.data.local.DueTotals
import com.hisabbondhu.app.data.local.ExpenseCategoryTotal
import com.hisabbondhu.app.data.local.MethodTotal
import com.hisabbondhu.app.data.local.PeriodSummary
import com.hisabbondhu.app.ui.appContainer
import com.hisabbondhu.app.ui.appViewModel
import com.hisabbondhu.app.ui.components.AppTopBar
import com.hisabbondhu.app.ui.components.LabelValue
import com.hisabbondhu.app.ui.components.NavRow
import com.hisabbondhu.app.ui.components.RowDivider
import com.hisabbondhu.app.ui.components.SectionCard
import com.hisabbondhu.app.ui.components.StatTile
import com.hisabbondhu.app.ui.expenseCategoryRes
import com.hisabbondhu.app.ui.labelRes
import com.hisabbondhu.app.ui.money
import com.hisabbondhu.app.ui.nav.Routes
import com.hisabbondhu.app.ui.num
import com.hisabbondhu.app.ui.theme.LocalMoneyColors
import com.hisabbondhu.app.util.Exporter
import com.hisabbondhu.app.util.Share
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

fun periodLabel(p: ReportPeriod): Int = when (p) {
    ReportPeriod.TODAY -> R.string.period_today
    ReportPeriod.YESTERDAY -> R.string.period_yesterday
    ReportPeriod.LAST_7_DAYS -> R.string.period_7_days
    ReportPeriod.THIS_MONTH -> R.string.period_this_month
    ReportPeriod.LAST_MONTH -> R.string.period_last_month
    ReportPeriod.ALL -> R.string.period_all
}

data class ReportsUi(
    val period: ReportPeriod = ReportPeriod.THIS_MONTH,
    val summary: PeriodSummary = PeriodSummary.EMPTY,
    val methods: List<MethodTotal> = emptyList(),
    val expenses: List<ExpenseCategoryTotal> = emptyList(),
    val dues: DueTotals = DueTotals(0, 0),
    val stockValue: Long = 0,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModel(c: AppContainer) : ViewModel() {
    val period = MutableStateFlow(ReportPeriod.THIS_MONTH)

    private val periodData = period.flatMapLatest { p ->
        val (from, to) = Dates.range(p)
        combine(c.ledger.summary(from, to), c.ledger.methodTotals(from, to), c.ledger.expenseByCategory(from, to)) { s, m, e ->
            Triple(p, s, m to e)
        }
    }

    val ui: StateFlow<ReportsUi> = combine(periodData, c.ledger.dueTotals(), c.ledger.stockValue()) { pd, dues, stock ->
        ReportsUi(pd.first, pd.second, pd.third.first, pd.third.second, dues, stock)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportsUi())
}

@Composable
fun ReportsScreen(nav: NavHostController) {
    val vm = appViewModel { c, _ -> ReportsViewModel(c) }
    val ui by vm.ui.collectAsStateWithLifecycle()
    val mc = LocalMoneyColors.current
    val context = LocalContext.current
    val container = appContainer()
    val scope = rememberCoroutineScope()
    val s = ui.summary

    Scaffold(topBar = { AppTopBar(stringResource(R.string.nav_reports)) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReportPeriod.entries.forEach { p ->
                        FilterChip(ui.period == p, { vm.period.value = p }, { Text(stringResource(periodLabel(p))) })
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile(stringResource(R.string.report_sales), money(s.netSales), mc.sale, Modifier.weight(1f))
                        StatTile(stringResource(R.string.report_purchases), money(s.purchases), mc.purchase, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile(stringResource(R.string.report_expenses), money(s.expenses), mc.expense, Modifier.weight(1f))
                        StatTile(
                            stringResource(R.string.report_profit),
                            money(s.estimatedProfit),
                            if (s.estimatedProfit >= 0) mc.positive else mc.negative,
                            Modifier.weight(1f),
                        )
                    }
                }
            }
            item {
                SectionCard(title = stringResource(R.string.cash_flow)) {
                    LabelValue(stringResource(R.string.cash_in_total), money(s.cashIn), mc.positive)
                    LabelValue(stringResource(R.string.cash_out_total), money(s.cashOut), mc.negative)
                    LabelValue(stringResource(R.string.net_cash), money(s.cashIn - s.cashOut))
                    LabelValue(stringResource(R.string.due_sales), money(s.dueSales), mc.negative)
                    LabelValue(stringResource(R.string.collections), money(s.paymentsIn), mc.paymentIn)
                    LabelValue(stringResource(R.string.transaction_count), num(s.txCount))
                }
            }
            if (ui.methods.any { it.amount != 0L }) {
                item {
                    SectionCard(title = stringResource(R.string.by_payment_method)) {
                        ui.methods.filter { it.amount != 0L }.forEach { m ->
                            LabelValue(stringResource(m.method.labelRes()), money(m.amount, plus = true))
                        }
                    }
                }
            }
            if (ui.expenses.isNotEmpty()) {
                item {
                    SectionCard(title = stringResource(R.string.expense_breakdown)) {
                        val max = ui.expenses.maxOf { it.amount }.coerceAtLeast(1)
                        ui.expenses.forEach { e ->
                            LabelValue(stringResource(expenseCategoryRes(e.category)), money(e.amount))
                            LinearProgressIndicator(
                                progress = { e.amount.toFloat() / max },
                                modifier = Modifier.fillMaxWidth().height(6.dp),
                                color = mc.expense,
                                trackColor = mc.expense.copy(alpha = 0.12f),
                            )
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
            }
            item {
                SectionCard(title = stringResource(R.string.balances_now)) {
                    LabelValue(stringResource(R.string.total_receivable), money(ui.dues.receivable), mc.negative)
                    LabelValue(stringResource(R.string.total_payable), money(ui.dues.payable), mc.warning)
                    LabelValue(stringResource(R.string.stock_value), money(ui.stockValue))
                }
            }
            item {
                SectionCard(title = stringResource(R.string.export_data), contentPadding = PaddingValues(vertical = 8.dp)) {
                    NavRow(Icons.Rounded.ListAlt, stringResource(R.string.all_transactions)) { nav.navigate(Routes.TRANSACTIONS) }
                    RowDivider()
                    NavRow(Icons.Rounded.TableChart, stringResource(R.string.export_csv), stringResource(R.string.export_csv_desc)) {
                        scope.launch {
                            val (from, to) = Dates.range(ui.period)
                            val file = Exporter.transactionsCsv(context, container, from, to)
                            Share.file(context, file, "text/csv", context.getString(R.string.export_csv))
                        }
                    }
                    RowDivider()
                    NavRow(Icons.Rounded.Backup, stringResource(R.string.export_backup), stringResource(R.string.export_backup_desc)) {
                        scope.launch {
                            val file = Exporter.fullBackupJson(context, container)
                            Share.file(context, file, "application/json", context.getString(R.string.export_backup))
                            Toast.makeText(context, R.string.backup_ready, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            item {
                Text(
                    stringResource(R.string.profit_disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
