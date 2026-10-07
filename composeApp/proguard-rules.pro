# Proguard / R8 Shrinking & Obfuscation Keep Rules for PayslipMax

# 1. Kotlin & Coroutines
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable
-keepclassmembers class * extends kotlin.coroutines.jvm.internal.ContinuationImpl {
    *** invokeSuspend(...);
}

# 2. Kotlinx Serialization
-keepattributes *Annotation*,ElementValuePairs

# 3. Room Database & SQLite
-dontwarn androidx.room.paging.**

# 5. Koin Dependency Injection
-keepclassmembers class * {
    @org.koin.core.annotation.* <fields>;
    @org.koin.core.annotation.* <methods>;
}

# 6. LiteRT & JNI Native Methods
-keepclasseswithmembernames class * {
    native <methods>;
}
-keep class com.google.ai.edge.litertlm.** { *; }
-dontwarn com.google.ai.edge.litertlm.**

# 7. Compose Runtime & Activity
-dontwarn androidx.compose.ui.platform.**

# 8. Optional Transitive Dependencies (PdfBox, Play Core, SLF4J)
-dontwarn com.gemalto.jp2.**
-dontwarn com.google.android.gms.common.annotation.**
-dontwarn org.slf4j.impl.**

# 9. Firebase Crashlytics & Telemetry
-dontwarn com.google.firebase.crashlytics.**
