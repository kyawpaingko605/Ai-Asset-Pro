fun sendMessage(message: String) {
    val userMessage = ChatMessage(
        id = System.currentTimeMillis().toString(),
        text = message,
        isUser = true,
        timestamp = System.currentTimeMillis()
    )
    
    // ၁။ UI ပေါ်မှာ အသုံးပြုသူပို့တဲ့စာကို ချက်ချင်း အရင်ပြလိုက်မယ်
    _chatMessages.value = _chatMessages.value + userMessage
    
    // Database သိမ်းတာကို နောက်ကွယ်ကနေ သီးသန့်လုပ်ခိုင်းမယ် (UI ကို မပိတ်ဆို့တော့ဘူး)
    viewModelScope.launch(Dispatchers.IO) {
        try { historyRepo.saveMessage(userMessage) } catch (e: Exception) {}
    }
    
    // ၂။ Key ရှိမရှိ စစ်မယ်
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
    
    // ၃။ Loading ပြပြီး Gemini AI ဆီ တန်းခေါ်မယ်
    _isAiLoading.value = true
    
    viewModelScope.launch(Dispatchers.Main) {
        try {
            // AI ဆီက အဖြေကို တိုက်ရိုက်တောင်းမယ်
            val response = callGeminiApi(message)
            
            val aiMessage = ChatMessage(
                id = System.currentTimeMillis().toString(),
                text = response,
                isUser = false,
                timestamp = System.currentTimeMillis()
            )
            
            // AI အဖြေကို UI ပေါ် တင်ပေးမယ်
            _chatMessages.value = _chatMessages.value + aiMessage
            
            // AI အဖြေကို နောက်ကွယ်ကနေ Database ထဲ သိမ်းမယ်
            viewModelScope.launch(Dispatchers.IO) {
                try { historyRepo.saveMessage(aiMessage) } catch (e: Exception) {}
            }
            
        } catch (e: Exception) {
            val errorMessage = ChatMessage(
                id = System.currentTimeMillis().toString(),
                text = "❌ App Error: ${e.localizedMessage}",
                isUser = false,
                timestamp = System.currentTimeMillis()
            )
            _chatMessages.value = _chatMessages.value + errorMessage
        } finally {
            _isAiLoading.value = false
        }
    }
}
