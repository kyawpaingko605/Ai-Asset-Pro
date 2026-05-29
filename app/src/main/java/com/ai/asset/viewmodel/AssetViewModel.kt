package com.ai.asset.viewmodel

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class ChatMessage(
    val text: String, 
    val isUser: Boolean, 
    val timestamp: Long = System.currentTimeMillis()
)

class AssetViewModel : ViewModel() {
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages
    
    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading
    
    private val _isGoogleLoggedIn = MutableStateFlow(false)
    val isGoogleLoggedIn: StateFlow<Boolean> = _isGoogleLoggedIn
    
    private val _hasValidApiKey = MutableStateFlow(false)
    val hasValidApiKey: StateFlow<Boolean> = _hasValidApiKey
    
    private val _geminiApiKey = MutableStateFlow("")
    val geminiApiKey: StateFlow<String> = _geminiApiKey
    
    private val _googleUserName = MutableStateFlow("")
    val googleUserName: StateFlow<String> = _googleUserName
    
    fun initData(context: Context) {
        // Load saved data if needed
    }
    
    fun loginWithGoogle(context: Context) {
        // Simulate Google Login
        _isGoogleLoggedIn.value = true
        _googleUserName.value = "Test User"
    }
    
    fun logoutGoogle(context: Context) {
        _isGoogleLoggedIn.value = false
        _googleUserName.value = ""
        _geminiApiKey.value = ""
        _hasValidApiKey.value = false
        _chatMessages.value = emptyList()
    }
    
    fun saveApiKey(context: Context, key: String) {
        _geminiApiKey.value = key
        _hasValidApiKey.value = key.isNotEmpty() && key.startsWith("AIza") && key.length > 20
    }
    
    fun sendMessage(context: Context, message: String) {
        // Add user message
        val userMessage = ChatMessage(message, true)
        _chatMessages.value = _chatMessages.value + userMessage
        
        // Start AI loading
        _isAiLoading.value = true
        
        // Simulate AI response (replace with actual Gemini API call)
        Handler(Looper.getMainLooper()).postDelayed({
            val aiResponse = if (_geminiApiKey.value.isNotEmpty()) {
                "This is a response from Gemini AI using your API key: ${_geminiApiKey.value.take(10)}...\n\nYour message was: \"$message\""
            } else {
                "Please enter a valid Gemini API Key in settings to get real AI responses."
            }
            val aiMessage = ChatMessage(aiResponse, false)
            _chatMessages.value = _chatMessages.value + aiMessage
            _isAiLoading.value = false
        }, 1500)
    }
}
