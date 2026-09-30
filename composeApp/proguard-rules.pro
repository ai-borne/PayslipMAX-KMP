# Proguard / R8 Shrinking & Obfuscation Keep Rules for PayslipMax

# 1. Kotlin & Coroutines
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable
-keepclassmembers class * extends kotlin.coroutines.jvm.internal.ContinuationImpl {
    *** invokeSuspend(...);
}

# 2. Kotlinx Serialization
-keepattributes *Annotation*,ElementValuePairs
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keepclassmembers class * extends kotlinx.serialization.KSerializer {
    *** INSTANCE;
}
-keepclassmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# 2b. Navigation & Enum Obfuscation Safety (rememberSaveable Screen restoration)
-keepclassmembers enum com.payslipmax.pdfparser.Screen {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    **[] $VALUES;
}
-keep enum com.payslipmax.pdfparser.Screen { *; }
-keepclassmembers enum com.payslipmax.pcdao.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 3. Room Database & SQLite
-dontwarn androidx.room.paging.**

# 4. Ktor Client & Networking
-dontwarn io.ktor.**

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
