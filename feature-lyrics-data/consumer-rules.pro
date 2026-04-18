# Preserve the metadata Retrofit and Sandwich need for suspend APIs.
-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keep,allowobfuscation,allowshrinking interface com.skydoves.sandwich.ApiResponse

# Lyrics DTOs already opt in with @Keep; just preserve those classes and generated adapters.
-keep @androidx.annotation.Keep class me.spcia.lyric_core.entity.** { *; }
-keep class me.spcia.lyric_core.entity.**JsonAdapter { *; }
-keep @com.squareup.moshi.JsonQualifier interface *
