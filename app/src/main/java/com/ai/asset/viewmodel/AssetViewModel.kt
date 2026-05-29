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
                
                val aiMessage = ChatMessage(
                    id = System.currentTimeMillis().toString(),
                    text = "**🤖 Gemini AI Response**\n\nYou asked: \"$message\"\n\nModel: ${_currentModel.value.replace("models/", "")}\n\n*This is a pro-level response with markdown support.*",
                    isUser = false,
                    timestamp = System.currentTimeMillis()
                )
                
                _chatMessages.value = _chatMessages.value + aiMessage
                historyRepo.saveMessage(aiMessage)
                _isAiLoading.value = false
                
            } catch (e: Exception) {
                _isAiLoading.value = false
            }
        }
    }
    
    fun clearChatHistory() {
        _chatMessages.value = emptyList()
        viewModelScope.launch {
            historyRepo.clearAllMessages()
        }
    }
}
