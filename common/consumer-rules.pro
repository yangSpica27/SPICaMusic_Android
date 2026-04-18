# Keep serial names stable for navigation arguments and saved state payloads.
-keepnames @kotlinx.serialization.Serializable class me.spica27.spicamusic.common.entity.**

# Keep serializer accessors and Parcelable creators while still allowing field/method shrinking.
-keepclassmembers class me.spica27.spicamusic.common.entity.** {
    static **$Companion Companion;
    public static final android.os.Parcelable$Creator CREATOR;
}
-keepclassmembers class me.spica27.spicamusic.common.entity.**$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}
