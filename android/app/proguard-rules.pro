# ---- HisabBondhu R8 rules ----
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*, SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile

# Retrofit (suspend functions + generic responses)
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>

# Network DTOs are serialized by kotlinx.serialization — keep them intact.
-keep class com.hisabbondhu.app.data.remote.dto.** { *; }
-keepclassmembers class com.hisabbondhu.app.data.remote.dto.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}

# OkHttp optional platform classes
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
