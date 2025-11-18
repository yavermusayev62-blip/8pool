package com.poolmod.menu

import android.app.Application
import android.util.Log

class PoolModApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // Debug Logger'ı başlat (ilk önce - hataları yakalamak için)
        DebugLogger.init(this)
        DebugLogger.logProcessEvent("PoolModApplication", "PROCESS_STARTED", 
            android.os.Process.myPid(), packageName)
        DebugLogger.logInfo("PoolModApplication", "Uygulama başlatılıyor...")
        
        // Crash Handler'ı ayarla
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(CrashHandler(defaultHandler))
        DebugLogger.logInfo("PoolModApplication", "Crash handler ayarlandı")
        
        try {
            // Güçlü stealth bypass başlat (25. parti yazılım tespitini bypass eder)
            StealthBypass.init(this)
            
            // Anti-cheat bypass başlat
            AntiCheatBypass.init(this)
            
            // Log temizleme (anti-detection)
            hideAppTraces()
            
            DebugLogger.logInfo("PoolMod", "Stealth bypass aktif - Uygulama sistem paketi gibi görünüyor")
            Log.d("PoolMod", "Stealth bypass aktif - Uygulama sistem paketi gibi görünüyor")
        } catch (e: Exception) {
            DebugLogger.logException("PoolModApplication", "onCreate hatası", e)
            throw e
        }
    }

    private fun hideAppTraces() {
        try {
            // Android 10+ versiyalarında System.setProperty qadağandır
            // Bu xətələr normaldır və proqramın işləməsinə mane olmur
            // Yalnız köhnə Android versiyalarında işləyir
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
                try {
                    System.setProperty("java.vm.name", "Dalvik")
                    System.setProperty("java.class.path", "")
                } catch (e: Exception) {
                    // Android 10+ versiyalarında icazə verilmir - normaldır
                }
            }
            
            // Process name'i gizle (reflection ile)
            // Android 10+ versiyalarında bu metod gizlidir və icazə verilmir
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
                try {
                    val processClass = Class.forName("android.os.Process")
                    val setArgV0Method = processClass.getDeclaredMethod("setArgV0", String::class.java)
                    setArgV0Method.isAccessible = true
                    setArgV0Method.invoke(null, "system_server")
                } catch (e: Exception) {
                    // Android 10+ versiyalarında icazə verilmir - normaldır
                }
            }
        } catch (e: Exception) {
            // Sessizce devam et
        }
    }
}

