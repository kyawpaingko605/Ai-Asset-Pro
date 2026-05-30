package com.ai.asset.viewmodel // ✨ FIX: စာလုံးအသေးဖြင့်သာ သုံးရပါမည်

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
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
import java.io.InputStream

class AssetViewModel : ViewModel() {
    
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
            _hasValidApiKey.value = savedKey.length > 20
        }
        
        _isDarkTheme.value = apiKeyRepo.getThemePreference()
        
        val savedModel = apiKeyRepo.getSelectedModel()
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
        _hasValidApiKey.value = cleanedKey.length > 20
    }
    
    fun sendMessage(context: Context, message: String, imageUri: Uri? = null) {
        val displayMessage = if (imageUri != null && message.isBlank()) "[Sent an Image]" else message
        
        val userMessage = ChatMessage(
            id = System.currentTimeMillis().toString(),
            text = displayMessage,
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
                text = "⚠️ ကျေးဇူးပြု၍ သင့် Gemini API Key ကို Settings တွင် ထည့်သွင်းပါ။",
                isUser = false,
                timestamp = System.currentTimeMillis()
            )
            _chatMessages.value = _chatMessages.value + errorMessage
            return
        }
        
        _isAiLoading.value = true
        
        viewModelScope.launch {
            try {
                var bitmap: Bitmap? = null
                if (imageUri != null) {
                    bitmap = uriToBitmap(context, imageUri)
                }
                
                val response = callGeminiApi(message, bitmap)
                
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
                    text = "❌ Error: ${e.message}",
                    isUser = false,
                    timestamp = System.currentTimeMillis()
                )
                _chatMessages.value = _chatMessages.value + errorMessage
                _isAiLoading.value = false
            }
        }
    }
    
    private fun uriToBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            null
        }
    }
    
    private suspend fun callGeminiApi(prompt: String, bitmap: Bitmap?): String {
        return withContext(Dispatchers.IO) {
            try {
                // ✨ FIX: modelName ကို အမြဲတမ်း 'models/' prefix မပါအောင် သေချာအောင်လုပ်ထားသည်
                val modelName = _currentModel.value.replace("models/", "")
                val generativeModel = GenerativeModel(
                    modelName = modelName,
                    apiKey = _geminiApiKey.value
                )
                
                val response = if (bitmap != null) {
                    val inputContent = content {
                        image(bitmap)
                        text(prompt.ifBlank { "Describe this image in detail." })
                    }
                    generativeModel.generateContent(inputContent)
                } else {
                    generativeModel.generateContent(prompt)
                }
                
                response.text ?: "AI မှ တုံ့ပြန်မှု မရှိပါ။ ထပ်မံကြိုးစားကြည့်ပါ။"
                
            } catch (e: Exception) {
                when {
                    e.message?.contains("403") == true -> "⚠️ API Key မမှန်ကန်ပါ။"
                    e.message?.contains("429") == true -> "⚠️ Request အများကြီးပို့မိပါသည်။ ခဏစောင့်ပါ။"
                    else -> "❌ Error: ${e.message}"
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
