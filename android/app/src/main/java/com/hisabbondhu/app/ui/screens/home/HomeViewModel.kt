package com.hisabbondhu.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hisabbondhu.app.AppContainer
import com.hisabbondhu.app.core.Dates
import com.hisabbondhu.app.core.ReportPeriod
import com.hisabbondhu.app.data.local.DueTotals
import com.hisabbondhu.app.data.local.PeriodSummary
import com.hisabbondhu.app.data.local.TxRow
import com.hisabbondhu.app.ui.components.SyncUiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeUi(
    val businessName: String = "",
    val today: PeriodSummary = PeriodSummary.EMPTY,
    val dues: DueTotals = DueTotals(0, 0),
    val cashBalance: Long = 0,
    val lowStock: Int = 0,
    val recent: List<TxRow> = emptyList(),
    val online: Boolean = true,
    val sync: SyncUiState = SyncUiState.OFFLINE_ONLY,
    val pending: Int = 0,
    val loaded: Boolean = false,
)

fun syncUiState(loggedIn: Boolean, online: Boolean, running: Boolean, pending: Int, lastError: String?): SyncUiState = when {
    !loggedIn -> SyncUiState.OFFLINE_ONLY
    running -> SyncUiState.SYNCING
    !online && pending > 0 -> SyncUiState.OFFLINE_PENDING
    lastError != null && lastError != "network" -> SyncUiState.ERROR
    pending > 0 -> SyncUiState.PENDING
    else -> SyncUiState.SYNCED
}

class HomeViewModel(c: AppContainer) : ViewModel() {
    private val todayRange = Dates.range(ReportPeriod.TODAY)

    private val syncState = combine(
        c.settings.auth, c.connectivity.online, c.scheduler.running, c.ledger.pending(), c.settings.syncInfo,
    ) { auth, online, running, pending, info ->
        Triple(syncUiState(auth.loggedIn, online, running, pending.total, info.lastError), pending.total, online)
    }

    private val money = combine(
        c.ledger.summary(todayRange.first, todayRange.second),
        c.ledger.dueTotals(),
        c.ledger.balanceByMethod(),
        c.settings.business,
        c.ledger.lowStockCount(),
    ) { today, dues, methods, biz, low ->
        HomeUi(
            businessName = biz?.name.orEmpty(),
            today = today,
            dues = dues,
            cashBalance = (biz?.openingCashPaisa ?: 0) + methods.sumOf { it.amount },
            lowStock = low,
        )
    }

    val ui: StateFlow<HomeUi> = combine(money, c.ledger.recent(8), syncState) { m, recent, s ->
        m.copy(recent = recent, sync = s.first, pending = s.second, online = s.third, loaded = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUi())
}
