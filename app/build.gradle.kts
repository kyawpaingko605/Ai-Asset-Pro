plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp") version "1.9.20-1.0.14"
}

android {
    namespace = "com.ai.asset"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ai.asset"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true // ကုဒ်တွေကို Obfuscate လုပ်ပြီး Size သေးအောင်လုပ်ခြင်း
            isShrinkResources = true // မလိုအပ်တဲ့ resource တွေကို ဖယ်ရှားခြင်း
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
    
    buildFeatures {
        compose = true
    }
    
    composeOptions {
        // Kotlin 1.9.20 နဲ့ တွဲဖက်သုံးနိုင်ရန် Version အမှန်ဖြစ်ပါသည်
        kotlinCompilerExtensionVersion = "1.5.4"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.0")
    
    // Compose
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    
    // ViewModel + Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    
    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    
    // ✨ FIX: Gemini SDK ကို 0.2.0 မှ 0.9.0 သို့ အဆင့်မြှင့်တင်ထားပါသည်
    // ဒါမှသာ gemini-1.5-pro / flash တို့ကို အသုံးပြုနိုင်မှာ ဖြစ်ပါတယ်
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
    
    // Security
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Coil Library
    implementation("io.coil-kt:coil-compose:2.6.0")
}
