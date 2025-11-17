package com.poolmod.menu

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

object GameDetector {
    
    // 8 Ball Pool paket isimleri (farklı versiyonlar için)
    private val POOL_PACKAGES = listOf(
        "com.miniclip.eightballpool",
        "com.miniclip.8ballpool",
        "com.miniclip.eightballpoolmod",
        "com.miniclip.pool"
    )

    /**
     * Cihazda 8 Ball Pool oyununu tespit et
     */
    fun detectGame(context: Context): GameInfo? {
        val pm = context.packageManager
        
        for (packageName in POOL_PACKAGES) {
            try {
                val packageInfo = pm.getPackageInfo(packageName, 0)
                if (packageInfo != null) {
                    val appInfo = pm.getApplicationInfo(packageName, 0)
                    val appName = pm.getApplicationLabel(appInfo).toString()
                    
                    Log.d(TAG, "Oyun bulundu: $packageName - $appName")
                    
                    return GameInfo(
                        packageName = packageName,
                        appName = appName,
                        versionCode = packageInfo.longVersionCode,
                        versionName = packageInfo.versionName ?: "Unknown"
                    )
                }
            } catch (e: PackageManager.NameNotFoundException) {
                // Paket bulunamadı, devam et
                continue
            } catch (e: Exception) {
                Log.e(TAG, "Oyun tespit hatası: ${e.message}")
            }
        }
        
        return null
    }

    /**
     * Oyunun yüklü olup olmadığını kontrol et
     */
    fun isGameInstalled(context: Context): Boolean {
        return detectGame(context) != null
    }

    data class GameInfo(
        val packageName: String,
        val appName: String,
        val versionCode: Long,
        val versionName: String
    )

    private const val TAG = "GameDetector"
}

