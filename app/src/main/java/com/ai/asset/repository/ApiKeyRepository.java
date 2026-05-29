package com.ai.asset.repository

import android.content.Context
import android.content.SharedPreferences

class ApiKeyRepository(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "ai_asset_pro_prefs", 
        Context.MODE_PRIVATE
    )
    
    fun saveApiKey(key: String) {
        prefs.edit().putString("gemini_api_key", key).apply()
    }
    
    fun getApiKey(): String {
        return prefs.getString("gemini_api_key", "") ?: ""
    }
    
    fun saveSelectedModel(model: String) {
        prefs.edit().putString("selected_model", model).apply()
    }
    
    fun getSelectedModel(): String {
        return prefs.getString("selected_model", "models/gemini-1.5-pro") ?: "models/gemini-1.5-pro"
    }
    
    fun saveThemePreference(isDark: Boolean) {
        prefs.edit().putBoolean("dark_theme", isDark).apply()
    }
    
    fun getThemePreference(): Boolean {
        return prefs.getBoolean("dark_theme", false)
    }
}
