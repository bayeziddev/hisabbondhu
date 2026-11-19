package com.hisabbondhu.app.sync

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hisabbondhu.app.HisabApp
import com.hisabbondhu.app.MainActivity
import com.hisabbondhu.app.R
import com.hisabbondhu.app.core.Dates
import com.hisabbondhu.app.core.Money
import kotlinx.coroutines.flow.first

/** Once a day: notify the shop owner about customers whose collection date has arrived. */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as HisabApp).container
        if (!container.settings.remindersEnabled.first()) return Result.success()
        val endOfToday = Dates.startOfToday() + 24L * 60 * 60 * 1000
        val due = container.db.dao().dueReminders(endOfToday).filter { it.receivablePaisa > 0 }
        if (due.isEmpty()) return Result.success()
        Notifications.showDueReminder(
            applicationContext,
            count = due.size,
            total = due.sumOf { it.receivablePaisa },
        )
        return Result.success()
    }
}

object Notifications {
    const val CHANNEL_REMINDERS = "reminders"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_REMINDERS,
                    context.getString(R.string.notif_channel_reminders),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply { description = context.getString(R.string.notif_channel_reminders_desc) },
            )
        }
    }

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun showDueReminder(context: Context, count: Int, total: Long) {
        if (!canNotify(context)) return
        val bengali = context.resources.configuration.locales[0].language == "bn"
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context, 1, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val countText = if (bengali) Money.toBengaliDigits(count.toString()) else count.toString()
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentTitle(context.getString(R.string.notif_due_title, countText))
            .setContentText(context.getString(R.string.notif_due_text, Money.format(total, bengali)))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(1001, notification)
        } catch (_: SecurityException) {
            // Permission revoked between check and notify — ignore.
        }
    }
}
