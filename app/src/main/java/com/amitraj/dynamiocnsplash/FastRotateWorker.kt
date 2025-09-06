package com.amitraj.dynamiocnsplash

import android.content.Context
import android.util.Log
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import androidx.core.content.edit

private const val TAG = "FastRotateWorker"
private const val PREFS = "icon_rotate_prefs"
private const val KEY_LAST = "last_flavor"
private const val UNIQUE_FAST_ROTATE = "fast_icon_rotate"

class FastRotateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        /** how long to wait before rescheduling itself — change for testing (SECONDS) or production (MINUTES/HOURS) */
        private const val RESCHEDULE_DELAY_MINUTES = 1L // 1 minute for quick testing
    }

    override suspend fun doWork(): Result {
        try {
            // 1) Compute next flavor to rotate to (persist last in SharedPreferences)
            val prefs = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val last = prefs.getString(KEY_LAST, LauncherFlavor.Default.name) ?: LauncherFlavor.Default.name
            val idx = LauncherFlavor.entries.indexOfFirst { it.name == last }.let { if (it >= 0) it else 0 }
            val next = LauncherFlavor.entries[(idx + 1) % LauncherFlavor.entries.size]

            // 2) Switch alias on Main thread (IconSwitcher expects @MainThread)
            withContext(Dispatchers.Main) {
                IconSwitcher.setIcon(applicationContext, next)
            }

            // 3) Persist the new last flavor
            prefs.edit { putString(KEY_LAST, next.name) }
            Log.d(TAG, "Rotated icon to ${next.name}")

            // 4) Re-enqueue a new one-time FastRotateWorker to run after a short delay.
            // Use enqueueUniqueWork with REPLACE to avoid accumulating jobs.
            val nextReq = OneTimeWorkRequestBuilder<FastRotateWorker>()
                .setInitialDelay(RESCHEDULE_DELAY_MINUTES, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(applicationContext)
                .enqueueUniqueWork(UNIQUE_FAST_ROTATE, ExistingWorkPolicy.REPLACE, nextReq)

            return Result.success()
        } catch (t: Throwable) {
            Log.w(TAG, "FastRotate failed", t)

            // In case of transient issues, retry (WorkManager will back off)
            return Result.retry()
        }
    }
}
