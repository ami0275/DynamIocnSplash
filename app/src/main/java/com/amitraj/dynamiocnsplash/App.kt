package com.amitraj.dynamiocnsplash

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent

class App : Application() {
    companion object {
        fun restartApp(context: Context) {
            val intent = context.packageManager
                .getLaunchIntentForPackage(context.packageName)
            intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            if (context is Activity) {
                context.finish()
            }
            Runtime.getRuntime().exit(0)
        }
    }

    override fun onCreate() {
        super.onCreate()
        IconWorkScheduler.startFastRotation(this)
    }
}