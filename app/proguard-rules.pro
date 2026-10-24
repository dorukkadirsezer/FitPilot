# ==============================================================================
# FitPilot Beta — ProGuard / R8 Security & Serialization Rules (M3)
# ==============================================================================

-keepattributes *Annotation*,InnerClasses,EnclosingMethod,Signature,Exceptions

# 1. Strip debug logs and raw stack traces in release builds
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
-assumenosideeffects class java.lang.Throwable {
    public void printStackTrace();
}

# 2. Kotlinx Serialization (@Serializable DTOs, Companion & $serializer)
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep all @Serializable classes, their Companion objects, and generated $serializer classes
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
    <fields>;
    <init>(...);
}

-if @kotlinx.serialization.Serializable class **
-keepnames class <1>$Companion

-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1>$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}

-if @kotlinx.serialization.Serializable class **
-keep class <1>$$serializer {
    *;
}

# Explicitly preserve FitPilot Domain & AI Serialization DTOs
-keep class com.doruk.fitprogress.core.domain.model.** { *; }
-keep class com.doruk.fitprogress.core.ai.** { *; }

# 3. Room Database Entities, DAOs, and Relation/Embedded DTOs
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep class com.doruk.fitprogress.core.database.entity.** { *; }
-keep class com.doruk.fitprogress.core.database.dao.** { *; }

# 4. Google Generative AI SDK, Ktor Client, OkHttp, and Google Tink
-keep class com.google.ai.client.generativeai.** { *; }
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn io.ktor.**
-dontwarn kotlinx.coroutines.debug.**
-dontwarn org.slf4j.**
