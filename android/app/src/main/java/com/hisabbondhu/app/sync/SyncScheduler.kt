package com.hisabbondhu.app.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

/**
 * WorkManager keeps sync alive across app kills and reboots, and only runs when online.
 */
class SyncScheduler(context: Context) {
    private val wm = WorkManager.getInstance(context)

    private val online = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun requestSync(delaySeconds: Long = 3) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(online)
            .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag(TAG_SYNC)
            .build()
        wm.enqueueUniqueWork(WORK_SYNC_NOW, ExistingWorkPolicy.REPLACE, request)
    }

    fun schedulePeriodic() {
        val periodic = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(online)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
            .addTag(TAG_SYNC)
            .build()
        wm.enqueueUniquePeriodicWork(WORK_SYNC_PERIODIC, ExistingPeriodicWorkPolicy.KEEP, periodic)

        val reminders = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS).build()
        wm.enqueueUniquePeriodicWork(WORK_REMINDERS, ExistingPeriodicWorkPolicy.KEEP, reminders)
    }

    /** true while a sync job is actively running. */
    val running: Flow<Boolean> = wm.getWorkInfosByTagFlow(TAG_SYNC)
        .map { list -> list.any { it.state == WorkInfo.State.RUNNING } }

    fun cancelAll() {
        wm.cancelUniqueWork(WORK_SYNC_NOW)
        wm.cancelUniqueWork(WORK_SYNC_PERIODIC)
    }

    companion object {
        const val TAG_SYNC = "hisab-sync"
        private const val WORK_SYNC_NOW = "sync-now"
        private const val WORK_SYNC_PERIODIC = "sync-periodic"
        private const val WORK_REMINDERS = "due-reminders"
    }
}
