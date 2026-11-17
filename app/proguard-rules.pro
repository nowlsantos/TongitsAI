# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# ONNX Runtime
-keep class ai.onnxruntime.** { *; }
-dontwarn ai.onnxruntime.**

# Koin Dependency Injection
-keep class org.koin.** { *; }
-dontwarn org.koin.**

# Kotlin Serialization
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keepclassmembers class * implements kotlinx.serialization.KSerializer {
    *;
}
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}

# TongitsAI Domain Models & Data
-keep class com.nowlsantos.tongitsai.domain.model.** { *; }
-keep class com.nowlsantos.tongitsai.data.** { *; }

# Preserve line number information for debugging stack traces
-keepattributes SourceFile,LineNumberTable
