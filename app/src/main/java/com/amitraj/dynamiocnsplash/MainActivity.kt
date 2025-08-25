package com.amitraj.dynamiocnsplash

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.MainThread
import androidx.annotation.RequiresApi
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import coil.Coil
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.amitraj.dynamiocnsplash.App.Companion.restartApp
import kotlinx.coroutines.delay
import java.util.logging.Handler

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1) Show system splash ASAP
        val splash = installSplashScreen()

        // 2) Pick active flavor early
        val flavor = LauncherFlavorDetector.getActiveFlavor(this)
        IconSwitcher.setIcon(this, flavor)
        // 3) Hook exit animation for S+ only
        if (Build.VERSION.SDK_INT >= 31) {
            splash.configureExitFor(flavor, this)
        }

        // 4) Proceed with normal creation
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    IconSwitcherScreen(
                        onSetDefault = { IconSwitcher.setIcon(this, LauncherFlavor.Default)
                            //restartApp()
                                       },
                        onSetHalloween = {
                           IconSwitcher.setIcon(this, LauncherFlavor.Halloween)
                            //restartApp()
                        },
                        onSetSale = { IconSwitcher.setIcon(this, LauncherFlavor.Sale)
                            //restartApp()
                        }
                    )
                }
            }
        }
    }
    fun Context.restartApp() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
        Runtime.getRuntime().exit(0)
    }

}

/*@RequiresApi(31)
fun SplashScreen.configureExitFor(flavor: LauncherFlavor, context: Context) {
    setOnExitAnimationListener { provider ->
        val meta = flavor.meta
        val root = provider.view
        val iconView = provider.iconView

        // Either per-flavor color...
        root.setBackgroundColor(ContextCompat.getColor(context, meta.bgColor))
        // ...or theme primary:
        // root.setBackgroundColor(MaterialTheme.colorScheme.primary.toArgb()) // if called from Compose

        (iconView as? ImageView)?.let { iv ->
            AppCompatResources.getDrawable(context, meta.icon)?.let(iv::setImageDrawable)
            iv.animate()
                .alpha(0f)
                .scaleX(1.12f)
                .scaleY(1.12f)
                .setDuration(1500L)
                .setInterpolator(FastOutSlowInInterpolator())
                .withEndAction { provider.remove() }
                .start()
        } ?: provider.remove()
    }
}*/

private const val TAG = "SplashIcon"

@RequiresApi(31)
fun SplashScreen.configureExitFor(flavor: LauncherFlavor, context: Context) {
    setOnExitAnimationListener { provider ->
        val meta = flavor.meta
        val root = provider.view
        val iconView = provider.iconView as? ImageView

        root.setBackgroundColor(ContextCompat.getColor(context, meta.bgColor))
        if (iconView == null) {
            Log.w(TAG, "iconView is not an ImageView; removing splash")
            provider.remove()
            return@setOnExitAnimationListener
        }

        fun ImageView.startFadeZoomAndRemove() {
            animate()
                .alpha(0f)
                .scaleX(1.12f)
                .scaleY(1.12f)
                .setDuration(1500L)
                .setInterpolator(FastOutSlowInInterpolator())
                .withEndAction { provider.remove() }
                .start()
        }

        when (val src = meta.icon) {
            is IconSource.Res -> {
                iconView.setImageResource(src.resId)
                iconView.startFadeZoomAndRemove()
            }

            is IconSource.Url -> {
                // Start with fallback so user sees something instantly
                iconView.setImageResource(src.fallbackRes)

                // Optional: enable detailed Coil logs while debugging
                // Coil.setImageLoader(
                //   Coil.imageLoader(context).newBuilder().logger(DebugLogger()).build()
                // )

                val request = ImageRequest.Builder(context)
                    .data(src.url)
                    .placeholder(src.fallbackRes)
                    .error(src.fallbackRes)
                    // If your URL can be SVGs, include the decoder:
                    // .decoderFactory(SvgDecoder.Factory())
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .allowHardware(false) // safer for crossfade/anim
                    .listener(
                        onStart = { Log.d(TAG, "Coil start: ${src.url}") },
                        onSuccess = { _, _ ->
                            Log.d(TAG, "Coil success: ${src.url}")
                            iconView.startFadeZoomAndRemove()
                        },
                        onError = { _, result ->
                            Log.w(TAG, "Coil error: ${src.url} cause=${result.throwable}")
                            iconView.startFadeZoomAndRemove()
                        }
                    )
                    .target(
                        onStart = { d -> d?.let(iconView::setImageDrawable) },
                        onSuccess = {
                            d -> iconView.setImageDrawable(d)
                                    },
                        onError = { d ->
                            // Use fallback drawable if provided, then animate
                            d?.let(iconView::setImageDrawable) ?: iconView.setImageResource(src.fallbackRes)
                        }
                    )
                    .build()

                Coil.imageLoader(context).enqueue(request)

                // Safety timeout: if neither success/error fired (rare), finish anyway.
                android.os.Handler(Looper.getMainLooper()).postDelayed({
                    if (provider.view.isAttachedToWindow) {
                        Log.w(TAG, "Timeout waiting for image; proceeding with fallback")
                        iconView.startFadeZoomAndRemove()
                    }
                }, 2000L)
            }
        }
    }
}



