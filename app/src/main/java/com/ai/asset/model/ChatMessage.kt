package com.ai.asset.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ChatMessage(
    // id ကို မလိုအပ်ရင် အပြင်ကပေးစရာမလိုအောင် Default တန်ဖိုးပေးထားပါတယ်
    val id: String = System.currentTimeMillis().toString(),
    
    // မက်ဆေ့ချ်စာသား
    val text: String,
    
    // User ပေးပို့တာလား၊ AI ပေးပို့တာလား
    val isUser: Boolean,
    
    // ပုံလမ်းကြောင်း (အရောင်း Project အတွက် အရေးကြီးပါတယ်)
    val imageUri: String? = null,
    
    // အချိန်
    val timestamp: Long = System.currentTimeMillis()
) {
    // UI မှာ ပြသဖို့အတွက် အချိန်ကို စနစ်တကျဖော်ပြခြင်း
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
}
