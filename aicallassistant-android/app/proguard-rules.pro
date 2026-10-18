# Proguard rules for AiCallAssistant
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
