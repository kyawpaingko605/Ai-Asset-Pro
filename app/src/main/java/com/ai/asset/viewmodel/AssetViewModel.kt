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
import java.io.InputStream

class AssetViewModel : ViewModel() {

    // စီးပွားရေးလုပ်ငန်းသုံးအတွက် ယုံကြည်စိတ်ချရဆုံး မော်ဒယ်လ်များသာ ထည့်သွင်းထားသည်
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

        val savedKey = apiKeyRepo.getApiKey().trim()
        _geminiApiKey.value = savedKey
        _hasValidApiKey.value = savedKey.length > 30 // API Key များသည် အများအားဖြင့် ရှည်လျားသည်

        _isDarkTheme.value = apiKeyRepo.getThemePreference()

        val savedModel = apiKeyRepo.getSelectedModel()
        if (availableModels.contains(savedModel)) {
            _currentModel.value = savedModel
        }

        viewModelScope.launch {
            historyRepo.loadChatHistory().collect { _chatMessages.value = it }
        }
    }

    fun saveApiKey(context: Context, key: String) {
        val cleanedKey = key.trim()
        apiKeyRepo.saveApiKey(cleanedKey)
        _geminiApiKey.value = cleanedKey
        _hasValidApiKey.value = cleanedKey.length > 30
    }

    fun sendMessage(context: Context, prompt: String, imageUri: Uri? = null) {
        if (!_hasValidApiKey.value) return

        val userMessage = ChatMessage(System.currentTimeMillis().toString(), prompt, true)
        _chatMessages.value += userMessage

        viewModelScope.launch {
            _isAiLoading.value = true
            try {
                val bitmap = imageUri?.let { uriToBitmap(context, it) }
                val response = callGemini(prompt, bitmap)
                val aiMessage = ChatMessage(System.currentTimeMillis().toString(), response, false)
                _chatMessages.value += aiMessage
                historyRepo.saveMessage(userMessage)
                historyRepo.saveMessage(aiMessage)
            } catch (e: Exception) {
                _chatMessages.value += ChatMessage(System.currentTimeMillis().toString(), "Error: ${e.localizedMessage}", false)
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    private suspend fun callGemini(prompt: String, bitmap: Bitmap?): String = withContext(Dispatchers.IO) {
        val model = GenerativeModel(_currentModel.value, _geminiApiKey.value)
        val response = if (bitmap != null) {
            model.generateContent(content { image(bitmap); text(prompt) })
        } else {
            model.generateContent(prompt)
        }
        response.text ?: "တုံ့ပြန်မှု မရှိပါ။"
    }

    private fun uriToBitmap(context: Context, uri: Uri): Bitmap? = try {
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
    } catch (e: Exception) { null }

    fun toggleTheme() { _isDarkTheme.value = !_isDarkTheme.value; apiKeyRepo.saveThemePreference(_isDarkTheme.value) }
    fun updateModel(model: String) { _currentModel.value = model; apiKeyRepo.saveSelectedModel(model) }
}
