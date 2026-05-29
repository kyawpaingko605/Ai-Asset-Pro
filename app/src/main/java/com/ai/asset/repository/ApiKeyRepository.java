package com.ai.asset.repository;

import android.content.Context;
import android.content.SharedPreferences;

public class ApiKeyRepository {
    private static final String PREFS_NAME = "ai_asset_secure_prefs";
    private static final String KEY_GEMINI_API = "gemini_api_key";
    private final SharedPreferences sharedPreferences;
    
    public ApiKeyRepository(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    public void saveApiKey(String apiKey) {
        sharedPreferences.edit().putString(KEY_GEMINI_API, apiKey).apply();
    }
    
    public String getApiKey() {
        return sharedPreferences.getString(KEY_GEMINI_API, "");
    }
    
    public boolean hasValidApiKey() {
        String key = getApiKey();
        return key != null && key.startsWith("AIza") && key.length() > 20;
    }
    
    public void deleteApiKey() {
        sharedPreferences.edit().remove(KEY_GEMINI_API).apply();
    }
}
