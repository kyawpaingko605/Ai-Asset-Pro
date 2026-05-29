package com.ai.asset.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai.asset.model.ChatMessage
import com.ai.asset.repository.ApiKeyRepository
import com.ai.asset.repository.ChatHistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AssetViewModel : ViewModel() {
    
    val availableModels = listOf(
        "models/gemini-1.5-pro",
        "models/gemini-1.5-flash", 
        "models/gemini-pro",
        "models/gemini-pro-vision"
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
    
    private lateinit var apiKeyRepo: ApiKeyRepository
    private lateinit var historyRepo: ChatHistoryRepository
    
    fun initData(context: Context) {
        apiKeyRepo = ApiKeyRepository(context)
        historyRepo = ChatHistoryRepository(context)
        
        val savedKey = apiKeyRepo.getApiKey()
        if (savedKey.isNotEmpty()) {
            _geminiApiKey.value = savedKey
            _hasValidApiKey.value = savedKey.startsWith("AIza") && savedKey.length > 20
        }
        
        _isDarkTheme.value = apiKeyRepo.getThemePreference()
        
        val savedModel = apiKeyRepo.getSelectedModel()
        if (savedModel.isNotEmpty() && availableModels.contains(savedModel)) {
            _currentModel.value = savedModel
        }
        
        viewModelScope.launch {
            historyRepo.loadChatHistory().collect { history ->
                _chatMessages.value = history
            }
        }
    }
    
    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
        apiKeyRepo.saveThemePreference(_isDarkTheme.value)
    }
    
    fun updateModel(model: String) {
        _currentModel.value = model
        apiKeyRepo.saveSelectedModel(model)
    }
    
    fun saveApiKey(context: Context, key: String) {
        apiKeyRepo.saveApiKey(key)
        _geminiApiKey.value = key
        _hasValidApiKey.value = key.startsWith("AIza") && key.length > 20
    }
    
    fun sendMessage(message: String) {
        val userMessage = ChatMessage(
            id = System.currentTimeMillis().toString(),
            text = message,
            isUser = true,
            timestamp = System.currentTimeMillis()
        )
        
        _chatMessages.value = _chatMessages.value + userMessage
        viewModelScope.launch {
            historyRepo.saveMessage(userMessage)
        }
        
        _isAiLoading.value = true
        
        viewModelScope.launch {
            try {
                kotlinx.coroutines.delay(1500)
                
                val responseText = buildResponse(message, _currentModel.value)
                
                val aiMessage = ChatMessage(
                    id = System.currentTimeMillis().toString(),
                    text = responseText,
                    isUser = false,
                    timestamp = System.currentTimeMillis()
                )
                
                _chatMessages.value = _chatMessages.value + aiMessage
                historyRepo.saveMessage(aiMessage)
                _isAiLoading.value = false
                
            } catch (e: Exception) {
                val errorMessage = ChatMessage(
                    id = System.currentTimeMillis().toString(),
                    text = "❌ Error: ${e.message}",
                    isUser = false,
                    timestamp = System.currentTimeMillis()
                )
                _chatMessages.value = _chatMessages.value + errorMessage
                _isAiLoading.value = false
            }
        }
    }
    
    private fun buildResponse(message: String, model: String): String {
        val modelName = model.replace("models/", "")
            .replace("gemini-", "Gemini ")
            .replace("-pro", " Pro")
            .replace("-flash", " Flash")
            .replace("-vision", " Vision")
        
        return """
**🤖 Gemini AI Response**

You asked: "$message"

**Model:** $modelName
**Status:** Demo mode

---

*This is a pro-level response. Connect to actual Gemini API for real AI responses.*

> 💡 Tip: You can add your Gemini API Key in Settings
        """.trimIndent()
    }
    
    fun clearChatHistory() {
        _chatMessages.value = emptyList()
        viewModelScope.launch {
            historyRepo.clearAllMessages()
        }
    }
}
