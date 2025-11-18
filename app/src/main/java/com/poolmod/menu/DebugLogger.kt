package com.poolmod.menu

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Debug Logger - Hataları dosyaya yazar ve exception'ları yakalar
 */
object DebugLogger {
    private const val TAG = "DebugLogger"
    private const val MAX_LOG_SIZE = 5 * 1024 * 1024 // 5MB
    private const val MAX_LOG_FILES = 5
    
    private var logDir: File? = null
    private var errorLogFile: File? = null
    private var crashLogFile: File? = null
    private var debugLogFile: File? = null
    private val lock = ReentrantLock()
    private var isInitialized = false
    
    // Thread-safe date formatter
    private val dateFormat: ThreadLocal<SimpleDateFormat> = ThreadLocal.withInitial {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    }
    
    private fun formatDate(date: Date): String {
        return try {
            dateFormat.get().format(date)
        } catch (e: Exception) {
            // Fallback to system default
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(date)
        }
    }
    
    /**
     * Debug logger'ı başlat
     */
    fun init(context: Context) {
        try {
            logDir = File(context.filesDir, "debug_logs")
            if (!logDir!!.exists()) {
                logDir!!.mkdirs()
            }
            
            errorLogFile = File(logDir, "errors.log")
            crashLogFile = File(logDir, "crashes.log")
            debugLogFile = File(logDir, "debug.log")
            
            // Eski log dosyalarını temizle
            cleanupOldLogs()
            
            isInitialized = true
            
            Log.d(TAG, "✅ DebugLogger başlatıldı: ${logDir?.absolutePath}")
            
            // init() tamamlandıktan sonra güvenli şekilde log yaz
            try {
                logDebug("DebugLogger", "DebugLogger başlatıldı - ${Build.MANUFACTURER} ${Build.MODEL} - Android ${Build.VERSION.RELEASE}")
            } catch (e: Exception) {
                // İlk log yazma hatası kritik değil
                Log.e(TAG, "İlk log yazılamadı (normal olabilir): ${e.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ DebugLogger başlatılamadı: ${e.message}", e)
            isInitialized = false
        }
    }
    
    /**
     * Debug log yaz
     */
    fun logDebug(tag: String, message: String) {
        if (!isInitialized) {
            // Henüz başlatılmadıysa sadece logcat'e yaz
            Log.d(tag, message)
            return
        }
        try {
            val logMessage = formatLog("DEBUG", tag, message, null)
            writeToFile(debugLogFile, logMessage)
            Log.d(tag, message)
        } catch (e: Exception) {
            Log.e(TAG, "Log yazılamadı: ${e.message}")
        }
    }
    
    /**
     * Error log yaz
     */
    fun logError(tag: String, message: String, throwable: Throwable? = null) {
        // Her zaman logcat'e yaz
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
        
        if (!isInitialized) {
            return
        }
        
        try {
            val logMessage = formatLog("ERROR", tag, message, throwable)
            writeToFile(errorLogFile, logMessage)
        } catch (e: Exception) {
            Log.e(TAG, "Error log dosyaya yazılamadı: ${e.message}")
        }
    }
    
    /**
     * Warning log yaz
     */
    fun logWarning(tag: String, message: String) {
        Log.w(tag, message)
        
        if (!isInitialized) {
            return
        }
        
        try {
            val logMessage = formatLog("WARN", tag, message, null)
            writeToFile(debugLogFile, logMessage)
        } catch (e: Exception) {
            Log.e(TAG, "Warning log dosyaya yazılamadı: ${e.message}")
        }
    }
    
    /**
     * Info log yaz
     */
    fun logInfo(tag: String, message: String) {
        Log.i(tag, message)
        
        if (!isInitialized) {
            return
        }
        
        try {
            val logMessage = formatLog("INFO", tag, message, null)
            writeToFile(debugLogFile, logMessage)
        } catch (e: Exception) {
            Log.e(TAG, "Info log dosyaya yazılamadı: ${e.message}")
        }
    }
    
    /**
     * Crash log yaz
     */
    fun logCrash(thread: Thread, throwable: Throwable) {
        // Her zaman logcat'e yaz (kritik)
        Log.e(TAG, "❌ CRASH DETECTED!", throwable)
        
        if (!isInitialized) {
            return
        }
        
        try {
            val crashInfo = buildString {
                appendLine("=".repeat(80))
                appendLine("CRASH DETECTED")
                appendLine("=".repeat(80))
                appendLine("Time: ${formatDate(Date())}")
                appendLine("Thread: ${thread.name} (${thread.id})")
                appendLine("Exception: ${throwable.javaClass.name}")
                appendLine("Message: ${throwable.message}")
                appendLine()
                appendLine("Stack Trace:")
                appendLine(getStackTrace(throwable))
                appendLine()
                appendLine("Device Info:")
                appendLine("  Manufacturer: ${Build.MANUFACTURER}")
                appendLine("  Model: ${Build.MODEL}")
                appendLine("  Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                appendLine("  App Version: ${getAppVersion()}")
                appendLine("=".repeat(80))
                appendLine()
            }
            
            writeToFile(crashLogFile, crashInfo)
        } catch (e: Exception) {
            Log.e(TAG, "Crash log dosyaya yazılamadı: ${e.message}", e)
        }
    }
    
    /**
     * Exception yakala ve log yaz
     */
    fun logException(tag: String, message: String, exception: Throwable) {
        // Her zaman logcat'e yaz
        Log.e(tag, message, exception)
        
        if (!isInitialized) {
            return
        }
        
        try {
            val exceptionInfo = buildString {
                appendLine("=".repeat(80))
                appendLine("EXCEPTION: $tag")
                appendLine("=".repeat(80))
                appendLine("Time: ${formatDate(Date())}")
                appendLine("Message: $message")
                appendLine("Exception: ${exception.javaClass.name}")
                appendLine("Exception Message: ${exception.message}")
                appendLine()
                appendLine("Stack Trace:")
                appendLine(getStackTrace(exception))
                appendLine("=".repeat(80))
                appendLine()
            }
            
            writeToFile(errorLogFile, exceptionInfo)
        } catch (e: Exception) {
            Log.e(TAG, "Exception log dosyaya yazılamadı: ${e.message}")
        }
    }
    
    /**
     * Dosyaya yaz
     */
    private fun writeToFile(file: File?, message: String) {
        if (file == null || !isInitialized) return
        
        lock.withLock {
            try {
                // Log dizini var mı kontrol et
                val parentDir = file.parentFile
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs()
                }
                
                // Dosya boyutunu kontrol et
                if (file.exists() && file.length() > MAX_LOG_SIZE) {
                    rotateLogFile(file)
                }
                
                FileWriter(file, true).use { writer ->
                    writer.append(message)
                    writer.append("\n")
                    writer.flush()
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "Dosyaya yazma izni yok: ${e.message}")
            } catch (e: java.io.IOException) {
                Log.e(TAG, "Dosyaya yazma hatası (IO): ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Dosyaya yazılamadı: ${e.message}", e)
            }
        }
    }
    
    /**
     * Log dosyasını rotate et (eski dosyayı yedekle)
     */
    private fun rotateLogFile(file: File) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val backupFile = File(file.parent, "${file.nameWithoutExtension}_$timestamp.${file.extension}")
            file.copyTo(backupFile, overwrite = true)
            file.delete()
            
            // Eski backup dosyalarını temizle
            cleanupOldLogs()
        } catch (e: Exception) {
            Log.e(TAG, "Log rotate edilemedi: ${e.message}")
        }
    }
    
    /**
     * Eski log dosyalarını temizle
     */
    private fun cleanupOldLogs() {
        try {
            val logFiles = logDir?.listFiles { _, name ->
                name.endsWith(".log") || name.matches(Regex(".*_\\d{8}_\\d{6}\\.log"))
            } ?: return
            
            // Tarihe göre sırala (en yeni önce)
            val sortedFiles = logFiles.sortedByDescending { it.lastModified() }
            
            // En fazla MAX_LOG_FILES dosya tut
            if (sortedFiles.size > MAX_LOG_FILES) {
                sortedFiles.drop(MAX_LOG_FILES).forEach { file ->
                    try {
                        file.delete()
                        Log.d(TAG, "Eski log dosyası silindi: ${file.name}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Log dosyası silinemedi: ${file.name}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Log temizleme hatası: ${e.message}")
        }
    }
    
    /**
     * Log formatla
     */
    private fun formatLog(level: String, tag: String, message: String, throwable: Throwable?): String {
        return buildString {
            append("[${formatDate(Date())}] ")
            append("[$level] ")
            append("[$tag] ")
            append(message)
            if (throwable != null) {
                append("\n")
                append(getStackTrace(throwable))
            }
        }
    }
    
    /**
     * Stack trace al
     */
    private fun getStackTrace(throwable: Throwable): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        return sw.toString()
    }
    
    /**
     * App version al
     */
    private fun getAppVersion(): String {
        return try {
            "1.0.0 (1)" // TODO: BuildConfig'den al
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    /**
     * Tüm log dosyalarının listesini al
     */
    fun getLogFiles(): List<File> {
        return logDir?.listFiles { _, name ->
            name.endsWith(".log")
        }?.toList() ?: emptyList()
    }
    
    /**
     * Log dosyasını oku
     */
    fun readLogFile(fileName: String): String? {
        return try {
            val file = File(logDir, fileName)
            if (file.exists()) {
                file.readText()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Log dosyası okunamadı: ${e.message}")
            null
        }
    }
    
    /**
     * Tüm log dosyalarını temizle
     */
    fun clearAllLogs() {
        try {
            logDir?.listFiles()?.forEach { file ->
                if (file.name.endsWith(".log")) {
                    file.delete()
                }
            }
            Log.d(TAG, "Tüm log dosyaları temizlendi")
        } catch (e: Exception) {
            Log.e(TAG, "Log temizleme hatası: ${e.message}")
        }
    }
    
    /**
     * Log dizinini al
     */
    fun getLogDirectory(): File? = logDir
    
    /**
     * AppOps hatası logla (SYSTEM_ALERT_WINDOW izni ile ilgili)
     */
    fun logAppOpsError(tag: String, message: String, uid: Int? = null, packageName: String? = null) {
        try {
            val logMessage = buildString {
                append("APPOPS ERROR: $message")
                if (uid != null) {
                    append(" | UID: $uid")
                }
                if (packageName != null) {
                    append(" | Package: $packageName")
                }
                append(" | Operation: SYSTEM_ALERT_WINDOW")
            }
            
            // Logcat'e yaz
            Log.e(tag, logMessage)
            if (uid != null || packageName != null) {
                Log.e(tag, "  Possible Cause: Overlay permission not granted or revoked")
                Log.e(tag, "  Solution: Check Settings -> Apps -> Special permissions -> Display over other apps")
            }
            
            // Dosyaya yaz
            val errorInfo = buildString {
                appendLine("=".repeat(80))
                appendLine("APPOPS ERROR: $tag")
                appendLine("=".repeat(80))
                appendLine("Time: ${formatDate(Date())}")
                appendLine("Message: $message")
                if (uid != null) {
                    appendLine("UID: $uid")
                }
                if (packageName != null) {
                    appendLine("Package: $packageName")
                }
                appendLine("Operation: SYSTEM_ALERT_WINDOW")
                appendLine("Possible Cause: Overlay permission not granted or revoked")
                appendLine("Solution: Check Settings -> Apps -> Special permissions -> Display over other apps")
                appendLine("=".repeat(80))
                appendLine()
            }
            writeToFile(errorLogFile, errorInfo)
        } catch (e: Exception) {
            Log.e(TAG, "AppOps error log yazılamadı: ${e.message}")
        }
    }
    
    /**
     * Overlay izni durumu logla
     */
    fun logOverlayPermissionStatus(tag: String, hasPermission: Boolean, context: String = "") {
        try {
            val status = if (hasPermission) "GRANTED" else "DENIED"
            val message = "Overlay permission $status${if (context.isNotEmpty()) " - $context" else ""}"
            
            // Logcat'e yaz
            if (!hasPermission) {
                Log.w(tag, message)
                Log.w(tag, "  Action Required: Grant overlay permission in Settings")
                Log.w(tag, "  Path: Settings -> Apps -> Special permissions -> Display over other apps")
            } else {
                Log.i(tag, message)
            }
            
            // Dosyaya yaz
            if (!hasPermission) {
                val errorInfo = buildString {
                    appendLine("[${formatDate(Date())}] [WARN] [$tag] $message")
                    appendLine("  Action Required: Grant overlay permission in Settings")
                    appendLine("  Path: Settings -> Apps -> Special permissions -> Display over other apps")
                    appendLine()
                }
                writeToFile(errorLogFile, errorInfo)
            } else {
                val logMessage = formatLog("INFO", tag, message, null)
                writeToFile(debugLogFile, logMessage)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Overlay permission log yazılamadı: ${e.message}")
        }
    }
    
    /**
     * Screen capture permission durumu logla
     */
    fun logScreenCapturePermission(tag: String, granted: Boolean, reason: String? = null) {
        try {
            val status = if (granted) "GRANTED" else "DENIED"
            val logMessage = "Screen capture permission $status${if (reason != null) " - $reason" else ""}"
            
            // Logcat'e yaz
            if (granted) {
                Log.i(tag, logMessage)
            } else {
                Log.e(tag, logMessage)
                Log.e(tag, "  Impact: Auto-aim and trajectory features will not work")
                Log.e(tag, "  Solution: User needs to grant screen capture permission when prompted")
            }
            
            // Dosyaya yaz
            val message = buildString {
                appendLine("=".repeat(80))
                appendLine("SCREEN CAPTURE PERMISSION: $status")
                appendLine("=".repeat(80))
                appendLine("Time: ${formatDate(Date())}")
                appendLine("Tag: $tag")
                if (reason != null) {
                    appendLine("Reason: $reason")
                }
                if (!granted) {
                    appendLine("Impact: Auto-aim and trajectory features will not work")
                    appendLine("Solution: User needs to grant screen capture permission when prompted")
                }
                appendLine("=".repeat(80))
                appendLine()
            }
            
            if (granted) {
                writeToFile(debugLogFile, message)
                val logMsg = formatLog("INFO", tag, logMessage, null)
                writeToFile(debugLogFile, logMsg)
            } else {
                writeToFile(errorLogFile, message)
                val logMsg = formatLog("ERROR", tag, logMessage, null)
                writeToFile(errorLogFile, logMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Screen capture permission log yazılamadı: ${e.message}")
        }
    }
    
    /**
     * Process başlama/bitme olayı logla
     */
    fun logProcessEvent(tag: String, event: String, pid: Int? = null, packageName: String? = null) {
        try {
            val logMessage = buildString {
                append("Process Event: $event")
                if (pid != null) {
                    append(" | PID: $pid")
                }
                if (packageName != null) {
                    append(" | Package: $packageName")
                }
            }
            
            // Logcat'e yaz
            Log.i(tag, logMessage)
            
            // Dosyaya yaz
            val message = buildString {
                appendLine("[${formatDate(Date())}] [INFO] [$tag] Process Event: $event")
                if (pid != null) {
                    appendLine("  PID: $pid")
                }
                if (packageName != null) {
                    appendLine("  Package: $packageName")
                }
                appendLine()
            }
            writeToFile(debugLogFile, message)
        } catch (e: Exception) {
            Log.e(TAG, "Process event log yazılamadı: ${e.message}")
        }
    }
    
    /**
     * WindowManager işlemi logla
     */
    fun logWindowManagerOperation(tag: String, operation: String, success: Boolean, details: String? = null) {
        try {
            val status = if (success) "SUCCESS" else "FAILED"
            val logMessage = buildString {
                append("WindowManager: $operation - $status")
                if (details != null) {
                    append(" | $details")
                }
            }
            
            // Logcat'e yaz
            if (success) {
                Log.i(tag, logMessage)
            } else {
                Log.e(tag, logMessage)
            }
            
            // Dosyaya yaz
            val message = buildString {
                appendLine("[${formatDate(Date())}] [${if (success) "INFO" else "ERROR"}] [$tag] WindowManager: $operation - $status")
                if (details != null) {
                    appendLine("  Details: $details")
                }
                appendLine()
            }
            
            if (success) {
                writeToFile(debugLogFile, message)
            } else {
                writeToFile(errorLogFile, message)
            }
        } catch (e: Exception) {
            Log.e(TAG, "WindowManager operation log yazılamadı: ${e.message}")
        }
    }
}

