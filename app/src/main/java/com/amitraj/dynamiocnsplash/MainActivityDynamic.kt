package com.amitraj.dynamiocnsplash

import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Observer
import androidx.work.WorkInfo
import androidx.work.WorkManager

class MainActivityDynamic : ComponentActivity() {

    private lateinit var currentFlavorText: TextView
    private val workManager by lazy { WorkManager.getInstance(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        // show system splash asap
        val splash = installSplashScreen()

        // detect active alias so splash can be configured correctly
        val flavor = LauncherFlavorDetector.getActiveFlavor(this)
        IconSwitcher.setIcon(this, flavor)

        if (Build.VERSION.SDK_INT >= 31) {
            splash.configureExitFor(flavor, this)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        currentFlavorText = findViewById(R.id.currentFlavorText)
        currentFlavorText.text = "Current: ${flavor.name}"

        findViewById<Button>(R.id.btnDefault).setOnClickListener {
            // immediate one-time change (optional)
            IconWorkScheduler.enqueueOneTimeChange(this, LauncherFlavor.Default.name)
        }

        findViewById<Button>(R.id.btnHalloween).setOnClickListener {
            // immediate one-time change (optional)
            IconWorkScheduler.enqueueOneTimeChange(this, LauncherFlavor.Halloween.name)
        }

        findViewById<Button>(R.id.btnSale).setOnClickListener {
            // immediate one-time change (optional)
            IconWorkScheduler.enqueueOneTimeChange(this, LauncherFlavor.Sale.name)
        }

        // start/stop fast rotation test controls (you can wire these up to UI)
        // Example: start immediately when activity launches (for testing)
        // IconWorkScheduler.startFastRotation(this)

        // If you want to observe the fast rotate unique work status (optional)
        val liveData = workManager.getWorkInfosForUniqueWorkLiveData("fast_icon_rotate")
        liveData.observe(this, Observer { infos ->
            val info = infos?.firstOrNull()
            if (info == null) return@Observer

            when (info.state) {
                WorkInfo.State.ENQUEUED -> currentFlavorText.text = "Rotation: Enqueued"
                WorkInfo.State.RUNNING -> currentFlavorText.text = "Rotation: Running"
                WorkInfo.State.SUCCEEDED -> currentFlavorText.text = "Rotation: Succeeded"
                WorkInfo.State.FAILED -> currentFlavorText.text = "Rotation: Failed"
                WorkInfo.State.CANCELLED -> currentFlavorText.text = "Rotation: Cancelled"
                else -> { /* BLOCKED etc */ }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
