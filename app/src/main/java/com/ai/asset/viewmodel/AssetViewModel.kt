package com.ai.asset.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai.asset.model.ChatMessage
import com.ai.asset.repository.ApiKeyRepository
import com.ai.asset.repository.ChatHistoryRepository
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AssetViewModel : ViewModel() {
    
    // ✨ FIX: models/ ကို ဖြုတ်ပြီး Gemini SDK သိမယ့် Model Name အမှန်တွေ ပြောင်းထားပါတယ်
    val availableModels = listOf(
        "gemini-1.5-flash",
        "gemini-1.5-pro", 
        "gemini-pro"
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
        
        val savedKey = apiKeyRepo.getApiKey().trim()
        if (savedKey.isNotEmpty()) {
            _geminiApiKey.value = savedKey
            _hasValidApiKey.value = savedKey.length > 20 && savedKey.startsWith("AIza")
        }
        
        _isDarkTheme.value = apiKeyRepo.getThemePreference()
        
        val savedModel = apiKeyRepo.getSelectedModel()
        // ✨ FIX: သိမ်းထားတဲ့ model အဟောင်းမှာ models/ ပါနေရင် ဖယ်ထုတ်ပစ်ဖို့ စစ်ဆေးချက်ထည့်ထားပါတယ်
        val cleanedModel = savedModel.replace("models/", "")
        if (cleanedModel.isNotEmpty() && availableModels.contains(cleanedModel)) {
            _currentModel.value = cleanedModel
        } else {
            _currentModel.value = availableModels[0]
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
        val cleanedModel = model.replace("models/", "")
        _currentModel.value = cleanedModel
        apiKeyRepo.saveSelectedModel(cleanedModel)
    }
    
    fun saveApiKey(context: Context, key: String) {
        val cleanedKey = key.trim()
        apiKeyRepo.saveApiKey(cleanedKey)
        _geminiApiKey.value = cleanedKey
        _hasValidApiKey.value = cleanedKey.length > 20 && cleanedKey.startsWith("AIza")
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
    
    private suspend fun callGeminiApi(prompt: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val apiKey = _geminiApiKey.value
                val modelName = _currentModel.value
                
                val generativeModel = GenerativeModel(
                    modelName = modelName,
                    apiKey = apiKey
                )
                
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
