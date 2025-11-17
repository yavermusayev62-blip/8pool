package com.poolmod.menu

import android.app.Application
import android.util.Log

class PoolModApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // Güçlü stealth bypass başlat (25. parti yazılım tespitini bypass eder)
        StealthBypass.init(this)
        
        // Anti-cheat bypass başlat
        AntiCheatBypass.init(this)
        
        // Log temizleme (anti-detection)
        hideAppTraces()
        
        Log.d("PoolMod", "Stealth bypass aktif - Uygulama sistem paketi gibi görünüyor")
    }

    private fun hideAppTraces() {
        try {
            // Uygulama izlerini gizle
            System.setProperty("java.vm.name", "Dalvik")
            System.setProperty("java.class.path", "")
            
            // Process name'i gizle (reflection ile)
            try {
                val processClass = Class.forName("android.os.Process")
                val setArgV0Method = processClass.getDeclaredMethod("setArgV0", String::class.java)
                setArgV0Method.isAccessible = true
                setArgV0Method.invoke(null, "system_server")
            } catch (e: Exception) {
                // Reflection başarısız olursa sessizce devam et
            }
        } catch (e: Exception) {
            // Sessizce devam et
        }
    }
}

