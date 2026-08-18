package com.sleepsaver.app.reminder

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
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.sleepsaver.app.MainActivity
import com.sleepsaver.app.R
import com.sleepsaver.app.data.AppSettings
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

object ReminderConstants {
    const val UNIQUE_WORK_NAME = "sleep-saver-bedtime-reminder"
    const val CHANNEL_ID = "sleep-reminder"
    const val NOTIFICATION_ID = 2300
}

object SleepReminderScheduler {
    fun apply(context: Context, settings: AppSettings) {
        val manager = WorkManager.getInstance(context)
        if (!settings.reminderEnabled) {
            manager.cancelUniqueWork(ReminderConstants.UNIQUE_WORK_NAME)
            return
        }

        val now = ZonedDateTime.now()
        var next = now
            .withHour(settings.bedtimeHour)
            .withMinute(settings.bedtimeMinute)
            .withSecond(0)
            .withNano(0)
            .minusMinutes(settings.reminderAdvanceMinutes.toLong())
        if (!next.isAfter(now)) next = next.plusDays(1)

        val request = PeriodicWorkRequestBuilder<SleepReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(Duration.between(now, next))
            .build()

        manager.enqueueUniquePeriodicWork(
            ReminderConstants.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}

class SleepReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success()
        }

        createNotificationChannel()
        val openApp = PendingIntent.getActivity(
            applicationContext,
            0,
            Intent(applicationContext, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(
            applicationContext,
            ReminderConstants.CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_moon)
            .setContentTitle(applicationContext.getString(R.string.reminder_title))
            .setContentText(applicationContext.getString(R.string.reminder_body))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify(ReminderConstants.NOTIFICATION_ID, notification)
        return Result.success()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                ReminderConstants.CHANNEL_ID,
                applicationContext.getString(R.string.reminder_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
    }
}

