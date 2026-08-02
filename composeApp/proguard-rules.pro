# Proguard / R8 Shrinking & Obfuscation Keep Rules for PayslipMax

# 1. Kotlin & Coroutines
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable
-keepclassmembers class * extends kotlin.coroutines.jvm.internal.ContinuationImpl {
    *** invokeSuspend(...);
}

# 2. Kotlinx Serialization
-keepattributes *Annotation*,ElementValuePairs
-keepclassmembers class * implements kotlinx.serialization.KSerializer {
    *** INSTANCE;
}
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
    @kotlinx.serialization.Serializable <fields>;
}
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# 3. Room Database & SQLite
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers class * {
    @androidx.room.* <fields>;
    @androidx.room.* <methods>;
}
-dontwarn androidx.room.paging.**

# 4. Ktor Client & Networking
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# 5. Koin Dependency Injection
-keep class * implements org.koin.core.module.Module
-keepclassmembers class * {
    @org.koin.core.annotation.* <fields>;
    @org.koin.core.annotation.* <methods>;
}

# 6. LiteRT & JNI Native Methods
-keepclasseswithmembernames class * {
    native <methods>;
}
-keep class com.google.ai.edge.litert.** { *; }
-dontwarn com.google.ai.edge.litert.**

# 7. Compose Runtime & Activity
-keep class androidx.compose.ui.platform.** { *; }
-dontwarn androidx.compose.ui.platform.**

# 8. Optional Transitive Dependencies (PdfBox, Play Core, SLF4J)
-dontwarn com.gemalto.jp2.**
-dontwarn com.google.android.gms.common.annotation.**
-dontwarn org.slf4j.impl.**