/** Flavor enum + single source of truth for alias + splash visuals */
/*enum class LauncherFlavor(
    val aliasSuffix: String,
    @DrawableRes val icon: Int,
    @ColorRes val bgColor: Int
) {
    Default(
        aliasSuffix = "Default",
        icon = R.drawable.splash_icon_default,
        bgColor = android.R.color.holo_blue_light
    ),
    Halloween(
        aliasSuffix = "Halloween",
        icon = R.drawable.splash_icon_halloween,
        bgColor = android.R.color.black
    ),
    Sale(
        aliasSuffix = "Sale",
        icon = R.drawable.splash_icon_sale,
        bgColor = android.R.color.holo_red_light
    );

    val meta: FlavorMeta get() = FlavorMeta(icon = icon, bgColor = bgColor)

    data class FlavorMeta(@DrawableRes val icon: Int, @ColorRes val bgColor: Int)

    fun aliasComponent(packageName: String): ComponentName =
        ComponentName(packageName, "$packageName.alias.$aliasSuffix")

    companion object {
        fun fromAliasClassNameOrNull(className: String?): LauncherFlavor? =
            entries.firstOrNull { className?.endsWith(".alias.${it.aliasSuffix}") == true }
    }
}*/

/** Detect which launcher alias is effectively active */
object LauncherFlavorDetector {

    fun getActiveFlavor(context: Context): LauncherFlavor {
        val pm = context.packageManager
        val pkg = context.packageName

        // 1) What does the launcher currently resolve?
        resolveCurrentLauncherComponent(pm, pkg)?.let { cn ->
            LauncherFlavor.fromAliasClassNameOrNull(cn.className)?.let { return it }
        }

        // 2) Otherwise, find any enabled alias (manifest or programmatic)
        firstEnabledByAnyMeans(pm, pkg)?.let { cn ->
            LauncherFlavor.fromAliasClassNameOrNull(cn.className)?.let { return it }
        }

        // 3) Fallback
        return LauncherFlavor.Default
    }

    private fun resolveCurrentLauncherComponent(pm: PackageManager, pkg: String): ComponentName? {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setPackage(pkg)
        val ri = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) ?: return null
        return ComponentName(pkg, ri.activityInfo.name)
    }

    private fun manifestSaysEnabled(pm: PackageManager, cn: ComponentName): Boolean {
        return try {
            val flags = PackageManager.MATCH_DISABLED_COMPONENTS
            pm.getActivityInfo(cn, flags).enabled
        } catch (_: Throwable) {
            false
        }
    }

    private fun PackageManager.isEffectivelyEnabled(cn: ComponentName): Boolean {
        return when (getComponentEnabledSetting(cn)) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED -> false
            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> manifestSaysEnabled(this, cn)
            else -> false
        }
    }

    private fun firstEnabledByAnyMeans(pm: PackageManager, pkg: String): ComponentName? =
        LauncherFlavor.entries
            .map { it.aliasComponent(pkg) }
            .firstOrNull { pm.isEffectivelyEnabled(it) }
}

/** Switcher that only touches what changed */
object IconSwitcher {

    private const val TAG = "IconSwitcher"

