package com.poolmod.menu

import android.util.Log
import java.lang.Thread.UncaughtExceptionHandler

/**
 * Crash Handler - Uygulama çöktüğünde hataları yakalar ve log dosyasına yazar
 */
class CrashHandler(private val defaultHandler: UncaughtExceptionHandler?) : UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            // Crash'i log dosyasına yaz
            DebugLogger.logCrash(thread, throwable)

            // Ek bilgileri logla
            Log.e("CrashHandler", "❌ UNCAUGHT EXCEPTION!", throwable)
            Log.e("CrashHandler", "Thread: ${thread.name} (${thread.id})")
            Log.e("CrashHandler", "Exception: ${throwable.javaClass.name}")
            Log.e("CrashHandler", "Message: ${throwable.message}")

            // ModMenuService durumunu logla
            try {
                logServiceState()
            } catch (e: Exception) {
                Log.e("CrashHandler", "Service state loglanamadı: ${e.message}")
            }

        } catch (e: Exception) {
            Log.e("CrashHandler", "Crash handler hatası: ${e.message}", e)
        } finally {
            // Varsayılan handler'ı çağır (sistem crash dialog'unu gösterir)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    /**
     * Service durumunu logla
     */
    private fun logServiceState() {
        // Bu bilgileri crash log'una ekleyebiliriz
        // Şu an için sadece logcat'e yazıyoruz
        Log.d("CrashHandler", "Service state logging (placeholder)")
    }
}

