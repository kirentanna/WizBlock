package com.wizblock.domain

import android.content.Context
import android.content.pm.PackageManager
import com.wizblock.model.TargetDisplayInfo
import com.wizblock.model.TargetKey
import com.wizblock.model.TargetType

class TargetDisplayResolver(
    context: Context,
    private val targetNormalizer: TargetNormalizer
) {
    private val appContext = context.applicationContext

    fun resolve(targetType: TargetType, rawValue: String): TargetDisplayInfo {
        val key = targetNormalizer.normalize(targetType, rawValue) ?: TargetKey(targetType, rawValue.trim())
        return when (targetType) {
            TargetType.APP_PACKAGE -> resolveApp(key, rawValue)
            TargetType.DOMAIN -> TargetDisplayInfo(
                targetKey = key,
                title = key.targetValue,
                subtitle = "Website"
            )
            TargetType.KEYWORD -> TargetDisplayInfo(
                targetKey = key,
                title = "\"${key.targetValue}\"",
                subtitle = "Keyword"
            )
        }
    }

    private fun resolveApp(key: TargetKey, rawValue: String): TargetDisplayInfo {
        val packageName = resolveInstalledPackage(rawValue.trim()) ?: key.targetValue
        val appInfo = runCatching { appContext.packageManager.getApplicationInfo(packageName, 0) }.getOrNull()
        val label = appInfo?.let {
            runCatching { appContext.packageManager.getApplicationLabel(it).toString() }.getOrNull()
        }
        return TargetDisplayInfo(
            targetKey = TargetKey(TargetType.APP_PACKAGE, packageName),
            title = label?.takeIf { it.isNotBlank() } ?: packageName,
            subtitle = packageName,
            iconPackageName = packageName
        )
    }

    private fun resolveInstalledPackage(packageName: String): String? {
        if (packageName.isBlank()) return null
        runCatching {
            appContext.packageManager.getPackageInfo(packageName, 0)
            return packageName
        }
        val packages = runCatching {
            appContext.packageManager.getInstalledPackages(PackageManager.MATCH_DISABLED_COMPONENTS)
        }.getOrDefault(emptyList())
        return packages.firstOrNull { it.packageName.equals(packageName, ignoreCase = true) }?.packageName
    }
}
