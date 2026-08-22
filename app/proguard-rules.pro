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

# Media3 Proguard rules
-keepclassmembers class androidx.media3.session.MediaSession {
    androidx.media3.session.MediaSessionImpl impl;
}
-keepclassmembers class androidx.media3.session.MediaSessionImpl {
    androidx.media3.session.MediaSessionLegacyStub sessionLegacyStub;
    androidx.media3.session.PlayerWrapper getPlayerWrapper();
}
-keepclassmembers class androidx.media3.session.MediaSessionLegacyStub {
    void setAvailableCommands(androidx.media3.session.SessionCommands, androidx.media3.common.Player$Commands);
    void setPlatformCustomLayout(com.google.common.collect.ImmutableList);
    void updateLegacySessionPlaybackStateAndQueue(androidx.media3.session.PlayerWrapper);
}
-keep interface androidx.media3.** { *; }
-dontwarn androidx.media3.**
-keepclassmembers class * extends androidx.media3.session.MediaLibraryService {
    <methods>;
}
-keepclassmembers class * extends androidx.media3.session.MediaSession$Callback {
    <methods>;
}

