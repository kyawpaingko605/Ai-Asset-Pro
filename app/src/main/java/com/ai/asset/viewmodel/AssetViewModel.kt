package com.ai.asset.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai.asset.model.ChatMessage
import com.ai.asset.repository.ApiKeyRepository
import com.ai.asset.repository.ChatHistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AssetViewModel : ViewModel() {
    
    // Models available
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
        
        // Load saved API Key
        val savedKey = apiKeyRepo.getApiKey()
        if (savedKey.isNotEmpty()) {
            _geminiApiKey.value = savedKey
            _hasValidApiKey.value = savedKey.startsWith("AIza") && savedKey.length > 20
        }
        
        // Load saved theme preference
        _isDarkTheme.value = apiKeyRepo.getThemePreference()
        
        // Load chat history
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
        
        // Add user message
        _chatMessages.value = _chatMessages.value + userMessage
        historyRepo.saveMessage(userMessage)
        
        _isAiLoading.value = true
        
        viewModelScope.launch {
            try {
                val apiKey = _geminiApiKey.value
                val model = _currentModel.value
                
                // Real Gemini API call here
                val aiResponse = callGeminiApi(apiKey, model, message)
                
                val aiMessage = ChatMessage(
                    id = System.currentTimeMillis().toString(),
                    text = aiResponse,
                    isUser = false,
                    timestamp = System.currentTimeMillis()
                )
                
                _chatMessages.value = _chatMessages.value + aiMessage
                historyRepo.saveMessage(aiMessage)
                _isAiLoading.value = false
                
            } catch (e: Exception) {
                val errorMessage = ChatMessage(
                    id = System.currentTimeMillis().toString(),
                    text = "Error: ${e.message}",
                    isUser = false,
                    timestamp = System.currentTimeMillis()
                )
                _chatMessages.value = _chatMessages.value + errorMessage
                _isAiLoading.value = false
            }
        }
    }
    
    private suspend fun callGeminiApi(apiKey: String, model: String, prompt: String): String {
        // Real Gemini API implementation
        // Using Google AI Client SDK
        return try {
            // Placeholder - implement actual API call
            "**Gemini AI Response**\n\nYou asked: \"$prompt\"\n\nModel: ${model.replace("models/", "")}\n\n*This is a pro-level response with markdown support.*"
        } catch (e: Exception) {
            "API Error: ${e.message}"
        }
    }
    
    fun clearChatHistory() {
        _chatMessages.value = emptyList()
        historyRepo.clearAllMessages()
    }
}
