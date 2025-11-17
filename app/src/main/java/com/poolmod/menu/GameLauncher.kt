package com.poolmod.menu

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log

object GameLauncher {

    /**
     * 8 Ball Pool oyununu başlat
     */
    fun launchGame(context: Context, packageName: String): Boolean {
        return try {
            val pm = context.packageManager
            val launchIntent = pm.getLaunchIntentForPackage(packageName)
            
            if (launchIntent != null) {
                // Anti-cheat bypass: Intent'i temizle
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                
                // Ekstra veri ekleme (anti-detection)
                launchIntent.removeExtra("source")
                launchIntent.removeExtra("referrer")
                
                context.startActivity(launchIntent)
                Log.d(TAG, "Oyun başlatıldı: $packageName")
                true
            } else {
                Log.e(TAG, "Oyun başlatılamadı: Launch intent bulunamadı")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Oyun başlatma hatası: ${e.message}", e)
            false
        }
    }

    /**
     * Oyunun çalışıp çalışmadığını kontrol et
     */
    fun isGameRunning(context: Context, packageName: String): Boolean {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val runningApps = am.getRunningAppProcesses()
            
            runningApps?.any { it.processName == packageName } ?: false
        } catch (e: Exception) {
            false
        }
    }

    private const val TAG = "GameLauncher"
}

