# Keep route names stable because Navigation 3 uses @Serializable route types.
-keepnames @kotlinx.serialization.Serializable class me.spica27.spicamusic.navigation.Screen$*
-keepclassmembers class me.spica27.spicamusic.navigation.Screen$* {
    static **$Companion Companion;
}
-keepclassmembers class me.spica27.spicamusic.navigation.Screen$*$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}

# Retrofit and Sandwich depend on generic signatures and runtime annotations.
-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keep,allowobfuscation,allowshrinking interface com.skydoves.sandwich.ApiResponse
