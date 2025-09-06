package com.amitraj.dynamiocnsplash

import android.content.Context
import androidx.work.*
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

object IconWorkScheduler {
    private const val UNIQUE_FAST_ROTATE = "fast_icon_rotate"
    private const val UNIQUE_ONE_TIME = "icon_alias_change_one_time"

    /**
     * Start the fast rotate chain immediately (for testing).
     * This enqueues a unique OneTime fast-rotate worker with no initial delay, which then reschedules itself.
     */
    fun startFastRotation(context: Context) {
        val req = OneTimeWorkRequestBuilder<FastRotateWorker>()
            .setInitialDelay(10, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(UNIQUE_FAST_ROTATE, ExistingWorkPolicy.REPLACE, req)
    }

    /** Stop the fast rotation chain by cancelling the unique work. */
    fun stopFastRotation(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_FAST_ROTATE)
    }

    /**
     * Enqueue a single immediate alias change (one-time work).
     * Useful if you want to switch to a specific flavor once (button tap).
     */
    fun enqueueOneTimeChange(context: Context, flavorName: String) = run {
        val input = Data.Builder().putString(IconAliasChangeWorker.INPUT_FLAVOR, flavorName).build()
        val req = OneTimeWorkRequestBuilder<IconAliasChangeWorker>()
            .setInputData(input)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(UNIQUE_ONE_TIME, ExistingWorkPolicy.REPLACE, req)
    }

    /**
     * Optional: schedule a one-time change at a specific LocalDateTime (device zone).
     * (keeps same wall-clock time; if input time is past it schedules for next day)
     */
    fun scheduleOneTimeAt(context: Context, flavorName: String, scheduledTime: LocalDateTime) {
        val now = LocalDateTime.now(ZoneId.systemDefault())
        var target = scheduledTime
        if (!target.isAfter(now)) target = target.plusDays(1)

        val delayMillis = Duration.between(now, target).toMillis()

        val input = Data.Builder().putString(IconAliasChangeWorker.INPUT_FLAVOR, flavorName).build()
        val req = OneTimeWorkRequestBuilder<IconAliasChangeWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(input)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(UNIQUE_ONE_TIME, ExistingWorkPolicy.REPLACE, req)
    }
}
