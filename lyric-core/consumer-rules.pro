# Lyric module consumer ProGuard rules
-keep class me.spica27.lyric_core.**.api.** { *; }

# Moshi
-keep class me.spica27.lyric_core.**.dto.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}
-keep class **JsonAdapter { *; }
-keep @com.squareup.moshi.JsonQualifier interface *

# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Sandwich
-keep class com.skydoves.sandwich.** { *; }
-keep,allowobfuscation,allowshrinking interface com.skydoves.sandwich.ApiResponse
