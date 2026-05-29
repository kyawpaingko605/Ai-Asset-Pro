package com.ai.asset.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

data class ChatMessage(
    val id: String = System.currentTimeMillis().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
) {
    val formattedTime: String
        get() = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
            .format(java.util.Date(timestamp))
}

class AssetViewModel : ViewModel() {
    
    val availableModels = listOf(
        "models/gemini-1.5-pro",
        "models/gemini-1.5-flash", 
        "models/gemini-pro"
    )
    
    private val _currentModel = MutableStateFlow(availableModels[0])
    val currentModel: StateFlow<String> = _currentModel
    
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages
    
    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading
    
    private val _hasValidApiKey = MutableStateFlow(false)
    val hasValidApiKey: StateFlow<Boolean> = _hasValidApiKey
    
    private val _geminiApiKey = MutableStateFlow("")
    val geminiApiKey: StateFlow<String> = _geminiApiKey
    
    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme
    
    private lateinit var prefs: SharedPreferences
    
    fun initData(context: Context) {
        prefs = context.getSharedPreferences("ai_asset_pro_prefs", Context.MODE_PRIVATE)
        
        val savedKey = prefs.getString("gemini_api_key", "") ?: ""
        if (savedKey.isNotEmpty()) {
            _geminiApiKey.value = savedKey
            _hasValidApiKey.value = savedKey.startsWith("AIza") && savedKey.length > 20
        }
        
        _isDarkTheme.value = prefs.getBoolean("dark_theme", false)
        
        // Load saved messages
        loadSavedMessages()
    }
    
    private fun loadSavedMessages() {
        // Simple: load from SharedPreferences
        val savedMessagesJson = prefs.getString("chat_history", "")
        if (savedMessagesJson.isNullOrEmpty()) return
        
        // Parse and restore (simplified)
    }
    
    private fun saveMessagesToPrefs() {
        // Simplified: save to SharedPreferences
    }
    
    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
        prefs.edit().putBoolean("dark_theme", _isDarkTheme.value).apply()
    }
    
    fun updateModel(model: String) {
        _currentModel.value = model
        prefs.edit().putString("selected_model", model).apply()
    }
    
    fun saveApiKey(context: Context, key: String) {
        prefs.edit().putString("gemini_api_key", key).apply()
        _geminiApiKey.value = key
        _hasValidApiKey.value = key.startsWith("AIza") && key.length > 20
    }
    
    fun sendMessage(message: String) {
        val userMessage = ChatMessage(
            id = System.currentTimeMillis().toString(),
            text = message,
            isUser = true
        )
        
        _chatMessages.value = _chatMessages.value + userMessage
        
        _isAiLoading.value = true
        
        CoroutineScope(Dispatchers.Main).launch {
            delay(1500)
            
            val aiMessage = ChatMessage(
                id = System.currentTimeMillis().toString(),
                text = "**🤖 Gemini AI Response**\n\nYou asked: \"$message\"\n\nModel: ${_currentModel.value.replace("models/", "")}\n\n*This is a demo response. Your API Key: ${_geminiApiKey.value.take(10)}...*",
                isUser = false
            )
            
            _chatMessages.value = _chatMessages.value + aiMessage
            _isAiLoading.value = false
        }
    }
    
    fun clearChatHistory() {
        _chatMessages.value = emptyList()
        prefs.edit().remove("chat_history").apply()
    }
}
