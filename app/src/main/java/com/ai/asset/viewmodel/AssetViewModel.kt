package com.ai.asset.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai.asset.model.ChatMessage
import com.ai.asset.repository.ApiKeyRepository
import com.ai.asset.repository.ChatHistoryRepository
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
        // Create user message
        val userMessage = ChatMessage(
            id = System.currentTimeMillis().toString(),
            text = message,
            isUser = true,
            timestamp = System.currentTimeMillis()
        )
        
        // Add to UI and save to database
        _chatMessages.value = _chatMessages.value + userMessage
        viewModelScope.launch {
            historyRepo.saveMessage(userMessage)
        }
        
        // Check if API key is valid
        if (!_hasValidApiKey.value) {
            val errorMessage = ChatMessage(
                id = System.currentTimeMillis().toString(),
                text = "⚠️ ကျေးဇူးပြု၍ သင့် Gemini API Key ကို Settings တွင် ထည့်သွင်းပါ။\n\nPlease enter your Gemini API Key in Settings.",
                isUser = false,
                timestamp = System.currentTimeMillis()
            )
            _chatMessages.value = _chatMessages.value + errorMessage
            return
        }
        
        _isAiLoading.value = true
        
        // Call Gemini API
        viewModelScope.launch {
            try {
                val response = callGeminiApi(message)
                
                val aiMessage = ChatMessage(
                    id = System.currentTimeMillis().toString(),
                    text = response,
                    isUser = false,
                    timestamp = System.currentTimeMillis()
                )
                
                _chatMessages.value = _chatMessages.value + aiMessage
                historyRepo.saveMessage(aiMessage)
                _isAiLoading.value = false
                
            } catch (e: Exception) {
                val errorMessage = ChatMessage(
                    id = System.currentTimeMillis().toString(),
                    text = "❌ Error: ${e.message}\n\nကျေးဇူးပြု၍ သင်၏ အင်တာနက်ချိတ်ဆက်မှုနှင့် API Key ကို စစ်ဆေးပါ။",
                    isUser = false,
                    timestamp = System.currentTimeMillis()
                )
                _chatMessages.value = _chatMessages.value + errorMessage
                _isAiLoading.value = false
            }
        }
    }
    
    // Real Gemini API Call - Supports Burmese/Myanmar Language
    private suspend fun callGeminiApi(prompt: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val apiKey = _geminiApiKey.value
                val modelName = _currentModel.value
                
                // Create Gemini model instance
                val generativeModel = GenerativeModel(
                    modelName = modelName,
                    apiKey = apiKey
                )
                
                // System instruction to support Burmese language
                val systemPrompt = """
                    You are a helpful AI assistant. You can understand and respond in Burmese (Myanmar language) fluently.
                    You should respond in the same language as the user's question.
                    If user asks in Burmese, respond in Burmese.
                    If user asks in English, respond in English.
                    Be helpful, accurate, and concise.
                """.trimIndent()
                
                // Send message with context
                val chat = generativeModel.startChat()
                val response = chat.sendMessage(prompt)
                
                response.text ?: "No response from AI. Please try again."
                
            } catch (e: Exception) {
                when {
                    e.message?.contains("403") == true -> "⚠️ API Key မမှန်ကန်ပါ။ ကျေးဇူးပြု၍ သင်၏ Gemini API Key ကို စစ်ဆေးပါ။"
                    e.message?.contains("429") == true -> "⚠️ Request အများကြီးပို့မိပါသည်။ ခဏစောင့်ပြီး ထပ်ကြိုးစားပါ။"
                    e.message?.contains("404") == true -> "⚠️ AI Model ကို မတွေ့ပါ။ အခြား Model ကို ရွေးချယ်ပါ။"
                    else -> "❌ Error: ${e.message}\n\nPlease check your internet connection and try again."
                }
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
