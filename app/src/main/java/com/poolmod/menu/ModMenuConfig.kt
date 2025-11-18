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

    // Delik pozisyon ayarları
    fun getHoleOffsetX(): Float {
        return prefs.getFloat("hole_offset_x", 0f)
    }

    fun setHoleOffsetX(offset: Float) {
        prefs.edit().putFloat("hole_offset_x", offset).apply()
    }

    fun getHoleOffsetY(): Float {
        return prefs.getFloat("hole_offset_y", 0f)
    }

    fun setHoleOffsetY(offset: Float) {
        prefs.edit().putFloat("hole_offset_y", offset).apply()
    }

    fun getHoleScale(): Float {
        return prefs.getFloat("hole_scale", 1f)
    }

    fun setHoleScale(scale: Float) {
        prefs.edit().putFloat("hole_scale", scale).apply()
    }

    companion object {
        private const val PREFS_NAME = "pool_mod_config"

        // Mod anahtarları
        const val MOD_AUTO_AIM = "auto_aim" // Otomatik nişan alma
        const val MOD_BALL_TRAJECTORY = "ball_trajectory" // Top yolu gösterimi
    }
}

