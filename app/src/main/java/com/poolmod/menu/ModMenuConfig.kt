package com.poolmod.menu

import android.content.Context
import android.content.SharedPreferences

class ModMenuConfig(context: Context) {

    private val prefs: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isModEnabled(modKey: String): Boolean {
        return prefs.getBoolean(modKey, false)
    }

    fun setModEnabled(modKey: String, enabled: Boolean) {
        prefs.edit().putBoolean(modKey, enabled).apply()
    }

    fun getModValue(modKey: String, defaultValue: Float): Float {
        return prefs.getFloat(modKey, defaultValue)
    }

    fun setModValue(modKey: String, value: Float) {
        prefs.edit().putFloat(modKey, value).apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "pool_mod_config"

        // Mod anahtarları
        const val MOD_AUTO_AIM = "auto_aim" // Otomatik nişan alma
        const val MOD_BALL_TRAJECTORY = "ball_trajectory" // Top yolu gösterimi
    }
}

