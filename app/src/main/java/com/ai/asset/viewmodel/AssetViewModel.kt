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
    
    val availableModels = listOf(
        "models/gemini-1.5-flash",
        "models/gemini-1.5-pro"
    )
    
    private val _currentModel = MutableStateFlow(availableModels[0])
    val currentModel: StateFlow<String> = _currentModel
    
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages
    
    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading
    
    private val _geminiApiKey = MutableStateFlow("")
    val geminiApiKey: StateFlow<String> = _geminiApiKey
    
    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme
    
    private lateinit var apiKeyRepo: ApiKeyRepository
    private lateinit var historyRepo: ChatHistoryRepository
    
    fun initData(context: Context) {
        apiKeyRepo = ApiKeyRepository(context)
        historyRepo = ChatHistoryRepository(context)
        
        _geminiApiKey.value = apiKeyRepo.getApiKey().trim()
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
        val cleanedKey = key.trim()
        apiKeyRepo.saveApiKey(cleanedKey)
        _geminiApiKey.value = cleanedKey
    }
    
    fun sendMessage(message: String) {
        val userMessage = ChatMessage(
            id = System.currentTimeMillis().toString(),
            text = message,
            isUser = true,
            timestamp = System.currentTimeMillis()
        )
        
        // UI ပေါ် အသုံးပြုသူပို့လိုက်တဲ့စာကို ချက်ချင်းတင်ပေးမည်
        _chatMessages.value = _chatMessages.value + userMessage
        
        // Database သိမ်းတာကို နောက်ကွယ်ကနေ သီးသန့်လုပ်ခိုင်းမည် (UI ကို ပိတ်မဆို့တော့ပါ)
        viewModelScope.launch(Dispatchers.IO) {
            try { historyRepo.saveMessage(userMessage) } catch (e: Exception) {}
        }
        
        if (_geminiApiKey.value.isBlank()) {
            val noKeyMessage = ChatMessage(
                id = System.currentTimeMillis().toString(),
                text = "⚠️ API Key မထည့်ရသေးပါ။ ညာဘက်အပေါ်က သော့ပုံစံ Settings ထဲမှာ အရင်ထည့်ပေးပါဗျာ။",
                isUser = false,
                timestamp = System.currentTimeMillis()
            )
            _chatMessages.value = _chatMessages.value + noKeyMessage
            return
        }
        
        _isAiLoading.value = true
        
        viewModelScope.launch(Dispatchers.Main) {
            try {
                // Gemini AI ဆီ တိုက်ရိုက်တန်းခေါ်မည်
                val response = callGeminiApi(message)
                
                val aiMessage = ChatMessage(
                    id = System.currentTimeMillis().toString(),
                    text = response,
                    isUser = false,
                    timestamp = System.currentTimeMillis()
                )
                
                _chatMessages.value = _chatMessages.value + aiMessage
                
                viewModelScope.launch(Dispatchers.IO) {
                    try { historyRepo.saveMessage(aiMessage) } catch (e: Exception) {}
                }
            } catch (e: Exception) {
                val errorMessage = ChatMessage(
                    id = System.currentTimeMillis().toString(),
                    text = "❌ App Exception: ${e.localizedMessage}",
                    isUser = false,
                    timestamp = System.currentTimeMillis()
                )
                _chatMessages.value = _chatMessages.value + errorMessage
            } finally {
                _isAiLoading.value = false
            }
        }
    }
    
    private suspend fun callGeminiApi(prompt: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val generativeModel = GenerativeModel(
                    modelName = _currentModel.value,
                    apiKey = _geminiApiKey.value
                )
                val response = generativeModel.generateContent(prompt)
                response.text ?: "AI ထံမှ တုံ့ပြန်မှု ဗလာဖြစ်နေသည်။ နောက်တစ်ကြိမ် ကြိုးစားပါ။"
            } catch (e: Exception) {
                "⚠️ Gemini SDK Error: ${e.localizedMessage}\n\n💡 လမ်းညွှန်ချက်: Free Key သုံးစွဲမှု ပိတ်ပင်ခံရပါက VPN (USA သို့မဟုတ် Singapore) ကို ဖွင့်သုံးကြည့်ပါဗျာ။"
            }
        }
    }
    
    fun clearChatHistory() {
        _chatMessages.value = emptyList()
        viewModelScope.launch { historyRepo.clearAllMessages() }
    }
}
