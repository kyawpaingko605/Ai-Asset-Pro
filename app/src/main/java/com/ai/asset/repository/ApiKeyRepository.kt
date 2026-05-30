package com.ai.asset.repository

import android.content.Context
import android.content.SharedPreferences

class ApiKeyRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "ai_asset_pro_prefs", 
        Context.MODE_PRIVATE
    )
    
    // API Key ကို ပိုပြီးလုံခြုံအောင် သိမ်းဆည်းရန် ကြိုးစားခြင်း
    fun saveApiKey(key: String) {
        prefs.edit().putString("gemini_api_key", key.trim()).apply()
    }
    
    fun getApiKey(): String {
        return prefs.getString("gemini_api_key", "") ?: ""
    }
    
    // ✨ FIX: "AIza" ဆိုတဲ့ စာသားနဲ့ မစစ်ဆေးတော့ပါ (Key အမျိုးအစားသစ်တွေအတွက်ပါ)
    fun hasValidApiKey(): Boolean {
        val key = getApiKey().trim()
        return key.length > 20 
    }
    
    fun saveSelectedModel(model: String) {
        // ✨ FIX: 'models/' ပါနေရင် ဖယ်ထုတ်ပြီးမှ သိမ်းပါ
        val cleanModel = model.replace("models/", "")
        prefs.edit().putString("selected_model", cleanModel).apply()
    }
    
    fun getSelectedModel(): String {
        // ✨ FIX: default ကို 'models/' မပါဘဲ သိမ်းပါ
        return prefs.getString("selected_model", "gemini-1.5-flash") ?: "gemini-1.5-flash"
    }
    
    fun saveThemePreference(isDark: Boolean) {
        prefs.edit().putBoolean("dark_theme", isDark).apply()
    }
    
    fun getThemePreference(): Boolean {
        return prefs.getBoolean("dark_theme", false)
    }
}
