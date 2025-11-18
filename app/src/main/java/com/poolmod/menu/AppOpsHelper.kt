package com.poolmod.menu

import android.app.AppOpsManager
import android.content.Context
import android.os.Build
import android.util.Log
import android.provider.Settings
import androidx.annotation.RequiresApi

/**
 * AppOps Helper - AppOps hatalarını düzeltmek için yardımcı sınıf
 */
object AppOpsHelper {
    private const val TAG = "AppOpsHelper"
    private const val OP_SYSTEM_ALERT_WINDOW = 24 // AppOpsManager.OP_SYSTEM_ALERT_WINDOW
    
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun checkOpQ(appOpsManager: AppOpsManager, uid: Int, packageName: String): Int {
        return appOpsManager.checkOpNoThrow(
            AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW,
            uid,
            packageName
        )
    }
    
    @Suppress("DEPRECATION")
    private fun checkOpLegacy(appOpsManager: AppOpsManager, uid: Int, packageName: String): Int {
        // Android M-P (API 23-28) için Int parametreli deprecated metod
        // Bu metod sadece API < Q için çağrılır
        // Reflection kullanarak tip kontrolünü bypass ediyoruz
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            try {
                val method = appOpsManager.javaClass.getMethod(
                    "checkOpNoThrow",
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                    String::class.java
                )
                @Suppress("UNCHECKED_CAST")
                val result = method.invoke(appOpsManager, OP_SYSTEM_ALERT_WINDOW, uid, packageName) as? Int
                result ?: AppOpsManager.MODE_ERRORED
            } catch (e: Exception) {
                Log.e(TAG, "checkOpLegacy reflection hatası: ${e.message}", e)
                AppOpsManager.MODE_ERRORED
            }
        } else {
            AppOpsManager.MODE_ERRORED
        }
    }
    
    @Suppress("DEPRECATION")
    private fun startOpLegacy(appOpsManager: AppOpsManager, uid: Int, packageName: String): Int {
        // Android M-P (API 23-28) için Int parametreli deprecated metod
        // Bu metod sadece API < Q için çağrılır
        // Reflection kullanarak tip kontrolünü bypass ediyoruz
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            try {
                val method = appOpsManager.javaClass.getMethod(
                    "startOpNoThrow",
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                    String::class.java
                )
                @Suppress("UNCHECKED_CAST")
                val result = method.invoke(appOpsManager, OP_SYSTEM_ALERT_WINDOW, uid, packageName) as? Int
                result ?: AppOpsManager.MODE_ERRORED
            } catch (e: Exception) {
                Log.e(TAG, "startOpLegacy reflection hatası: ${e.message}", e)
                AppOpsManager.MODE_ERRORED
            }
        } else {
            AppOpsManager.MODE_ERRORED
        }
    }
    
    /**
     * Overlay iznini kontrol et ve AppOps'u başlat
     */
    fun checkAndStartOverlayPermission(context: Context): Boolean {
        try {
            // Önce Settings kontrolü
            val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                true
            }
            
            if (!hasPermission) {
                Log.w(TAG, "Overlay izni Settings'de yok")
                DebugLogger.logAppOpsError(TAG, "Overlay izni Settings'de yok", 
                    android.os.Process.myUid(), context.packageName)
                return false
            }
            
            // AppOps kontrolü (Android M+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
                    if (appOpsManager != null) {
                        val mode: Int
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            // Android 10+ (Q) için String kullan
                            @RequiresApi(Build.VERSION_CODES.Q)
                            mode = checkOpQ(appOpsManager, android.os.Process.myUid(), context.packageName)
                        } else {
                            // Android M-P için Int kullan
                            @Suppress("DEPRECATION")
                            mode = checkOpLegacy(appOpsManager, android.os.Process.myUid(), context.packageName)
                        }
                        
                        Log.d(TAG, "AppOps mode: $mode (0=ALLOWED, 1=IGNORED, 2=ERROR, 3=DEFAULT)")
                        
                        when (mode) {
                            AppOpsManager.MODE_ALLOWED -> {
                                Log.d(TAG, "✅ AppOps izni ALLOWED")
                                DebugLogger.logInfo(TAG, "AppOps izni ALLOWED")
                                return true
                            }
                            AppOpsManager.MODE_IGNORED -> {
                                Log.w(TAG, "⚠️ AppOps izni IGNORED - izin verilmiş ama AppOps kaydı yok")
                                DebugLogger.logAppOpsError(TAG, "AppOps izni IGNORED - Settings'de izin var ama AppOps kaydı yok", 
                                    android.os.Process.myUid(), context.packageName)
                                // AppOps'u başlatmayı dene (sistem seviyesi, başarısız olabilir)
                                return tryStartAppOps(context, appOpsManager)
                            }
                            AppOpsManager.MODE_ERRORED -> {
                                Log.e(TAG, "❌ AppOps izni ERRORED")
                                DebugLogger.logAppOpsError(TAG, "AppOps izni ERRORED", 
                                    android.os.Process.myUid(), context.packageName)
                                return false
                            }
                            AppOpsManager.MODE_DEFAULT -> {
                                Log.w(TAG, "⚠️ AppOps izni DEFAULT - kontrol ediliyor")
                                // Default durumda Settings kontrolüne güven
                                return hasPermission
                            }
                            else -> {
                                Log.w(TAG, "⚠️ AppOps mode bilinmeyen: $mode")
                                return hasPermission
                            }
                        }
                    } else {
                        Log.w(TAG, "AppOpsManager null")
                        return hasPermission
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "AppOps kontrolü hatası: ${e.message}", e)
                    DebugLogger.logException(TAG, "AppOps kontrolü hatası", e)
                    // Hata durumunda Settings kontrolüne güven
                    return hasPermission
                }
            }
            
            return hasPermission
        } catch (e: Exception) {
            Log.e(TAG, "Overlay izni kontrolü hatası: ${e.message}", e)
            DebugLogger.logException(TAG, "Overlay izni kontrolü hatası", e)
            return false
        }
    }
    
    /**
     * AppOps'u başlatmayı dene (sistem seviyesi, başarısız olabilir)
     */
    private fun tryStartAppOps(context: Context, appOpsManager: AppOpsManager): Boolean {
        return try {
            // Not: Normal uygulamalar AppOps'u başlatamaz, sadece sistem uygulamaları yapabilir
            // Bu yüzden bu genellikle başarısız olur ama denemek zarar vermez
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ için noteOp kullan (String)
                val result = appOpsManager.noteOpNoThrow(
                    AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW,
                    android.os.Process.myUid(),
                    context.packageName
                )
                
                when (result) {
                    AppOpsManager.MODE_ALLOWED -> {
                        Log.d(TAG, "✅ AppOps noteOp başarılı - MODE_ALLOWED")
                        DebugLogger.logInfo(TAG, "AppOps noteOp başarılı")
                        true
                    }
                    else -> {
                        Log.w(TAG, "⚠️ AppOps noteOp sonucu: $result (izin verilmiş olabilir)")
                        // noteOp başarısız olsa bile Settings'de izin varsa devam et
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            Settings.canDrawOverlays(context)
                        } else {
                            true
                        }
                    }
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                // Android 9 ve öncesi (M-P) için startOp kullan (deprecated ama çalışabilir)
                @Suppress("DEPRECATION")
                val result: Int = startOpLegacy(
                    appOpsManager,
                    android.os.Process.myUid(),
                    context.packageName
                )
                
                when (result) {
                    AppOpsManager.MODE_ALLOWED -> {
                        Log.d(TAG, "✅ AppOps startOp başarılı - MODE_ALLOWED")
                        DebugLogger.logInfo(TAG, "AppOps startOp başarılı")
                        true
                    }
                    else -> {
                        Log.w(TAG, "⚠️ AppOps startOp sonucu: $result (izin verilmiş olabilir)")
                        // startOp başarısız olsa bile Settings'de izin varsa devam et
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            Settings.canDrawOverlays(context)
                        } else {
                            true
                        }
                    }
                }
            } else {
                // Android L ve öncesi için AppOps mevcut değil
                Log.w(TAG, "AppOps not available on Android < M")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Settings.canDrawOverlays(context)
                } else {
                    true
                }
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "AppOps başlatma SecurityException (normal - sistem seviyesi işlem): ${e.message}")
            // SecurityException normal - normal uygulamalar AppOps'u başlatamaz
            // Settings kontrolüne güven
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "AppOps başlatma hatası: ${e.message}", e)
            DebugLogger.logException(TAG, "AppOps başlatma hatası", e)
            // Hata durumunda Settings kontrolüne güven
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                true
            }
        }
    }
    
    /**
     * Overlay izni durumunu detaylı kontrol et
     */
    fun getDetailedOverlayPermissionStatus(context: Context): String {
        return try {
            val hasSettingsPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                true
            }
            
            val status = buildString {
                appendLine("Overlay Permission Status:")
                appendLine("  Settings.canDrawOverlays: $hasSettingsPermission")
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
                    if (appOpsManager != null) {
                        val mode: Int
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            // Android 10+ (Q) için String kullan
                            @RequiresApi(Build.VERSION_CODES.Q)
                            mode = checkOpQ(appOpsManager, android.os.Process.myUid(), context.packageName)
                        } else {
                            // Android M-P için Int kullan
                            @Suppress("DEPRECATION")
                            mode = checkOpLegacy(appOpsManager, android.os.Process.myUid(), context.packageName)
                        }
                        appendLine("  AppOps mode: $mode")
                        appendLine("    (0=ALLOWED, 1=IGNORED, 2=ERRORED, 3=DEFAULT)")
                    } else {
                        appendLine("  AppOpsManager: null")
                    }
                } else {
                    appendLine("  AppOps: Not available (Android < M)")
                }
            }
            
            status
        } catch (e: Exception) {
            "Error getting permission status: ${e.message}"
        }
    }
}

