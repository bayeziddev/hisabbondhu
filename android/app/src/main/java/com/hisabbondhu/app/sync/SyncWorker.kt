package com.hisabbondhu.app.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hisabbondhu.app.HisabApp
import com.hisabbondhu.app.data.repo.SyncOutcome

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as HisabApp).container
        return when (val outcome = container.syncEngine.syncOnce()) {
            SyncOutcome.Success, SyncOutcome.NotLoggedIn -> Result.success()
            is SyncOutcome.Failure ->
                if (outcome.retryable && runAttemptCount < 10) Result.retry() else Result.failure()
        }
    }
}
