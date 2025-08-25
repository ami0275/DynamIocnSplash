package com.amitraj.dynamiocnsplash

import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen


class MainActivityDynamic : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()

        val flavor = LauncherFlavorDetector.getActiveFlavor(this)
        IconSwitcher.setIcon(this, flavor)

        if (Build.VERSION.SDK_INT >= 31) {
            splash.configureExitFor(flavor, this)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val currentFlavorText = findViewById<TextView>(R.id.currentFlavorText)
        currentFlavorText.text = "Current: ${flavor.name}"

        findViewById<Button>(R.id.btnDefault).setOnClickListener {
            IconSwitcher.setIcon(this, LauncherFlavor.Default)
            currentFlavorText.text = "Current: Default"
        }

        findViewById<Button>(R.id.btnHalloween).setOnClickListener {
            IconSwitcher.setIcon(this, LauncherFlavor.Halloween)
            currentFlavorText.text = "Current: Halloween"
        }

        findViewById<Button>(R.id.btnSale).setOnClickListener {
            IconSwitcher.setIcon(this, LauncherFlavor.Sale)
            currentFlavorText.text = "Current: Sale"
        }
    }
}