    @MainThread
    fun setIcon(context: Context, target: LauncherFlavor) {
        val pm = context.packageManager
        val pkg = context.packageName

        val targetCN = target.aliasComponent(pkg)
        val currentCN = resolveCurrentForWrite(pm, pkg) ?: firstEnabledByAnyMeans(pm, pkg)

        // If already on target and it's enabled, nothing to do.
        val targetEnabled = pm.isEffectivelyEnabled(targetCN)
        if (currentCN == targetCN && targetEnabled) {
            Log.d(TAG, "Icon already set to ${target.aliasSuffix}; skipping.")
            return
        }

        // Enable target (avoid killing the app)
        pm.safeSetComponentEnabledSetting(
            targetCN,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
        Log.d("AliasActive", targetCN.className)

        // Disable others
        LauncherFlavor.entries
            .map { it.aliasComponent(pkg) }
            .filter { it != targetCN }
            .forEach { other ->
                if (pm.isEffectivelyEnabled(other)) {
                    pm.safeSetComponentEnabledSetting(
                        other,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                    Log.d("AliasDisabled", other.className)
                }
            }

        Log.d(TAG, "Switched icon to ${target.aliasSuffix}")
    }

    /** Prefer resolved component as seen by the launcher */
    private fun resolveCurrentForWrite(pm: PackageManager, pkg: String): ComponentName? {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setPackage(pkg)
        val ri = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) ?: return null
        return ComponentName(pkg, ri.activityInfo.name)
    }

    private fun firstEnabledByAnyMeans(pm: PackageManager, pkg: String): ComponentName? =
        LauncherFlavor.entries
            .map { it.aliasComponent(pkg) }
            .firstOrNull { pm.isEffectivelyEnabled(it) }

    private fun PackageManager.safeSetComponentEnabledSetting(
        componentName: ComponentName,
        newState: Int,
        flags: Int
    ) {
        try {
            setComponentEnabledSetting(componentName, newState, flags)
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to set $componentName to $newState", t)
        }
    }

    private fun PackageManager.isEffectivelyEnabled(cn: ComponentName): Boolean {
        return when (getComponentEnabledSetting(cn)) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED -> false
            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> {
                try {
                    val flags = PackageManager.MATCH_DISABLED_COMPONENTS
                    getActivityInfo(cn, flags).enabled
                } catch (_: Throwable) {
                    false
                }
            }

            else -> false
        }
    }
}

/** Simple UI to switch icons and show current flavor */
@Composable
private fun IconSwitcherScreen(
    onSetDefault: () -> Unit,
    onSetHalloween: () -> Unit,
    onSetSale: () -> Unit
) {
    /* val randomNumber = (10..50).random()
     if(randomNumber%2==0){
         IconSwitcher.setIcon(LocalContext.current, LauncherFlavor.Halloween)
     }else {
         IconSwitcher.setIcon(LocalContext.current, LauncherFlavor.Sale)
     }*/
    val ctx = LocalContext.current
    var current by remember { mutableStateOf(LauncherFlavorDetector.getActiveFlavor(ctx)) }

    fun switch(to: LauncherFlavor) {
        IconSwitcher.setIcon(ctx, to)
        current = to
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Dynamic App Icon Demo", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text("Current: ${current.name}", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(24.dp))

        Button(onClick = {
            //switch(LauncherFlavor.Default);
            onSetDefault() }) {
            Text("Use Default Icon")
        }
        Spacer(Modifier.height(12.dp))

        Button(onClick = {
            //switch(LauncherFlavor.Halloween);
            onSetHalloween() }) {
            Text("Use Halloween Icon 🎃")
        }
        Spacer(Modifier.height(12.dp))

        Button(onClick = {
            //switch(LauncherFlavor.Sale);
            onSetSale() }) {
            Text("Use Sale Icon 🛍")
        }
        Spacer(Modifier.height(32.dp))

        Text(
            "Tip: On Android 13+, themed icons tint automatically when enabled.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

// New sealed model for icons
sealed interface IconSource {
    data class Res(@DrawableRes val resId: Int) : IconSource
    data class Url(val url: String, @DrawableRes val fallbackRes: Int) : IconSource
}

enum class LauncherFlavor(
    val aliasSuffix: String,
    val icon: IconSource,          // <— changed
    @ColorRes val bgColor: Int
) {
    Default(
        aliasSuffix = "Default",
        icon = IconSource.Res(R.drawable.splash_icon_default),
        bgColor = android.R.color.holo_blue_light
    ),
    Halloween(
        aliasSuffix = "Halloween",
        // Load from the web, but keep your existing halloween drawable as fallback
        icon = IconSource.Url(
            url = "https://images.unsplash.com/photo-1503023345310-bd7c1de61c7d?w=800",
            fallbackRes = R.drawable.splash_icon_halloween
        ),
        bgColor = android.R.color.black
    ),
    Sale(
        aliasSuffix = "Sale",
        icon = IconSource.Url(
            url = "https://picsum.photos/300/200",
            fallbackRes = R.drawable.splash_icon_sale
        ),
        bgColor = android.R.color.holo_red_light
    );

    val meta: FlavorMeta
        get() = FlavorMeta(icon = icon, bgColor = bgColor)

    data class FlavorMeta(val icon: IconSource, @ColorRes val bgColor: Int)

    fun aliasComponent(packageName: String): ComponentName =
        ComponentName(packageName, "$packageName.alias.$aliasSuffix")

    companion object {
        fun fromAliasClassNameOrNull(className: String?): LauncherFlavor? =
            entries.firstOrNull { className?.endsWith(".alias.${it.aliasSuffix}") == true }
    }
}

