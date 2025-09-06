package com.amitraj.dynamiocnsplash

import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG_ALIAS = "IconAliasChangeWorker"

class IconAliasChangeWorker(
    context: android.content.Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val INPUT_FLAVOR = "input_flavor"
    }

    override suspend fun doWork(): Result {
        val flavorName = inputData.getString(INPUT_FLAVOR)
        if (flavorName.isNullOrBlank()) {
            Log.w(TAG_ALIAS, "No flavor supplied")
            return Result.failure()
        }

        val flavor = try {
            LauncherFlavor.valueOf(flavorName)
        } catch (t: Throwable) {
            Log.w(TAG_ALIAS, "Unknown flavor: $flavorName", t)
            return Result.failure()
        }

        return try {
            withContext(Dispatchers.Main) {
                IconSwitcher.setIcon(applicationContext, flavor)
            }
            Log.d(TAG_ALIAS, "Alias switch requested -> ${flavor.name}")
            Result.success()
        } catch (t: Throwable) {
            Log.w(TAG_ALIAS, "Alias switch failed", t)
            Result.retry()
        }
    }
}
