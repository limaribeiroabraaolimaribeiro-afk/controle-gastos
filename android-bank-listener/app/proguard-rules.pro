# Supabase / Ktor
-keep class io.github.jan.supabase.** { *; }
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-dontwarn io.github.jan.supabase.**

# Kotlin Serialization
-keepattributes *Annotation*
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class ** {
    @kotlinx.serialization.Serializable *;
}

# App models
-keep class com.controlpro.banklistener.data.model.** { *; }

# Prevent R8 from stripping interface information
-keepattributes Signature
-keepattributes InnerClasses
