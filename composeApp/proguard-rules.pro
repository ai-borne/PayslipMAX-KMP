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

# 10. Claim Guide bundle models (docs/Plan/rule_cards/16_guide_phase_plan.md). Narrow on purpose: only the
# generated serializers of the guide model package, renaming allowed. scripts/check_r8_guide.py fails the
# release gate if any model or serializer is removed.
-keep,allowobfuscation class com.payslipmax.pdfparser.guide.model.*$$serializer { *; }
-keep,allowobfuscation class com.payslipmax.pdfparser.guide.model.*$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}
