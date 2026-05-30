# =====================================================================
# ✨ Premium AI Asset Pro - Production Ready ProGuard Rules
# =====================================================================

# 1. Google Gemini AI SDK အတွက် ကာကွယ်ရေး Rule များ
-keep class com.google.ai.client.generativeai.** { *; }
-keep interface com.google.ai.client.generativeai.** { *; }
-dontwarn com.google.ai.client.generativeai.**

# 2. Room Database & SQLite အတွက် ကာကွယ်ရေး Rule များ
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.Entity
-dontwarn androidx.room.**

# 3. Kotlin Coroutines & Serialization အတွက် Rule များ
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# 4. Coil (Image Loader) အတွက် Rule များ
-keep class io.coilkt.** { *; }
-dontwarn io.coilkt.**
