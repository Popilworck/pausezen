package com.pausezen.app.core

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

data class InstalledApp(
    val packageName: String,
    val label: String,
    val icon: Drawable?
)

object InstalledAppsProvider {
    fun loadLaunchableApps(context: Context): List<InstalledApp> {
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolved = pm.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
        val selfPackage = context.packageName

        return resolved
            .mapNotNull { info ->
                val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
                if (pkg == selfPackage) return@mapNotNull null
                InstalledApp(
                    packageName = pkg,
                    label = info.loadLabel(pm)?.toString()?.ifBlank { pkg } ?: pkg,
                    icon = runCatching { info.loadIcon(pm) }.getOrNull()
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    fun getAppLabel(context: Context, packageName: String): String {
        return runCatching {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(appInfo).toString()
        }.getOrDefault(packageName)
    }

    fun getAppIcon(context: Context, packageName: String): Drawable? {
        return runCatching {
            context.packageManager.getApplicationIcon(packageName)
        }.getOrNull()
    }
}
