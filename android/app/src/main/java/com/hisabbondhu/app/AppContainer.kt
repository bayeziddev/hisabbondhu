package com.hisabbondhu.app

import android.app.Application
import com.hisabbondhu.app.data.local.AppDatabase
import com.hisabbondhu.app.data.prefs.SettingsStore
import com.hisabbondhu.app.data.remote.Network
import com.hisabbondhu.app.data.repo.AuthRepository
import com.hisabbondhu.app.data.repo.LedgerRepository
import com.hisabbondhu.app.data.repo.SyncEngine
import com.hisabbondhu.app.sync.ConnectivityObserver
import com.hisabbondhu.app.sync.SyncScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Simple manual dependency container — fast startup, no reflection, easy to read. */
class AppContainer(app: Application) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val settings = SettingsStore(app)
    val db: AppDatabase = AppDatabase.build(app)
    val scheduler = SyncScheduler(app)
    val network = Network(settings)
    val connectivity = ConnectivityObserver(app)

    @Volatile
    private var loggedIn = false

    val ledger = LedgerRepository(db, settings) { if (loggedIn) scheduler.requestSync() }
    val syncEngine = SyncEngine(db, settings, network)
    val auth = AuthRepository(db, settings, network, scheduler)

    init {
        appScope.launch { settings.auth.collect { loggedIn = it.loggedIn } }
    }
}
