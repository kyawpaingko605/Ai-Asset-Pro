package com.ai.asset.viewmodel

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AssetViewModel : ViewModel() {

    val availableModels = listOf("gemini-1.5-flash", "gemini-1.5-pro")

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
        _geminiApiKey.value = savedKey
        _hasValidApiKey.value = savedKey.length > 20
        _isDarkTheme.value = apiKeyRepo.getThemePreference()

        val savedModel = apiKeyRepo.getSelectedModel()
        if (availableModels.contains(savedModel)) _currentModel.value = savedModel

        viewModelScope.launch {
            historyRepo.loadChatHistory().collect { _chatMessages.value = it }
        }
    }

    fun saveApiKey(context: Context, key: String) {
        val cleaned = key.trim()
        apiKeyRepo.saveApiKey(cleaned)
        _geminiApiKey.value = cleaned
        _hasValidApiKey.value = cleaned.length > 20
    }

    fun sendMessage(context: Context, prompt: String, imageUri: Uri? = null) {
        if (!_hasValidApiKey.value) return

        val userMsg = ChatMessage(text = prompt, isUser = true, imageUri = imageUri?.toString())
        _chatMessages.value += userMsg

        viewModelScope.launch {
            _isAiLoading.value = true
            try {
                val bitmap = imageUri?.let { uriToBitmap(context, it) }
                val response = callGemini(prompt, bitmap)
                val aiMsg = ChatMessage(text = response, isUser = false)
                
                _chatMessages.value += aiMsg
                historyRepo.saveMessage(userMsg)
                historyRepo.saveMessage(aiMsg)
            } catch (e: Exception) {
                _chatMessages.value += ChatMessage(text = "Error: ${e.localizedMessage}", isUser = false)
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    private suspend fun callGemini(prompt: String, bitmap: Bitmap?): String = withContext(Dispatchers.IO) {
        val model = GenerativeModel(_currentModel.value, _geminiApiKey.value)
        val response = if (bitmap != null) {
            model.generateContent(content { image(bitmap); text(prompt.ifBlank { "Describe this" }) })
        } else {
            model.generateContent(prompt)
        }
        response.text ?: "တုံ့ပြန်မှု မရှိပါ။"
    }

    private fun uriToBitmap(context: Context, uri: Uri): Bitmap? = try {
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
    } catch (e: Exception) { null }

    fun toggleTheme() { 
        _isDarkTheme.value = !_isDarkTheme.value
        apiKeyRepo.saveThemePreference(_isDarkTheme.value) 
    }

    fun updateModel(model: String) { 
        _currentModel.value = model
        apiKeyRepo.saveSelectedModel(model) 
    }
}
