# R8 rules for Alpaca release builds.

# --- kotlinx.serialization: keep generated serializers for our models ---
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.alpaca.app.**$$serializer { *; }
-keepclassmembers class com.alpaca.app.** { *** Companion; }
-keepclasseswithmembers class com.alpaca.app.** { kotlinx.serialization.KSerializer serializer(...); }

# --- Room: entities/database referenced reflectively by generated code ---
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep @androidx.room.Entity class * { *; }

# --- OkHttp / Okio ---
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# --- Play Billing ---
-keep class com.android.vending.billing.** { *; }
-keep class com.android.billingclient.** { *; }

# --- Gemini Live client uses WebSocket + reflection-free streams; nothing extra needed. ---
