plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp") version "1.9.20-1.0.14"  // ✅ Room အတွက်
}

android {
    // ... အကုန်မှန်ပါတယ်
}

dependencies {
    // ... အကုန်မှန်ပါတယ်
    
    // Gemini AI ✅
    implementation("com.google.ai.client.generativeai:generativeai:0.2.0")
    
    // Room Database ✅
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
}
